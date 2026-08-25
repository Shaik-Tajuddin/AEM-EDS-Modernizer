package com.adobe.aem.modernizer.dashboard.servlets;

import com.adobe.aem.modernizer.dashboard.ApiRouter;
import com.adobe.aem.modernizer.persistence.Store;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * REST Servlet for Modernizer Project Configuration and Sub-resources (Master §3, §31).
 * Serves {@code /bin/aem-eds-modernizer/projects/*} including dryrun, migrate, files, events.
 */
@Component(service = Servlet.class, immediate = true, property = {
    "sling.servlet.paths=/bin/aem-eds-modernizer/projects",
    "sling.servlet.methods=GET",
    "sling.servlet.methods=POST",
    "sling.servlet.methods=PUT",
    "sling.servlet.methods=DELETE"
})
public class ProjectServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectServlet.class);
    private final transient ApiRouter router;
    private final transient Store store;

    @Activate
    public ProjectServlet(@Reference ApiRouter router, @Reference Store store) {
        this.router = router;
        this.store = store;
    }

    public ProjectServlet() {
        this.router = null;
        this.store = null;
    }

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        handle(request, response);
    }

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        handle(request, response);
    }

    @Override
    public void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        handle(request, response);
    }

    private void handle(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        String suffix = request.getRequestPathInfo().getSuffix();
        String path = "projects" + (suffix != null ? suffix : "");
        String method = request.getMethod();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString();

        if (router != null) {
            String json = router.route(method, path, body, response);
            response.getWriter().write(json);
        } else {
            response.setStatus(503);
            response.getWriter().write("{\"error\":\"ApiRouter not initialized\"}");
        }
    }
}
