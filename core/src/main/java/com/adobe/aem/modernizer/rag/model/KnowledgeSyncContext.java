package com.adobe.aem.modernizer.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Context passed during knowledge scanning, tracking project ID, repository URL,
 * credentials, and inclusion/exclusion settings.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeSyncContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String syncId;
    private String projectId;
    private String sourceId;
    private String repoUrl;
    private String branch = "main";
    private String localPath;
    private boolean forceReindex = false;
    private Map<String, Object> options = new LinkedHashMap<>();

    public KnowledgeSyncContext() {
    }

    public KnowledgeSyncContext(String syncId, String projectId, String sourceId) {
        this.syncId = syncId;
        this.projectId = projectId;
        this.sourceId = sourceId;
    }

    public String getSyncId() {
        return syncId;
    }

    public void setSyncId(String syncId) {
        this.syncId = syncId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public boolean isForceReindex() {
        return forceReindex;
    }

    public void setForceReindex(boolean forceReindex) {
        this.forceReindex = forceReindex;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options != null ? new LinkedHashMap<>(options) : new LinkedHashMap<>();
    }
}
