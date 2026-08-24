package com.adobe.aem.modernizer.connectors;

import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;

import java.util.List;

/**
 * Connector interface for GitHub repo operations (branches, commits, PRs).
 */
public interface GitHubClient {
    boolean testConnection();
    String getRepoUrl();
    boolean branchExists(String branch);
    void createBranch(String branch);
    void commitFiles(String branch, List<GeneratedFileRecord> files, String commitMessage);
    String createPullRequest(String title, String body, String headBranch, String baseBranch);
}
