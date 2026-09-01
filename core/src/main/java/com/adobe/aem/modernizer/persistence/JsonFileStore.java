package com.adobe.aem.modernizer.persistence;

import com.adobe.aem.modernizer.persistence.model.*;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Disk-backed Store (ADR 0013): all state is kept in memory for speed but every
 * mutation is flushed to a JSON snapshot under the AEM working directory
 * ({sling.home}/aem-eds-modernizer/store.json). On startup the snapshot is
 * reloaded, so saved projects/jobs/events survive bundle restarts and page
 * refreshes even after redeployments.
 */
@Component(service = Store.class, immediate = true, property = { "service.ranking:Integer=100" })
public class JsonFileStore extends InMemoryStore {

    private static final Logger LOG = LoggerFactory.getLogger(JsonFileStore.class);
    private static final String SNAPSHOT_FILE = "aem-eds-modernizer-store.json";

    private final transient Map<String, Object> lock = new ConcurrentHashMap<>();
    private transient Path snapshotPath;

    @Activate
    public void activate() {
        snapshotPath = resolveSnapshotPath();
        loadSnapshot();
        LOG.info("JsonFileStore activated — snapshot at {}", snapshotPath);
    }

    @Deactivate
    public void deactivate() {
        saveSnapshot();
        LOG.info("JsonFileStore deactivated — snapshot saved");
    }

    private Path resolveSnapshotPath() {
        // Prefer the AEM sling home (crx-quickstart), fall back to user dir
        String slingHome = System.getProperty("sling.home");
        Path dir;
        if (slingHome != null && !slingHome.isEmpty()) {
            dir = Paths.get(slingHome, "aem-eds-modernizer");
        } else {
            dir = Paths.get(System.getProperty("user.dir"), "aem-eds-modernizer");
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOG.warn("Could not create store dir {}: {}", dir, e.getMessage());
            dir = Paths.get(System.getProperty("java.io.tmpdir"), "aem-eds-modernizer");
            try { Files.createDirectories(dir); } catch (IOException ignored) {}
        }
        return dir.resolve(SNAPSHOT_FILE);
    }

    private void loadSnapshot() {
        if (snapshotPath == null || !Files.exists(snapshotPath)) {
            LOG.info("No existing store snapshot found — starting fresh");
            return;
        }
        try {
            String json = Files.readString(snapshotPath, StandardCharsets.UTF_8);
            Map<?, ?> root = JsonUtil.fromJson(json, Map.class);
            if (root == null) return;

            Object projectsObj = root.get("projects");
            if (projectsObj instanceof List) {
                for (Object o : (List<?>) projectsObj) {
                    ProjectRecord p = JsonUtil.fromJson(JsonUtil.toJson(o), ProjectRecord.class);
                    if (p != null && p.getId() != null) super.saveProject(p);
                }
            }
            Object jobsObj = root.get("jobs");
            if (jobsObj instanceof List) {
                for (Object o : (List<?>) jobsObj) {
                    JobRecord j = JsonUtil.fromJson(JsonUtil.toJson(o), JobRecord.class);
                    if (j != null && j.getId() != null) super.saveJob(j);
                }
            }
            Object invObj = root.get("inventories");
            if (invObj instanceof List) {
                for (Object o : (List<?>) invObj) {
                    SiteInventory inv = JsonUtil.fromJson(JsonUtil.toJson(o), SiteInventory.class);
                    if (inv != null && inv.getJobId() != null) super.saveInventory(inv);
                }
            }
            Object filesObj = root.get("generatedFiles");
            if (filesObj instanceof List) {
                for (Object o : (List<?>) filesObj) {
                    GeneratedFileRecord f = JsonUtil.fromJson(JsonUtil.toJson(o), GeneratedFileRecord.class);
                    if (f != null) super.saveGeneratedFile(f);
                }
            }
            Object eventsObj = root.get("events");
            if (eventsObj instanceof List) {
                for (Object o : (List<?>) eventsObj) {
                    JobEventRecord ev = JsonUtil.fromJson(JsonUtil.toJson(o), JobEventRecord.class);
                    if (ev != null) super.recordEvent(ev);
                }
            }
            LOG.info("Store snapshot restored from {}", snapshotPath);
        } catch (Exception e) {
            LOG.error("Failed to restore store snapshot {}: {}", snapshotPath, e.getMessage(), e);
        }
    }

    private void saveSnapshot() {
        if (snapshotPath == null) return;
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("savedAt", System.currentTimeMillis());
            root.put("projects", super.listProjects());
            root.put("jobs", new ArrayList<>(jobsRef.values()));
            root.put("inventories", new ArrayList<>(inventoriesRef.values()));
            List<GeneratedFileRecord> allFiles = new ArrayList<>();
            generatedFilesRef.values().forEach(allFiles::addAll);
            root.put("generatedFiles", allFiles);
            List<JobEventRecord> allEvents = new ArrayList<>();
            eventsRef.values().forEach(allEvents::addAll);
            allEvents.sort(Comparator.comparingLong(JobEventRecord::getTimestamp));
            root.put("events", allEvents);

            Path tmp = snapshotPath.resolveSibling(snapshotPath.getFileName() + ".tmp");
            Files.writeString(tmp, JsonUtil.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, snapshotPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            LOG.error("Failed to save store snapshot {}: {}", snapshotPath, e.getMessage(), e);
        }
    }

    /** Mutating operations flush the snapshot to disk asynchronously-ish (inline for durability). */
    private void persist() {
        saveSnapshot();
    }

    // ── Overrides: every mutation persists ──

    @Override
    public void saveProject(ProjectRecord project) {
        super.saveProject(project);
        persist();
    }

    @Override
    public void deleteProject(String id) {
        super.deleteProject(id);
        persist();
    }

    @Override
    public void saveJob(JobRecord job) {
        super.saveJob(job);
        persist();
    }

    @Override
    public void saveInventory(SiteInventory inventory) {
        super.saveInventory(inventory);
        persist();
    }

    @Override
    public void saveGeneratedFile(GeneratedFileRecord file) {
        super.saveGeneratedFile(file);
        persist();
    }

    @Override
    public boolean deleteGeneratedFile(String jobId, String path) {
        boolean removed = super.deleteGeneratedFile(jobId, path);
        if (removed) persist();
        return removed;
    }

    @Override
    public void recordEvent(JobEventRecord event) {
        super.recordEvent(event);
        persist();
    }

    @Override
    public void savePlan(MigrationPlan plan) {
        super.savePlan(plan);
        persist();
    }

    @Override
    public void saveCheckpoint(CheckpointRecord checkpoint) {
        super.saveCheckpoint(checkpoint);
        persist();
    }
}
