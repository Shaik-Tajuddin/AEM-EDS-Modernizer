package com.adobe.aem.modernizer.ai.chat;

import com.adobe.aem.modernizer.agents.Orchestrator;
import com.adobe.aem.modernizer.connectors.GitHubClient;
import com.adobe.aem.modernizer.connectors.GitHubFlow;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.JobRecord;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Bounded tools for Agent Chat over the Modernizer tool workspace and EDS GitHub repo.
 */
public class ChatToolRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ChatToolRegistry.class);

    private final Store store;
    private final Orchestrator orchestrator;
    private final GitHubClient gitHubClient;
    private final Path toolRepoRoot;

    public ChatToolRegistry(Store store, Orchestrator orchestrator, GitHubClient gitHubClient) {
        this.store = store;
        this.orchestrator = orchestrator;
        this.gitHubClient = gitHubClient;
        Path root = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path alt = Path.of("D:/eds personal/AEM-EDS-Modernizer");
        this.toolRepoRoot = Files.isDirectory(alt) ? alt : root;
    }

    public List<Map<String, Object>> toolDescriptors() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(tool("project_status", "Summarize project, latest job, and inventory counts"));
        tools.add(tool("list_events", "List recent job events for the project"));
        tools.add(tool("list_blocks", "List generated or EDS blocks/ paths"));
        tools.add(tool("read_tool_file", "Read a file from the Modernizer tool repo (relative path)"));
        tools.add(tool("read_eds_file", "Read a file from the EDS GitHub repo"));
        tools.add(tool("search_tool_repo", "Search filenames under the tool repo"));
        tools.add(tool("check_ci_status", "Check the latest GitHub Actions CI workflow run status, conclusion, commit SHA, and URL"));
        tools.add(tool("get_ci_logs", "Fetch the CI workflow run error logs and build failure details"));
        tools.add(tool("list_changed_files", "List files changed on the feature branch compared to target Git branch"));
        tools.add(tool("run_dry_run", "Start a dry-run migration job"));
        tools.add(tool("run_migrate", "Start a full migrate job"));
        return tools;
    }

    private static Map<String, Object> tool(String name, String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("description", description);
        return m;
    }

    public String execute(String projectId, String toolName, Map<String, Object> args) {
        if (toolName == null || toolName.isBlank()) {
            return error("Missing tool name");
        }
        String name = toolName.trim().toLowerCase(Locale.ROOT);
        try {
            switch (name) {
                case "project_status":
                    return projectStatus(projectId);
                case "list_events":
                    return listEvents(projectId);
                case "list_blocks":
                    return listBlocks(projectId);
                case "read_tool_file":
                    return readToolFile(str(args, "path"));
                case "read_eds_file":
                    return readEdsFile(projectId, str(args, "path"));
                case "search_tool_repo":
                    return searchToolRepo(str(args, "query"));
                case "check_ci_status":
                    return checkCiStatus(projectId, str(args, "branch"));
                case "get_ci_logs":
                    return getCiLogs(projectId, str(args, "runId"));
                case "list_changed_files":
                    return listChangedFiles(projectId, str(args, "branch"));
                case "run_dry_run":
                    return runJob(projectId, true);
                case "run_migrate":
                    return runJob(projectId, false);
                default:
                    return error("Unknown tool: " + toolName);
            }
        } catch (Exception e) {
            LOG.warn("Chat tool {} failed: {}", toolName, e.getMessage());
            return error(e.getMessage());
        }
    }

    private String projectStatus(String projectId) {
        Optional<ProjectRecord> p = store.getProject(projectId);
        if (p.isEmpty()) {
            return error("Project not found: " + projectId);
        }
        ProjectRecord pr = p.get();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", pr.getId());
        out.put("name", pr.getName());
        out.put("aiProvider", pr.getAiProvider());
        out.put("aiModel", pr.getAiModel());
        out.put("contentRoot", pr.getContentRoot());
        out.put("edsGitRepoUrl", pr.getEdsGitRepoUrl());
        store.getLatestJob(projectId).ifPresent(j -> {
            out.put("latestJobId", j.getId());
            out.put("latestJobState", j.getState());
            out.put("latestJobMode", j.getMode());
        });
        store.getLatestJob(projectId).flatMap(j -> store.getInventory(j.getId())).ifPresent(inv -> {
            out.put("pages", inv.getPages() != null ? inv.getPages().size() : 0);
            out.put("components", inv.getComponents() != null ? inv.getComponents().size() : 0);
        });
        return JsonUtil.toJson(out);
    }

    private String listEvents(String projectId) {
        Optional<JobRecord> latest = store.getLatestJob(projectId);
        if (latest.isEmpty()) {
            return "{\"events\":[]}";
        }
        List<JobEventRecord> events = store.getEvents(latest.get().getId());
        List<Map<String, Object>> rows = new ArrayList<>();
        int from = Math.max(0, events.size() - 40);
        for (int i = from; i < events.size(); i++) {
            JobEventRecord e = events.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", e.getAgent());
            row.put("message", e.getMessage());
            rows.add(row);
        }
        return JsonUtil.toJson(Map.of("events", rows));
    }

    private String listBlocks(String projectId) {
        List<String> blocks = new ArrayList<>();
        store.getLatestJob(projectId).ifPresent(job -> {
            for (GeneratedFileRecord f : store.getGeneratedFiles(job.getId())) {
                String path = f.getPath();
                if (path != null && path.startsWith("blocks/")) {
                    String rest = path.substring("blocks/".length());
                    int slash = rest.indexOf('/');
                    String name = slash > 0 ? rest.substring(0, slash) : rest;
                    if (!name.isBlank() && !blocks.contains(name)) {
                        blocks.add(name);
                    }
                }
            }
        });
        ProjectRecord pr = store.getProject(projectId).orElse(null);
        if (gitHubClient != null && pr != null && pr.getEdsGitRepoUrl() != null) {
            try {
                GitHubClient gh = GitHubFlow.clientFor(gitHubClient, pr);
                String ref = pr.getEdsBranch() != null ? pr.getEdsBranch() : gh.getDefaultBranch();
                for (String path : gh.listFilePaths(ref, "blocks/")) {
                    if (path == null || !path.startsWith("blocks/")) continue;
                    String rest = path.substring("blocks/".length());
                    int slash = rest.indexOf('/');
                    String name = slash > 0 ? rest.substring(0, slash) : rest;
                    if (!name.isBlank() && !blocks.contains(name)) {
                        blocks.add(name);
                    }
                }
            } catch (Exception e) {
                LOG.debug("EDS list blocks failed: {}", e.getMessage());
            }
        }
        return JsonUtil.toJson(Map.of("blocks", blocks));
    }

    private String readToolFile(String relPath) throws Exception {
        if (relPath == null || relPath.isBlank() || relPath.contains("..")) {
            return error("Invalid path");
        }
        Path target = toolRepoRoot.resolve(relPath).normalize();
        if (!target.startsWith(toolRepoRoot) || !Files.isRegularFile(target)) {
            return error("File not found in tool repo: " + relPath);
        }
        String content = Files.readString(target, StandardCharsets.UTF_8);
        if (content.length() > 20000) {
            content = content.substring(0, 20000) + "\n…(truncated)";
        }
        return JsonUtil.toJson(Map.of("path", relPath, "content", content));
    }

    private String readEdsFile(String projectId, String relPath) {
        if (relPath == null || relPath.isBlank() || relPath.contains("..")) {
            return error("Invalid path");
        }
        ProjectRecord pr = store.getProject(projectId).orElse(null);
        if (pr == null || gitHubClient == null) {
            return error("EDS GitHub client or project unavailable");
        }
        GitHubClient gh = GitHubFlow.clientFor(gitHubClient, pr);
        String ref = pr.getEdsBranch() != null ? pr.getEdsBranch() : gh.getDefaultBranch();
        String content = gh.getFileContent(ref, relPath);
        if (content == null) {
            return error("File not found in EDS repo: " + relPath);
        }
        if (content.length() > 20000) {
            content = content.substring(0, 20000) + "\n…(truncated)";
        }
        return JsonUtil.toJson(Map.of("path", relPath, "content", content));
    }

    private String searchToolRepo(String query) throws Exception {
        if (query == null || query.isBlank()) {
            return error("Missing query");
        }
        String q = query.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<>();
        try (var stream = Files.walk(toolRepoRoot, 6)) {
            stream.filter(Files::isRegularFile)
                    .map(p -> toolRepoRoot.relativize(p).toString().replace('\\', '/'))
                    .filter(p -> !p.contains("node_modules") && !p.contains(".git") && !p.contains("target/"))
                    .filter(p -> p.toLowerCase(Locale.ROOT).contains(q))
                    .limit(40)
                    .forEach(hits::add);
        }
        return JsonUtil.toJson(Map.of("hits", hits));
    }

    private String runJob(String projectId, boolean dryRun) throws Exception {
        if (orchestrator == null) {
            return error("Orchestrator unavailable");
        }
        ProjectRecord pr = store.getProject(projectId).orElse(null);
        if (pr == null) {
            return error("Project not found: " + projectId);
        }
        JobRecord job = dryRun
                ? orchestrator.runDryRun(pr, "chat-agent")
                : orchestrator.runMigration(pr, "chat-agent");
        return JsonUtil.toJson(Map.of(
                "started", true,
                "dryRun", dryRun,
                "jobId", job != null ? job.getId() : "",
                "state", job != null ? String.valueOf(job.getState()) : ""
        ));
    }

    private String checkCiStatus(String projectId, String branchArg) {
        ProjectRecord pr = store.getProject(projectId).orElse(null);
        if (pr == null || gitHubClient == null) {
            return error("Project or GitHub client unavailable");
        }
        GitHubClient gh = GitHubFlow.clientFor(gitHubClient, pr);
        String branch = (branchArg != null && !branchArg.isBlank()) ? branchArg : GitHubFlow.featureBranch(projectId);
        Map<String, Object> run = gh.getLatestWorkflowRun(branch);
        if (run == null) {
            return JsonUtil.toJson(Map.of("branch", branch, "status", "NO_RUNS_FOUND", "message", "No GitHub Actions workflow runs found for branch: " + branch));
        }
        Map<String, Object> out = new LinkedHashMap<>(run);
        out.put("branch", branch);
        out.put("isFailure", "failure".equalsIgnoreCase(String.valueOf(run.get("conclusion"))));
        out.put("isSuccess", "success".equalsIgnoreCase(String.valueOf(run.get("conclusion"))));
        return JsonUtil.toJson(out);
    }

    private String getCiLogs(String projectId, String runIdArg) {
        ProjectRecord pr = store.getProject(projectId).orElse(null);
        if (pr == null || gitHubClient == null) {
            return error("Project or GitHub client unavailable");
        }
        GitHubClient gh = GitHubFlow.clientFor(gitHubClient, pr);
        String runId = runIdArg;
        if (runId == null || runId.isBlank()) {
            String branch = GitHubFlow.featureBranch(projectId);
            Map<String, Object> run = gh.getLatestWorkflowRun(branch);
            if (run != null && run.get("runId") != null) {
                runId = String.valueOf(run.get("runId"));
            }
        }
        if (runId == null || runId.isBlank()) {
            return error("No CI workflow runId found to retrieve logs");
        }
        String rawLogs = gh.getWorkflowRunLogs(runId);
        if (rawLogs == null || rawLogs.isBlank()) {
            return JsonUtil.toJson(Map.of("runId", runId, "logs", "No logs available or workflow is still running"));
        }
        // Extract key error lines
        StringBuilder keyErrors = new StringBuilder();
        String[] lines = rawLogs.split("\\r?\\n");
        for (String line : lines) {
            String l = line.toLowerCase(Locale.ROOT);
            if (l.contains("error") || l.contains("failed") || l.contains("err!") || l.contains("fatal") || l.contains("exit code")) {
                if (keyErrors.length() < 8000) {
                    keyErrors.append(line).append("\n");
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("runId", runId);
        out.put("summaryErrors", keyErrors.length() > 0 ? keyErrors.toString() : "No explicit 'error' lines found in log stream");
        out.put("fullLogLength", rawLogs.length());
        out.put("tailLogs", rawLogs.length() > 4000 ? rawLogs.substring(rawLogs.length() - 4000) : rawLogs);
        return JsonUtil.toJson(out);
    }

    private String listChangedFiles(String projectId, String branchArg) {
        ProjectRecord pr = store.getProject(projectId).orElse(null);
        if (pr == null || gitHubClient == null) {
            return error("Project or GitHub client unavailable");
        }
        GitHubClient gh = GitHubFlow.clientFor(gitHubClient, pr);
        String branch = (branchArg != null && !branchArg.isBlank()) ? branchArg : GitHubFlow.featureBranch(projectId);
        String base = (pr.getEdsBranch() != null && !pr.getEdsBranch().isBlank()) ? pr.getEdsBranch() : gh.getDefaultBranch();
        List<Map<String, Object>> files = gh.listChangedFiles(base, branch);
        return JsonUtil.toJson(Map.of(
                "baseBranch", base,
                "featureBranch", branch,
                "count", files != null ? files.size() : 0,
                "files", files != null ? files : List.of()
        ));
    }

    private static String str(Map<String, Object> args, String key) {
        if (args == null || args.get(key) == null) {
            return null;
        }
        return String.valueOf(args.get(key));
    }

    private static String error(String message) {
        return JsonUtil.toJson(Map.of("ok", false, "error", message != null ? message : "error"));
    }
}
