package com.adobe.aem.modernizer.dashboard.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.io.IOException;

/**
 * Vanity-path entry for the Modernizer Dashboard.
 * <p>
 * The canonical UI is the HTL page component at
 * {@code /content/aem-eds-modernizer/home} ({@code home.html} + clientlibs).
 * This servlet only redirects legacy vanity URLs so we do not maintain a second
 * inline HTML renderer ({@code StaticDashboard}).
 */
@Component(service = Servlet.class, immediate = true, property = {
    "sling.servlet.paths=/aem-eds-modernizer",
    "sling.servlet.paths=/aem-eds-modernizer.html",
    "sling.servlet.methods=GET"
})
public class ModernizerHomeServlet extends SlingSafeMethodsServlet {

    public static final String CANONICAL_HOME = "/content/aem-eds-modernizer/home.html";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String qs = request.getQueryString();
        String target = CANONICAL_HOME + (qs == null || qs.isEmpty() ? "" : ("?" + qs));
        response.setStatus(SlingHttpServletResponse.SC_FOUND);
        response.setHeader("Location", target);
        response.setHeader("Cache-Control", "no-store");
    }
}
