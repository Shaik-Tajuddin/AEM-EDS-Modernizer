package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Persisted checkpoint for resumability (ADR 0012).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckpointRecord {

    private String id;
    private String projectId;
    private String jobId;
    private String state;
    private String resumeHint;
    private Map<String, Object> stateData = new HashMap<>();
    private long timestamp;

    public CheckpointRecord() {
        this.timestamp = System.currentTimeMillis();
    }

    public CheckpointRecord(String id, String projectId, String jobId, String state, String resumeHint) {
        this();
        this.id = id;
        this.projectId = projectId;
        this.jobId = jobId;
        this.state = state;
        this.resumeHint = resumeHint;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getResumeHint() { return resumeHint; }
    public void setResumeHint(String resumeHint) { this.resumeHint = resumeHint; }

    public Map<String, Object> getStateData() { return stateData; }
    public void setStateData(Map<String, Object> stateData) { this.stateData = stateData; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
