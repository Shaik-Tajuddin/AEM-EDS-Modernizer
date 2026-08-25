package com.adobe.aem.modernizer.dashboard.servlets;

import com.adobe.aem.modernizer.dashboard.ApiRouter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * JSON API servlet serving {@code /bin/aem-eds-modernizer/api/*} within AEM Author.
 */
@Component(service = Servlet.class, immediate = true, property = {
    "sling.servlet.paths=/bin/aem-eds-modernizer/api",
    "sling.servlet.methods=GET",
    "sling.servlet.methods=POST",
    "sling.servlet.methods=DELETE"
})
@SlingServletPaths(value = {
    "/bin/aem-eds-modernizer/api",
    "/bin/aem-eds-modernizer/api/",
    "/bin/aem-eds-modernizer/api/projects",
    "/bin/aem-eds-modernizer/api/projects/"
})
public class DashboardApi extends SlingAllMethodsServlet {

    private final transient ApiRouter router;

    @Activate
    public DashboardApi(@Reference ApiRouter router) {
        this.router = router;
    }

    public DashboardApi() {
        this.router = null;
    }

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        if (router != null) {
            router.handle(request, response);
        } else {
            response.sendError(503, "ApiRouter not initialized");
        }
    }

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        if (router != null) {
            router.handle(request, response);
        } else {
            response.sendError(503, "ApiRouter not initialized");
        }
    }

    @Override
    public void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        if (router != null) {
            router.handle(request, response);
        } else {
            response.sendError(503, "ApiRouter not initialized");
        }
    }
}
