package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Normalized event log entry (ADR 0013).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobEventRecord {

    private String id;
    private String projectId;
    private String jobId;
    private String level = "INFO"; // "INFO", "WARN", "ERROR"
    private String agent;
    private String fromState;
    private String toState;
    private String message;
    private String actor = "system";
    private long timestamp;

    public JobEventRecord() {
        this.timestamp = System.currentTimeMillis();
    }

    public JobEventRecord(String id, String projectId, String jobId, String agent, String message) {
        this();
        this.id = id;
        this.projectId = projectId;
        this.jobId = jobId;
        this.agent = agent;
        this.message = message;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }

    public String getFromState() { return fromState; }
    public void setFromState(String fromState) { this.fromState = fromState; }

    public String getToState() { return toState; }
    public void setToState(String toState) { this.toState = toState; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
