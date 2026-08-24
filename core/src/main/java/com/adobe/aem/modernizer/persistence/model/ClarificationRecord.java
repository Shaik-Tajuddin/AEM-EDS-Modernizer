package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Persisted clarification question for the operator.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClarificationRecord {

    private String id;
    private String projectId;
    private String jobId;
    private String question;
    private String rationale;
    private List<String> options = new ArrayList<>();
    private String defaultOption;
    private String selectedOption;
    private String status = "WAITING_FOR_USER"; // "WAITING_FOR_USER", "RESOLVED", "DEFAULT_APPLIED", "SKIPPED"
    private List<String> affectedPages = new ArrayList<>();
    private long createdAt;
    private long resolvedAt;

    public ClarificationRecord() {
        this.createdAt = System.currentTimeMillis();
    }

    public ClarificationRecord(String id, String projectId, String jobId, String question, String defaultOption) {
        this();
        this.id = id;
        this.projectId = projectId;
        this.jobId = jobId;
        this.question = question;
        this.defaultOption = defaultOption;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getDefaultOption() { return defaultOption; }
    public void setDefaultOption(String defaultOption) { this.defaultOption = defaultOption; }

    public String getSelectedOption() { return selectedOption; }
    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
        this.status = "RESOLVED";
        this.resolvedAt = System.currentTimeMillis();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getAffectedPages() { return affectedPages; }
    public void setAffectedPages(List<String> affectedPages) { this.affectedPages = affectedPages; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(long resolvedAt) { this.resolvedAt = resolvedAt; }
}
