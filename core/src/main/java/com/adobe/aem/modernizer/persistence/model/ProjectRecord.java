package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Persisted Project representation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectRecord {

    private String id;
    private String name;
    private String aemAuthorUrl;
    private String aemPublishUrl;
    private String contentRoot;
    private String pageScope;
    private String scopeMode = "RECURSIVE";
    private String edsGitRepoUrl;
    private String edsBranch;
    private String figmaUrl;
    private String markerProperty;
    private String markerValue;
    private String authoringStrategy;
    private String aiProvider;
    private String aiModel;
    private double maxBudgetUsd;
    private int maxRepairAttempts;
    private boolean buildDocs = false;
    private long createdAt;
    private long updatedAt;
    private Map<String, Object> properties = new HashMap<>();

    public ProjectRecord() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.maxBudgetUsd = 100.0;
        this.maxRepairAttempts = 5;
        this.authoringStrategy = "UNIVERSAL_EDITOR";
        this.scopeMode = "RECURSIVE";
        this.buildDocs = false;
    }

    public ProjectRecord(String id, String name, String aemAuthorUrl, String contentRoot, String edsGitRepoUrl) {
        this();
        this.id = id;
        this.name = name;
        this.aemAuthorUrl = aemAuthorUrl;
        this.contentRoot = contentRoot;
        this.edsGitRepoUrl = edsGitRepoUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAemAuthorUrl() { return aemAuthorUrl; }
    public void setAemAuthorUrl(String aemAuthorUrl) { this.aemAuthorUrl = aemAuthorUrl; }

    public String getAemPublishUrl() { return aemPublishUrl; }
    public void setAemPublishUrl(String aemPublishUrl) { this.aemPublishUrl = aemPublishUrl; }

    public String getContentRoot() { return contentRoot; }
    public void setContentRoot(String contentRoot) { this.contentRoot = contentRoot; }

    public String getPageScope() { return pageScope; }
    public void setPageScope(String pageScope) { this.pageScope = pageScope; }

    public String getScopeMode() { return scopeMode; }
    public void setScopeMode(String scopeMode) { this.scopeMode = scopeMode; }

    public String getEdsGitRepoUrl() { return edsGitRepoUrl; }
    public void setEdsGitRepoUrl(String edsGitRepoUrl) { this.edsGitRepoUrl = edsGitRepoUrl; }

    public String getEdsBranch() { return edsBranch; }
    public void setEdsBranch(String edsBranch) { this.edsBranch = edsBranch; }

    public String getFigmaUrl() { return figmaUrl; }
    public void setFigmaUrl(String figmaUrl) { this.figmaUrl = figmaUrl; }

    public String getMarkerProperty() { return markerProperty; }
    public void setMarkerProperty(String markerProperty) { this.markerProperty = markerProperty; }

    public String getMarkerValue() { return markerValue; }
    public void setMarkerValue(String markerValue) { this.markerValue = markerValue; }

    public String getAuthoringStrategy() { return authoringStrategy; }
    public void setAuthoringStrategy(String authoringStrategy) { this.authoringStrategy = authoringStrategy; }

    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }

    public double getMaxBudgetUsd() { return maxBudgetUsd; }
    public void setMaxBudgetUsd(double maxBudgetUsd) { this.maxBudgetUsd = maxBudgetUsd; }

    public int getMaxRepairAttempts() { return maxRepairAttempts; }
    public void setMaxRepairAttempts(int maxRepairAttempts) { this.maxRepairAttempts = maxRepairAttempts; }

    public boolean isBuildDocs() { return buildDocs; }
    public boolean getBuildDocs() { return buildDocs; }
    public void setBuildDocs(boolean buildDocs) { this.buildDocs = buildDocs; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Object> getProperties() { return properties != null ? new HashMap<>(properties) : new HashMap<>(); }
    public void setProperties(Map<String, Object> properties) { this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>(); }
}
