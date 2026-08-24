package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Persisted migration job record.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobRecord {

    private String id;
    private String projectId;
    private String state; // e.g. "CREATED", "DISCOVERING", "COMPLETED", "FAILED"
    private String mode; // "DRY_RUN" or "MIGRATE"
    private boolean dryRun;
    private long startedAt;
    private long finishedAt;
    private double actualAiCostUsd;
    private int aiCallsMade;
    private String lastError;
    private String actor;
    private Map<String, Object> metadata = new HashMap<>();

    public JobRecord() {
        this.startedAt = System.currentTimeMillis();
        this.state = "CREATED";
        this.actor = "admin";
    }

    public JobRecord(String id, String projectId, String mode) {
        this();
        this.id = id;
        this.projectId = projectId;
        this.mode = mode;
        this.dryRun = "DRY_RUN".equalsIgnoreCase(mode);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getMode() { return mode; }
    public void setMode(String mode) {
        this.mode = mode;
        this.dryRun = "DRY_RUN".equalsIgnoreCase(mode);
    }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }

    public long getFinishedAt() { return finishedAt; }
    public void setFinishedAt(long finishedAt) { this.finishedAt = finishedAt; }

    public double getActualAiCostUsd() { return actualAiCostUsd; }
    public void setActualAiCostUsd(double actualAiCostUsd) { this.actualAiCostUsd = actualAiCostUsd; }

    public int getAiCallsMade() { return aiCallsMade; }
    public void setAiCallsMade(int aiCallsMade) { this.aiCallsMade = aiCallsMade; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
