package com.adobe.aem.modernizer.rag.servlets;

import com.adobe.aem.modernizer.rag.model.KnowledgeSyncRun;
import com.adobe.aem.modernizer.rag.persistence.RagStore;
import com.adobe.aem.modernizer.rag.source.EDSRepositoryKnowledgeSource;
import com.adobe.aem.modernizer.rag.sync.RagSyncJobConsumer;
import com.adobe.aem.modernizer.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * REST Endpoint for RAG Knowledge Synchronization:
 * - {@code POST /bin/modernizer/rag/sync} — Dispatches background Sling Job
 * - {@code GET /bin/modernizer/rag/sync} — Polls status of sync execution
 */
@Component(service = Servlet.class, immediate = true, property = {
        "sling.servlet.paths=/bin/modernizer/rag/sync",
        "sling.servlet.methods=GET",
        "sling.servlet.methods=POST"
})
@SlingServletPaths(value = {
        "/bin/modernizer/rag/sync",
        "/bin/modernizer/rag/sync/"
})
public class RagSyncServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(RagSyncServlet.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private transient RagStore ragStore;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient JobManager jobManager;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient RagSyncJobConsumer jobConsumer;

    public RagSyncServlet() {
    }

    public RagSyncServlet(RagStore ragStore, JobManager jobManager) {
        this.ragStore = ragStore;
        this.jobManager = jobManager;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String syncId = request.getParameter("syncId");
        String projectId = request.getParameter("projectId");

        if ("sync".equalsIgnoreCase(request.getParameter("action")) || "true".equalsIgnoreCase(request.getParameter("sync"))) {
            doPost(request, response);
            return;
        }

        if (syncId != null && !syncId.isBlank()) {
            Optional<KnowledgeSyncRun> run = ragStore.getSyncRun(syncId);
            if (run.isPresent()) {
                response.getWriter().write(JsonUtil.toJson(run.get()));
            } else {
                response.setStatus(404);
                response.getWriter().write("{\"error\":\"Sync run not found: " + syncId + "\"}");
            }
            return;
        }

        // List all runs for project
        List<KnowledgeSyncRun> runs = ragStore.listSyncRuns(projectId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("projectId", projectId != null ? projectId : "all");
        out.put("runs", runs);
        out.put("totalDocuments", ragStore.getDocumentCount(projectId));
        out.put("totalChunks", ragStore.getChunkCount(projectId));
        response.getWriter().write(JsonUtil.toJson(out));
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String projectId = "default";
        boolean forceReindex = false;
        String repoUrl = null;
        String branch = "main";
        String localPath = null;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString();

        if (!body.isBlank()) {
            try {
                JsonNode json = MAPPER.readTree(body);
                if (json.has("projectId")) projectId = json.get("projectId").asText();
                if (json.has("forceReindex")) forceReindex = json.get("forceReindex").asBoolean();
                if (json.has("repoUrl")) repoUrl = json.get("repoUrl").asText();
                if (json.has("branch")) branch = json.get("branch").asText();
                if (json.has("localPath")) localPath = json.get("localPath").asText();
            } catch (Exception e) {
                LOG.debug("Could not parse sync POST body as JSON, reading query params", e);
            }
        }

        if (request.getParameter("projectId") != null) {
            projectId = request.getParameter("projectId");
        }
        if ("true".equalsIgnoreCase(request.getParameter("forceReindex"))) {
            forceReindex = true;
        }

        String syncId = "sync-" + System.currentTimeMillis();
        KnowledgeSyncRun run = new KnowledgeSyncRun(syncId, projectId, EDSRepositoryKnowledgeSource.SOURCE_ID);
        run.setStatus("CREATED");
        ragStore.saveSyncRun(run);

        Map<String, Object> jobProps = new HashMap<>();
        jobProps.put("syncId", syncId);
        jobProps.put("projectId", projectId);
        jobProps.put("forceReindex", forceReindex);
        if (repoUrl != null) jobProps.put("repoUrl", repoUrl);
        if (branch != null) jobProps.put("branch", branch);
        if (localPath != null) jobProps.put("localPath", localPath);

        boolean queued = false;
        if (jobManager != null) {
            jobManager.addJob(RagSyncJobConsumer.JOB_TOPIC, jobProps);
            queued = true;
            LOG.info("Dispatched Sling Job for RAG Sync [{}] via JobManager", syncId);
        } else if (jobConsumer != null) {
            // Fallback for standalone/local tests
            final String fSyncId = syncId;
            final String fProjId = projectId;
            final boolean fForce = forceReindex;
            final String fRepo = repoUrl;
            final String fBranch = branch;
            final String fLocal = localPath;
            CompletableFuture.runAsync(() -> {
                jobConsumer.runSync(fSyncId, fProjId, fForce, fRepo, fBranch, fLocal);
            });
            queued = true;
            LOG.info("Dispatched async task for RAG Sync [{}] via fallback consumer", syncId);
        }

        response.setStatus(202);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "ACCEPTED");
        res.put("syncId", syncId);
        res.put("projectId", projectId);
        res.put("queued", queued);
        res.put("message", "Knowledge repository synchronization started in background Sling Job.");
        response.getWriter().write(JsonUtil.toJson(res));
    }
}
