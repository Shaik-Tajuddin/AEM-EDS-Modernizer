package com.adobe.aem.modernizer.rag.servlets;

import com.adobe.aem.modernizer.rag.retrieval.RetrievalRequest;
import com.adobe.aem.modernizer.rag.retrieval.RetrievalResponse;
import com.adobe.aem.modernizer.rag.retrieval.RetrievalService;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * REST Endpoint for Hybrid Retrieval Search:
 * {@code GET /bin/modernizer/rag/search?q=...&projectId=...&topK=...}
 */
@Component(service = Servlet.class, immediate = true, property = {
        "sling.servlet.paths=/bin/modernizer/rag/search",
        "sling.servlet.methods=GET"
})
@SlingServletPaths(value = {
        "/bin/modernizer/rag/search",
        "/bin/modernizer/rag/search/"
})
public class RagSearchServlet extends SlingSafeMethodsServlet {

    @Reference
    private transient RetrievalService retrievalService;

    public RagSearchServlet() {
    }

    public RagSearchServlet(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String query = request.getParameter("q");
        if (query == null || query.isBlank()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\":\"Missing required query parameter 'q'\"}");
            return;
        }

        String projectId = request.getParameter("projectId");
        int topK = 8;
        if (request.getParameter("topK") != null) {
            try {
                topK = Integer.parseInt(request.getParameter("topK"));
            } catch (NumberFormatException ignored) {}
        }

        RetrievalRequest req = new RetrievalRequest(query, projectId != null ? projectId : "default");
        req.setTopK(topK);

        RetrievalResponse res = retrievalService.retrieve(req);
        response.getWriter().write(JsonUtil.toJson(res));
    }
}
