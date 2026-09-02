package com.adobe.aem.modernizer.rag.servlets;

import com.adobe.aem.modernizer.rag.eval.RagEvaluationRun;
import com.adobe.aem.modernizer.rag.eval.RagEvaluationService;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * REST Endpoint for RAG Benchmark Evaluation:
 * {@code POST /bin/modernizer/rag/evaluate?projectId=...}
 */
@Component(service = Servlet.class, immediate = true, property = {
        "sling.servlet.paths=/bin/modernizer/rag/evaluate",
        "sling.servlet.methods=GET",
        "sling.servlet.methods=POST"
})
@SlingServletPaths(value = {
        "/bin/modernizer/rag/evaluate",
        "/bin/modernizer/rag/evaluate/"
})
public class RagEvaluationServlet extends SlingAllMethodsServlet {

    @Reference
    private transient RagEvaluationService evaluationService;

    public RagEvaluationServlet() {
    }

    public RagEvaluationServlet(RagEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String projectId = request.getParameter("projectId");
        if (projectId == null || projectId.isBlank()) {
            projectId = "default";
        }

        if (evaluationService != null) {
            RagEvaluationRun run = evaluationService.runEvaluation(projectId);
            response.getWriter().write(JsonUtil.toJson(run));
        } else {
            response.setStatus(503);
            response.getWriter().write("{\"error\":\"RagEvaluationService not initialized\"}");
        }
    }
}
