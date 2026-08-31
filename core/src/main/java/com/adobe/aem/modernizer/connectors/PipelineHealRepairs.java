package com.adobe.aem.modernizer.connectors;

import com.adobe.aem.modernizer.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON sanitization and section-filter registration used by the pipeline healer
 * and by {@link com.adobe.aem.modernizer.agents.BlockGenerationAgent} when emitting UE models.
 */
public final class PipelineHealRepairs {

    public static final String SECTION_FILTER_PATH = "models/_section.json";
    public static final String COMPONENT_LIST_PATH = "block-configs/component-list.json";

    private static final Pattern BLOCK_MODEL_JSON = Pattern.compile(
            "^blocks/([A-Za-z0-9._-]+)/_[A-Za-z0-9._-]+\\.json$");

    private PipelineHealRepairs() {}

    public static String escapeJsonString(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    /**
     * If {@code raw} parses as JSON it is left unchanged. Otherwise control characters
     * inside string literals are escaped so {@code JSON.parse} can succeed.
     */
    public static String sanitizeBlockJson(String raw) {
        if (raw == null) {
            return "";
        }
        try {
            JsonUtil.mapper().readTree(raw);
            return raw;
        } catch (Exception ignored) {
            return escapeControlCharsInStrings(raw);
        }
    }

    static String escapeControlCharsInStrings(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 16);
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (!inString) {
                out.append(c);
                if (c == '"') {
                    inString = true;
                }
                continue;
            }
            if (escaped) {
                out.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                out.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                out.append(c);
                inString = false;
                continue;
            }
            if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c < 0x20) {
                out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static boolean isBlockModelJson(String path) {
        return path != null && BLOCK_MODEL_JSON.matcher(path.replace('\\', '/')).matches();
    }

    public static String blockIdFromModelPath(String path) {
        if (path == null) {
            return null;
        }
        Matcher matcher = BLOCK_MODEL_JSON.matcher(path.replace('\\', '/'));
        return matcher.matches() ? matcher.group(1) : null;
    }

    public static Set<String> collectBlockIds(Collection<String> paths) {
        Set<String> ids = new LinkedHashSet<>();
        if (paths == null) {
            return ids;
        }
        for (String path : paths) {
            String id = blockIdFromModelPath(path);
            if (id != null && !id.isBlank()) {
                ids.add(id);
            }
        }
        return ids;
    }

    public static String mergeSectionFilter(String json, Collection<String> blockIds) {
        return mergeStringArrayField(json, "filters", "section", "components", blockIds);
    }

    public static String mergeComponentList(String json, Collection<String> blockIds) {
        if (json == null || json.isBlank() || blockIds == null || blockIds.isEmpty()) {
            return json;
        }
        try {
            JsonNode root = JsonUtil.mapper().readTree(json);
            if (!(root instanceof ObjectNode)) {
                return json;
            }
            ObjectNode object = (ObjectNode) root;
            JsonNode compsNode = object.get("components");
            if (compsNode == null || !compsNode.isArray()) {
                ArrayNode created = object.putArray("components");
                for (String id : blockIds) {
                    if (id != null && !id.isBlank()) {
                        created.add(id);
                    }
                }
                return JsonUtil.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(object);
            }
            ArrayNode comps = (ArrayNode) compsNode;
            Set<String> existing = existingComponentIds(comps);
            boolean changed = false;
            for (String id : blockIds) {
                if (id == null || id.isBlank() || existing.contains(id)) {
                    continue;
                }
                if (comps.size() > 0 && comps.get(0).isObject()) {
                    ObjectNode item = comps.addObject();
                    item.put("id", id);
                } else {
                    comps.add(id);
                }
                existing.add(id);
                changed = true;
            }
            return changed
                    ? JsonUtil.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(object)
                    : json;
        } catch (Exception e) {
            return json;
        }
    }

    public static String sanitizeGeneratedJs(String raw) {
        if (raw == null) {
            return "";
        }
        String out = raw;
        Pattern jcrIdent = Pattern.compile("\\bjcr:([A-Za-z][A-Za-z0-9]*)");
        Matcher matcher = jcrIdent.matcher(out);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String rest = matcher.group(1);
            String ident = "jcr" + Character.toUpperCase(rest.charAt(0)) + rest.substring(1);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(ident));
        }
        matcher.appendTail(sb);
        out = sb.toString();
        out = out.replaceAll(
                "function appendEvents\\(config\\) \\{\\s*(?:if \\(!config\\?\\.mainEl\\) return;\\s*)?\\}",
                "function appendEvents() {\n}");
        out = out.replace("getHtmlFromBlockRow", "getHtmlFromRow");
        return out;
    }

    public static boolean isValidJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        try {
            JsonUtil.mapper().readTree(raw);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String classifyLogs(String logs) {
        if (logs == null || logs.isBlank()) {
            return "unknown";
        }
        String text = logs.toLowerCase(Locale.ROOT);
        if (text.contains("bad control character") || text.contains("build:json")
                || text.contains("unexpected token") && text.contains(".json")) {
            return "build:json";
        }
        if (text.contains("eslint") || text.contains("lint:js")) {
            return "eslint";
        }
        if (text.contains("stylelint") || text.contains("lint:css")) {
            return "stylelint";
        }
        return "unknown";
    }

    private static String mergeStringArrayField(String json, String filtersKey, String sectionId,
                                                String componentsKey, Collection<String> blockIds) {
        if (json == null || json.isBlank() || blockIds == null || blockIds.isEmpty()) {
            return json;
        }
        try {
            JsonNode root = JsonUtil.mapper().readTree(json);
            if (!(root instanceof ObjectNode)) {
                return json;
            }
            JsonNode filters = root.get(filtersKey);
            if (filters == null || !filters.isArray()) {
                return json;
            }
            boolean changed = false;
            for (JsonNode filter : filters) {
                if (!(filter instanceof ObjectNode)) {
                    continue;
                }
                ObjectNode object = (ObjectNode) filter;
                if (!sectionId.equals(object.path("id").asText())) {
                    continue;
                }
                JsonNode compsNode = object.get(componentsKey);
                ArrayNode comps;
                if (compsNode instanceof ArrayNode) {
                    comps = (ArrayNode) compsNode;
                } else {
                    comps = object.putArray(componentsKey);
                }
                Set<String> existing = existingComponentIds(comps);
                for (String id : blockIds) {
                    if (id == null || id.isBlank() || existing.contains(id)) {
                        continue;
                    }
                    comps.add(id);
                    existing.add(id);
                    changed = true;
                }
            }
            return changed
                    ? JsonUtil.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(root)
                    : json;
        } catch (Exception e) {
            return json;
        }
    }

    private static Set<String> existingComponentIds(ArrayNode comps) {
        Set<String> existing = new LinkedHashSet<>();
        for (JsonNode item : comps) {
            if (item.isTextual()) {
                existing.add(item.asText());
            } else if (item.isObject()) {
                String id = item.path("id").asText("");
                if (id.isBlank()) {
                    id = item.path("name").asText("");
                }
                if (!id.isBlank()) {
                    existing.add(id);
                }
            }
        }
        return existing;
    }

}
