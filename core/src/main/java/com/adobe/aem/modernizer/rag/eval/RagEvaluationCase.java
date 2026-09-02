package com.adobe.aem.modernizer.rag.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark evaluation case testing retrieval precision, groundedness, and citation correctness (Section 30).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RagEvaluationCase implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String question;
    private String expectedTopic;
    private List<String> expectedSources = new ArrayList<>();
    private double minExpectedConfidence = 0.70;

    // Actual evaluation outputs
    private double actualConfidence;
    private boolean sourcesMatched;
    private boolean citationVerified;
    private double groundednessScore;
    private long latencyMs;
    private boolean passed;

    public RagEvaluationCase() {
    }

    public RagEvaluationCase(String id, String question, String expectedTopic, List<String> expectedSources) {
        this.id = id;
        this.question = question;
        this.expectedTopic = expectedTopic;
        this.expectedSources = expectedSources != null ? expectedSources : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getExpectedTopic() {
        return expectedTopic;
    }

    public void setExpectedTopic(String expectedTopic) {
        this.expectedTopic = expectedTopic;
    }

    public List<String> getExpectedSources() {
        return expectedSources;
    }

    public void setExpectedSources(List<String> expectedSources) {
        this.expectedSources = expectedSources;
    }

    public double getMinExpectedConfidence() {
        return minExpectedConfidence;
    }

    public void setMinExpectedConfidence(double minExpectedConfidence) {
        this.minExpectedConfidence = minExpectedConfidence;
    }

    public double getActualConfidence() {
        return actualConfidence;
    }

    public void setActualConfidence(double actualConfidence) {
        this.actualConfidence = actualConfidence;
    }

    public boolean isSourcesMatched() {
        return sourcesMatched;
    }

    public void setSourcesMatched(boolean sourcesMatched) {
        this.sourcesMatched = sourcesMatched;
    }

    public boolean isCitationVerified() {
        return citationVerified;
    }

    public void setCitationVerified(boolean citationVerified) {
        this.citationVerified = citationVerified;
    }

    public double getGroundednessScore() {
        return groundednessScore;
    }

    public void setGroundednessScore(double groundednessScore) {
        this.groundednessScore = groundednessScore;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }
}
