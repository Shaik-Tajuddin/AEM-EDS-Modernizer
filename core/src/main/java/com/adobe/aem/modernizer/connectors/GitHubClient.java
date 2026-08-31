package com.adobe.aem.modernizer.connectors;

import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;

import java.util.List;
import java.util.Map;

/**
 * Connector interface for GitHub repo operations (branches, commits, PRs).
 */
public interface GitHubClient {
    boolean testConnection();
    String getRepoUrl();

    /** OSGi-configured default/base branch to use when a project does not specify its own EDS Branch. */
    default String getDefaultBranch() {
        return "main";
    }

    boolean branchExists(String branch);
    void createBranch(String branch);
    void commitFiles(String branch, List<GeneratedFileRecord> files, String commitMessage);
    String createPullRequest(String title, String body, String headBranch, String baseBranch);

    /** Lists files changed on {@code headBranch} relative to {@code baseBranch} (GitHub compare API). */
    List<Map<String, Object>> listChangedFiles(String baseBranch, String headBranch);

    /** Returns the most recent GitHub Actions workflow run for the given branch, or null if none/unavailable. */
    Map<String, Object> getLatestWorkflowRun(String branch);
}
