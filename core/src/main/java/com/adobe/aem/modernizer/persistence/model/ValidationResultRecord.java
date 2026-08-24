package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Persisted validation result for a page or block.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationResultRecord {

    private String id;
    private String projectId;
    private String jobId;
    private String targetPath;
    private String validationType; // "FUNCTIONAL", "VISUAL", "A11Y", "SEO"
    private boolean passed;
    private double visualScore; // 0.0 to 1.0 (1.0 = perfect match)
    private double a11yScore;
    private List<String> issues = new ArrayList<>();
    private String screenshotBase64;
    private long timestamp;

    public ValidationResultRecord() {
        this.timestamp = System.currentTimeMillis();
        this.passed = true;
        this.visualScore = 1.0;
        this.a11yScore = 1.0;
    }

    public ValidationResultRecord(String id, String projectId, String jobId, String targetPath, String validationType, boolean passed) {
        this();
        this.id = id;
        this.projectId = projectId;
        this.jobId = jobId;
        this.targetPath = targetPath;
        this.validationType = validationType;
        this.passed = passed;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getTargetPath() { return targetPath; }
    public void setTargetPath(String targetPath) { this.targetPath = targetPath; }

    public String getValidationType() { return validationType; }
    public void setValidationType(String validationType) { this.validationType = validationType; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public double getVisualScore() { return visualScore; }
    public void setVisualScore(double visualScore) { this.visualScore = visualScore; }

    public double getA11yScore() { return a11yScore; }
    public void setA11yScore(double a11yScore) { this.a11yScore = a11yScore; }

    public List<String> getIssues() { return issues; }
    public void setIssues(List<String> issues) { this.issues = issues; }

    public String getScreenshotBase64() { return screenshotBase64; }
    public void setScreenshotBase64(String screenshotBase64) { this.screenshotBase64 = screenshotBase64; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
