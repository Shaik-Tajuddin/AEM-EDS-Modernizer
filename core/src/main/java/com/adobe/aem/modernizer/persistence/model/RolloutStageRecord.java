package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Persisted record of a rollout stage (Phase 2).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RolloutStageRecord {

    private String id;
    private String projectId;
    private String jobId;
    private int stageIndex;
    private String stageName; // "PREVIEW", "INTERNAL", "CANARY", "BATCH", "BROAD", "FULL"
    private int targetTrafficPercent;
    private int pagesIncluded;
    private String status; // "PENDING", "IN_PROGRESS", "PASSED", "HALTED"
    private String stopConditionTriggered;
    private long startedAt;
    private long completedAt;

    public RolloutStageRecord() {}

    public RolloutStageRecord(String id, String projectId, String jobId, int stageIndex, String stageName, int targetTrafficPercent) {
        this.id = id;
        this.projectId = projectId;
        this.jobId = jobId;
        this.stageIndex = stageIndex;
        this.stageName = stageName;
        this.targetTrafficPercent = targetTrafficPercent;
        this.status = "PENDING";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public int getStageIndex() { return stageIndex; }
    public void setStageIndex(int stageIndex) { this.stageIndex = stageIndex; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public int getTargetTrafficPercent() { return targetTrafficPercent; }
    public void setTargetTrafficPercent(int targetTrafficPercent) { this.targetTrafficPercent = targetTrafficPercent; }

    public int getPagesIncluded() { return pagesIncluded; }
    public void setPagesIncluded(int pagesIncluded) { this.pagesIncluded = pagesIncluded; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStopConditionTriggered() { return stopConditionTriggered; }
    public void setStopConditionTriggered(String stopConditionTriggered) { this.stopConditionTriggered = stopConditionTriggered; }

    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
}
