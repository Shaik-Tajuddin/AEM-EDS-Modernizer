package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Decides create / leave / enhance for mapped components vs EDS {@code blocks/}.
 */
public final class BlockReconcileHelper {

    private static final Logger LOG = LoggerFactory.getLogger(BlockReconcileHelper.class);

    public enum Action {
        CREATE,
        LEAVE,
        ENHANCE
    }

    public static final class Decision {
        private final String blockName;
        private final String resourceType;
        private final Action action;

        public Decision(String blockName, String resourceType, Action action) {
            this.blockName = blockName;
            this.resourceType = resourceType;
            this.action = action;
        }

        public String getBlockName() { return blockName; }
        public String getResourceType() { return resourceType; }
        public Action getAction() { return action; }
    }

    private BlockReconcileHelper() {}

    /**
     * List existing block folder names from local {@code blocks/} and stored generated files.
     */
    public static Set<String> listExistingBlockNames(AgentContext ctx) {
        return listExistingBlockNames(ctx, null);
    }

    public static Set<String> listExistingBlockNames(AgentContext ctx, Store store) {
        Set<String> names = new LinkedHashSet<>(listFromFilesystem(ctx));
        if (store != null && ctx != null && ctx.getProject() != null) {
            try {
                String jobId = ctx.getJob() != null ? ctx.getJob().getId() : null;
                if (jobId == null) {
                    jobId = store.getLatestJob(ctx.getProject().getId()).map(j -> j.getId()).orElse(null);
                }
                if (jobId != null) {
                    List<GeneratedFileRecord> files = store.getGeneratedFiles(jobId);
                    if (files != null) {
                        for (GeneratedFileRecord f : files) {
                            String path = f.getPath();
                            if (path == null || !path.startsWith("blocks/")) continue;
                            String rest = path.substring("blocks/".length());
                            int slash = rest.indexOf('/');
                            String name = slash > 0 ? rest.substring(0, slash) : rest;
                            if (!name.isBlank()) {
                                names.add(name.toLowerCase(Locale.ROOT));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debug("Could not list stored block files: {}", e.getMessage());
            }
        }
        return names;
    }

    private static Set<String> listFromFilesystem(AgentContext ctx) {
        Set<String> names = new LinkedHashSet<>();
        String[] roots = new String[] {
                "D:/eds personal/AEM-EDS-Modernizer",
                "d:/eds personal/AEM-EDS-Modernizer",
                System.getProperty("user.dir")
        };
        String projectId = (ctx != null && ctx.getProject() != null) ? ctx.getProject().getId() : null;
        for (String root : roots) {
            if (root == null) continue;
            // 1. Check eds/<projectId>/blocks
            if (projectId != null) {
                File projBlocks = new File(root, "eds/" + projectId + "/blocks");
                if (projBlocks.isDirectory()) {
                    File[] kids = projBlocks.listFiles(File::isDirectory);
                    if (kids != null) {
                        for (File k : kids) {
                            names.add(k.getName().toLowerCase(Locale.ROOT));
                        }
                    }
                }
            }
            // 2. Check any eds/*/blocks
            File edsDir = new File(root, "eds");
            if (edsDir.isDirectory()) {
                File[] projDirs = edsDir.listFiles(File::isDirectory);
                if (projDirs != null) {
                    for (File p : projDirs) {
                        File pBlocks = new File(p, "blocks");
                        if (pBlocks.isDirectory()) {
                            File[] kids = pBlocks.listFiles(File::isDirectory);
                            if (kids != null) {
                                for (File k : kids) {
                                    names.add(k.getName().toLowerCase(Locale.ROOT));
                                }
                            }
                        }
                    }
                }
            }
            // 3. Check legacy root blocks/
            File blocks = new File(root, "blocks");
            if (blocks.isDirectory()) {
                File[] kids = blocks.listFiles(File::isDirectory);
                if (kids != null) {
                    for (File k : kids) {
                        names.add(k.getName().toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        return names;
    }

    /**
     * @param existingJs optional existing block JS; blank/null with existing folder → LEAVE;
     *                   when explicitly marked mismatched by caller → ENHANCE
     */
    public static Action decide(String blockName, Set<String> existingBlocks, String existingJs) {
        String name = blockName != null ? blockName.toLowerCase(Locale.ROOT) : "";
        Set<String> existing = existingBlocks != null ? existingBlocks : Collections.emptySet();
        if (!existing.contains(name)) {
            return Action.CREATE;
        }
        if (existingJs != null && existingJs.contains("NEEDS_ENHANCE")) {
            return Action.ENHANCE;
        }
        // Exists: leave unless caller forces enhance via marker
        return Action.LEAVE;
    }

    /** Force enhance when folder exists but content is known mismatched. */
    public static Action decideWithMismatch(String blockName, Set<String> existingBlocks, boolean mismatch) {
        String name = blockName != null ? blockName.toLowerCase(Locale.ROOT) : "";
        Set<String> existing = existingBlocks != null ? existingBlocks : Collections.emptySet();
        if (!existing.contains(name)) {
            return Action.CREATE;
        }
        return mismatch ? Action.ENHANCE : Action.LEAVE;
    }

    public static String summarize(List<Decision> decisions) {
        if (decisions == null || decisions.isEmpty()) {
            return "No reconcile decisions.";
        }
        int c = 0, l = 0, e = 0;
        StringBuilder sb = new StringBuilder("Block reconcile: ");
        for (Decision d : decisions) {
            if (d.getAction() == Action.CREATE) c++;
            else if (d.getAction() == Action.LEAVE) l++;
            else e++;
            sb.append(d.getBlockName()).append('=').append(d.getAction()).append(' ');
        }
        sb.append("| totals create=").append(c).append(" leave=").append(l).append(" enhance=").append(e);
        return sb.toString().trim();
    }
}
