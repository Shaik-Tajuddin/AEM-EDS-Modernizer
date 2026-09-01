package com.adobe.aem.modernizer.connectors;

import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the local EDS repository lifecycle under {@code eds/<projectId>}:
 * clone/update, branch checkout, duplicate-block pruning, local lint/build healing
 * and the {@code aem up} dev server on localhost:3000.
 */
@Component(service = LocalEdsRepoManager.class, immediate = true)
public class LocalEdsRepoManager {

    private static final Logger LOG = LoggerFactory.getLogger(LocalEdsRepoManager.class);

    /** Workspace root that contains the {@code eds/} folder. */
    private static final String[] WORKSPACE_ROOTS = {
        "D:/eds personal/AEM-EDS-Modernizer",
        "d:/eds personal/AEM-EDS-Modernizer",
        System.getProperty("user.dir")
    };

    private final Map<String, Process> devServers = new ConcurrentHashMap<>();

    /** Resolves (creating if needed) the {@code eds/<projectId>} repo directory. */
    public File edsRepoDir(String projectId) {
        File root = workspaceRoot();
        File edsRoot = new File(root, "eds");
        if (!edsRoot.exists()) edsRoot.mkdirs();
        File repo = new File(edsRoot, projectId);
        if (!repo.exists()) repo.mkdirs();
        return repo;
    }

    public File workspaceRoot() {
        for (String root : WORKSPACE_ROOTS) {
            File dir = new File(root);
            if (dir.isDirectory() && (new File(dir, "pom.xml").exists() || new File(dir, "eds").exists())) {
                return dir;
            }
        }
        return new File(System.getProperty("user.dir"));
    }

    /**
     * Clones the project's EDS repo into {@code eds/<projectId>} if absent, otherwise
     * fetches and pulls latest. Best-effort: failures are reported in the returned
     * status map rather than thrown, so Dry Run never blocks.
     */
    public Map<String, Object> cloneOrUpdate(ProjectRecord project) {
        Map<String, Object> status = new LinkedHashMap<>();
        String projectId = project != null ? project.getId() : "project";
        String repoUrl = project != null ? project.getEdsGitRepoUrl() : null;
        File repo = edsRepoDir(projectId);
        status.put("path", repo.getAbsolutePath());
        if (repoUrl == null || repoUrl.isBlank()) {
            status.put("status", "SKIPPED");
            status.put("reason", "No EDS Git repository URL configured");
            return status;
        }
        try {
            if (!new File(repo, ".git").exists()) {
                run(repo.getParentFile(), Duration.ofMinutes(5),
                        "git", "clone", "--branch", branchOrMain(project), repoUrl, repo.getName());
                status.put("status", "CLONED");
            } else {
                run(repo, Duration.ofMinutes(2), "git", "fetch", "--all", "--prune");
                run(repo, Duration.ofMinutes(1), "git", "reset", "--hard");
                run(repo, Duration.ofMinutes(1), "git", "pull", "--ff-only");
                status.put("status", "UPDATED");
            }
            boolean installed = run(repo, Duration.ofMinutes(10), "npm", "install");
            status.put("npmInstall", installed ? "OK" : "FAILED");
        } catch (Exception e) {
            LOG.warn("[LocalEdsRepo] clone/update failed for {}: {}", projectId, e.getMessage());
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
        }
        return status;
    }

    private String branchOrMain(ProjectRecord project) {
        String b = project != null ? project.getEdsBranch() : null;
        return (b == null || b.isBlank()) ? "main" : b.trim();
    }

    /** Checks out {@code branch} in the local repo (creating it from HEAD when needed). */
    public boolean checkoutBranch(File repoDir, String branch) {
        if (repoDir == null || branch == null || branch.isBlank() || !new File(repoDir, ".git").exists()) {
            return false;
        }
        try {
            if (!run(repoDir, Duration.ofSeconds(30), "git", "rev-parse", "--verify", branch)) {
                run(repoDir, Duration.ofSeconds(30), "git", "checkout", "-b", branch);
            } else {
                run(repoDir, Duration.ofSeconds(30), "git", "checkout", branch);
            }
            return true;
        } catch (Exception e) {
            LOG.warn("[LocalEdsRepo] checkout {} failed: {}", branch, e.getMessage());
            return false;
        }
    }

    /** Removes redundant/duplicate block directories (e.g. {@code hero-1}, {@code hero copy}). */
    public List<String> pruneDuplicateBlocks(File repoDir) {
        List<String> pruned = new ArrayList<>();
        File blocks = new File(repoDir, "blocks");
        File[] dirs = blocks != null && blocks.isDirectory() ? blocks.listFiles(File::isDirectory) : null;
        if (dirs == null) return pruned;
        Arrays.sort(dirs);
        for (File dir : dirs) {
            String name = dir.getName().toLowerCase();
            String base = name.replaceAll("[-_\\s]*(copy|backup|old|\\d+)$", "");
            if (!base.equals(name) && new File(blocks, base).isDirectory()) {
                if (deleteRecursively(dir)) pruned.add(dir.getName());
            }
        }
        if (!pruned.isEmpty()) {
            LOG.info("[LocalEdsRepo] Pruned duplicate blocks: {}", pruned);
        }
        return pruned;
    }

    /**
     * Runs local healing: {@code npm run lint:fix} then {@code npm run build:json},
     * commits and pushes any auto-fixed files. {@code ok=true} opens the Create PR gate.
     */
    public Map<String, Object> runLintAndBuild(File repoDir, String branch) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("lintFix", run(repoDir, Duration.ofMinutes(5), "npm", "run", "lint:fix") ? "OK" : "FAILED");
        status.put("buildJson", run(repoDir, Duration.ofMinutes(5), "npm", "run", "build:json") ? "OK" : "FAILED");
        boolean ok = "OK".equals(status.get("lintFix")) && "OK".equals(status.get("buildJson"));
        try {
            run(repoDir, Duration.ofSeconds(30), "git", "add", "-A");
            boolean hasChanges = !runCapture(repoDir, Duration.ofSeconds(30), "git", "status", "--porcelain").isBlank();
            status.put("dirty", hasChanges);
            if (hasChanges) {
                run(repoDir, Duration.ofSeconds(30), "git", "commit", "-m",
                        "chore: pre-PR automated healing (lint:fix + build:json)");
            }
            if (branch != null && !branch.isBlank()) {
                status.put("push", run(repoDir, Duration.ofMinutes(2), "git", "push", "origin", branch) ? "OK" : "FAILED");
                if (!"OK".equals(status.get("push"))) ok = false;
            }
        } catch (Exception e) {
            LOG.warn("[LocalEdsRepo] commit/push failed: {}", e.getMessage());
            status.put("push", "ERROR: " + e.getMessage());
            ok = false;
        }
        status.put("ok", ok);
        return status;
    }

    /** Starts {@code aem up} (AEM CLI dev server) on localhost:3000. */
    public Map<String, Object> startAemUpDevServer(File repoDir, String projectId) {
        Map<String, Object> status = new LinkedHashMap<>();
        Process existing = devServers.get(projectId);
        if (existing != null && existing.isAlive()) {
            status.put("status", "RUNNING");
            status.put("url", "http://localhost:3000");
            return status;
        }
        try {
            run(repoDir, Duration.ofMinutes(10), "npm", "install");
            ProcessBuilder pb = new ProcessBuilder("npx", "@adobe/aem-cli", "up", "--port", "3000");
            pb.directory(repoDir);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.to(new File(repoDir, "aem-up.log")));
            Process proc = pb.start();
            devServers.put(projectId, proc);
            status.put("status", "STARTED");
            status.put("url", "http://localhost:3000");
            LOG.info("[LocalEdsRepo] aem up started for {} in {}", projectId, repoDir);
        } catch (Exception e) {
            LOG.warn("[LocalEdsRepo] aem up failed: {}", e.getMessage());
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
        }
        return status;
    }

    public Map<String, Object> stopAemUpDevServer(String projectId) {
        Map<String, Object> status = new LinkedHashMap<>();
        Process proc = devServers.remove(projectId);
        if (proc != null && proc.isAlive()) {
            proc.destroy();
            status.put("status", "STOPPED");
        } else {
            status.put("status", "NOT_RUNNING");
        }
        return status;
    }

    public Map<String, Object> devServerStatus(String projectId) {
        Process proc = devServers.get(projectId);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("running", proc != null && proc.isAlive());
        status.put("url", "http://localhost:3000");
        return status;
    }

    /** Writes a generated artifact directly into {@code eds/<projectId>/<relPath>}. */
    public Path writeProjectFile(String projectId, String relPath, String content) {
        try {
            Path target = edsRepoDir(projectId).toPath().resolve(relPath.replace('\\', '/'));
            Files.createDirectories(target.getParent());
            Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
            return target;
        } catch (IOException e) {
            LOG.warn("[LocalEdsRepo] Could not write {} for {}: {}", relPath, projectId, e.getMessage());
            return null;
        }
    }

    public boolean deleteProjectFile(String projectId, String relPath) {
        if (projectId == null || relPath == null) return false;
        try {
            Path target = edsRepoDir(projectId).toPath().resolve(relPath.replace('\\', '/'));
            File f = target.toFile();
            if (f.exists()) {
                boolean deleted = f.delete();
                LOG.info("[LocalEdsRepo] Deleted local file: {}", target);
                return deleted;
            }
            return true;
        } catch (Exception e) {
            LOG.warn("[LocalEdsRepo] Could not delete local file {} for {}: {}", relPath, projectId, e.getMessage());
            return false;
        }
    }

    private boolean run(File dir, Duration timeout, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(dir);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.to(new File(dir, ".modernizer-last-cmd.log")));
            Process p = pb.start();
            if (!p.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                LOG.warn("[LocalEdsRepo] Command timed out: {}", String.join(" ", command));
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception e) {
            LOG.debug("[LocalEdsRepo] Command '{}' failed: {}", String.join(" ", command), e.getMessage());
            return false;
        }
    }

    private String runCapture(File dir, Duration timeout, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(dir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            return out == null ? "" : out;
        } catch (Exception e) {
            return "";
        }
    }

    private boolean deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!deleteRecursively(child)) return false;
            }
        }
        return file.delete();
    }
}
