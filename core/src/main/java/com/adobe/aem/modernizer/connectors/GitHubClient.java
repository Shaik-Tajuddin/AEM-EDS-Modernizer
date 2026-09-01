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

    /** GitHub repository default branch (from the repo API), used to register workflow_dispatch workflows. */
    default String getRepositoryDefaultBranch() {
        return getDefaultBranch();
    }

    boolean branchExists(String branch);
    void createBranch(String branch);
    void commitFiles(String branch, List<GeneratedFileRecord> files, String commitMessage);
    String createPullRequest(String title, String body, String headBranch, String baseBranch);

    /** Lists files changed on {@code headBranch} relative to {@code baseBranch} (GitHub compare API). */
    List<Map<String, Object>> listChangedFiles(String baseBranch, String headBranch);

    /** Returns the most recent GitHub Actions workflow run for the given branch, or null if none/unavailable. */
    Map<String, Object> getLatestWorkflowRun(String branch);

    /**
     * Dispatches a {@code workflow_dispatch} workflow on {@code ref} and returns a run summary
     * ({@code runId}, {@code status}, {@code htmlUrl}) when the run can be resolved.
     */
    default Map<String, Object> dispatchWorkflow(String ref, String workflowFile, Map<String, String> inputs) {
        throw new UnsupportedOperationException("dispatchWorkflow is not supported by this GitHub client");
    }

    /** Poll a workflow run by numeric id. */
    default Map<String, Object> getWorkflowRun(String runId) {
        return null;
    }

    /** Best-effort job logs for a workflow run (may be truncated). */
    default String getWorkflowRunLogs(String runId) {
        return "";
    }

    /** File text at {@code path} on {@code ref}, or null if missing. */
    default String getFileContent(String ref, String path) {
        return null;
    }

    /**
     * Blob paths on {@code ref} whose path starts with {@code pathPrefix}
     * (for example {@code blocks/}). Empty when listing is unsupported.
     */
    default List<String> listFilePaths(String ref, String pathPrefix) {
        return List.of();
    }

    /** Deletes {@code path} on {@code branch} (new commit). */
    default void deleteFile(String branch, String path) {
        throw new UnsupportedOperationException("deleteFile is not supported by this GitHub client");
    }

    /** Looks up existing PR URL for {@code headBranch} if one is already open. */
    default String findExistingPullRequest(String headBranch) {
        return null;
    }
}
