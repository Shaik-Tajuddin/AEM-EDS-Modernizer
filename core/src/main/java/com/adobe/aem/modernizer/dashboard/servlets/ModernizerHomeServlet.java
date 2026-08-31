package com.adobe.aem.modernizer.dashboard.servlets;

import com.adobe.aem.modernizer.dashboard.StaticDashboard;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.io.IOException;

/**
 * Serves the Modernizer Dashboard SPA at direct vanity paths /aem-eds-modernizer (Master §5, §6).
 */
@Component(service = Servlet.class, immediate = true, property = {
    "sling.servlet.paths=/aem-eds-modernizer",
    "sling.servlet.paths=/aem-eds-modernizer.html",
    "sling.servlet.resourceTypes=aem-eds-modernizer/components/page/home",
    "sling.servlet.extensions=html",
    "sling.servlet.methods=GET"
})
public class ModernizerHomeServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String apiBase = scheme + "://" + host
                + ((port == 80 || port == 443) ? "" : (":" + port))
                + "/bin/aem-eds-modernizer/";

        String html = StaticDashboard.html(apiBase);
        response.setContentType("text/html; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(html);
    }
}
