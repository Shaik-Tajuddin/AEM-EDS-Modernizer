package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Persisted dependency edge between components, blocks, or pages.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DependencyEdgeRecord {

    private String id;
    private String projectId;
    private String jobId;
    private String source; // e.g. "page:/content/wknd/en/adventures"
    private String target; // e.g. "block:adventure-card"
    private String edgeType; // "PAGE_TO_BLOCK", "BLOCK_TO_CSS", "PAGE_TO_ASSET"
    private String impactLevel = "LOW"; // "LOW", "MEDIUM", "HIGH"

    public DependencyEdgeRecord() {}

    public DependencyEdgeRecord(String id, String projectId, String jobId, String source, String target, String edgeType) {
        this.id = id;
        this.projectId = projectId;
        this.jobId = jobId;
        this.source = source;
        this.target = target;
        this.edgeType = edgeType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getEdgeType() { return edgeType; }
    public void setEdgeType(String edgeType) { this.edgeType = edgeType; }

    public String getImpactLevel() { return impactLevel; }
    public void setImpactLevel(String impactLevel) { this.impactLevel = impactLevel; }
}
