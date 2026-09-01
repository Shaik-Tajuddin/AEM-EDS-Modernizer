package com.adobe.aem.modernizer.dashboard;

import com.adobe.aem.modernizer.ModernizerException;
import com.adobe.aem.modernizer.agents.Orchestrator;
import com.adobe.aem.modernizer.connectors.GitHubClient;
import com.adobe.aem.modernizer.connectors.GitHubFlow;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.*;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
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
    @Reference private transient com.adobe.aem.modernizer.ai.AiGateway aiGateway;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient GitHubClient gitHubClient;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient com.adobe.aem.modernizer.connectors.LocalEdsRepoManager localEdsRepo;

    public ApiRouter() {}

    public ApiRouter(Store store, Orchestrator orchestrator) {
        this.store = store;
        this.orchestrator = orchestrator;
    }

    public ApiRouter(Store store, Orchestrator orchestrator, GitHubClient gitHubClient) {
        this.store = store;
        this.orchestrator = orchestrator;
        this.gitHubClient = gitHubClient;
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
                org.apache.sling.api.request.RequestPathInfo requestPathInfo = ((org.apache.sling.api.SlingHttpServletRequest) req).getRequestPathInfo();
                if (requestPathInfo != null) {
                    pathInfo = requestPathInfo.getSuffix();
                }
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
                    if (resp != null) resp.setStatus(405);
                    return "{\"error\":\"Method not allowed\"}";
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

                    if ("delete".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        if (store != null) store.deleteProject(projectId);
                        return "{\"status\":\"DELETED\"}";
                    }

                    if ("dryrun".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        ProjectRecord p = getOrCreateStubProject(projectId);
                        // Local EDS repo workflow: clone/update + npm install BEFORE dry run
                        Map<String, Object> repoStatus = cloneLocalEdsRepo(p);
                        JobRecord job = (orchestrator != null) ? orchestrator.runDryRun(p, "admin") : new JobRecord("job-mock", projectId, "DRY_RUN");
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("job", job);
                        result.put("localRepo", repoStatus);
                        return JsonUtil.toJson(result);
                    }

                    if ("migrate".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        ProjectRecord p = getOrCreateStubProject(projectId);
                        JobRecord job = (orchestrator != null) ? orchestrator.runMigration(p, "admin") : new JobRecord("job-mock", projectId, "MIGRATE");
                        return JsonUtil.toJson(job);
                    }

                    if ("preview".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        ProjectRecord p = getOrCreateStubProject(projectId);
                        // Pre-PR healing: checkout feature branch, prune duplicates, lint+build, push
                        Map<String, Object> healing = runLocalHealing(p);
                        JobRecord job = (orchestrator != null) ? orchestrator.pushToPreviewBranch(p, "admin") : new JobRecord("job-mock", projectId, "PREVIEW");
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("job", job);
                        result.put("healing", healing);
                        result.put("prReady", Boolean.TRUE.equals(healing.get("ok")));
                        return JsonUtil.toJson(result);
                    }

                    // Local dev server: POST /projects/{id}/aem-up  body: {"action":"start|stop|status"}
                    if ("aem-up".equalsIgnoreCase(sub)) {
                        return handleAemUp(projectId, body, resp);
                    }

                    // AI page comparison: POST /projects/{id}/compare
                    // body: {"aemPagePath":"/content/wknd/.../about-us","edsPagePath":"/about-us","blockName":"hero"}
                    if ("compare".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        return handleAiCompare(projectId, body, resp);
                    }

                    if ("publish".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        ProjectRecord p = getOrCreateStubProject(projectId);
                        // PR gate: only allow when pre-PR healing completed successfully on the branch
                        if (store != null) {
                            Optional<JobRecord> latest = store.getLatestJob(projectId);
                            boolean healed = latest.isPresent() && latest.get().getMetadata() != null
                                    && Boolean.parseBoolean(String.valueOf(latest.get().getMetadata().get("healingOk")));
                            if (!healed) {
                                if (resp != null) resp.setStatus(409);
                                return "{\"error\":\"Create PR is locked: pre-PR healing (branch checkout, deduplication, lint:fix, build:json, push) has not completed successfully. Run the preview step first.\"}";
                            }
                        }
                        JobRecord job = (orchestrator != null) ? orchestrator.openPullRequest(p, "admin") : new JobRecord("job-mock", projectId, "PUBLISH");
                        return JsonUtil.toJson(job);
                    }

                    if ("npm".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        return handleNpmPost(projectId, body, resp);
                    }

                    if ("npm".equalsIgnoreCase(sub) && "GET".equalsIgnoreCase(method) && tokens.length >= 4) {
                        return handleNpmGet(projectId, tokens[3], resp);
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
                        }
                        files.addAll(scanLocalBlockFiles(projectId));
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

                    if ("branch-status".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        return handleBranchStatus(projectId, body, resp);
                    }

                    if ("workspace".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        if (tokens.length >= 4 && "file".equalsIgnoreCase(tokens[3])) {
                            return handleWorkspaceFile(projectId, body, resp);
                        }
                        if (tokens.length >= 4 && "save".equalsIgnoreCase(tokens[3])) {
                            return handleWorkspaceSave(projectId, body, resp);
                        }
                        if (tokens.length >= 4 && "delete".equalsIgnoreCase(tokens[3])) {
                            return handleWorkspaceDelete(projectId, body, resp);
                        }
                        return handleWorkspaceList(projectId, body, resp);
                    }

                    // ─────────────────────────────────────────────────────────────
                    // Antigravity Middleware routes
                    // ─────────────────────────────────────────────────────────────

                    // GET /projects/{id}/components-pending
                    // Returns component list that Antigravity should generate blocks for.
                    if ("chat".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        return handleChatPost(projectId, body, resp);
                    }

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
     * Realtime agent chat endpoint.
     * POST /api/projects/{id}/chat
     * Body: { "message": "...", "agent": "optional-agent-name", "model": "optional-model" }
     * Responds: { "reply": "...", "provider": "...", "model": "...", "timestamp": ... }
     */
    @SuppressWarnings("unchecked")
    private String handleChatPost(String projectId, String body, HttpServletResponse resp) {
        Map<?, ?> payload = (body != null && !body.trim().isEmpty())
                ? JsonUtil.fromJson(body, Map.class) : null;
        String message = payload != null ? Objects.toString(payload.get("message"), null) : null;
        if (message == null || message.trim().isEmpty()) {
            if (resp != null) resp.setStatus(400);
            return "{\"error\":\"Missing required field: message\"}";
        }

        String agent = payload != null ? Objects.toString(payload.get("agent"), "dashboard-assistant") : "dashboard-assistant";

        StringBuilder historySb = new StringBuilder();
        if (payload != null && payload.get("history") instanceof List) {
            List<?> history = (List<?>) payload.get("history");
            int from = Math.max(0, history.size() - 10);
            for (int i = from; i < history.size(); i++) {
                Object turn = history.get(i);
                if (turn instanceof Map) {
                    String role = Objects.toString(((Map<?, ?>) turn).get("role"), "user");
                    String text = Objects.toString(((Map<?, ?>) turn).get("text"), "");
                    if (!text.isEmpty()) {
                        historySb.append("user".equals(role) ? "Operator: " : "Agent: ")
                                .append(text).append("\n");
                    }
                }
            }
        }

        ProjectRecord project = store != null ? store.getProject(projectId).orElse(null) : null;
        com.adobe.aem.modernizer.ai.chat.ChatAgentRuntime runtime =
                new com.adobe.aem.modernizer.ai.chat.ChatAgentRuntime(aiGateway, store, orchestrator, gitHubClient);
        Map<String, Object> agentResult = runtime.handle(projectId, project, message, historySb.toString());

        String reply = Objects.toString(agentResult.get("reply"), "");
        String providerName = Objects.toString(agentResult.get("provider"), "unknown");
        String modelName = Objects.toString(agentResult.get("model"), "unknown");

        if (store != null) {
            String jobId = store.getLatestJob(projectId).map(JobRecord::getId).orElse("chat-" + projectId);
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(), projectId, jobId, "chat-user",
                    message));
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(), projectId, jobId, "chat-agent",
                    reply));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply", reply);
        result.put("agent", agent);
        result.put("provider", providerName);
        result.put("model", modelName);
        result.put("steps", agentResult.get("steps"));
        result.put("timestamp", System.currentTimeMillis());
        return JsonUtil.toJson(result);
    }

    private String handleNpmPost(String projectId, String body, HttpServletResponse resp) {
        if (gitHubClient == null) {
            if (resp != null) resp.setStatus(503);
            return "{\"error\":\"GitHub client not available\"}";
        }
        ProjectRecord project = store != null ? store.getProject(projectId).orElse(null) : null;
        if (project == null) {
            if (resp != null) resp.setStatus(404);
            return "{\"error\":\"Project not found\"}";
        }
        Map<?, ?> payload = (body != null && !body.trim().isEmpty())
                ? JsonUtil.fromJson(body, Map.class) : null;
        String command = payload != null ? Objects.toString(payload.get("command"), "") : "";
        command = command.trim();
        if (!"lint:fix".equals(command) && !"build:json".equals(command)
                && !"install-workflow".equals(command) && !"heal".equals(command)) {
            if (resp != null) resp.setStatus(400);
            return "{\"error\":\"command must be lint:fix, build:json, heal, or install-workflow\"}";
        }

        String branch = GitHubFlow.featureBranch(project.getId());
        GitHubClient client = GitHubFlow.clientFor(gitHubClient, project);

        if ("heal".equals(command)) {
            JobRecord job = store != null ? store.getLatestJob(project.getId()).orElse(null) : null;
            if (job == null) {
                job = new JobRecord(UUID.randomUUID().toString(), project.getId(), "PREVIEWING");
                if (store != null) {
                    store.saveJob(job);
                }
            }
            com.adobe.aem.modernizer.agents.AgentContext ctx =
                    new com.adobe.aem.modernizer.agents.AgentContext(project, job);
            com.adobe.aem.modernizer.connectors.PipelineHealLoop.start(client, ctx, store, aiGateway);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("command", "heal");
            result.put("branch", branch);
            result.put("status", client instanceof com.adobe.aem.modernizer.connectors.RealGitHubClient
                    ? "started" : "completed");
            if (job.getMetadata() != null && job.getMetadata().get("ciHeal") != null) {
                result.put("ciHeal", job.getMetadata().get("ciHeal"));
            }
            return JsonUtil.toJson(result);
        }

        if ("install-workflow".equals(command)) {
            try {
                installNpmWorkflow(client, project, branch);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "installed");
                result.put("branch", branch);
                result.put("path", GitHubFlow.NPM_WORKFLOW_PATH);
                return JsonUtil.toJson(result);
            } catch (RuntimeException e) {
                if (resp != null) resp.setStatus(502);
                return "{\"error\":\"Failed to install workflow: " + escapeJson(e.getMessage()) + "\"}";
            }
        }

        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("command", command);
        try {
            Map<String, Object> run = client.dispatchWorkflow(branch, GitHubFlow.NPM_WORKFLOW_FILE, inputs);
            if (run == null) {
                run = new LinkedHashMap<>();
            }
            run.put("command", command);
            run.put("branch", branch);
            return JsonUtil.toJson(run);
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("404") || msg.toLowerCase().contains("workflow")) {
                try {
                    installNpmWorkflow(client, project, branch);
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    Map<String, Object> run = client.dispatchWorkflow(branch, GitHubFlow.NPM_WORKFLOW_FILE, inputs);
                    if (run == null) {
                        run = new LinkedHashMap<>();
                    }
                    run.put("command", command);
                    run.put("branch", branch);
                    run.put("workflowInstalled", true);
                    return JsonUtil.toJson(run);
                } catch (RuntimeException retry) {
                    if (resp != null) resp.setStatus(502);
                    return "{\"error\":\"GitHub Actions dispatch failed. The workflow must exist on the repository default branch, and the token needs Actions write. "
                            + escapeJson(retry.getMessage()) + "\"}";
                }
            }
            if (resp != null) resp.setStatus(502);
            return "{\"error\":\"GitHub Actions dispatch failed: " + escapeJson(msg) + "\"}";
        }
    }

    private String handleNpmGet(String projectId, String runId, HttpServletResponse resp) {
        if (gitHubClient == null) {
            if (resp != null) resp.setStatus(503);
            return "{\"error\":\"GitHub client not available\"}";
        }
        ProjectRecord project = store != null ? store.getProject(projectId).orElse(null) : null;
        GitHubClient client = GitHubFlow.clientFor(gitHubClient, project);
        Map<String, Object> run = client.getWorkflowRun(runId);
        if (run == null) {
            if (resp != null) resp.setStatus(404);
            return "{\"error\":\"Workflow run not found\"}";
        }
        String logs = client.getWorkflowRunLogs(runId);
        run.put("logs", logs != null ? logs : "");
        return JsonUtil.toJson(run);
    }

    private void installNpmWorkflow(GitHubClient client, ProjectRecord project, String branch) {
        String jobId = store != null ? store.getLatestJob(project.getId()).map(JobRecord::getId).orElse("preview") : "preview";
        GeneratedFileRecord yaml = new GeneratedFileRecord(
                UUID.randomUUID().toString(),
                project.getId(),
                jobId,
                GitHubFlow.NPM_WORKFLOW_PATH,
                "CONFIG",
                com.adobe.aem.modernizer.connectors.ModernizerNpmWorkflow.YAML
        );
        java.util.LinkedHashSet<String> targets = new java.util.LinkedHashSet<>();
        String repoDefault = client.getRepositoryDefaultBranch();
        if (repoDefault != null && !repoDefault.isBlank()) {
            targets.add(repoDefault);
        }
        String edsBranch = project.getEdsBranch();
        if (edsBranch != null && !edsBranch.isBlank()) {
            targets.add(edsBranch);
        }
        if (branch != null && !branch.isBlank()) {
            client.createBranch(branch);
            targets.add(branch);
        }
        RuntimeException last = null;
        boolean committed = false;
        for (String target : targets) {
            try {
                client.commitFiles(target, Collections.singletonList(yaml),
                        "chore: fold npm scripts into the Build workflow");
                GitHubFlow.deleteLegacyNpmWorkflow(client, target);
                committed = true;
            } catch (RuntimeException e) {
                last = e;
            }
        }
        if (!committed && last != null) {
            throw last;
        }
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    /** Clone/update the local eds/<projectId> repo (best-effort, never throws). */
    private Map<String, Object> cloneLocalEdsRepo(ProjectRecord project) {
        if (localEdsRepo == null) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("status", "UNAVAILABLE");
            return s;
        }
        Map<String, Object> status = localEdsRepo.cloneOrUpdate(project);
        LOG.info("[LocalEdsRepo] dry-run clone/update for {}: {}", project.getId(), status.get("status"));
        return status;
    }

    /**
     * Pre-PR automated healing: checkout feat/<projectId>, prune duplicate blocks,
     * npm run lint:fix + build:json, commit and push. Records healingOk on the latest job
     * so the Create PR gate can open.
     */
    private Map<String, Object> runLocalHealing(ProjectRecord project) {
        Map<String, Object> healing = new LinkedHashMap<>();
        healing.put("ok", false);
        if (localEdsRepo == null) {
            healing.put("status", "UNAVAILABLE");
            healing.put("reason", "LocalEdsRepoManager not bound — falling back to GitHub-only flow");
            // Without a local repo manager we do not block the legacy flow
            healing.put("ok", true);
            markHealingOk(project.getId(), true);
            return healing;
        }
        try {
            java.io.File repo = localEdsRepo.edsRepoDir(project.getId());
            String branch = GitHubFlow.featureBranch(project.getId());
            if (!new java.io.File(repo, ".git").exists()) {
                localEdsRepo.cloneOrUpdate(project);
            }
            healing.put("checkout", localEdsRepo.checkoutBranch(repo, branch) ? "OK" : "SKIPPED");
            healing.put("prunedBlocks", localEdsRepo.pruneDuplicateBlocks(repo));
            healing.putAll(localEdsRepo.runLintAndBuild(repo, branch));
        } catch (Exception e) {
            LOG.warn("[LocalEdsRepo] healing failed: {}", e.getMessage());
            healing.put("error", e.getMessage());
        }
        boolean ok = Boolean.TRUE.equals(healing.get("ok"));
        healing.put("ok", ok);
        markHealingOk(project.getId(), ok);
        return healing;
    }

    private void markHealingOk(String projectId, boolean ok) {
        if (store == null) return;
        try {
            Optional<JobRecord> latest = store.getLatestJob(projectId);
            if (latest.isPresent()) {
                JobRecord job = latest.get();
                Map<String, Object> meta = new LinkedHashMap<>(job.getMetadata());
                meta.put("healingOk", ok);
                job.setMetadata(meta);
                store.saveJob(job);
            }
        } catch (Exception e) {
            LOG.debug("[LocalEdsRepo] could not persist healingOk: {}", e.getMessage());
        }
    }

    /** Local dev server control: {"action":"start"|"stop"|"status"}. */
    private String handleAemUp(String projectId, String body, HttpServletResponse resp) {
        if (localEdsRepo == null) {
            if (resp != null) resp.setStatus(503);
            return "{\"error\":\"LocalEdsRepoManager not available\"}";
        }
        Map<?, ?> payload = (body != null && !body.trim().isEmpty()) ? JsonUtil.fromJson(body, Map.class) : null;
        String action = payload != null ? Objects.toString(payload.get("action"), "status") : "status";
        java.io.File repo = localEdsRepo.edsRepoDir(projectId);
        Map<String, Object> result;
        switch (action.toLowerCase()) {
            case "start":
                result = localEdsRepo.startAemUpDevServer(repo, projectId);
                break;
            case "stop":
                result = localEdsRepo.stopAemUpDevServer(projectId);
                break;
            default:
                result = localEdsRepo.devServerStatus(projectId);
        }
        result.put("action", action);
        return JsonUtil.toJson(result);
    }

    /** AI compare & match: AEM source page vs local EDS render (aem up). */
    private String handleAiCompare(String projectId, String body, HttpServletResponse resp) {
        ProjectRecord project = store != null ? store.getProject(projectId).orElse(null) : null;
        if (project == null) {
            if (resp != null) resp.setStatus(404);
            return "{\"error\":\"Project not found\"}";
        }
        Map<?, ?> payload = (body != null && !body.trim().isEmpty()) ? JsonUtil.fromJson(body, Map.class) : null;
        String aemPagePath = payload != null ? Objects.toString(payload.get("aemPagePath"), null) : null;
        if (aemPagePath == null || aemPagePath.isBlank()) {
            aemPagePath = project.getContentRoot();
        }
        String edsPagePath = payload != null ? Objects.toString(payload.get("edsPagePath"), "") : "";
        String blockName = payload != null ? Objects.toString(payload.get("blockName"), null) : null;

        String jobId = store != null ? store.getLatestJob(projectId).map(JobRecord::getId)
                .orElse("compare-" + projectId) : "compare-" + projectId;
        com.adobe.aem.modernizer.agents.AiPageComparisonAgent agent =
                new com.adobe.aem.modernizer.agents.AiPageComparisonAgent(store, aiGateway, localEdsRepo);
        Map<String, Object> report = agent.compareAndRefine(project, jobId, aemPagePath.trim(), edsPagePath.trim(), blockName);
        return JsonUtil.toJson(report);
    }

    /**
     * Loads the project, or creates a minimal stub if it doesn't exist yet. The stub's EDS
     * Git repository URL / branch come from the OSGi-configured {@link GitHubClient} fallback
     * (Config {@code repoUrl}/{@code defaultBranch}) rather than a hardcoded literal, so each
     * project can be pointed at its own repo simply by editing it afterwards in the dashboard.
     */
    private ProjectRecord getOrCreateStubProject(String projectId) {
        ProjectRecord p = store != null ? store.getProject(projectId).orElse(null) : null;
        if (p == null) {
            String fallbackRepoUrl = gitHubClient != null && gitHubClient.getRepoUrl() != null
                    && !gitHubClient.getRepoUrl().isBlank() ? gitHubClient.getRepoUrl() : "https://github.com/company/wknd-eds";
            String fallbackBranch = gitHubClient != null ? gitHubClient.getDefaultBranch() : "main";
            p = new ProjectRecord(projectId, "Project " + projectId, "https://mock-aem.local", "/content/wknd", fallbackRepoUrl);
            p.setEdsBranch(fallbackBranch);
            if (store != null) store.saveProject(p);
        }
        return p;
    }

    /**
     * Reports the file changes and latest CI status for a given branch of the project's
     * GitHub repository, plus a ready-to-open vscode.dev link for that branch.
     */
    private String handleBranchStatus(String projectId, String body, HttpServletResponse resp) {
        if (gitHubClient == null) {
            if (resp != null) resp.setStatus(503);
            return "{\"error\":\"GitHub client not available\"}";
        }
        ProjectRecord project = store != null ? store.getProject(projectId).orElse(null) : null;
        if (project == null) {
            if (resp != null) resp.setStatus(404);
            return "{\"error\":\"Project not found\"}";
        }

        Map<?, ?> payload = (body != null && !body.trim().isEmpty())
                ? JsonUtil.fromJson(body, Map.class) : null;
        String branch = payload != null ? Objects.toString(payload.get("branch"), null) : null;
        if (branch == null || branch.trim().isEmpty()) {
            if (resp != null) resp.setStatus(400);
            return "{\"error\":\"Missing required field: branch\"}";
        }
        branch = branch.trim();
        String baseBranch = project.getEdsBranch() != null && !project.getEdsBranch().isBlank()
                ? project.getEdsBranch().trim() : gitHubClient.getDefaultBranch();

        GitHubClient client = GitHubFlow.clientFor(gitHubClient, project);

        List<Map<String, Object>> changedFiles;
        Map<String, Object> latestRun;
        try {
            changedFiles = client.listChangedFiles(baseBranch, branch);
        } catch (Exception e) {
            LOG.warn("[BranchStatus] listChangedFiles failed for '{}': {}", branch, e.getMessage());
            changedFiles = Collections.emptyList();
        }
        try {
            latestRun = client.getLatestWorkflowRun(branch);
        } catch (Exception e) {
            LOG.warn("[BranchStatus] getLatestWorkflowRun failed for '{}': {}", branch, e.getMessage());
            latestRun = null;
        }

        String vscodeUrl = GitHubFlow.vscodeUrl(project.getEdsGitRepoUrl(), branch);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("branch", branch);
        result.put("baseBranch", baseBranch);
        result.put("changedFiles", changedFiles);
        result.put("latestRun", latestRun);
        result.put("vscodeUrl", vscodeUrl);
        return JsonUtil.toJson(result);
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
            if (comp.getResourceType() != null && (comp.getResourceType().contains("/components/container")
                    || comp.getResourceType().contains("/components/page")
                    || comp.getResourceType().endsWith("/container")
                    || comp.getResourceType().endsWith("/page"))) {
                continue;
            }
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

    private GitHubClient clientFor(ProjectRecord project) {
        return GitHubFlow.clientFor(gitHubClient, project);
    }

    private String requiredBranch(String body, HttpServletResponse resp) {
        Map<?, ?> payload = (body != null && !body.trim().isEmpty())
                ? JsonUtil.fromJson(body, Map.class) : null;
        String branch = payload != null ? Objects.toString(payload.get("branch"), "") : "";
        if (branch.isBlank()) {
            if (resp != null) resp.setStatus(400);
            return null;
        }
        return branch.trim();
    }

    private String handleWorkspaceList(String projectId, String body, HttpServletResponse resp) {
        ProjectRecord project = store != null ? store.getProject(projectId).orElse(null) : null;
        if (project == null) {
            if (resp != null) resp.setStatus(404);
            return "{\"error\":\"Project not found\"}";
        }
        String branch = requiredBranch(body, resp);
        if (branch == null) {
            return "{\"error\":\"Missing required field: branch\"}";
        }
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        GitHubClient client = clientFor(project);
        if (client != null) {
            String base = project.getEdsBranch() != null && !project.getEdsBranch().isBlank()
                    ? project.getEdsBranch().trim() : client.getDefaultBranch();
            try {
                for (Map<String, Object> changed : client.listChangedFiles(base, branch)) {
                    Object name = changed.get("filename");
                    if (name != null && !GitHubFlow.skipFromCommit(String.valueOf(name))) {
                        paths.add(String.valueOf(name));
                    }
                }
            } catch (RuntimeException e) {
                LOG.warn("[Workspace] listChangedFiles failed: {}", e.getMessage());
            }
        }
        if (store != null) {
            store.getLatestJob(projectId).ifPresent(job -> {
                for (GeneratedFileRecord file : store.getGeneratedFiles(job.getId())) {
                    if (file.getPath() != null && !GitHubFlow.skipFromCommit(file.getPath())) {
                        paths.add(file.getPath());
                    }
                }
            });
        }
        List<Map<String, Object>> files = new ArrayList<>();
        for (String path : paths) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", path);
            files.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("branch", branch);
        result.put("vscodeUrl", GitHubFlow.vscodeUrl(project.getEdsGitRepoUrl(), branch));
        result.put("files", files);
        result.put("note", "vscode.dev cannot be embedded in AEM. Review files here or open vscode.dev in a new tab.");
        if (store != null) {
            store.getLatestJob(projectId).ifPresent(job -> {
                if (job.getMetadata() != null && job.getMetadata().get("ciHeal") != null) {
                    result.put("ciHeal", job.getMetadata().get("ciHeal"));
                }
            });
        }
        return JsonUtil.toJson(result);
    }

    private String handleWorkspaceFile(String projectId, String body, HttpServletResponse resp) {
        ProjectRecord project = store != null ? store.getProject(projectId).orElse(null) : null;
        if (project == null) {
            if (resp != null) resp.setStatus(404);
            return "{\"error\":\"Project not found\"}";
        }
        Map<?, ?> payload = (body != null && !body.trim().isEmpty())
                ? JsonUtil.fromJson(body, Map.class) : null;
        String branch = payload != null ? Objects.toString(payload.get("branch"), "").trim() : "";
        String path = payload != null ? Objects.toString(payload.get("path"), "").trim() : "";
        if (branch.isEmpty() || path.isEmpty()) {
            if (resp != null) resp.setStatus(400);
            return "{\"error\":\"Missing required fields: branch, path\"}";
        }
        String content = null;
        GitHubClient client = clientFor(project);
        if (client != null) {
            content = client.getFileContent(branch, path);
        }
        if (content == null && store != null) {
            Optional<JobRecord> latest = store.getLatestJob(projectId);
            if (latest.isPresent()) {
                for (GeneratedFileRecord file : store.getGeneratedFiles(latest.get().getId())) {
                    if (path.equals(file.getPath())) {
                        content = file.getContent();
                        break;
                    }
                }
            }
        }
        if (content == null) {
            if (resp != null) resp.setStatus(404);
            return "{\"error\":\"File not found on branch or in generated files\"}";
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", path);
        result.put("branch", branch);
        result.put("content", content);
        result.put("readOnly", GitHubFlow.skipFromCommit(path));
        return JsonUtil.toJson(result);
    }

    private String handleWorkspaceSave(String projectId, String body, HttpServletResponse resp) {
        ProjectRecord project = store != null ? store.getProject(projectId).orElse(null) : null;
        if (project == null) {
            if (resp != null) resp.setStatus(404);
            return "{\"error\":\"Project not found\"}";
        }
        if (gitHubClient == null) {
            if (resp != null) resp.setStatus(503);
            return "{\"error\":\"GitHub client not available\"}";
        }
        Map<?, ?> payload = (body != null && !body.trim().isEmpty())
                ? JsonUtil.fromJson(body, Map.class) : null;
        String branch = payload != null ? Objects.toString(payload.get("branch"), "").trim() : "";
        String path = payload != null ? Objects.toString(payload.get("path"), "").trim() : "";
        String content = payload != null ? Objects.toString(payload.get("content"), null) : null;
        if (branch.isEmpty() || path.isEmpty() || content == null) {
            if (resp != null) resp.setStatus(400);
            return "{\"error\":\"Missing required fields: branch, path, content\"}";
        }
        if (GitHubFlow.skipFromCommit(path)) {
            if (resp != null) resp.setStatus(400);
            return "{\"error\":\"fstab.yaml is not edited by Modernizer\"}";
        }
        GitHubClient client = clientFor(project);
        GeneratedFileRecord file = new GeneratedFileRecord(
                UUID.randomUUID().toString(),
                projectId,
                store.getLatestJob(projectId).map(JobRecord::getId).orElse("workspace"),
                path,
                path.endsWith(".css") ? "BLOCK_CSS" : path.endsWith(".md") ? "SECTION_MD" : "BLOCK_JS",
                content
        );
        client.commitFiles(branch, List.of(file), "chore: dashboard workspace edit " + path);
        if (store != null) {
            store.saveGeneratedFile(file);
        }
        String refreshed = client.getFileContent(branch, path);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("path", path);
        result.put("branch", branch);
        result.put("content", refreshed != null ? refreshed : content);
        result.put("committed", true);
        return JsonUtil.toJson(result);
    }

    private String handleWorkspaceDelete(String projectId, String body, HttpServletResponse resp) {
        ProjectRecord project = store != null ? store.getProject(projectId).orElse(null) : null;
        if (project == null) {
            if (resp != null) resp.setStatus(404);
            return "{\"error\":\"Project not found\"}";
        }
        if (gitHubClient == null) {
            if (resp != null) resp.setStatus(503);
            return "{\"error\":\"GitHub client not available\"}";
        }
        Map<?, ?> payload = (body != null && !body.trim().isEmpty())
                ? JsonUtil.fromJson(body, Map.class) : null;
        String branch = payload != null ? Objects.toString(payload.get("branch"), "").trim() : "";
        String path = payload != null ? Objects.toString(payload.get("path"), "").trim() : "";
        if (branch.isEmpty() || path.isEmpty()) {
            if (resp != null) resp.setStatus(400);
            return "{\"error\":\"Missing required fields: branch, path\"}";
        }
        if (GitHubFlow.skipFromCommit(path)) {
            if (resp != null) resp.setStatus(400);
            return "{\"error\":\"fstab.yaml is not edited by Modernizer\"}";
        }

        // 1. Delete locally from eds/<projectId>/
        if (localEdsRepo != null) {
            try {
                localEdsRepo.deleteProjectFile(projectId, path);
            } catch (Exception e) {
                LOG.debug("Could not delete local file {}: {}", path, e.getMessage());
            }
        }

        // 2. Delete from store job records if present
        if (store != null) {
            store.getLatestJob(projectId).ifPresent(j -> {
                store.deleteGeneratedFile(j.getId(), path);
            });
        }

        // 3. Delete from GitHub branch if GitHub client available
        try {
            GitHubClient client = clientFor(project);
            if (client != null) {
                client.deleteFile(branch, path);
            }
        } catch (Exception e) {
            LOG.warn("GitHub delete for '{}' on '{}' failed or skipped: {}", path, branch, e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("deleted", true);
        result.put("path", path);
        result.put("branch", branch);
        return JsonUtil.toJson(result);
    }
}
