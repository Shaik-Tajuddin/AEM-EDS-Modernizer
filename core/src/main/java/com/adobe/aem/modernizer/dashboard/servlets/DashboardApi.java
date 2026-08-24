package com.adobe.aem.modernizer.dashboard.servlets;

import com.adobe.aem.modernizer.dashboard.ApiRouter;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JSON API endpoint serving `/bin/aem-eds-modernizer/*` within AEM Author (Master §5).
 */
@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = {"/bin/aem-eds-modernizer/*"})
public class DashboardApi extends HttpServlet {

    private final transient ApiRouter router;

    @Activate
    public DashboardApi(@Reference ApiRouter router) {
        this.router = router;
    }

    public DashboardApi() {
        this.router = null;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (router != null) {
            router.handle(req, resp);
        } else {
            resp.sendError(503, "ApiRouter not initialized");
        }
    }
}
