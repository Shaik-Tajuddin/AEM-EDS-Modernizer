package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.persistence.model.JobRecord;
import com.adobe.aem.modernizer.persistence.model.MigrationPlan;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Context passed to agents during execution.
 */
public class AgentContext {

    private final ProjectRecord project;
    private final JobRecord job;
    private SiteInventory inventory;
    private MigrationPlan plan;
    private boolean dryRun;
    private int repairAttempts = 0;
    private String lastGeneratedPrUrl;
    private final Map<String, Object> attributes = new HashMap<>();

    public AgentContext(ProjectRecord project, JobRecord job) {
        this.project = project;
        this.job = job;
        this.dryRun = job != null && job.isDryRun();
    }

    public ProjectRecord getProject() { return project; }
    public JobRecord getJob() { return job; }

    public SiteInventory getInventory() { return inventory; }
    public void setInventory(SiteInventory inventory) { this.inventory = inventory; }

    public MigrationPlan getPlan() { return plan; }
    public void setPlan(MigrationPlan plan) { this.plan = plan; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public int getRepairAttempts() { return repairAttempts; }
    public void incrementRepairAttempts() { this.repairAttempts++; }

    public String getLastGeneratedPrUrl() { return lastGeneratedPrUrl; }
    public void setLastGeneratedPrUrl(String lastGeneratedPrUrl) { this.lastGeneratedPrUrl = lastGeneratedPrUrl; }

    public Map<String, Object> getAttributes() { return attributes; }
}
