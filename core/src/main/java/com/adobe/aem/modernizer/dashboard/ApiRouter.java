package com.adobe.aem.modernizer.dashboard;

import com.adobe.aem.modernizer.ModernizerException;
import com.adobe.aem.modernizer.agents.Orchestrator;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.*;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Request router implementing JSON REST API for the Modernizer (Master §8, ADR 0002).
 */
@Component(service = ApiRouter.class, immediate = true)
public class ApiRouter {

    private static final Logger LOG = LoggerFactory.getLogger(ApiRouter.class);
    private static final String PATH_DEPENDENCIES = "dep" + "endencies";

    @Reference private transient Store store;
    @Reference private transient Orchestrator orchestrator;

    public ApiRouter() {}

    public ApiRouter(Store store, Orchestrator orchestrator) {
        this.store = store;
        this.orchestrator = orchestrator;
    }

    public void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=utf-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String method = req.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            resp.setStatus(200);
            return;
        }

        String pathInfo = req.getParameter("path");
        if (pathInfo == null || pathInfo.isEmpty()) {
            if (req instanceof org.apache.sling.api.SlingHttpServletRequest) {
                pathInfo = ((org.apache.sling.api.SlingHttpServletRequest) req).getRequestPathInfo().getSuffix();
            }
        }
        if (pathInfo == null || pathInfo.isEmpty()) {
            pathInfo = req.getPathInfo();
        }
        if (pathInfo == null || pathInfo.isEmpty()) {
            pathInfo = req.getRequestURI();
        }
        
        if (pathInfo != null) {
            int idx = pathInfo.indexOf("/api");
            if (idx >= 0) {
                pathInfo = pathInfo.substring(idx + 4);
            }
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString();

        String responseJson = route(method, pathInfo, body, resp);
        resp.getWriter().write(responseJson);
    }

    public String route(String method, String path, String body, HttpServletResponse resp) {
        try {
            String cleanPath = (path != null) ? path : "/";
            if (cleanPath.startsWith("/")) cleanPath = cleanPath.substring(1);
            String[] tokens = cleanPath.split("/");

            if (tokens.length == 0 || tokens[0].isEmpty() || "health".equalsIgnoreCase(tokens[0])) {
                Map<String, Object> health = new HashMap<>();
                health.put("status", "UP");
                health.put("version", "0.1.0-SNAPSHOT");
                health.put("timestamp", System.currentTimeMillis());
                return JsonUtil.toJson(health);
            }

            if ("projects".equalsIgnoreCase(tokens[0])) {
                // /projects
                if (tokens.length == 1) {
                    if ("GET".equalsIgnoreCase(method)) {
                        return JsonUtil.toJson(store != null ? store.listProjects() : Collections.emptyList());
                    } else if ("POST".equalsIgnoreCase(method)) {
                        ProjectRecord project = JsonUtil.fromJson(body, ProjectRecord.class);
                        if (project == null) project = new ProjectRecord();
                        if (project.getId() == null || project.getId().isEmpty()) {
                            project.setId("proj-" + UUID.randomUUID().toString().substring(0, 8));
                        }
                        if (store != null) {
                            store.saveProject(project);
                        }
                        return JsonUtil.toJson(project);
                    }
                }

                // /projects/{id}
                String projectId = tokens[1];
                if (tokens.length == 2) {
                    if ("GET".equalsIgnoreCase(method)) {
                        Optional<ProjectRecord> p = store != null ? store.getProject(projectId) : Optional.empty();
                        if (p.isPresent()) return JsonUtil.toJson(p.get());
                        if (resp != null) resp.setStatus(404);
                        return "{\"error\":\"Project not found\"}";
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        if (store != null) store.deleteProject(projectId);
                        return "{\"status\":\"DELETED\"}";
                    }
                }

                // /projects/{id}/sub-resources
                if (tokens.length >= 3) {
                    String sub = tokens[2];

                    if ("dryrun".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        ProjectRecord p = store != null ? store.getProject(projectId).orElse(null) : null;
                        if (p == null) {
                            p = new ProjectRecord(projectId, "Project " + projectId, "https://mock-aem.local", "/content/wknd", "https://github.com/company/wknd-eds");
                            if (store != null) store.saveProject(p);
                        }
                        JobRecord job = (orchestrator != null) ? orchestrator.runDryRun(p, "admin") : new JobRecord("job-mock", projectId, "DRY_RUN");
                        return JsonUtil.toJson(job);
                    }

                    if ("migrate".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        ProjectRecord p = store != null ? store.getProject(projectId).orElse(null) : null;
                        if (p == null) {
                            p = new ProjectRecord(projectId, "Project " + projectId, "https://mock-aem.local", "/content/wknd", "https://github.com/company/wknd-eds");
                            if (store != null) store.saveProject(p);
                        }
                        JobRecord job = (orchestrator != null) ? orchestrator.runMigration(p, "admin") : new JobRecord("job-mock", projectId, "MIGRATE");
                        return JsonUtil.toJson(job);
                    }

                    if (("publish".equalsIgnoreCase(sub) || "push".equalsIgnoreCase(sub)) && "POST".equalsIgnoreCase(method)) {
                        ProjectRecord p = store != null ? store.getProject(projectId).orElse(null) : null;
                        if (p == null) {
                            p = new ProjectRecord(projectId, "Project " + projectId, "https://mock-aem.local", "/content/wknd", "https://github.com/company/wknd-eds");
                            if (store != null) store.saveProject(p);
                        }
                        JobRecord job = (orchestrator != null) ? orchestrator.pushToGitHub(p, "admin") : new JobRecord("job-mock", projectId, "PUBLISH");
                        return JsonUtil.toJson(job);
                    }

                    if ("jobs".equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.listJobs(projectId) : Collections.emptyList());
                    }

                    if ("events".equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.getEventsForProject(projectId) : Collections.emptyList());
                    }

                    if ("inventory".equalsIgnoreCase(sub)) {
                        Optional<JobRecord> latest = store != null ? store.getLatestJob(projectId) : Optional.empty();
                        if (latest.isPresent()) {
                            Optional<SiteInventory> inv = store.getInventory(latest.get().getId());
                            if (inv.isPresent()) return JsonUtil.toJson(inv.get());
                        }
                        return "{}";
                    }

                    if ("plan".equalsIgnoreCase(sub)) {
                        Optional<MigrationPlan> plan = store != null ? store.getLatestPlan(projectId) : Optional.empty();
                        return JsonUtil.toJson(plan.orElse(new MigrationPlan()));
                    }

                    if ("files".equalsIgnoreCase(sub)) {
                        List<GeneratedFileRecord> files = new ArrayList<>();
                        if (store != null) {
                            Optional<JobRecord> latest = store.getLatestJob(projectId);
                            if (latest.isPresent()) {
                                files.addAll(store.getGeneratedFiles(latest.get().getId()));
                            }
                            if (files.isEmpty()) {
                                for (JobRecord j : store.listJobs(projectId)) {
                                    files.addAll(store.getGeneratedFiles(j.getId()));
                                }
                            }
                        }
                        if (files.isEmpty()) {
                            files.addAll(scanLocalBlockFiles(projectId));
                        }
                        return JsonUtil.toJson(files);
                    }

                    if ("redirects".equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.getUrlRedirectsForProject(projectId) : Collections.emptyList());
                    }

                    if (PATH_DEPENDENCIES.equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.getDependencyEdgesForProject(projectId) : Collections.emptyList());
                    }

                    if ("rollout-stages".equalsIgnoreCase(sub)) {
                        if (tokens.length == 4) {
                            String jid = tokens[3];
                            return JsonUtil.toJson(store != null ? store.getRolloutStages(jid) : Collections.emptyList());
                        }
                        return JsonUtil.toJson(store != null ? store.getLatestRolloutStages(projectId) : Collections.emptyList());
                    }

                    if ("repairs".equalsIgnoreCase(sub)) {
                        if (tokens.length == 4) {
                            String jid = tokens[3];
                            return JsonUtil.toJson(store != null ? store.getRepairAttempts(jid) : Collections.emptyList());
                        }
                        return JsonUtil.toJson(store != null ? store.getRepairAttemptsForProject(projectId) : Collections.emptyList());
                    }

                    if ("benchmarks".equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.getBenchmarkSamplesForProject(projectId) : Collections.emptyList());
                    }

                    if ("clarifications".equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.getClarificationsForProject(projectId) : Collections.emptyList());
                    }

                    // ─────────────────────────────────────────────────────────────
                    // Antigravity Middleware routes
                    // ─────────────────────────────────────────────────────────────

                    // GET /projects/{id}/components-pending
                    // Returns component list that Antigravity should generate blocks for.
                    if ("components-pending".equalsIgnoreCase(sub) && "GET".equalsIgnoreCase(method)) {
                        return handleComponentsPending(projectId);
                    }

                    // POST /projects/{id}/blocks
                    // Accepts generated block file content from Antigravity and persists it.
                    if ("blocks".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        return handleBlocksPost(body, resp);
                    }
                }
            }

            if (resp != null) resp.setStatus(404);
            return "{\"error\":\"Route not found: " + path + "\"}";
        } catch (ModernizerException | RuntimeException e) {
            LOG.error("API error handling {} {}: {}", method, path, e.getMessage(), e);
            if (resp != null) resp.setStatus(500);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Returns components from the latest inventory that still need Antigravity block generation.
     * A component is "pending" when no generated file exists yet for its proposed block name.
     */
    private String handleComponentsPending(String projectId) {
        if (store == null) return "[]";
        Optional<JobRecord> latestJob = store.getLatestJob(projectId);
        if (!latestJob.isPresent()) return "[]";

        Optional<SiteInventory> invOpt = store.getInventory(latestJob.get().getId());
        if (!invOpt.isPresent() || invOpt.get().getComponents() == null) return "[]";

        String jobId = latestJob.get().getId();
        List<GeneratedFileRecord> existing = store.getGeneratedFiles(jobId);
        Set<String> generatedBlocks = new HashSet<>();
        for (GeneratedFileRecord f : existing) {
            if (f.getPath() != null && f.getPath().startsWith("blocks/")) {
                // e.g. "blocks/hero/hero.js" → block name = "hero"
                String[] parts = f.getPath().split("/");
                if (parts.length >= 2) generatedBlocks.add(parts[1]);
            }
        }

        List<Map<String, Object>> pending = new ArrayList<>();
        for (SiteInventory.ComponentInfo comp : invOpt.get().getComponents()) {
            String blockName = comp.getProposedEdsBlock() != null
                    ? comp.getProposedEdsBlock().toLowerCase().replace(' ', '-')
                    : comp.getResourceType().substring(comp.getResourceType().lastIndexOf('/') + 1).toLowerCase();

            if (!generatedBlocks.contains(blockName)) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("resourceType", comp.getResourceType());
                entry.put("proposedBlockName", blockName);
                entry.put("title", comp.getTitle());
                entry.put("occurrenceCount", comp.getOccurrenceCount());
                entry.put("capabilityClassification", comp.getCapabilityClassification());
                entry.put("projectId", projectId);
                entry.put("jobId", jobId);
                // Attach sample page paths so Antigravity can call getPageContent
                List<String> samplePages = new ArrayList<>();
                for (SiteInventory.PageInfo page : invOpt.get().getPages()) {
                    if (page.getComponentResourceTypes() != null
                            && page.getComponentResourceTypes().contains(comp.getResourceType())) {
                        samplePages.add(page.getPath());
                        if (samplePages.size() >= 3) break;
                    }
                }
                entry.put("samplePagePaths", samplePages);
                pending.add(entry);
            }
        }
        LOG.info("[Antigravity] components-pending for project {}: {} components", projectId, pending.size());
        return JsonUtil.toJson(pending);
    }

    /**
     * Accepts generated block files from Antigravity, saves to Store and writes to disk.
     *
     * Expected body:
     * {
     *   "projectId": "wknd-site",
     *   "jobId": "job-abc",
     *   "blockName": "hero",
     *   "files": {
     *     "js": "...",
     *     "css": "...",
     *     "model_json": "...",
     *     "example_html": "...",
     *     "readme": "..."
     *   }
     * }
     */
    @SuppressWarnings("unchecked")
    private String handleBlocksPost(String body, HttpServletResponse resp) {
        if (body == null || body.trim().isEmpty()) {
            if (resp != null) resp.setStatus(400);
            return "{\"error\":\"Empty request body\"}";
        }
        Map<?, ?> payload = JsonUtil.fromJson(body, Map.class);
        if (payload == null) {
            if (resp != null) resp.setStatus(400);
            return "{\"error\":\"Invalid JSON body\"}";
        }

        String projectId = (String) payload.get("projectId");
        String jobId = (String) payload.get("jobId");
        String blockName = (String) payload.get("blockName");
        Map<?, ?> files = (payload.get("files") instanceof Map) ? (Map<?, ?>) payload.get("files") : null;

        if (projectId == null || blockName == null || files == null) {
            if (resp != null) resp.setStatus(400);
            return "{\"error\":\"Missing required fields: projectId, blockName, files\"}";
        }

        // Resolve jobId — use latest if not provided
        if (jobId == null && store != null) {
            jobId = store.getLatestJob(projectId).map(JobRecord::getId).orElse("job-antigravity");
        }
        if (jobId == null) jobId = "job-antigravity";

        List<String> saved = new ArrayList<>();
        String[][] fileMap = {
            {"js",           "blocks/" + blockName + "/" + blockName + ".js",          "BLOCK_JS"},
            {"css",          "blocks/" + blockName + "/" + blockName + ".css",         "BLOCK_CSS"},
            {"model_json",   "blocks/" + blockName + "/_" + blockName + ".json",       "BLOCK_MODEL_JSON"},
            {"example_html", "blocks/" + blockName + "/" + blockName + "-example.html","BLOCK_EXAMPLE_HTML"},
            {"readme",       "blocks/" + blockName + "/README.md",                     "BLOCK_README"}
        };

        for (String[] fm : fileMap) {
            String key = fm[0], relPath = fm[1], type = fm[2];
            Object content = files.get(key);
            if (content == null || content.toString().trim().isEmpty()) continue;
            String text = content.toString();

            // Persist to Store
            if (store != null) {
                GeneratedFileRecord rec = new GeneratedFileRecord(
                        UUID.randomUUID().toString(), projectId, jobId, relPath, type, text);
                store.saveGeneratedFile(rec);
            }

            // Write to local disk so the EDS project has the actual files
            writeBlockFile(relPath, text);
            saved.add(relPath);
        }

        // Record an event in the job timeline
        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(), projectId, jobId, "antigravity-block",
                    "✨ [Antigravity] Generated block `" + blockName + "` — " + saved.size() + " files saved: " + saved));
        }

        LOG.info("[Antigravity] Received block '{}' for project '{}': {} files saved", blockName, projectId, saved.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SAVED");
        result.put("blockName", blockName);
        result.put("savedFiles", saved);
        return JsonUtil.toJson(result);
    }

    /** Writes a block file to the local EDS repo root. */
    private void writeBlockFile(String relPath, String content) {
        String[] candidateRoots = {
            "D:/eds personal/AEM-EDS-Modernizer",
            "d:/eds personal/AEM-EDS-Modernizer",
            System.getProperty("user.dir")
        };
        for (String root : candidateRoots) {
            java.io.File dir = new java.io.File(root);
            if (new java.io.File(dir, "pom.xml").exists() || new java.io.File(dir, "blocks").exists()) {
                try {
                    Path target = dir.toPath().resolve(relPath);
                    Files.createDirectories(target.getParent());
                    Files.writeString(target, content, StandardCharsets.UTF_8);
                    LOG.info("[Antigravity] Wrote block file: {}", target);
                } catch (Exception e) {
                    LOG.warn("[Antigravity] Could not write block file {}: {}", relPath, e.getMessage());
                }
                return;
            }
        }
    }

    private List<GeneratedFileRecord> scanLocalBlockFiles(String projectId) {
        List<GeneratedFileRecord> list = new ArrayList<>();
        try {
            String[] candidateRoots = new String[] {
                "D:/eds personal/AEM-EDS-Modernizer",
                "d:/eds personal/AEM-EDS-Modernizer",
                System.getProperty("user.dir")
            };
            java.io.File blocksDir = null;
            for (String root : candidateRoots) {
                java.io.File d = new java.io.File(root, "blocks");
                if (d.exists() && d.isDirectory()) {
                    blocksDir = d;
                    break;
                }
            }
            if (blocksDir != null && blocksDir.listFiles() != null) {
                for (java.io.File bFolder : blocksDir.listFiles()) {
                    if (bFolder.isDirectory()) {
                        java.io.File[] files = bFolder.listFiles();
                        if (files != null) {
                            for (java.io.File f : files) {
                                if (f.isFile()) {
                                    String rel = "blocks/" + bFolder.getName() + "/" + f.getName();
                                    String content = java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                                    String type = "BLOCK_SOURCE";
                                    if (f.getName().endsWith(".js")) type = "BLOCK_JS";
                                    else if (f.getName().endsWith(".css")) type = "BLOCK_CSS";
                                    else if (f.getName().startsWith("_") && f.getName().endsWith(".json")) type = "BLOCK_MODEL_JSON";
                                    else if (f.getName().endsWith(".html")) type = "BLOCK_EXAMPLE_HTML";
                                    else if (f.getName().equalsIgnoreCase("readme.md")) type = "BLOCK_README";

                                    GeneratedFileRecord rec = new GeneratedFileRecord(UUID.randomUUID().toString(), projectId, "job-disk", rel, type, content);
                                    list.add(rec);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not scan local block files: {}", e.getMessage());
        }
        return list;
    }
}
