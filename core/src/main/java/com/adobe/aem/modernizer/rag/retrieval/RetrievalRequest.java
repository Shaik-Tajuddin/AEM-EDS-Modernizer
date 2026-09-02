package com.adobe.aem.modernizer.rag.retrieval;

import com.adobe.aem.modernizer.rag.model.Citation;
import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a multi-channel retrieval query.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetrievalRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String query;
    private String projectId;
    private String userId;
    private int topK = 8;
    private double minSimilarity = 0.55;
    private boolean includeAemFacts = true;
    private boolean includeMigrationHistory = true;
    private boolean includeEdsKnowledge = true;

    public RetrievalRequest() {
    }

    public RetrievalRequest(String query, String projectId) {
        this.query = query;
        this.projectId = projectId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinSimilarity() {
        return minSimilarity;
    }

    public void setMinSimilarity(double minSimilarity) {
        this.minSimilarity = minSimilarity;
    }

    public boolean isIncludeAemFacts() {
        return includeAemFacts;
    }

    public void setIncludeAemFacts(boolean includeAemFacts) {
        this.includeAemFacts = includeAemFacts;
    }

    public boolean isIncludeMigrationHistory() {
        return includeMigrationHistory;
    }

    public void setIncludeMigrationHistory(boolean includeMigrationHistory) {
        this.includeMigrationHistory = includeMigrationHistory;
    }

    public boolean isIncludeEdsKnowledge() {
        return includeEdsKnowledge;
    }

    public void setIncludeEdsKnowledge(boolean includeEdsKnowledge) {
        this.includeEdsKnowledge = includeEdsKnowledge;
    }
}
