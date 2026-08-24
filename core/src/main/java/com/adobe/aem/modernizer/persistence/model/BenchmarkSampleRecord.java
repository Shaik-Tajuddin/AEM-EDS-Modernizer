package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Persisted benchmark measurement sample (Phase 2).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BenchmarkSampleRecord {

    private String id;
    private String projectId;
    private String jobId;
    private String agent;
    private String operation;
    private long durationMs;
    private double costMicros;
    private int promptTokens;
    private int completionTokens;
    private boolean success;
    private long timestamp;

    public BenchmarkSampleRecord() {
        this.timestamp = System.currentTimeMillis();
    }

    public BenchmarkSampleRecord(String id, String projectId, String jobId, String agent, String operation, long durationMs, double costMicros) {
        this();
        this.id = id;
        this.projectId = projectId;
        this.jobId = jobId;
        this.agent = agent;
        this.operation = operation;
        this.durationMs = durationMs;
        this.costMicros = costMicros;
        this.success = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public double getCostMicros() { return costMicros; }
    public void setCostMicros(double costMicros) { this.costMicros = costMicros; }

    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }

    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
