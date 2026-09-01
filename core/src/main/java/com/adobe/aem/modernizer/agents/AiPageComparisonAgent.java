package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.ModelCapability;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI Agent that compares the original AEM source page (author rootpath) against the local
 * EDS render (aem up on localhost:3000) and generates/refines block JS &amp; CSS so the EDS
 * page matches the original AEM visual & functional experience.
 */
public class AiPageComparisonAgent {

    private static final Logger LOG = LoggerFactory.getLogger(AiPageComparisonAgent.class);

    private final Store store;
    private final AiGateway ai;
    private final com.adobe.aem.modernizer.connectors.LocalEdsRepoManager edsRepo;

    public AiPageComparisonAgent(Store store, AiGateway ai,
            com.adobe.aem.modernizer.connectors.LocalEdsRepoManager edsRepo) {
        this.store = store;
        this.ai = ai;
        this.edsRepo = edsRepo;
    }

    public String getName() {
        return "ai-page-comparison";
    }

    /**
     * Compares the AEM source page with the local EDS page and refines block code.
     *
     * @param project    active project (authorUrl, contentRoot, ids)
     * @param jobId      job to attribute events/files to
     * @param aemPagePath e.g. /content/wknd/language-masters/en/about-us
     * @param edsPagePath e.g. /about-us (rendered on localhost:3000)
     * @param blockName  block being refined (optional — when null, page-level report only)
     * @return report map with fetched HTML, AI analysis and applied file changes
     */
    public Map<String, Object> compareAndRefine(ProjectRecord project, String jobId,
            String aemPagePath, String edsPagePath, String blockName) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("aemPagePath", aemPagePath);
        report.put("edsPagePath", edsPagePath);
        report.put("edsUrl", "http://localhost:3000" + (edsPagePath == null ? "" : edsPagePath));

        String aemHtml = fetchPage(project != null ? project.getAemAuthorUrl() : null, aemPagePath);
        String edsHtml = fetchPage("http://localhost:3000", edsPagePath);
        report.put("aemFetched", !aemHtml.isEmpty());
        report.put("edsFetched", !edsHtml.isEmpty());

        if (aemHtml.isEmpty()) {
            report.put("status", "AEM_PAGE_UNREACHABLE");
            return report;
        }

        // 1. Visual/layout & behavior analysis prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert AEM Edge Delivery Services (EDS) engineer.\n")
              .append("Compare the ORIGINAL AEM page markup with the current EDS block render and produce\n")
              .append("refined CSS and JS so the EDS page matches the AEM visual layout, typography,\n")
              .append("spacing, DOM structure and interactive behavior (tabs, carousels, accordions, dialogs).\n\n")
              .append("Target block: ").append(blockName != null ? blockName : "(page-level)").append("\n")
              .append("Use the authorable `id` (blockId anchor) and `classes` (cssClass variants) fields\n")
              .append("for page-specific overrides rather than new one-off block types.\n\n")
              .append("=== ORIGINAL AEM PAGE HTML (truncated) ===\n")
              .append(truncate(aemHtml, 8000)).append("\n\n")
              .append("=== CURRENT EDS PAGE HTML (truncated) ===\n")
              .append(truncate(edsHtml, 6000)).append("\n\n")
              .append("Return ONLY a fenced ```css block and/or ```js block with the refined code.\n");

        String analysis = "";
        String css = null;
        String js = null;
        if (ai != null) {
            try {
                ChatRequest req = new ChatRequest(getName(), prompt.toString());
                req.setTargetCapability(ModelCapability.CAP_CODE);
                req.setPreferredProvider(project != null ? project.getAiProvider() : null);
                req.setPreferredModel(project != null ? project.getAiModel() : null);
                req.setProjectId(project != null ? project.getId() : null);
                req.setJobId(jobId);
                ChatResponse resp = ai.dispatch(req);
                analysis = resp.getContent() != null ? resp.getContent() : "";
            } catch (Exception e) {
                LOG.warn("[AiPageComparison] AI dispatch failed: {}", e.getMessage());
                report.put("aiError", e.getMessage());
            }
        }

        // 2. Extract fenced css/js blocks from the AI response
        css = extractFence(analysis, "css");
        js = extractFence(analysis, "js");
        report.put("analysis", truncate(analysis, 4000));
        report.put("cssGenerated", css != null);
        report.put("jsGenerated", js != null);

        // 3. Apply refinements directly into eds/<projectId>/blocks/<blockName>/
        List<String> updatedFiles = new ArrayList<>();
        if (edsRepo != null && project != null && blockName != null && !blockName.isBlank()) {
            String base = "blocks/" + blockName + "/";
            if (css != null) {
                mergeAndWrite(project.getId(), base + blockName + ".css", css);
                updatedFiles.add(base + blockName + ".css");
            }
            if (js != null) {
                mergeAndWrite(project.getId(), base + blockName + ".js", js);
                updatedFiles.add(base + blockName + ".js");
            }
        }
        report.put("updatedFiles", updatedFiles);

        // 4. Persist events & refined files in the store
        if (store != null && project != null) {
            for (String relPath : updatedFiles) {
                try {
                    String content = java.nio.file.Files.readString(
                            edsRepo.edsRepoDir(project.getId()).toPath().resolve(relPath),
                            java.nio.charset.StandardCharsets.UTF_8);
                    GeneratedFileRecord rec = new GeneratedFileRecord(
                            UUID.randomUUID().toString(), project.getId(), jobId,
                            relPath, relPath.endsWith(".css") ? "BLOCK_CSS" : "BLOCK_JS", content);
                    rec.setSourcePath(aemPagePath);
                    store.saveGeneratedFile(rec);
                } catch (Exception e) {
                    LOG.debug("[AiPageComparison] could not persist {}: {}", relPath, e.getMessage());
                }
            }
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(), project.getId(), jobId, getName(),
                    "🤖 AI page comparison: AEM '" + aemPagePath + "' vs EDS 'http://localhost:3000"
                            + (edsPagePath == null ? "" : edsPagePath) + "' — files updated: " + updatedFiles));
        }
        report.put("status", updatedFiles.isEmpty() ? "ANALYZED_NO_CHANGES" : "REFINED");
        return report;
    }

    /** Fetches page HTML with basic admin auth for AEM author. */
    private String fetchPage(String baseUrl, String pagePath) {
        if (pagePath == null || pagePath.isBlank()) return "";
        String url;
        if (pagePath.startsWith("http://") || pagePath.startsWith("https://")) {
            url = pagePath;
        } else {
            if (baseUrl == null || baseUrl.isBlank()) return "";
            url = baseUrl + (pagePath.startsWith("/") ? pagePath : "/" + pagePath);
            if (url.contains("4502") && !url.endsWith(".html") && !url.contains(".")) {
                url += ".html";
            }
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20)).GET();
            if (url.contains("4502") || url.contains("author") || url.contains("localhost")) {
                rb.header("Authorization", "Basic YWRtaW46YWRtaW4=");
            }
            HttpResponse<String> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200 && resp.body() != null ? resp.body() : "";
        } catch (Exception e) {
            LOG.debug("[AiPageComparison] fetch {} failed: {}", url, e.getMessage());
            return "";
        }
    }

    /** Appends a generated refinement section to the existing file (keeps prior content). */
    private void mergeAndWrite(String projectId, String relPath, String refinement) {
        try {
            java.nio.file.Path target = edsRepo.edsRepoDir(projectId).toPath().resolve(relPath);
            String existing = java.nio.file.Files.exists(target)
                    ? java.nio.file.Files.readString(target, java.nio.charset.StandardCharsets.UTF_8) : "";
            String merged = existing + "\n\n/* ===== AI page-comparison refinement "
                    + java.time.LocalDate.now() + " ===== */\n" + refinement + "\n";
            java.nio.file.Files.writeString(target, merged, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.warn("[AiPageComparison] merge write failed for {}: {}", relPath, e.getMessage());
        }
    }

    private static String extractFence(String text, String lang) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        int idx = lower.indexOf("```" + lang);
        if (idx < 0) return null;
        int start = text.indexOf('\n', idx);
        if (start < 0) return null;
        int end = text.indexOf("```", start + 1);
        if (end < 0) end = text.length();
        String body = text.substring(start + 1, end).trim();
        return body.isEmpty() ? null : body;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "\n... (truncated)";
    }
}
