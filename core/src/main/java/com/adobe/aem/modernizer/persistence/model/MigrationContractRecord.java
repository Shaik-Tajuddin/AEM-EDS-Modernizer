package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable Migration Contract approved before real migration (Master §11).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MigrationContractRecord {

    private String id;
    private String projectId;
    private String jobId;
    private String version = "1.0";
    private String approvedScope;
    private String authoringStrategy;
    private String assetPolicy;
    private String aiProvider;
    private String aiModel;
    private double approvedBudgetUsd;
    private int maxRepairAttempts;
    private boolean approvedByOperator;
    private String approvedBy;
    private long approvedAt;
    private List<String> acceptedRisks = new ArrayList<>();

    public MigrationContractRecord() {}

    public MigrationContractRecord(String id, String projectId, String jobId) {
        this.id = id;
        this.projectId = projectId;
        this.jobId = jobId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getApprovedScope() { return approvedScope; }
    public void setApprovedScope(String approvedScope) { this.approvedScope = approvedScope; }

    public String getAuthoringStrategy() { return authoringStrategy; }
    public void setAuthoringStrategy(String authoringStrategy) { this.authoringStrategy = authoringStrategy; }

    public String getAssetPolicy() { return assetPolicy; }
    public void setAssetPolicy(String assetPolicy) { this.assetPolicy = assetPolicy; }

    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }

    public double getApprovedBudgetUsd() { return approvedBudgetUsd; }
    public void setApprovedBudgetUsd(double approvedBudgetUsd) { this.approvedBudgetUsd = approvedBudgetUsd; }

    public int getMaxRepairAttempts() { return maxRepairAttempts; }
    public void setMaxRepairAttempts(int maxRepairAttempts) { this.maxRepairAttempts = maxRepairAttempts; }

    public boolean isApprovedByOperator() { return approvedByOperator; }
    public void setApprovedByOperator(boolean approvedByOperator) { this.approvedByOperator = approvedByOperator; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public long getApprovedAt() { return approvedAt; }
    public void setApprovedAt(long approvedAt) { this.approvedAt = approvedAt; }

    public List<String> getAcceptedRisks() { return acceptedRisks != null ? new ArrayList<>(acceptedRisks) : new ArrayList<>(); }
    public void setAcceptedRisks(List<String> acceptedRisks) { this.acceptedRisks = acceptedRisks != null ? new ArrayList<>(acceptedRisks) : new ArrayList<>(); }
}
