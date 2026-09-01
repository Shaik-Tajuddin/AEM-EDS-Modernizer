package com.adobe.aem.modernizer.connectors;

import com.adobe.aem.modernizer.persistence.model.ProjectRecord;

/**
 * Shared helpers for the preview-branch → vscode.dev → PR flow.
 */
public final class GitHubFlow {

    public static final String NPM_WORKFLOW_FILE = "main.yaml";
    public static final String NPM_WORKFLOW_PATH = ".github/workflows/" + NPM_WORKFLOW_FILE;
    public static final String LEGACY_NPM_WORKFLOW_PATH = ".github/workflows/modernizer-npm.yml";
    public static final String FSTAB_PATH = "fstab.yaml";

    public static boolean skipFromCommit(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }
        String normalized = path.replace('\\', '/').replaceFirst("^/+", "");
        return FSTAB_PATH.equalsIgnoreCase(normalized) || normalized.endsWith("/" + FSTAB_PATH);
    }

    /** Leftover site markdown at repo root (not under {@code docs/migrated-pages/}). */
    public static boolean skipLegacyPageMarkdown(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }
        String normalized = path.replace('\\', '/').replaceFirst("^/+", "");
        if (!normalized.endsWith(".md")) {
            return false;
        }
        if (normalized.startsWith("docs/migrated-pages/")) {
            return false;
        }
        if (normalized.startsWith("blocks/") || normalized.startsWith(".github/")
                || normalized.equalsIgnoreCase("README.md")) {
            return false;
        }
        return normalized.startsWith("language-masters/") || normalized.startsWith("content/");
    }

    private GitHubFlow() {}

    /** Project-scoped client when {@code gitHub} is a {@link RealGitHubClient}; otherwise the original client. */
    public static GitHubClient clientFor(GitHubClient gitHub, ProjectRecord project) {
        if (gitHub instanceof RealGitHubClient && project != null) {
            return ((RealGitHubClient) gitHub).forProject(project);
        }
        return gitHub;
    }

    public static String featureBranch(String projectId) {
        String id = (projectId == null || projectId.isBlank()) ? "project" : projectId.trim();
        return "feat/" + id;
    }

    public static String vscodeUrl(String repoUrl, String branch) {
        String cleaned = normalizeRepoUrl(repoUrl);
        if (cleaned == null) {
            return "https://vscode.dev";
        }
        String ownerRepo = cleaned.substring("https://github.com/".length());
        String ref = (branch == null || branch.isBlank()) ? "main" : branch.trim();
        return "https://vscode.dev/github/" + ownerRepo + "/tree/" + encodeBranchSegments(ref);
    }

    public static String edsPreviewUrl(String repoUrl, String branch) {
        String cleaned = normalizeRepoUrl(repoUrl);
        if (cleaned == null) {
            return null;
        }
        String ownerRepo = cleaned.substring("https://github.com/".length());
        String[] parts = ownerRepo.split("/");
        if (parts.length < 2) {
            return null;
        }
        String owner = parts[0].toLowerCase().trim();
        String repo = parts[1].toLowerCase().trim();
        String ref = (branch == null || branch.isBlank()) ? "main" : branch.trim();
        String sanitizedBranch = ref.replace('/', '-').replace('_', '-').toLowerCase();
        return "https://" + sanitizedBranch + "--" + repo + "--" + owner + ".aem.page/";
    }

    public static String normalizeRepoUrl(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            return null;
        }
        String cleaned = repoUrl.trim();
        if (cleaned.endsWith(".git")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (!cleaned.startsWith("https://github.com/")) {
            return null;
        }
        return cleaned;
    }

    /** Encodes each branch segment so {@code feat/foo} stays {@code feat/foo}, not {@code feat%2Ffoo}. */
    static String encodeBranchSegments(String branch) {
        if (branch == null || branch.isBlank()) {
            return "main";
        }
        String[] parts = branch.trim().split("/");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                out.append('/');
            }
            out.append(java.net.URLEncoder.encode(parts[i], java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return out.toString();
    }

    /** Removes the leftover standalone npm workflow after it was folded into {@code main.yaml}. */
    public static void deleteLegacyNpmWorkflow(GitHubClient client, String branch) {
        if (client == null || branch == null || branch.isBlank()) {
            return;
        }
        try {
            if (client.getFileContent(branch, LEGACY_NPM_WORKFLOW_PATH) != null) {
                client.deleteFile(branch, LEGACY_NPM_WORKFLOW_PATH);
            }
        } catch (RuntimeException ignored) {
            // Best-effort cleanup when the file is already gone or the token cannot delete it.
        }
    }
}
