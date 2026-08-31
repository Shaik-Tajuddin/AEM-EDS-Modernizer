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
            branchFiles.computeIfAbsent(branch, k -> Collections.synchronizedList(new ArrayList<>()))
                    .addAll(files);
            LOG.info("Committed {} files to branch '{}': {}", files.size(), branch, commitMessage);
        }
    }

    @Override
    public String createPullRequest(String title, String body, String headBranch, String baseBranch) {
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
}
