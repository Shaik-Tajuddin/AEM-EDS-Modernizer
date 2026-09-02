package com.adobe.aem.modernizer.rag.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the results of a full RAG evaluation benchmark run.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RagEvaluationRun implements Serializable {

    private static final long serialVersionUID = 1L;

    private String runId;
    private String projectId;
    private String executedAt;
    private int totalCases;
    private int passedCases;
    private double overallScore; // 0.0 to 1.0
    private double averagePrecision;
    private double averageGroundedness;
    private double citationCorrectnessRate;
    private double averageLatencyMs;
    private List<RagEvaluationCase> cases = new ArrayList<>();

    public RagEvaluationRun() {
        this.executedAt = Instant.now().toString();
    }

    public RagEvaluationRun(String runId, String projectId) {
        this();
        this.runId = runId;
        this.projectId = projectId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(String executedAt) {
        this.executedAt = executedAt;
    }

    public int getTotalCases() {
        return totalCases;
    }

    public void setTotalCases(int totalCases) {
        this.totalCases = totalCases;
    }

    public int getPassedCases() {
        return passedCases;
    }

    public void setPassedCases(int passedCases) {
        this.passedCases = passedCases;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public double getAveragePrecision() {
        return averagePrecision;
    }

    public void setAveragePrecision(double averagePrecision) {
        this.averagePrecision = averagePrecision;
    }

    public double getAverageGroundedness() {
        return averageGroundedness;
    }

    public void setAverageGroundedness(double averageGroundedness) {
        this.averageGroundedness = averageGroundedness;
    }

    public double getCitationCorrectnessRate() {
        return citationCorrectnessRate;
    }

    public void setCitationCorrectnessRate(double citationCorrectnessRate) {
        this.citationCorrectnessRate = citationCorrectnessRate;
    }

    public double getAverageLatencyMs() {
        return averageLatencyMs;
    }

    public void setAverageLatencyMs(double averageLatencyMs) {
        this.averageLatencyMs = averageLatencyMs;
    }

    public List<RagEvaluationCase> getCases() {
        return cases;
    }

    public void setCases(List<RagEvaluationCase> cases) {
        this.cases = cases != null ? cases : new ArrayList<>();
    }
}
