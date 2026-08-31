package com.adobe.aem.modernizer.mock;

import com.adobe.aem.modernizer.connectors.GitHubClient;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock GitHub Client simulating branch creation, file commits, and Pull Requests.
 */
@Component(service = GitHubClient.class, immediate = true)
public class MockGitHubClient implements GitHubClient {

    private static final Logger LOG = LoggerFactory.getLogger(MockGitHubClient.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String repoUrl;
    private final Set<String> branches = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, List<GeneratedFileRecord>> branchFiles = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> workflowRuns = new ConcurrentHashMap<>();
    private int commitCount;
    private int prCount;
    private int workflowRunSeq;

    public MockGitHubClient() {
        this("https://github.com/company/wknd-eds");
    }

    public MockGitHubClient(String repoUrl) {
        this.repoUrl = (repoUrl != null && !repoUrl.trim().isEmpty()) ? repoUrl : "https://github.com/company/wknd-eds";
        branches.add("main");
    }

    @Override
    public boolean testConnection() {
        LOG.info("Mock GitHub connection check passed for {}", repoUrl);
        return true;
    }

    @Override
    public String getRepoUrl() {
        return repoUrl;
    }

    public List<String> listBranches() {
        return new ArrayList<>(branches);
    }

    @Override
    public boolean branchExists(String branch) {
        return branches.contains(branch);
    }

    @Override
    public void createBranch(String branch) {
        branches.add(branch);
        LOG.info("Created mock Git branch: {}", branch);
    }

    public void createBranch(String branch, String sourceBranch) {
        createBranch(branch);
    }

    @Override
    public void commitFiles(String branch, List<GeneratedFileRecord> files, String commitMessage) {
        if (files != null) {
            List<GeneratedFileRecord> kept = new ArrayList<>();
            for (GeneratedFileRecord file : files) {
                if (file != null && !com.adobe.aem.modernizer.connectors.GitHubFlow.skipFromCommit(file.getPath())) {
                    kept.add(file);
                }
            }
            branchFiles.computeIfAbsent(branch, k -> Collections.synchronizedList(new ArrayList<>()))
                    .addAll(kept);
            if (!kept.isEmpty()) {
                commitCount++;
            }
            LOG.info("Committed {} files to branch '{}': {}", kept.size(), branch, commitMessage);
        }
    }

    public int getCommitCount() {
        return commitCount;
    }

    public int getPrCount() {
        return prCount;
    }

    @Override
    public String createPullRequest(String title, String body, String headBranch, String baseBranch) {
        prCount++;
        String prUrl = repoUrl + "/pull/" + (RANDOM.nextInt(900) + 100);
        LOG.info("Created mock PR from {} to {}: {}", headBranch, baseBranch, prUrl);
        return prUrl;
    }

    @Override
    public List<Map<String, Object>> listChangedFiles(String baseBranch, String headBranch) {
        List<GeneratedFileRecord> files = branchFiles.get(headBranch);
        List<Map<String, Object>> result = new ArrayList<>();
        if (files != null) {
            for (GeneratedFileRecord file : files) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("filename", file.getPath());
                entry.put("status", "modified");
                entry.put("additions", 1);
                entry.put("deletions", 0);
                result.add(entry);
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> getLatestWorkflowRun(String branch) {
        if (!branches.contains(branch)) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", "mock-ci");
        result.put("status", "completed");
        result.put("conclusion", "success");
        result.put("htmlUrl", repoUrl + "/actions");
        result.put("createdAt", "");
        result.put("updatedAt", "");
        return result;
    }

    @Override
    public Map<String, Object> dispatchWorkflow(String ref, String workflowFile, Map<String, String> inputs) {
        String command = inputs != null ? String.valueOf(inputs.getOrDefault("command", "lint:fix")) : "lint:fix";
        String id = String.valueOf(++workflowRunSeq);
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("runId", id);
        run.put("status", "completed");
        run.put("conclusion", "success");
        run.put("htmlUrl", repoUrl + "/actions/runs/" + id);
        run.put("branch", ref);
        run.put("workflowFile", workflowFile);
        run.put("logs", "$ npm run " + command + "\n> mock " + command + " completed successfully\n");
        workflowRuns.put(id, run);
        LOG.info("Dispatched mock workflow {} on {} (run {})", workflowFile, ref, id);
        return new LinkedHashMap<>(run);
    }

    @Override
    public Map<String, Object> getWorkflowRun(String runId) {
        Map<String, Object> run = workflowRuns.get(runId);
        return run != null ? new LinkedHashMap<>(run) : null;
    }

    @Override
    public String getWorkflowRunLogs(String runId) {
        Map<String, Object> run = workflowRuns.get(runId);
        return run != null ? String.valueOf(run.getOrDefault("logs", "")) : "";
    }

    @Override
    public String getFileContent(String ref, String path) {
        List<GeneratedFileRecord> files = branchFiles.get(ref);
        if (files == null) {
            return null;
        }
        for (int i = files.size() - 1; i >= 0; i--) {
            GeneratedFileRecord file = files.get(i);
            if (file != null && path != null && path.equals(file.getPath())) {
                return file.getContent();
            }
        }
        return null;
    }

    @Override
    public void deleteFile(String branch, String path) {
        List<GeneratedFileRecord> files = branchFiles.get(branch);
        if (files == null) {
            return;
        }
        files.removeIf(file -> file != null && path != null && path.equals(file.getPath()));
        commitCount++;
        LOG.info("Deleted mock file '{}' from branch '{}'", path, branch);
    }
}
