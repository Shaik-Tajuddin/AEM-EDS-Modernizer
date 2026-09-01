package com.adobe.aem.modernizer.connectors;

import com.adobe.aem.modernizer.ai.secret.SecretProvider;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import com.adobe.aem.modernizer.ssrf.UrlGuard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Real GitHub REST / Git Data API implementation of {@link GitHubClient}.
 *
 * <p>Per-project repository settings (repo URL, target branch) are read from the
 * {@link com.adobe.aem.modernizer.persistence.model.ProjectRecord} saved from the
 * dashboard home page — see {@link GitHubClientProvider}. The OSGi configuration
 * below only supplies fallbacks / global settings.</p>
 *
 * <p>Authentication uses a Personal Access Token (repo scope) resolved through a
 * {@link SecretProvider} reference ("env:GITHUB_TOKEN" style).</p>
 */
@Component(service = GitHubClient.class, immediate = true)
@Designate(ocd = RealGitHubClient.Config.class)
public class RealGitHubClient implements GitHubClient {

    @ObjectClassDefinition(name = "AEM EDS Modernizer - Real GitHub Client",
            description = "Commits generated EDS blocks directly to GitHub")
    @interface Config {
        @AttributeDefinition(name = "Fallback Repository URL",
                description = "Used only when the project record has no EDS Git Repo URL")
        String repoUrl() default "";

        @AttributeDefinition(name = "Token secret reference",
                description = "Secret reference for the PAT, e.g. env:GITHUB_TOKEN (repo scope required)")
        String tokenRef() default "env:GITHUB_TOKEN";

        @AttributeDefinition(name = "Fallback default branch",
                description = "Used only when the project record has no EDS Branch")
        String defaultBranch() default "main";

        @AttributeDefinition(name = "Enabled", description = "When false, fall back to the mock client")
        boolean enabled() default true;

        @AttributeDefinition(name = "API base URL", description = "GitHub API root (or GHES endpoint)")
        String apiBase() default "https://api.github.com";
    }

    private static final Logger LOG = LoggerFactory.getLogger(RealGitHubClient.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .build();

    private volatile String tokenRef = "env:GITHUB_TOKEN";
    private volatile String repoUrl = "";
    private volatile String owner;
    private volatile String repo;
    private volatile String apiBase = "https://api.github.com";
    private volatile String defaultBranch = "main";
    private volatile boolean enabled = true;
    private transient String staticToken;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient SecretProvider secretProvider;

    @Activate
    @Modified
    public void activate(Config config) {
        if (config != null) {
            this.repoUrl = config.repoUrl() != null ? config.repoUrl().trim() : "";
            this.apiBase = trimTrailingSlash(config.apiBase());
            this.defaultBranch = (config.defaultBranch() == null || config.defaultBranch().isBlank()) ? "main" : config.defaultBranch().trim();
            this.tokenRef = config.tokenRef();
            this.enabled = config.enabled();
            if (!this.repoUrl.isEmpty()) {
                parseOwnerRepo(this.repoUrl);
            }
        }
        LOG.info("RealGitHubClient activated (fallbackRepo='{}', defaultBranch='{}', enabled={})",
                this.repoUrl.isEmpty() ? "<per-project>" : this.repoUrl, this.defaultBranch, enabled);
    }

    public RealGitHubClient() {
        this("", null, "https://api.github.com", "main", "env:GITHUB_TOKEN", true);
    }

    public RealGitHubClient(Config config) {
        this(config.repoUrl(), null, config.apiBase(), config.defaultBranch(),
                config.tokenRef(), config.enabled());
    }

    /** Standalone/testing constructor. */
    public RealGitHubClient(String repoUrl, String token) {
        this(repoUrl, token, "https://api.github.com", "main", "env:GITHUB_TOKEN", true);
    }

    public RealGitHubClient(String repoUrl, String token, String apiBase, String defaultBranch) {
        this(repoUrl, token, apiBase, defaultBranch, "env:GITHUB_TOKEN", true);
    }

    public RealGitHubClient(String repoUrl, String token, String apiBase,
                            String defaultBranch, String tokenRef, boolean enabled) {
        this.repoUrl = repoUrl == null ? "" : repoUrl.trim();
        this.apiBase = trimTrailingSlash(apiBase);
        this.defaultBranch = (defaultBranch == null || defaultBranch.isBlank()) ? "main" : defaultBranch.trim();
        this.tokenRef = tokenRef != null ? tokenRef : "env:GITHUB_TOKEN";
        this.enabled = enabled;
        this.staticToken = token;
        if (!this.repoUrl.isEmpty()) {
            parseOwnerRepo(this.repoUrl);
        }
    }

    /**
     * Returns a client bound to the given project's home-page settings
     * ({@code edsGitRepoUrl}, {@code edsBranch}), falling back to this client's
     * configured values when the project does not specify them.
     */
    public RealGitHubClient forProject(com.adobe.aem.modernizer.persistence.model.ProjectRecord project) {
        String projRepo = project != null && project.getEdsGitRepoUrl() != null
                ? project.getEdsGitRepoUrl().trim() : "";
        String projBranch = project != null && project.getEdsBranch() != null
                ? project.getEdsBranch().trim() : "";
        RealGitHubClient client = new RealGitHubClient(
                projRepo.isEmpty() ? this.repoUrl : projRepo,
                this.staticToken,
                this.apiBase,
                projBranch.isEmpty() ? this.defaultBranch : projBranch,
                this.tokenRef,
                this.enabled);
        client.secretProvider = this.secretProvider;
        return client;
    }

    private void ensureOwnerRepo() {
        if (owner == null || repo == null) {
            if (repoUrl.isEmpty()) {
                throw new IllegalStateException(
                        "No GitHub repository configured. Set 'EDS Git Repository URL' on the project in the dashboard home page.");
            }
            parseOwnerRepo(repoUrl);
        }
    }

    private synchronized void parseOwnerRepo(String url) {
        String[] parts = splitOwnerRepo(url);
        this.owner = parts[0];
        this.repo = parts[1];
    }

    // ------------------------------------------------------------------
    // GitHubClient API
    // ------------------------------------------------------------------

    @Override
    public boolean testConnection() {
        try {
            ensureOwnerRepo();
            JsonNode repoJson = get("/repos/" + owner + "/" + repo);
            boolean ok = repoJson.has("full_name");
            LOG.info("GitHub connection {} for {}", ok ? "OK" : "FAILED", repoUrl);
            return ok;
        } catch (IOException | RuntimeException e) {
            LOG.error("GitHub connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getRepoUrl() {
        return repoUrl;
    }

    @Override
    public String getDefaultBranch() {
        return defaultBranch;
    }

    @Override
    public String getRepositoryDefaultBranch() {
        try {
            ensureOwnerRepo();
            JsonNode repoJson = get("/repos/" + owner + "/" + repo);
            String remoteDefault = repoJson.path("default_branch").asText("");
            if (!remoteDefault.isBlank()) {
                return remoteDefault;
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not read repository default branch: {}", e.getMessage());
        }
        return getDefaultBranch();
    }

    @Override
    public boolean branchExists(String branch) {
        try {
            ensureOwnerRepo();
            get("/repos/" + owner + "/" + repo + "/branches/" + urlEncode(branch));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void createBranch(String branch) {
        createBranch(branch, defaultBranch);
    }

    /** Creates a branch from the given source branch (Git Data API). */
    public void createBranch(String branch, String sourceBranch) {
        try {
            if (branchExists(branch)) {
                LOG.info("Branch '{}' already exists", branch);
                return;
            }
            String sha = getBranchHeadSha(sourceBranch);
            ObjectNode body = mapper.createObjectNode();
            body.put("ref", "refs/heads/" + branch);
            body.put("sha", sha);
            post("/repos/" + owner + "/" + repo + "/git/refs", body);
            LOG.info("Created branch '{}' from '{}'", branch, sourceBranch);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed to create branch '" + branch + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void commitFiles(String branch, List<GeneratedFileRecord> files, String commitMessage) {
        commitFilesInternal(branch, files, commitMessage, false);
    }

    /** Restores {@code fstab.yaml} from the EDS base branch; normal commits skip that path. */
    public void commitPathAllowingFstab(String branch, GeneratedFileRecord file, String commitMessage) {
        if (file == null) {
            return;
        }
        commitFilesInternal(branch, List.of(file), commitMessage, true);
    }

    private void commitFilesInternal(String branch, List<GeneratedFileRecord> files, String commitMessage,
                                     boolean allowFstab) {
        if (files == null || files.isEmpty()) {
            LOG.warn("commitFiles called with no files; skipping");
            return;
        }
        ensureOwnerRepo();
        try {
            String baseSha = getBranchHeadSha(branchExists(branch) ? branch : defaultBranch);
            if (!branchExists(branch)) {
                createBranch(branch);
                baseSha = getBranchHeadSha(defaultBranch);
            }
            String baseTreeSha = getCommitTreeSha(baseSha);

            // 1. Create blobs
            ArrayNode treeItems = mapper.createArrayNode();
            Map<String, String> createdBlobs = new LinkedHashMap<>();
            for (GeneratedFileRecord file : files) {
                String path = normalizePath(file.getPath());
                if (path == null || file.getContent() == null) {
                    continue;
                }
                if (!allowFstab && GitHubFlow.skipFromCommit(path)) {
                    continue;
                }
                String blobSha = createBlob(file.getContent());
                createdBlobs.put(path, blobSha);
                ObjectNode item = treeItems.addObject();
                item.put("path", path);
                item.put("mode", "100644");
                item.put("type", "blob");
                item.put("sha", blobSha);
            }
            if (createdBlobs.isEmpty()) {
                throw new IllegalStateException("No valid files to commit");
            }

            // 2. Create tree
            ObjectNode treeBody = mapper.createObjectNode();
            treeBody.set("tree", treeItems);
            treeBody.put("base_tree", baseTreeSha);
            String newTreeSha = post("/repos/" + owner + "/" + repo + "/git/trees", treeBody)
                    .path("sha").asText();

            // 3. Create commit
            ObjectNode commitBody = mapper.createObjectNode();
            commitBody.put("message", commitMessage);
            commitBody.put("tree", newTreeSha);
            ArrayNode parents = commitBody.putArray("parents");
            parents.add(baseSha);
            String commitSha = post("/repos/" + owner + "/" + repo + "/git/commits", commitBody)
                    .path("sha").asText();

            // 4. Update ref
            ObjectNode refBody = mapper.createObjectNode();
            refBody.put("sha", commitSha);
            refBody.put("force", false);
            patch("/repos/" + owner + "/" + repo + "/git/refs/heads/" + urlEncode(branch), refBody);

            LOG.info("Committed {} files to {}/{} as commit {}", createdBlobs.size(), owner, branch, commitSha);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed to commit files to GitHub: " + e.getMessage(), e);
        }
    }

    @Override
    public String createPullRequest(String title, String body, String headBranch, String baseBranch) {
        ensureOwnerRepo();
        // 1. Check if PR already exists for this branch
        String existing = findExistingPullRequest(headBranch);
        if (existing != null && !existing.isBlank()) {
            LOG.info("Pull request already exists for branch {}: {}", headBranch, existing);
            return existing;
        }

        try {
            ObjectNode pr = mapper.createObjectNode();
            pr.put("title", title);
            pr.put("head", headBranch);
            pr.put("base", baseBranch);
            pr.put("body", body == null ? "" : body);
            JsonNode resp = post("/repos/" + owner + "/" + repo + "/pulls", pr);
            String url = resp.path("html_url").asText();
            LOG.info("Created PR: {}", url);
            return url;
        } catch (IOException | RuntimeException e) {
            // Check if GitHub threw a 422 because PR already exists
            String fallback = findExistingPullRequest(headBranch);
            if (fallback != null && !fallback.isBlank()) {
                LOG.info("Resolved existing PR after 422 for branch {}: {}", headBranch, fallback);
                return fallback;
            }
            throw new IllegalStateException("Failed to create pull request: " + e.getMessage(), e);
        }
    }

    /**
     * Looks up any existing pull request URL for the given head branch across all PR states.
     */
    public String findExistingPullRequest(String headBranch) {
        if (headBranch == null || headBranch.isBlank()) {
            return null;
        }
        ensureOwnerRepo();
        try {
            // Check with owner:headBranch
            String headFilter = owner + ":" + headBranch.trim();
            JsonNode resp = get("/repos/" + owner + "/" + repo + "/pulls?head=" + urlEncode(headFilter) + "&state=all");
            if (resp.isArray() && !resp.isEmpty()) {
                String htmlUrl = resp.get(0).path("html_url").asText();
                if (htmlUrl != null && !htmlUrl.isBlank()) {
                    return htmlUrl;
                }
            }
            // Check with headBranch directly
            resp = get("/repos/" + owner + "/" + repo + "/pulls?head=" + urlEncode(headBranch.trim()) + "&state=all");
            if (resp.isArray() && !resp.isEmpty()) {
                String htmlUrl = resp.get(0).path("html_url").asText();
                if (htmlUrl != null && !htmlUrl.isBlank()) {
                    return htmlUrl;
                }
            }
            // Secondary scan over recent open PRs
            resp = get("/repos/" + owner + "/" + repo + "/pulls?state=open&per_page=30");
            if (resp.isArray()) {
                for (JsonNode item : resp) {
                    String ref = item.path("head").path("ref").asText();
                    if (headBranch.trim().equalsIgnoreCase(ref)) {
                        return item.path("html_url").asText();
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not lookup existing pull request for branch {}: {}", headBranch, e.getMessage());
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> listChangedFiles(String baseBranch, String headBranch) {
        ensureOwnerRepo();
        List<Map<String, Object>> result = new ArrayList<>();
        String base = (baseBranch == null || baseBranch.isBlank()) ? defaultBranch : baseBranch.trim();
        try {
            JsonNode resp = get("/repos/" + owner + "/" + repo + "/compare/"
                    + urlEncode(base) + "..." + urlEncode(headBranch));
            JsonNode files = resp.path("files");
            if (files.isArray()) {
                for (JsonNode f : files) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("filename", f.path("filename").asText());
                    entry.put("status", f.path("status").asText());
                    entry.put("additions", f.path("additions").asInt(0));
                    entry.put("deletions", f.path("deletions").asInt(0));
                    entry.put("patch", f.path("patch").asText(""));
                    result.add(entry);
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not list changed files between '{}' and '{}': {}", base, headBranch, e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getLatestWorkflowRun(String branch) {
        ensureOwnerRepo();
        try {
            JsonNode resp = get("/repos/" + owner + "/" + repo + "/actions/runs?branch="
                    + urlEncode(branch) + "&per_page=1");
            JsonNode runs = resp.path("workflow_runs");
            if (!runs.isArray() || runs.isEmpty()) {
                return null;
            }
            JsonNode run = runs.get(0);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("runId", run.path("id").asText());
            result.put("name", run.path("name").asText());
            result.put("status", run.path("status").asText());
            result.put("conclusion", run.path("conclusion").isNull() ? null : run.path("conclusion").asText());
            result.put("htmlUrl", run.path("html_url").asText());
            result.put("createdAt", run.path("created_at").asText());
            result.put("updatedAt", run.path("updated_at").asText());
            return result;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not fetch latest workflow run for branch '{}': {}", branch, e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, Object> dispatchWorkflow(String ref, String workflowFile, Map<String, String> inputs) {
        ensureOwnerRepo();
        String file = (workflowFile == null || workflowFile.isBlank())
                ? GitHubFlow.NPM_WORKFLOW_FILE : workflowFile.trim();
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("ref", ref);
            ObjectNode in = body.putObject("inputs");
            if (inputs != null) {
                inputs.forEach(in::put);
            }
            post("/repos/" + owner + "/" + repo + "/actions/workflows/" + urlEncode(file) + "/dispatches", body);

            String runId = null;
            Map<String, Object> found = null;
            for (int i = 0; i < 12 && found == null; i++) {
                sleepQuietly(500);
                JsonNode resp = get("/repos/" + owner + "/" + repo + "/actions/workflows/"
                        + urlEncode(file) + "/runs?event=workflow_dispatch&per_page=5");
                JsonNode runs = resp.path("workflow_runs");
                if (runs.isArray()) {
                    for (JsonNode run : runs) {
                        if (ref != null && ref.equals(run.path("head_branch").asText())) {
                            found = workflowRunMap(run);
                            runId = run.path("id").asText();
                            break;
                        }
                    }
                }
            }
            if (found == null) {
                found = new LinkedHashMap<>();
                found.put("status", "queued");
                found.put("conclusion", null);
                found.put("htmlUrl", null);
            }
            if (runId != null) {
                found.put("runId", runId);
            }
            found.put("branch", ref);
            found.put("workflowFile", file);
            return found;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed to dispatch workflow '" + file + "' on '" + ref + "': " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getWorkflowRun(String runId) {
        ensureOwnerRepo();
        if (runId == null || runId.isBlank()) {
            return null;
        }
        try {
            JsonNode run = get("/repos/" + owner + "/" + repo + "/actions/runs/" + urlEncode(runId.trim()));
            Map<String, Object> result = workflowRunMap(run);
            result.put("runId", run.path("id").asText(runId));
            return result;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not fetch workflow run '{}': {}", runId, e.getMessage());
            return null;
        }
    }

    @Override
    public String getWorkflowRunLogs(String runId) {
        ensureOwnerRepo();
        if (runId == null || runId.isBlank()) {
            return "";
        }
        try {
            JsonNode jobsResp = get("/repos/" + owner + "/" + repo + "/actions/runs/"
                    + urlEncode(runId.trim()) + "/jobs");
            JsonNode jobs = jobsResp.path("jobs");
            StringBuilder out = new StringBuilder();
            if (jobs.isArray()) {
                for (JsonNode job : jobs) {
                    out.append("== ").append(job.path("name").asText("job")).append(" (")
                            .append(job.path("status").asText()).append("/").append(job.path("conclusion").asText(""))
                            .append(") ==\n");
                    String jobId = job.path("id").asText();
                    if (!jobId.isEmpty()) {
                        try {
                            String logs = getText("/repos/" + owner + "/" + repo + "/actions/jobs/"
                                    + urlEncode(jobId) + "/logs");
                            if (logs != null && !logs.isBlank()) {
                                out.append(logs);
                                if (!logs.endsWith("\n")) {
                                    out.append('\n');
                                }
                            }
                        } catch (IOException e) {
                            JsonNode steps = job.path("steps");
                            if (steps.isArray()) {
                                for (JsonNode step : steps) {
                                    out.append("  - ").append(step.path("name").asText())
                                            .append(": ").append(step.path("conclusion").asText("pending"))
                                            .append('\n');
                                }
                            }
                        }
                    }
                }
            }
            String text = out.toString();
            return text.length() > 32000 ? text.substring(text.length() - 32000) : text;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not fetch logs for workflow run '{}': {}", runId, e.getMessage());
            return "";
        }
    }

    @Override
    public List<String> listFilePaths(String ref, String pathPrefix) {
        ensureOwnerRepo();
        List<String> out = new ArrayList<>();
        String prefix = pathPrefix == null ? "" : pathPrefix.replace('\\', '/');
        try {
            String branch = (ref == null || ref.isBlank()) ? defaultBranch : ref.trim();
            String commitSha = getBranchHeadSha(branch);
            String treeSha = getCommitTreeSha(commitSha);
            JsonNode tree = get("/repos/" + owner + "/" + repo + "/git/trees/"
                    + urlEncode(treeSha) + "?recursive=1");
            JsonNode items = tree.path("tree");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    if (!"blob".equals(item.path("type").asText())) {
                        continue;
                    }
                    String path = item.path("path").asText("");
                    if (!path.isBlank() && (prefix.isEmpty() || path.startsWith(prefix))) {
                        out.add(path);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not list files on '{}': {}", ref, e.getMessage());
        }
        if (out.isEmpty() && prefix.startsWith("blocks")) {
            out.addAll(listViaContents(ref, prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix));
        }
        return out;
    }

    private List<String> listViaContents(String ref, String dir) {
        List<String> out = new ArrayList<>();
        try {
            String encodedPath = java.net.URLEncoder.encode(dir.replace('\\', '/').replaceFirst("^/+", ""),
                    StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
            String q = (ref == null || ref.isBlank()) ? "" : ("?ref=" + urlEncode(ref.trim()));
            JsonNode node = get("/repos/" + owner + "/" + repo + "/contents/" + encodedPath + q);
            if (!node.isArray()) {
                return out;
            }
            for (JsonNode item : node) {
                String path = item.path("path").asText("");
                String type = item.path("type").asText("");
                if ("file".equals(type) && !path.isBlank()) {
                    out.add(path);
                } else if ("dir".equals(type) && !path.isBlank()) {
                    out.addAll(listViaContents(ref, path));
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not list directory '{}' on '{}': {}", dir, ref, e.getMessage());
        }
        return out;
    }

    @Override
    public void deleteFile(String branch, String path) {
        String cleanPath = normalizePath(path);
        if (cleanPath == null) return;
        ensureOwnerRepo();
        try {
            String encodedPath = java.net.URLEncoder.encode(cleanPath.replaceFirst("^/+", ""),
                    StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
            String targetBranch = (branch == null || branch.isBlank()) ? defaultBranch : branch.trim();
            String getUrl = "/repos/" + owner + "/" + repo + "/contents/" + encodedPath + "?ref=" + urlEncode(targetBranch);
            
            JsonNode fileNode;
            try {
                fileNode = get(getUrl);
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("404")) {
                    LOG.info("File '{}' not found on branch '{}' in GitHub; skipping remote delete", cleanPath, targetBranch);
                    return;
                }
                throw e;
            }
            String sha = fileNode.path("sha").asText("");
            if (sha.isBlank()) {
                LOG.info("Could not resolve SHA for file '{}' on branch '{}'; skipping delete", cleanPath, targetBranch);
                return;
            }
            ObjectNode del = mapper.createObjectNode();
            del.put("message", "chore: delete " + cleanPath + " via Modernizer workspace");
            del.put("sha", sha);
            del.put("branch", targetBranch);
            deleteJson("/repos/" + owner + "/" + repo + "/contents/" + encodedPath, del);
            LOG.info("Deleted '{}' on branch '{}'", cleanPath, targetBranch);
        } catch (Exception e) {
            LOG.warn("Failed to delete file '{}' on branch '{}': {}", cleanPath, branch, e.getMessage());
            throw new RuntimeException("Could not delete file " + cleanPath + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String getFileContent(String ref, String path) {
        ensureOwnerRepo();
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            String encodedPath = java.net.URLEncoder.encode(path.replace('\\', '/').replaceFirst("^/+", ""),
                    StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
            String q = (ref == null || ref.isBlank()) ? "" : ("?ref=" + urlEncode(ref.trim()));
            JsonNode node = get("/repos/" + owner + "/" + repo + "/contents/" + encodedPath + q);
            String encoded = node.path("content").asText("");
            if (encoded.isBlank()) {
                return null;
            }
            return new String(Base64.getDecoder().decode(encoded.replace("\n", "")), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not read '{}' on '{}': {}", path, ref, e.getMessage());
            return null;
        }
    }

    private static Map<String, Object> workflowRunMap(JsonNode run) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", run.path("name").asText());
        result.put("status", run.path("status").asText());
        result.put("conclusion", run.path("conclusion").isNull() ? null : run.path("conclusion").asText());
        result.put("htmlUrl", run.path("html_url").asText());
        result.put("createdAt", run.path("created_at").asText());
        result.put("updatedAt", run.path("updated_at").asText());
        result.put("headBranch", run.path("head_branch").asText());
        return result;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Owner/repo pair resolved from this client's configured repository URL. */
    public String[] ownerRepo() {
        ensureOwnerRepo();
        return new String[]{owner, repo};
    }

    // ------------------------------------------------------------------
    // Low-level helpers
    // ------------------------------------------------------------------

    private String token() {
        if (staticToken != null && !staticToken.isEmpty()) {
            return staticToken;
        }
        String ref = System.getenv("GITHUB_TOKEN");
        if (secretProvider != null) {
            String resolved = secretProvider.resolve("env:GITHUB_TOKEN");
            if (resolved != null && !resolved.isEmpty()) {
                ref = resolved;
            }
        }
        if (ref == null || ref.isEmpty()) {
            throw new IllegalStateException(
                    "GitHub PAT not configured. Set env GITHUB_TOKEN or OSGi tokenRef (repo scope required).");
        }
        return ref;
    }

    private java.net.http.HttpRequest.Builder baseRequest(String path) throws IOException {
        String url = apiBase + path;
        UrlGuard.validateUrl(url, false);
        return java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Authorization", "Bearer " + token())
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "aem-eds-modernizer")
                .timeout(java.time.Duration.ofSeconds(30));
    }

    private JsonNode get(String path) throws IOException {
        try {
            java.net.http.HttpResponse<String> resp = http.send(baseRequest(path).GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return readBody(resp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted: " + e.getMessage(), e);
        }
    }

    private String getText(String path) throws IOException {
        try {
            java.net.http.HttpResponse<String> resp = http.send(baseRequest(path).GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String text = resp.body() != null ? resp.body() : "";
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new IOException("GitHub API " + resp.statusCode() + ": " + truncate(text));
            }
            return text;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted: " + e.getMessage(), e);
        }
    }

    private JsonNode post(String path, ObjectNode body) throws IOException {
        try {
            byte[] bytes = mapper.writeValueAsBytes(body);
            java.net.http.HttpRequest req = baseRequest(path)
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();
            java.net.http.HttpResponse<String> resp = http.send(req,
                    java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return readBody(resp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted: " + e.getMessage(), e);
        }
    }

    private JsonNode deleteJson(String path, ObjectNode body) throws IOException {
        try {
            byte[] bytes = mapper.writeValueAsBytes(body);
            java.net.http.HttpRequest req = baseRequest(path)
                    .header("Content-Type", "application/json")
                    .method("DELETE", java.net.http.HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();
            java.net.http.HttpResponse<String> resp = http.send(req,
                    java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return readBody(resp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted: " + e.getMessage(), e);
        }
    }

    private JsonNode patch(String path, ObjectNode body) throws IOException {
        try {
            byte[] bytes = mapper.writeValueAsBytes(body);
            java.net.http.HttpRequest req = baseRequest(path)
                    .header("Content-Type", "application/json")
                    .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();
            java.net.http.HttpResponse<String> resp = http.send(req,
                    java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return readBody(resp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted: " + e.getMessage(), e);
        }
    }

    private JsonNode readBody(java.net.http.HttpResponse<String> resp) throws IOException {
        String text = resp.body() != null ? resp.body() : "";
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IOException("GitHub API " + resp.statusCode() + ": " + truncate(text));
        }
        return text.isEmpty() ? mapper.createObjectNode() : mapper.readTree(text);
    }

    private String getBranchHeadSha(String branch) throws IOException {
        return get("/repos/" + owner + "/" + repo + "/branches/" + urlEncode(branch))
                .path("commit").path("sha").asText();
    }

    private String getCommitTreeSha(String commitSha) throws IOException {
        return get("/repos/" + owner + "/" + repo + "/git/commits/" + commitSha)
                .path("tree").path("sha").asText();
    }

    private String createBlob(String content) throws IOException {
        ObjectNode blob = mapper.createObjectNode();
        blob.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
        blob.put("encoding", "base64");
        return post("/repos/" + owner + "/" + repo + "/git/blobs", blob).path("sha").asText();
    }

    private static String[] splitOwnerRepo(String repoUrl) {
        String cleaned = repoUrl.trim();
        if (cleaned.endsWith(".git")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        cleaned = trimTrailingSlash(cleaned);
        int slash = cleaned.lastIndexOf('/');
        int prevSlash = cleaned.lastIndexOf('/', slash - 1);
        if (slash < 0 || prevSlash < 0) {
            throw new IllegalArgumentException("Cannot parse owner/repo from URL: " + repoUrl);
        }
        return new String[]{cleaned.substring(prevSlash + 1, slash), cleaned.substring(slash + 1)};
    }

    private static String normalizePath(String path) {
        if (path == null) return null;
        String p = path.trim().replace('\\', '/');
        while (p.startsWith("/")) p = p.substring(1);
        return p.isEmpty() ? null : p;
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String trimTrailingSlash(String s) {
        return s != null && s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String truncate(String s) {
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
