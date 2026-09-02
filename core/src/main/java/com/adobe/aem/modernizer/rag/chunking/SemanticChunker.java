package com.adobe.aem.modernizer.rag.chunking;

import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.KnowledgeDocument;
import com.adobe.aem.modernizer.rag.model.KnowledgeMetadata;
import com.adobe.aem.modernizer.rag.util.FingerprintUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiered semantic chunker providing document-type-aware chunking:
 * - Markdown: splits on #, ##, ### headings with breadcrumb preservation.
 * - EDS Blocks: extracts block metadata, decorator JS logic, variant handlers, and CSS.
 * - JSON: splits by top-level or model definitions (e.g. component-models.json).
 * - YAML: splits by major configuration blocks.
 */
public class SemanticChunker {

    private static final Logger LOG = LoggerFactory.getLogger(SemanticChunker.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_CHUNK_TOKENS = 800;
    private static final int MIN_CHUNK_TOKENS = 20;

    private static final Pattern MD_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern JS_FUNCTION = Pattern.compile("(?:export\\s+default\\s+function|function|const\\s+\\w+\\s*=\\s*function|decorate\\s*\\()\\s*([a-zA-Z0-9_$]*)");

    public List<KnowledgeChunk> chunk(KnowledgeDocument doc) {
        if (doc == null || doc.getContent() == null || doc.getContent().isBlank()) {
            return Collections.emptyList();
        }

        String type = doc.getDocumentType() != null ? doc.getDocumentType().toUpperCase(Locale.ROOT) : "GENERAL";
        switch (type) {
            case "MARKDOWN":
                return chunkMarkdown(doc);
            case "EDS_BLOCK":
            case "BLOCK_JS":
            case "BLOCK_CSS":
                return chunkEdsBlock(doc);
            case "EDS_MODEL":
            case "COMPONENT_DEFINITION":
            case "CONFIG":
                if (doc.getPath() != null && (doc.getPath().endsWith(".json"))) {
                    return chunkJson(doc);
                } else if (doc.getPath() != null && (doc.getPath().endsWith(".yaml") || doc.getPath().endsWith(".yml"))) {
                    return chunkYaml(doc);
                }
                return chunkLineBased(doc, 40);
            case "SCRIPT":
                return chunkScript(doc);
            default:
                if (doc.getPath() != null && doc.getPath().endsWith(".md")) {
                    return chunkMarkdown(doc);
                } else if (doc.getPath() != null && (doc.getPath().endsWith(".js") || doc.getPath().endsWith(".ts"))) {
                    return chunkScript(doc);
                } else if (doc.getPath() != null && doc.getPath().endsWith(".css")) {
                    return chunkCss(doc, extractBlockName(doc.getPath()));
                } else if (doc.getPath() != null && doc.getPath().endsWith(".json")) {
                    return chunkJson(doc);
                }
                return chunkLineBased(doc, 50);
        }
    }

    private List<KnowledgeChunk> chunkMarkdown(KnowledgeDocument doc) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String[] lines = doc.getContent().split("\n");
        StringBuilder currentContent = new StringBuilder();
        String currentHeading = doc.getTitle() != null ? doc.getTitle() : "Overview";
        List<String> headingHierarchy = new ArrayList<>();
        int startLine = 1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = MD_HEADING.matcher(line);
            if (matcher.matches()) {
                if (currentContent.length() > 0 && !currentContent.toString().trim().isEmpty()) {
                    chunks.add(buildChunk(doc, currentContent.toString().trim(), currentHeading, String.join(" > ", headingHierarchy),
                            "MARKDOWN_SECTION", startLine, i));
                    currentContent.setLength(0);
                    startLine = i + 1;
                }

                int level = matcher.group(1).length();
                String headingText = matcher.group(2).trim();
                currentHeading = headingText;

                while (headingHierarchy.size() >= level) {
                    headingHierarchy.remove(headingHierarchy.size() - 1);
                }
                headingHierarchy.add(headingText);
            }
            currentContent.append(line).append('\n');

            if (estimateTokens(currentContent.toString()) >= MAX_CHUNK_TOKENS) {
                chunks.add(buildChunk(doc, currentContent.toString().trim(), currentHeading, String.join(" > ", headingHierarchy),
                        "MARKDOWN_SECTION", startLine, i + 1));
                currentContent.setLength(0);
                startLine = i + 2;
            }
        }

        if (currentContent.length() > 0 && !currentContent.toString().trim().isEmpty()) {
            chunks.add(buildChunk(doc, currentContent.toString().trim(), currentHeading, String.join(" > ", headingHierarchy),
                    "MARKDOWN_SECTION", startLine, lines.length));
        }

        if (chunks.isEmpty() && currentContent.length() > 0) {
            chunks.add(buildChunk(doc, currentContent.toString().trim(), currentHeading, currentHeading,
                    "MARKDOWN_SECTION", 1, lines.length));
        }

        return chunks;
    }

    private List<KnowledgeChunk> chunkEdsBlock(KnowledgeDocument doc) {
        String path = doc.getPath() != null ? doc.getPath() : "";
        String blockName = extractBlockName(path);

        if (path.endsWith(".js") || path.endsWith(".ts")) {
            return chunkScript(doc);
        } else if (path.endsWith(".css")) {
            return chunkCss(doc, blockName);
        } else if (path.endsWith(".md")) {
            return chunkMarkdown(doc);
        }
        return chunkLineBased(doc, 40);
    }

    private List<KnowledgeChunk> chunkScript(KnowledgeDocument doc) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String[] lines = doc.getContent().split("\n");
        StringBuilder blockBuffer = new StringBuilder();
        String currentSection = "Module Header";
        int startLine = 1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = JS_FUNCTION.matcher(line);
            if (matcher.find()) {
                if (blockBuffer.length() > 0 && estimateTokens(blockBuffer.toString()) >= MIN_CHUNK_TOKENS) {
                    chunks.add(buildChunk(doc, blockBuffer.toString().trim(), currentSection, currentSection,
                            "EDS_JS_DECORATOR", startLine, i));
                    blockBuffer.setLength(0);
                    startLine = i + 1;
                }
                String fnName = matcher.group(1);
                currentSection = (fnName != null && !fnName.isBlank()) ? fnName : "decorate()";
            }
            blockBuffer.append(line).append('\n');

            if (estimateTokens(blockBuffer.toString()) >= MAX_CHUNK_TOKENS) {
                chunks.add(buildChunk(doc, blockBuffer.toString().trim(), currentSection, currentSection,
                        "EDS_JS_DECORATOR", startLine, i + 1));
                blockBuffer.setLength(0);
                startLine = i + 2;
            }
        }

        if (blockBuffer.length() > 0) {
            chunks.add(buildChunk(doc, blockBuffer.toString().trim(), currentSection, currentSection,
                    "EDS_JS_DECORATOR", startLine, lines.length));
        }

        return chunks;
    }

    private List<KnowledgeChunk> chunkCss(KnowledgeDocument doc, String blockName) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String[] rules = doc.getContent().split("(?<=\\})\\s*");
        StringBuilder currentChunk = new StringBuilder();
        int lineCounter = 1;
        int chunkStartLine = 1;

        for (String rule : rules) {
            int linesInRule = rule.split("\n").length;
            currentChunk.append(rule).append("\n\n");
            lineCounter += linesInRule;

            if (estimateTokens(currentChunk.toString()) >= 200) {
                chunks.add(buildChunk(doc, currentChunk.toString().trim(), blockName + " CSS", blockName,
                        "EDS_CSS_RULES", chunkStartLine, lineCounter));
                currentChunk.setLength(0);
                chunkStartLine = lineCounter + 1;
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(buildChunk(doc, currentChunk.toString().trim(), blockName + " CSS", blockName,
                    "EDS_CSS_RULES", chunkStartLine, lineCounter));
        }

        return chunks;
    }

    private List<KnowledgeChunk> chunkJson(KnowledgeDocument doc) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(doc.getContent());
            if (root.isArray()) {
                int index = 0;
                for (JsonNode item : root) {
                    String title = item.has("id") ? item.get("id").asText() :
                            (item.has("name") ? item.get("name").asText() : "Item " + index);
                    String itemJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(item);
                    chunks.add(buildChunk(doc, itemJson, title, doc.getTitle(), "EDS_MODEL_JSON", 1, 1));
                    index++;
                }
                return chunks;
            } else if (root.isObject()) {
                if (root.has("definitions") && root.get("definitions").isArray()) {
                    for (JsonNode item : root.get("definitions")) {
                        String title = item.has("title") ? item.get("title").asText() :
                                (item.has("id") ? item.get("id").asText() : "Definition");
                        String itemJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(item);
                        chunks.add(buildChunk(doc, itemJson, title, doc.getTitle(), "EDS_MODEL_JSON", 1, 1));
                    }
                    return chunks;
                } else if (root.has("models") && root.get("models").isArray()) {
                    for (JsonNode item : root.get("models")) {
                        String title = item.has("title") ? item.get("title").asText() :
                                (item.has("id") ? item.get("id").asText() : "Model");
                        String itemJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(item);
                        chunks.add(buildChunk(doc, itemJson, title, doc.getTitle(), "EDS_MODEL_JSON", 1, 1));
                    }
                    return chunks;
                }

                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String key = entry.getKey();
                    JsonNode val = entry.getValue();
                    String nodeJson = key + ": " + MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(val);
                    chunks.add(buildChunk(doc, nodeJson, key, doc.getTitle(), "EDS_MODEL_JSON", 1, 1));
                }
                if (!chunks.isEmpty()) {
                    return chunks;
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse JSON for semantic chunking, falling back to line chunking: {}", doc.getPath());
        }
        return chunkLineBased(doc, 40);
    }

    private List<KnowledgeChunk> chunkYaml(KnowledgeDocument doc) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String[] lines = doc.getContent().split("\n");
        StringBuilder blockBuffer = new StringBuilder();
        String currentSection = doc.getTitle() != null ? doc.getTitle() : "Configuration";
        int startLine = 1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() > 0 && !Character.isWhitespace(line.charAt(0)) && line.contains(":")) {
                if (blockBuffer.length() > 0 && estimateTokens(blockBuffer.toString()) >= MIN_CHUNK_TOKENS) {
                    chunks.add(buildChunk(doc, blockBuffer.toString(), currentSection, currentSection,
                            "YAML_CONFIG", startLine, i));
                    blockBuffer.setLength(0);
                    startLine = i + 1;
                }
                currentSection = line.substring(0, line.indexOf(':')).trim();
            }
            blockBuffer.append(line).append('\n');
        }

        if (blockBuffer.length() > 0) {
            chunks.add(buildChunk(doc, blockBuffer.toString(), currentSection, currentSection,
                    "YAML_CONFIG", startLine, lines.length));
        }

        return chunks;
    }

    private List<KnowledgeChunk> chunkLineBased(KnowledgeDocument doc, int windowSize) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String[] lines = doc.getContent().split("\n");
        StringBuilder buffer = new StringBuilder();
        int startLine = 1;

        for (int i = 0; i < lines.length; i++) {
            buffer.append(lines[i]).append('\n');
            if ((i + 1) % windowSize == 0 || i == lines.length - 1) {
                if (buffer.length() > 0) {
                    chunks.add(buildChunk(doc, buffer.toString(), doc.getTitle() + " (Part " + (chunks.size() + 1) + ")",
                            doc.getTitle(), "GENERAL", startLine, i + 1));
                    buffer.setLength(0);
                    startLine = i + 2;
                }
            }
        }
        return chunks;
    }

    private KnowledgeChunk buildChunk(KnowledgeDocument doc, String content, String heading, String section,
                                       String chunkType, int startLine, int endLine) {
        String normalized = FingerprintUtil.normalizeText(content);
        String chunkFingerprint = FingerprintUtil.chunkFingerprint(doc.getId(), section, normalized);
        String chunkId = "chk:" + chunkFingerprint.substring(0, 16);

        KnowledgeChunk chunk = new KnowledgeChunk(chunkId, doc.getId(), normalized);
        chunk.setSourceId(doc.getSourceId());
        chunk.setRepository(doc.getRepository());
        chunk.setPath(doc.getPath());
        chunk.setHeading(heading != null ? heading : doc.getTitle());
        chunk.setSection(section != null ? section : "");
        chunk.setChunkType(chunkType);
        chunk.setStartLine(startLine);
        chunk.setEndLine(endLine);
        chunk.setTokenCount(estimateTokens(normalized));
        chunk.setFingerprint(chunkFingerprint);

        KnowledgeMetadata meta = new KnowledgeMetadata();
        if (doc.getMetadata() != null) {
            meta.setProjectId(doc.getMetadata().getProjectId());
            meta.setTenantId(doc.getMetadata().getTenantId());
            meta.setEnvironment(doc.getMetadata().getEnvironment());
            meta.setClassification(doc.getMetadata().getClassification());
            meta.setAuthoringContext(doc.getMetadata().getAuthoringContext());
            meta.setGlobal(doc.getMetadata().isGlobal());
            meta.setAttributes(doc.getMetadata().getAttributes());
        }
        chunk.setMetadata(meta);

        return chunk;
    }

    public static int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        String[] words = text.trim().split("\\s+");
        return (int) Math.ceil(words.length * 1.3);
    }

    private static String extractBlockName(String path) {
        String norm = path.replace('\\', '/');
        int idx = norm.indexOf("blocks/");
        if (idx >= 0) {
            String rest = norm.substring(idx + 7);
            int slash = rest.indexOf('/');
            if (slash > 0) {
                return rest.substring(0, slash);
            }
            return rest;
        }
        return "block";
    }
}
