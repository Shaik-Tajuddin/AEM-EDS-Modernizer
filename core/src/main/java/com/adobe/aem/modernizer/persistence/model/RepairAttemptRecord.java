package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Persisted record of an AI repair attempt (Phase 1 & Phase 2).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepairAttemptRecord {

    private String id;
    private String projectId;
    private String jobId;
    private String targetPath;
    private int attemptNumber;
    private String issueCategory;
    private String issueDescription;
    private String proposedFix;
    private String patchDiff;
    private boolean successful;
    private double aiCostMicros;
    private long durationMs;
    private long timestamp;

    public RepairAttemptRecord() {
        this.timestamp = System.currentTimeMillis();
    }

    public RepairAttemptRecord(String id, String projectId, String jobId, String targetPath, int attemptNumber, String issueDescription) {
        this();
        this.id = id;
        this.projectId = projectId;
        this.jobId = jobId;
        this.targetPath = targetPath;
        this.attemptNumber = attemptNumber;
        this.issueDescription = issueDescription;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getTargetPath() { return targetPath; }
    public void setTargetPath(String targetPath) { this.targetPath = targetPath; }

    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }

    public String getIssueCategory() { return issueCategory; }
    public void setIssueCategory(String issueCategory) { this.issueCategory = issueCategory; }

    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }

    public String getProposedFix() { return proposedFix; }
    public void setProposedFix(String proposedFix) { this.proposedFix = proposedFix; }

    public String getPatchDiff() { return patchDiff; }
    public void setPatchDiff(String patchDiff) { this.patchDiff = patchDiff; }

    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean successful) { this.successful = successful; }

    public double getAiCostMicros() { return aiCostMicros; }
    public void setAiCostMicros(double aiCostMicros) { this.aiCostMicros = aiCostMicros; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
