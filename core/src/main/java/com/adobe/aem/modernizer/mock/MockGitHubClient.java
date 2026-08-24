package com.adobe.aem.modernizer.mock;

import com.adobe.aem.modernizer.connectors.GitHubClient;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock GitHub Client simulating branch creation, file commits, and Pull Requests.
 */
@Component(service = GitHubClient.class, immediate = true)
public class MockGitHubClient implements GitHubClient {

    private static final Logger LOG = LoggerFactory.getLogger(MockGitHubClient.class);

    private String repoUrl = "https://github.com/company/wknd-eds";
    private final Set<String> branches = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, List<GeneratedFileRecord>> branchFiles = new ConcurrentHashMap<>();

    public MockGitHubClient() {
        branches.add("main");
    }

    public MockGitHubClient(String repoUrl) {
        this();
        this.repoUrl = repoUrl;
    }

    @Override
    public boolean testConnection() {
        return true;
    }

    @Override
    public String getRepoUrl() {
        return repoUrl;
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

    @Override
    public void commitFiles(String branch, List<GeneratedFileRecord> files, String commitMessage) {
        branchFiles.computeIfAbsent(branch, k -> Collections.synchronizedList(new ArrayList<>()))
                .addAll(files);
        LOG.info("Committed {} files to branch '{}': {}", files.size(), branch, commitMessage);
    }

    @Override
    public String createPullRequest(String title, String body, String headBranch, String baseBranch) {
        String prUrl = repoUrl + "/pull/" + (new Random().nextInt(900) + 100);
        LOG.info("Created mock PR from {} to {}: {}", headBranch, baseBranch, prUrl);
        return prUrl;
    }
}
