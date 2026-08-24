package com.adobe.aem.modernizer.dashboard.servlets;

import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Servlet for Modernizer Project Configuration and Management (Master §3, §31).
 * Supports GET (list or by ID), POST (create/update), and DELETE operations.
 */
@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = {"/bin/aem-eds-modernizer/projects", "/bin/aem-eds-modernizer/projects/"})
public class ProjectServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectServlet.class);
    private final transient Store store;

    @Activate
    public ProjectServlet(@Reference Store store) {
        this.store = store;
    }

    public ProjectServlet() {
        this.store = null;
    }

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String projectId = extractProjectId(request);

        if (projectId != null && !projectId.isEmpty()) {
            Optional<ProjectRecord> project = (store != null) ? store.getProject(projectId) : Optional.empty();
            if (project.isPresent()) {
                response.getWriter().write(JsonUtil.toJson(project.get()));
            } else {
                response.setStatus(404);
                response.getWriter().write("{\"error\":\"Project not found: " + projectId + "\"}");
            }
        } else {
            List<ProjectRecord> list = (store != null) ? store.listProjects() : Collections.emptyList();
            response.getWriter().write(JsonUtil.toJson(list));
        }
    }

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String body = sb.toString();
        ProjectRecord project = JsonUtil.fromJson(body, ProjectRecord.class);
        if (project == null) {
            project = new ProjectRecord();
        }

        if (project.getId() == null || project.getId().trim().isEmpty()) {
            project.setId("proj-" + UUID.randomUUID().toString().substring(0, 8));
        }

        if (project.getName() == null || project.getName().trim().isEmpty()) {
            project.setName("Project " + project.getId());
        }

        if (store != null) {
            store.saveProject(project);
            LOG.info("Saved project: id={}, name={}, contentRoot={}", project.getId(), project.getName(), project.getContentRoot());
        }

        response.setStatus(200);
        response.getWriter().write(JsonUtil.toJson(project));
    }

    @Override
    public void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String projectId = extractProjectId(request);

        if (projectId != null && !projectId.isEmpty()) {
            if (store != null) {
                store.deleteProject(projectId);
                LOG.info("Deleted project: id={}", projectId);
            }
            response.getWriter().write("{\"status\":\"DELETED\",\"id\":\"" + projectId + "\"}");
        } else {
            response.setStatus(400);
            response.getWriter().write("{\"error\":\"Missing project id for DELETE\"}");
        }
    }

    private String extractProjectId(SlingHttpServletRequest request) {
        String suffix = request.getRequestPathInfo().getSuffix();
        if (suffix != null && suffix.startsWith("/")) {
            suffix = suffix.substring(1);
        }
        if (suffix != null && !suffix.isEmpty()) {
            return suffix;
        }
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.trim().isEmpty()) {
            return idParam.trim();
        }
        return null;
    }
}
