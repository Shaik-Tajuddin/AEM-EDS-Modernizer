package com.adobe.aem.modernizer.dashboard.models;

import com.adobe.aem.modernizer.agents.Orchestrator;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.BenchmarkSampleRecord;
import com.adobe.aem.modernizer.persistence.model.MigrationPlan;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.persistence.model.RepairAttemptRecord;
import com.adobe.aem.modernizer.persistence.model.RolloutStageRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

/**
 * Sling Model for the Modernizer Dashboard and Project Configuration (Master §3, §31).
 * Adapts Sling requests to provide structured, secure project metadata and execution states to HTL.
 */
@Model(
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        adapters = {ModernizerDashboardModel.class}
)
public class ModernizerDashboardModel {

    @Self
    @Optional
    private SlingHttpServletRequest request;

    @OSGiService
    @Optional
    private Store store;

    @OSGiService
    @Optional
    private Orchestrator orchestrator;

    private String apiBaseUrl = "/bin/aem-eds-modernizer/api";
    private String activeProjectId = "wknd-site";
    private ProjectRecord activeProject;
    private List<ProjectRecord> projects = Collections.emptyList();
    private SiteInventory inventory;
    private MigrationPlan plan;
    private List<RolloutStageRecord> rolloutStages = Collections.emptyList();
    private List<RepairAttemptRecord> repairs = Collections.emptyList();
    private List<BenchmarkSampleRecord> benchmarks = Collections.emptyList();

    public ModernizerDashboardModel() {
        // Default constructor for standalone or unit tests
    }

    public ModernizerDashboardModel(Store store, Orchestrator orchestrator) {
        this.store = store;
        this.orchestrator = orchestrator;
        init();
    }

    @PostConstruct
    protected void init() {
        if (request != null) {
            String scheme = request.getScheme();
            String host = request.getServerName();
            int port = request.getServerPort();
            this.apiBaseUrl = scheme + "://" + host
                    + ((port == 80 || port == 443) ? "" : (":" + port))
                    + "/bin/aem-eds-modernizer/api";

            String projParam = request.getParameter("project");
            if (projParam != null && !projParam.trim().isEmpty()) {
                this.activeProjectId = projParam.trim();
            }
        }

        if (store != null) {
            this.projects = store.listProjects();
            if (this.projects != null && !this.projects.isEmpty()) {
                boolean found = false;
                for (ProjectRecord p : this.projects) {
                    if (p.getId().equals(this.activeProjectId)) {
                        this.activeProject = p;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    this.activeProject = this.projects.get(0);
                    this.activeProjectId = this.activeProject.getId();
                }
            } else {
                this.activeProject = createDefaultWkndProject();
            }

            if (this.activeProjectId != null) {
                this.inventory = store.getInventory(this.activeProjectId).orElse(null);
                this.plan = store.getLatestPlan(this.activeProjectId).orElse(null);
                this.rolloutStages = store.getLatestRolloutStages(this.activeProjectId);
                this.repairs = store.getRepairAttemptsForProject(this.activeProjectId);
                this.benchmarks = store.getBenchmarkSamplesForProject(this.activeProjectId);
            }
        } else {
            this.activeProject = createDefaultWkndProject();
        }
    }

    private ProjectRecord createDefaultWkndProject() {
        ProjectRecord p = new ProjectRecord("wknd-site", "WKND Site Modernization", "http://localhost:4502", "/content/wknd", "https://github.com/my-org/wknd-eds");
        p.setAemPublishUrl("http://localhost:4503");
        p.setPageScope("/content/wknd/*");
        p.setEdsBranch("main");
        p.setMarkerProperty("edsModernize");
        p.setMarkerValue("true");
        p.setAuthoringStrategy("UNIVERSAL_EDITOR");
        p.setAiProvider("anthropic");
        p.setAiModel("claude-3-5-sonnet-20241022");
        p.setMaxBudgetUsd(100.0);
        p.setMaxRepairAttempts(5);
        return p;
    }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public String getActiveProjectId() { return activeProjectId; }
    public void setActiveProjectId(String activeProjectId) { this.activeProjectId = activeProjectId; }

    public ProjectRecord getActiveProject() { return activeProject; }
    public void setActiveProject(ProjectRecord activeProject) { this.activeProject = activeProject; }

    public List<ProjectRecord> getProjects() { return projects; }

    public SiteInventory getInventory() { return inventory; }
    public void setInventory(SiteInventory inventory) { this.inventory = inventory; }

    public MigrationPlan getPlan() { return plan; }
    public void setPlan(MigrationPlan plan) { this.plan = plan; }

    public List<RolloutStageRecord> getRolloutStages() { return rolloutStages; }
    public List<RepairAttemptRecord> getRepairs() { return repairs; }
    public List<BenchmarkSampleRecord> getBenchmarks() { return benchmarks; }

    public int getPagesCount() {
        return (inventory != null && inventory.getPages() != null) ? inventory.getPages().size() : 0;
    }

    public int getComponentsCount() {
        return (inventory != null && inventory.getComponents() != null) ? inventory.getComponents().size() : 0;
    }

    public double getExpectedCost() {
        return (plan != null) ? plan.getCostExpected() : 0.0;
    }

    public double getExpectedTimeSec() {
        return (plan != null) ? plan.getTimeExpectedSec() : 0.0;
    }

    public boolean isConfigured() {
        return activeProject != null && activeProject.getAemAuthorUrl() != null && activeProject.getContentRoot() != null;
    }
}
