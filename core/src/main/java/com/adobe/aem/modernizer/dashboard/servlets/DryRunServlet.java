package com.adobe.aem.modernizer.dashboard.servlets;

import com.adobe.aem.modernizer.ModernizerException;
import com.adobe.aem.modernizer.agents.Orchestrator;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobRecord;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

/**
 * Servlet for triggering Modernizer Dry Runs (Discovery, Analysis, Estimation).
 */
@Component(service = Servlet.class, immediate = true, property = {
    "sling.servlet.paths=/bin/aem-eds-modernizer/dryrun",
    "sling.servlet.methods=GET",
    "sling.servlet.methods=POST"
})
public class DryRunServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(DryRunServlet.class);
    private final transient Store store;
    private final transient Orchestrator orchestrator;

    @Activate
    public DryRunServlet(@Reference Store store, @Reference Orchestrator orchestrator) {
        this.store = store;
        this.orchestrator = orchestrator;
    }

    public DryRunServlet() {
        this.store = null;
        this.orchestrator = null;
    }

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String projectId = request.getParameter("projectId");
        if (projectId == null || projectId.trim().isEmpty()) {
            projectId = "wknd-site";
        }

        ProjectRecord project = (store != null) ? store.getProject(projectId).orElse(null) : null;
        if (project == null) {
            project = new ProjectRecord(projectId, "Project " + projectId, "http://localhost:4502", "/content/wknd", "https://github.com/my-org/wknd-eds");
            if (store != null) {
                store.saveProject(project);
            }
        }

        String actor = (request.getUserPrincipal() != null) ? request.getUserPrincipal().getName() : "admin";
        LOG.info("Initiating Dry Run for project={} by actor={}", projectId, actor);

        try {
            JobRecord job = (orchestrator != null)
                    ? orchestrator.runDryRun(project, actor)
                    : new JobRecord("job-mock-dryrun", projectId, "DRY_RUN");
            response.getWriter().write(JsonUtil.toJson(job));
        } catch (ModernizerException e) {
            LOG.error("Dry run error: {}", e.getMessage(), e);
            response.setStatus(500);
            response.getWriter().write("{\"error\":\"Dry run failed: " + e.getMessage() + "\"}");
        }
    }
}
