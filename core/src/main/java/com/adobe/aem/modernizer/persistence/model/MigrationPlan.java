package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Migration Plan and pre-implementation / Dry Run estimate.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MigrationPlan {

    private String projectId;
    private String jobId;
    private String version = "1.0";
    private int pagesEligible;
    private int edsBlocksNew;
    private int aiRequestsExpected;
    private double costExpected;
    private double costLo;
    private double costHi;
    private long timeExpectedSec;
    private long timeLoSec;
    private long timeHiSec;
    private int validationsExpected;
    private int repairsExpected;
    private double automationConfidence = 0.92;
    private String status = "CURRENT"; // "CURRENT" or "STALE"
    private List<String> derivationTrail = new ArrayList<>();
    private List<String> blockers = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private Map<String, Object> details = new HashMap<>();
    private long generatedAt;

    public MigrationPlan() {
        this.generatedAt = System.currentTimeMillis();
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public int getPagesEligible() { return pagesEligible; }
    public void setPagesEligible(int pagesEligible) { this.pagesEligible = pagesEligible; }

    public int getEdsBlocksNew() { return edsBlocksNew; }
    public void setEdsBlocksNew(int edsBlocksNew) { this.edsBlocksNew = edsBlocksNew; }

    public int getAiRequestsExpected() { return aiRequestsExpected; }
    public void setAiRequestsExpected(int aiRequestsExpected) { this.aiRequestsExpected = aiRequestsExpected; }

    public double getCostExpected() { return costExpected; }
    public void setCostExpected(double costExpected) { this.costExpected = costExpected; }

    public double getCostLo() { return costLo; }
    public void setCostLo(double costLo) { this.costLo = costLo; }

    public double getCostHi() { return costHi; }
    public void setCostHi(double costHi) { this.costHi = costHi; }

    public long getTimeExpectedSec() { return timeExpectedSec; }
    public void setTimeExpectedSec(long timeExpectedSec) { this.timeExpectedSec = timeExpectedSec; }

    public long getTimeLoSec() { return timeLoSec; }
    public void setTimeLoSec(long timeLoSec) { this.timeLoSec = timeLoSec; }

    public long getTimeHiSec() { return timeHiSec; }
    public void setTimeHiSec(long timeHiSec) { this.timeHiSec = timeHiSec; }

    public int getValidationsExpected() { return validationsExpected; }
    public void setValidationsExpected(int validationsExpected) { this.validationsExpected = validationsExpected; }

    public int getRepairsExpected() { return repairsExpected; }
    public void setRepairsExpected(int repairsExpected) { this.repairsExpected = repairsExpected; }

    public double getAutomationConfidence() { return automationConfidence; }
    public void setAutomationConfidence(double automationConfidence) { this.automationConfidence = automationConfidence; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getDerivationTrail() { return derivationTrail != null ? new ArrayList<>(derivationTrail) : new ArrayList<>(); }
    public void setDerivationTrail(List<String> derivationTrail) { this.derivationTrail = derivationTrail != null ? new ArrayList<>(derivationTrail) : new ArrayList<>(); }

    public List<String> getBlockers() { return blockers != null ? new ArrayList<>(blockers) : new ArrayList<>(); }
    public void setBlockers(List<String> blockers) { this.blockers = blockers != null ? new ArrayList<>(blockers) : new ArrayList<>(); }

    public List<String> getWarnings() { return warnings != null ? new ArrayList<>(warnings) : new ArrayList<>(); }
    public void setWarnings(List<String> warnings) { this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>(); }

    public Map<String, Object> getDetails() { return details != null ? new HashMap<>(details) : new HashMap<>(); }
    public void setDetails(Map<String, Object> details) { this.details = details != null ? new HashMap<>(details) : new HashMap<>(); }

    public long getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(long generatedAt) { this.generatedAt = generatedAt; }
}
