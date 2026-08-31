package com.adobe.aem.modernizer.connectors;

import com.adobe.aem.modernizer.agents.AgentContext;
import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.ModelCapability;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.JobRecord;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.persistence.model.RepairAttemptRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * After preview push: restore {@code fstab.yaml} from the EDS base branch, then wait for CI.
 * On failure, run {@code lint:fix} and/or patch files from logs, push, and recheck until green
 * or {@code maxRepairAttempts} is exhausted.
 */
public final class PipelineHealLoop {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineHealLoop.class);
    private static final Pattern FILE_IN_LOG = Pattern.compile(
            "(blocks/[A-Za-z0-9._-]+/[A-Za-z0-9._-]+\\.(?:js|css|json)|scripts/[A-Za-z0-9._-]+\\.js|styles/[A-Za-z0-9._-]+\\.css)");
    private static final long POLL_MS = 8000L;
    private static final long WAIT_MS = 6L * 60L * 1000L;

    private PipelineHealLoop() {}

    public static void start(GitHubClient client, AgentContext ctx, Store store, AiGateway ai) {
        if (client == null || ctx == null || ctx.getProject() == null) {
            return;
        }
        Runnable work = () -> healUntilGreen(client, ctx, store, ai);
        if (client instanceof RealGitHubClient) {
            Thread thread = new Thread(work, "modernizer-ci-heal-" + ctx.getProject().getId());
            thread.setDaemon(true);
            thread.start();
            record(store, ctx, "CI heal started in background. Failures will be patched and pushed until the pipeline passes.");
        } else {
            work.run();
        }
    }

    public static void restoreFstabFromBase(GitHubClient client, ProjectRecord project, String featureBranch) {
        if (client == null || project == null || featureBranch == null) {
            return;
        }
        String base = project.getEdsBranch();
        if (base == null || base.isBlank()) {
            base = client.getRepositoryDefaultBranch();
        }
        if (base == null || base.isBlank() || base.equals(featureBranch)) {
            return;
        }
        String original = client.getFileContent(base, GitHubFlow.FSTAB_PATH);
        if (original == null) {
            return;
        }
        String current = client.getFileContent(featureBranch, GitHubFlow.FSTAB_PATH);
        if (original.equals(current)) {
            return;
        }
        GeneratedFileRecord file = new GeneratedFileRecord(
                UUID.randomUUID().toString(),
                project.getId(),
                "preview",
                GitHubFlow.FSTAB_PATH,
                "CONFIG",
                original
        );
        // Commit using a path that skipFromCommit would drop — use a one-off restore via
        // putting content only if we temporarily bypass. Restore must write fstab.yaml.
        restoreFstabDirect(client, featureBranch, file);
    }

    /**
     * Writes fstab.yaml from the base branch. {@link GitHubClient#commitFiles} skips fstab,
     * so RealGitHubClient restore uses a dedicated tree update through a wrapper record
     * committed only by {@link #forceCommit} in tests via mock file map.
     */
    static void restoreFstabDirect(GitHubClient client, String featureBranch, GeneratedFileRecord file) {
        if (!(client instanceof RealGitHubClient)) {
            return;
        }
        try {
            ((RealGitHubClient) client).commitPathAllowingFstab(featureBranch, file,
                    "chore: restore existing fstab.yaml (modernizer does not edit it)");
        } catch (RuntimeException e) {
            LOG.warn("Could not restore fstab.yaml on {}: {}", featureBranch, e.getMessage());
        }
    }

    static void healUntilGreen(GitHubClient client, AgentContext ctx, Store store, AiGateway ai) {
        ProjectRecord project = ctx.getProject();
        JobRecord job = ctx.getJob();
        String branch = GitHubFlow.featureBranch(project.getId());
        int max = project.getMaxRepairAttempts() > 0 ? project.getMaxRepairAttempts() : 5;
        String lastRunId = null;
        boolean repaired = applyJsonAndSectionFixes(client, ctx, store, branch)
                || applyJsFixes(client, ctx, store, branch);
        if (repaired) {
            record(store, ctx, "Committed UE JSON / generated JS repairs before waiting on CI.");
        }
        for (int attempt = 1; attempt <= max; attempt++) {
            Map<String, Object> run = waitForCi(client, branch, lastRunId);
            if (isSuccess(run)) {
                record(store, ctx, "Pipeline passed on " + branch + (run.get("htmlUrl") != null ? ": " + run.get("htmlUrl") : ""));
                putMeta(job, store, "ciHeal", "passed");
                return;
            }
            if (run == null) {
                record(store, ctx, "No CI run found yet on " + branch + "; waiting for the next attempt.");
            } else {
                record(store, ctx, "Pipeline failed (" + run.get("conclusion") + "). Repair attempt "
                        + attempt + "/" + max + ".");
            }
            lastRunId = run != null ? String.valueOf(run.getOrDefault("runId", "")) : null;
            String logs = run != null ? safeLogs(client, lastRunId) : "";
            String kind = PipelineHealRepairs.classifyLogs(logs);
            record(store, ctx, "CI failure classified as " + kind + ".");
            boolean committed = applyJsonAndSectionFixes(client, ctx, store, branch)
                    || applyJsFixes(client, ctx, store, branch)
                    || applyRepairs(client, ctx, store, ai, branch, logs);
            repaired = repaired || committed;
            if (!blockJsonValid(client, ctx, store, branch)) {
                record(store, ctx, "Block _*.json is still invalid. Not dispatching modernizer-npm until JSON is repaired.");
                putMeta(job, store, "ciHeal", "stuck");
                return;
            }
            if (!repaired && !committed) {
                record(store, ctx, "No file repairs were committed. Skipping npm dispatch so CI is not re-run on the same broken files.");
                putMeta(job, store, "ciHeal", "stuck");
                return;
            }
            record(store, ctx, "Dispatching lint:fix then build:json after file repairs.");
            boolean linted = tryDispatch(client, branch, "lint:fix");
            if (linted) {
                Map<String, Object> afterLint = waitForCi(client, branch, lastRunId);
                if (afterLint != null && afterLint.get("runId") != null) {
                    lastRunId = String.valueOf(afterLint.get("runId"));
                }
            }
            tryDispatch(client, branch, "build:json");
            boolean pushed = true;
            if (!pushed) {
                record(store, ctx, "No automatic patch could be produced from the CI logs.");
                putMeta(job, store, "ciHeal", "stuck");
                return;
            }
            record(store, ctx, "Pushed CI repairs. Rechecking the pipeline...");
        }
        Map<String, Object> finalRun = waitForCi(client, branch, lastRunId);
        if (isSuccess(finalRun)) {
            record(store, ctx, "Pipeline passed after bounded repairs.");
            putMeta(job, store, "ciHeal", "passed");
        } else {
            record(store, ctx, "Pipeline still failing after " + max + " repair attempts.");
            putMeta(job, store, "ciHeal", "failed");
        }
    }

    static Map<String, Object> waitForCi(GitHubClient client, String branch, String previousRunId) {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        Map<String, Object> last = null;
        while (System.currentTimeMillis() < deadline) {
            last = client.getLatestWorkflowRun(branch);
            if (last != null) {
                String id = String.valueOf(last.getOrDefault("runId", ""));
                String status = String.valueOf(last.getOrDefault("status", ""));
                boolean same = previousRunId != null && !previousRunId.isBlank() && previousRunId.equals(id);
                if (!same && ("completed".equals(status) || "failure".equals(status) || "cancelled".equals(status))) {
                    return last;
                }
                if (!same && isSuccess(last)) {
                    return last;
                }
            }
            if (!(client instanceof RealGitHubClient)) {
                return last;
            }
            sleep(POLL_MS);
        }
        return last;
    }

    static boolean applyJsFixes(GitHubClient client, AgentContext ctx, Store store, String branch) {
        List<GeneratedFileRecord> patched = new ArrayList<>();
        for (String path : collectRepoPaths(client, ctx, store, branch)) {
            if (path == null || !path.startsWith("blocks/") || !path.endsWith(".js")
                    || GitHubFlow.skipFromCommit(path)) {
                continue;
            }
            String content = contentFor(client, store, ctx, branch, path);
            if (content == null) {
                continue;
            }
            String fixed = PipelineHealRepairs.sanitizeGeneratedJs(content);
            if (!fixed.equals(content)) {
                patched.add(file(ctx, path, "BLOCK_JS", fixed));
            }
        }
        if (patched.isEmpty()) {
            return false;
        }
        client.commitFiles(branch, patched, "fix: make generated block JS lint-safe");
        if (store != null) {
            for (GeneratedFileRecord file : patched) {
                store.saveGeneratedFile(file);
            }
        }
        return true;
    }

    static boolean blockJsonValid(GitHubClient client, AgentContext ctx, Store store, String branch) {
        boolean saw = false;
        for (String path : collectRepoPaths(client, ctx, store, branch)) {
            if (!PipelineHealRepairs.isBlockModelJson(path)) {
                continue;
            }
            saw = true;
            String content = contentFor(client, store, ctx, branch, path);
            if (content == null || !PipelineHealRepairs.isValidJson(content)) {
                return false;
            }
        }
        return saw;
    }

    private static Set<String> collectRepoPaths(GitHubClient client, AgentContext ctx, Store store, String branch) {
        Set<String> paths = new LinkedHashSet<>();
        paths.addAll(client.listFilePaths(branch, "blocks/"));
        if (paths.isEmpty()) {
            paths.addAll(client.listFilePaths(branch, ""));
        }
        try {
            String base = ctx.getProject() != null ? ctx.getProject().getEdsBranch() : null;
            if (base == null || base.isBlank()) {
                base = client.getRepositoryDefaultBranch();
            }
            List<Map<String, Object>> changed = client.listChangedFiles(base, branch);
            if (changed != null) {
                for (Map<String, Object> row : changed) {
                    Object name = row.get("filename");
                    if (name != null) {
                        paths.add(String.valueOf(name).replace('\\', '/'));
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // listing compare is best-effort
        }
        if (store != null && ctx.getJob() != null) {
            List<GeneratedFileRecord> files = store.getGeneratedFiles(ctx.getJob().getId());
            if (files != null) {
                for (GeneratedFileRecord file : files) {
                    if (file != null && file.getPath() != null) {
                        paths.add(file.getPath().replace('\\', '/'));
                    }
                }
            }
        }
        return paths;
    }

    private static boolean tryDispatch(GitHubClient client, String branch, String command) {
        try {
            Map<String, String> inputs = new LinkedHashMap<>();
            inputs.put("command", command);
            client.dispatchWorkflow(branch, GitHubFlow.NPM_WORKFLOW_FILE, inputs);
            return true;
        } catch (RuntimeException e) {
            LOG.info("{} dispatch skipped: {}", command, e.getMessage());
            return false;
        }
    }

    static boolean applyJsonAndSectionFixes(GitHubClient client, AgentContext ctx, Store store, String branch) {
        List<GeneratedFileRecord> patched = new ArrayList<>();
        Set<String> paths = collectRepoPaths(client, ctx, store, branch);
        Set<String> blockIds = PipelineHealRepairs.collectBlockIds(paths);
        for (String path : paths) {
            if (!PipelineHealRepairs.isBlockModelJson(path) || GitHubFlow.skipFromCommit(path)) {
                continue;
            }
            String content = contentFor(client, store, ctx, branch, path);
            if (content == null) {
                continue;
            }
            String sanitized = PipelineHealRepairs.sanitizeBlockJson(content);
            if (!sanitized.equals(content)) {
                patched.add(file(ctx, path, "BLOCK_JSON", sanitized));
            }
        }

        String section = client.getFileContent(branch, PipelineHealRepairs.SECTION_FILTER_PATH);
        if (section == null && store != null && ctx.getJob() != null) {
            section = contentFor(client, store, ctx, branch, PipelineHealRepairs.SECTION_FILTER_PATH);
        }
        if (section != null && !blockIds.isEmpty()) {
            String merged = PipelineHealRepairs.mergeSectionFilter(section, blockIds);
            if (!merged.equals(section)) {
                patched.add(file(ctx, PipelineHealRepairs.SECTION_FILTER_PATH, "CONFIG", merged));
            }
        }

        String listJson = client.getFileContent(branch, PipelineHealRepairs.COMPONENT_LIST_PATH);
        if (listJson != null && !blockIds.isEmpty()) {
            String mergedList = PipelineHealRepairs.mergeComponentList(listJson, blockIds);
            if (!mergedList.equals(listJson)) {
                patched.add(file(ctx, PipelineHealRepairs.COMPONENT_LIST_PATH, "CONFIG", mergedList));
            }
        }

        if (patched.isEmpty()) {
            return false;
        }
        client.commitFiles(branch, patched, "fix: sanitize UE JSON and register section blocks");
        if (store != null) {
            for (GeneratedFileRecord file : patched) {
                store.saveGeneratedFile(file);
            }
        }
        return true;
    }

    private static GeneratedFileRecord file(AgentContext ctx, String path, String type, String content) {
        return new GeneratedFileRecord(
                UUID.randomUUID().toString(),
                ctx.getProject().getId(),
                ctx.getJob() != null ? ctx.getJob().getId() : "preview",
                path,
                type,
                content
        );
    }

    private static boolean applyRepairs(GitHubClient client, AgentContext ctx, Store store,
                                        AiGateway ai, String branch, String logs) {
        Set<String> paths = extractPaths(logs);
        if (paths.isEmpty()) {
            paths.addAll(generatedJsCss(store, ctx));
        }
        List<GeneratedFileRecord> patched = new ArrayList<>();
        for (String path : paths) {
            if (GitHubFlow.skipFromCommit(path)) {
                continue;
            }
            String content = contentFor(client, store, ctx, branch, path);
            if (content == null) {
                continue;
            }
            String fixed = fixFile(ai, path, content, logs);
            if (fixed != null && !fixed.equals(content)) {
                patched.add(new GeneratedFileRecord(
                        UUID.randomUUID().toString(),
                        ctx.getProject().getId(),
                        ctx.getJob() != null ? ctx.getJob().getId() : "preview",
                        path,
                        path.endsWith(".css") ? "BLOCK_CSS" : "BLOCK_JS",
                        fixed
                ));
            }
        }
        if (patched.isEmpty()) {
            return false;
        }
        client.commitFiles(branch, patched, "fix: apply CI log repairs");
        if (store != null) {
            for (GeneratedFileRecord file : patched) {
                store.saveGeneratedFile(file);
            }
        }
        return true;
    }

    private static String fixFile(AiGateway ai, String path, String content, String logs) {
        String heuristic = heuristicFix(content);
        if (ai == null) {
            return heuristic.equals(content) ? null : heuristic;
        }
        try {
            ChatRequest req = new ChatRequest("pipeline-heal",
                    "Fix this file so GitHub Actions lint/build passes. Return ONLY the full file contents.\n"
                            + "Path: " + path + "\nCI logs (excerpt):\n"
                            + excerpt(logs, 2500) + "\n\nFILE:\n" + content);
            req.setTargetCapability(ModelCapability.CAP_CODE);
            ChatResponse resp = ai.dispatch(req);
            String body = resp != null ? resp.getContent() : null;
            if (body != null && body.length() > 20 && !body.contains("```")) {
                return body;
            }
            if (body != null && body.contains("```")) {
                int start = body.indexOf('\n', body.indexOf("```"));
                int end = body.lastIndexOf("```");
                if (start > 0 && end > start) {
                    return body.substring(start + 1, end).trim();
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("AI repair failed for {}: {}", path, e.getMessage());
        }
        return heuristic.equals(content) ? null : heuristic;
    }

    static String heuristicFix(String content) {
        if (content == null) {
            return "";
        }
        String out = content.replaceAll("[ \\t]+(?=\\r?\\n)", "");
        out = out.replaceAll("(?m)^[ \\t]*console\\.(log|debug|info)\\(.*\\);?[ \\t]*$", "");
        return out;
    }

    static Set<String> extractPaths(String logs) {
        Set<String> paths = new LinkedHashSet<>();
        if (logs == null) {
            return paths;
        }
        Matcher matcher = FILE_IN_LOG.matcher(logs);
        while (matcher.find()) {
            paths.add(matcher.group(1));
        }
        return paths;
    }

    private static List<String> generatedJsCss(Store store, AgentContext ctx) {
        List<String> paths = new ArrayList<>();
        if (store == null || ctx.getJob() == null) {
            return paths;
        }
        List<GeneratedFileRecord> files = store.getGeneratedFiles(ctx.getJob().getId());
        if (files == null) {
            return paths;
        }
        for (GeneratedFileRecord file : files) {
            String path = file.getPath();
            if (path != null && (path.endsWith(".js") || path.endsWith(".css")) && !GitHubFlow.skipFromCommit(path)) {
                paths.add(path);
            }
        }
        return paths;
    }

    private static String contentFor(GitHubClient client, Store store, AgentContext ctx, String branch, String path) {
        String fromGit = client.getFileContent(branch, path);
        if (fromGit != null) {
            return fromGit;
        }
        if (store == null || ctx.getJob() == null) {
            return null;
        }
        List<GeneratedFileRecord> files = store.getGeneratedFiles(ctx.getJob().getId());
        if (files == null) {
            return null;
        }
        for (GeneratedFileRecord file : files) {
            if (path.equals(file.getPath())) {
                return file.getContent();
            }
        }
        return null;
    }

    private static String safeLogs(GitHubClient client, String runId) {
        if (runId == null || runId.isBlank()) {
            return "";
        }
        try {
            return client.getWorkflowRunLogs(runId);
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static boolean isSuccess(Map<String, Object> run) {
        if (run == null) {
            return false;
        }
        return "success".equals(String.valueOf(run.get("conclusion")));
    }

    private static void record(Store store, AgentContext ctx, String message) {
        LOG.info(message);
        if (store == null || ctx.getProject() == null || ctx.getJob() == null) {
            return;
        }
        store.recordEvent(new JobEventRecord(
                UUID.randomUUID().toString(),
                ctx.getProject().getId(),
                ctx.getJob().getId(),
                "pipeline-heal",
                message
        ));
    }

    private static void saveRepair(Store store, AgentContext ctx, int attempt, String logs, boolean pushed) {
        if (store == null || ctx.getJob() == null) {
            return;
        }
        RepairAttemptRecord rec = new RepairAttemptRecord(
                UUID.randomUUID().toString(),
                ctx.getProject().getId(),
                ctx.getJob().getId(),
                "ci-pipeline",
                attempt,
                excerpt(logs, 400)
        );
        rec.setIssueCategory("CI_FAILURE");
        rec.setProposedFix(pushed
                ? "sanitize UE JSON, section filter, lint:fix, build:json, and/or log patches"
                : "no patch produced");
        rec.setSuccessful(pushed);
        store.saveRepairAttempt(rec);
    }

    private static void putMeta(JobRecord job, Store store, String key, String value) {
        if (job == null) {
            return;
        }
        Map<String, Object> meta = job.getMetadata();
        if (meta == null) {
            meta = new LinkedHashMap<>();
        }
        meta.put(key, value);
        job.setMetadata(meta);
        if (store != null) {
            store.saveJob(job);
        }
    }

    private static String excerpt(String text, int max) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(trimmed.length() - max);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
