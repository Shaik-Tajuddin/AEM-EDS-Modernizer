package com.adobe.aem.modernizer.dashboard.servlets;

import com.adobe.aem.modernizer.dashboard.StaticDashboard;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.io.IOException;

/**
 * Serves the Modernizer Dashboard SPA directly, bypassing HTL page rendering (Master §5, §6).
 */
@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = {"/aem-eds-modernizer", "/aem-eds-modernizer/"})
@SlingServletResourceTypes(
        resourceTypes = "aem-eds-modernizer/components/page/home",
        methods = "GET",
        extensions = "html"
)
public class ModernizerHomeServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String apiBase = scheme + "://" + host
                + (port == 80 || port == 443 ? "" : ":" + port)
                + "/bin/aem-eds-modernizer/api";

        String html = StaticDashboard.html(apiBase);
        response.setContentType("text/html; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(html);
    }
}
