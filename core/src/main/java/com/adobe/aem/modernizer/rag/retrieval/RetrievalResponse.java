package com.adobe.aem.modernizer.rag.retrieval;

import com.adobe.aem.modernizer.rag.model.Citation;
import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result payload of a hybrid retrieval execution containing ranked results, citations,
 * confidence evaluation, and channel breakdown statistics.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetrievalResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String query;
    private String projectId;
    private double confidenceScore;
    private String confidenceLevel = "LOW"; // HIGH, MEDIUM, LOW
    private List<RetrievalResult> results = new ArrayList<>();
    private List<Citation> citations = new ArrayList<>();
    private int totalDiscovered;
    private long executionDurationMs;

    public RetrievalResponse() {
    }

    public RetrievalResponse(String query, String projectId) {
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

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public List<RetrievalResult> getResults() {
        return results;
    }

    public void setResults(List<RetrievalResult> results) {
        this.results = results != null ? results : new ArrayList<>();
    }

    public List<Citation> getCitations() {
        return citations;
    }

    public void setCitations(List<Citation> citations) {
        this.citations = citations != null ? citations : new ArrayList<>();
    }

    public int getTotalDiscovered() {
        return totalDiscovered;
    }

    public void setTotalDiscovered(int totalDiscovered) {
        this.totalDiscovered = totalDiscovered;
    }

    public long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }
}
