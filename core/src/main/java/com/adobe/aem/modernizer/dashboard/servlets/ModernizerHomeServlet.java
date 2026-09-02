package com.adobe.aem.modernizer.dashboard.servlets;

import com.adobe.aem.modernizer.dashboard.StaticDashboard;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Dashboard servlet serving {@code /bin/aem-eds-modernizer/dashboard} and vanity paths.
 */
@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = {
    "/aem-eds-modernizer",
    "/aem-eds-modernizer.html",
    "/bin/aem-eds-modernizer/dashboard",
    "/bin/aem-eds-modernizer/dashboard.html"
})
public class ModernizerHomeServlet extends SlingSafeMethodsServlet {

    public static final String CANONICAL_HOME = "/content/aem-eds-modernizer/home.html";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        
        String html = StaticDashboard.html("/bin/aem-eds-modernizer/api");
        response.getWriter().write(html);
        response.getWriter().flush();
    }
}
