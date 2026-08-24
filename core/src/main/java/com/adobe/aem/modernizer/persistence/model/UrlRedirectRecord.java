package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Persisted URL redirect record.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UrlRedirectRecord {

    private String id;
    private String projectId;
    private String jobId;
    private String sourceUrl;
    private String targetUrl;
    private int statusCode = 301;
    private String redirectType = "PAGE_PATH_CHANGE";
    private boolean conflict;
    private String conflictReason;

    public UrlRedirectRecord() {}

    public UrlRedirectRecord(String id, String projectId, String jobId, String sourceUrl, String targetUrl) {
        this.id = id;
        this.projectId = projectId;
        this.jobId = jobId;
        this.sourceUrl = sourceUrl;
        this.targetUrl = targetUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getRedirectType() { return redirectType; }
    public void setRedirectType(String redirectType) { this.redirectType = redirectType; }

    public boolean isConflict() { return conflict; }
    public void setConflict(boolean conflict) { this.conflict = conflict; }

    public String getConflictReason() { return conflictReason; }
    public void setConflictReason(String conflictReason) { this.conflictReason = conflictReason; }
}
