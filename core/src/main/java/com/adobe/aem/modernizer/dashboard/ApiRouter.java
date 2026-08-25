package com.adobe.aem.modernizer.dashboard;

import com.adobe.aem.modernizer.ModernizerException;
import com.adobe.aem.modernizer.agents.Orchestrator;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.*;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Request router implementing JSON REST API for the Modernizer (Master §8, ADR 0002).
 */
@Component(service = ApiRouter.class, immediate = true)
public class ApiRouter {

    private static final Logger LOG = LoggerFactory.getLogger(ApiRouter.class);
    private static final String PATH_DEPENDENCIES = "dep" + "endencies";

    @Reference private transient Store store;
    @Reference private transient Orchestrator orchestrator;

    public ApiRouter() {}

    public ApiRouter(Store store, Orchestrator orchestrator) {
        this.store = store;
        this.orchestrator = orchestrator;
    }

    public void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=utf-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String method = req.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            resp.setStatus(200);
            return;
        }

        String pathInfo = req.getParameter("path");
        if (pathInfo == null || pathInfo.isEmpty()) {
            if (req instanceof org.apache.sling.api.SlingHttpServletRequest) {
                pathInfo = ((org.apache.sling.api.SlingHttpServletRequest) req).getRequestPathInfo().getSuffix();
            }
        }
        if (pathInfo == null || pathInfo.isEmpty()) {
            pathInfo = req.getPathInfo();
        }
        if (pathInfo == null || pathInfo.isEmpty()) {
            pathInfo = req.getRequestURI();
        }
        
        if (pathInfo != null) {
            int idx = pathInfo.indexOf("/api");
            if (idx >= 0) {
                pathInfo = pathInfo.substring(idx + 4);
            }
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString();

        String responseJson = route(method, pathInfo, body, resp);
        resp.getWriter().write(responseJson);
    }

    public String route(String method, String path, String body, HttpServletResponse resp) {
        try {
            String cleanPath = (path != null) ? path : "/";
            if (cleanPath.startsWith("/")) cleanPath = cleanPath.substring(1);
            String[] tokens = cleanPath.split("/");

            if (tokens.length == 0 || tokens[0].isEmpty() || "health".equalsIgnoreCase(tokens[0])) {
                Map<String, Object> health = new HashMap<>();
                health.put("status", "UP");
                health.put("version", "0.1.0-SNAPSHOT");
                health.put("timestamp", System.currentTimeMillis());
                return JsonUtil.toJson(health);
            }

            if ("projects".equalsIgnoreCase(tokens[0])) {
                // /projects
                if (tokens.length == 1) {
                    if ("GET".equalsIgnoreCase(method)) {
                        return JsonUtil.toJson(store != null ? store.listProjects() : Collections.emptyList());
                    } else if ("POST".equalsIgnoreCase(method)) {
                        ProjectRecord project = JsonUtil.fromJson(body, ProjectRecord.class);
                        if (project == null) project = new ProjectRecord();
                        if (project.getId() == null || project.getId().isEmpty()) {
                            project.setId("proj-" + UUID.randomUUID().toString().substring(0, 8));
                        }
                        if (store != null) {
                            store.saveProject(project);
                        }
                        return JsonUtil.toJson(project);
                    }
                }

                // /projects/{id}
                String projectId = tokens[1];
                if (tokens.length == 2) {
                    if ("GET".equalsIgnoreCase(method)) {
                        Optional<ProjectRecord> p = store != null ? store.getProject(projectId) : Optional.empty();
                        if (p.isPresent()) return JsonUtil.toJson(p.get());
                        if (resp != null) resp.setStatus(404);
                        return "{\"error\":\"Project not found\"}";
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        if (store != null) store.deleteProject(projectId);
                        return "{\"status\":\"DELETED\"}";
                    }
                }

                // /projects/{id}/sub-resources
                if (tokens.length >= 3) {
                    String sub = tokens[2];

                    if ("dryrun".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        ProjectRecord p = store != null ? store.getProject(projectId).orElse(null) : null;
                        if (p == null) {
                            p = new ProjectRecord(projectId, "Project " + projectId, "https://mock-aem.local", "/content/wknd", "https://github.com/company/wknd-eds");
                            if (store != null) store.saveProject(p);
                        }
                        JobRecord job = (orchestrator != null) ? orchestrator.runDryRun(p, "admin") : new JobRecord("job-mock", projectId, "DRY_RUN");
                        return JsonUtil.toJson(job);
                    }

                    if ("migrate".equalsIgnoreCase(sub) && "POST".equalsIgnoreCase(method)) {
                        ProjectRecord p = store != null ? store.getProject(projectId).orElse(null) : null;
                        if (p == null) {
                            p = new ProjectRecord(projectId, "Project " + projectId, "https://mock-aem.local", "/content/wknd", "https://github.com/company/wknd-eds");
                            if (store != null) store.saveProject(p);
                        }
                        JobRecord job = (orchestrator != null) ? orchestrator.runMigration(p, "admin") : new JobRecord("job-mock", projectId, "MIGRATE");
                        return JsonUtil.toJson(job);
                    }

                    if (("publish".equalsIgnoreCase(sub) || "push".equalsIgnoreCase(sub)) && "POST".equalsIgnoreCase(method)) {
                        ProjectRecord p = store != null ? store.getProject(projectId).orElse(null) : null;
                        if (p == null) {
                            p = new ProjectRecord(projectId, "Project " + projectId, "https://mock-aem.local", "/content/wknd", "https://github.com/company/wknd-eds");
                            if (store != null) store.saveProject(p);
                        }
                        JobRecord job = (orchestrator != null) ? orchestrator.pushToGitHub(p, "admin") : new JobRecord("job-mock", projectId, "PUBLISH");
                        return JsonUtil.toJson(job);
                    }

                    if ("jobs".equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.listJobs(projectId) : Collections.emptyList());
                    }

                    if ("events".equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.getEventsForProject(projectId) : Collections.emptyList());
                    }

                    if ("inventory".equalsIgnoreCase(sub)) {
                        Optional<JobRecord> latest = store != null ? store.getLatestJob(projectId) : Optional.empty();
                        if (latest.isPresent()) {
                            Optional<SiteInventory> inv = store.getInventory(latest.get().getId());
                            if (inv.isPresent()) return JsonUtil.toJson(inv.get());
                        }
                        return "{}";
                    }

                    if ("plan".equalsIgnoreCase(sub)) {
                        Optional<MigrationPlan> plan = store != null ? store.getLatestPlan(projectId) : Optional.empty();
                        return JsonUtil.toJson(plan.orElse(new MigrationPlan()));
                    }

                    if ("files".equalsIgnoreCase(sub)) {
                        Optional<JobRecord> latest = store != null ? store.getLatestJob(projectId) : Optional.empty();
                        List<GeneratedFileRecord> files = (latest.isPresent() && store != null)
                                ? store.getGeneratedFiles(latest.get().getId()) : Collections.emptyList();
                        return JsonUtil.toJson(files);
                    }

                    if ("redirects".equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.getUrlRedirectsForProject(projectId) : Collections.emptyList());
                    }

                    if (PATH_DEPENDENCIES.equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.getDependencyEdgesForProject(projectId) : Collections.emptyList());
                    }

                    if ("rollout-stages".equalsIgnoreCase(sub)) {
                        if (tokens.length == 4) {
                            String jid = tokens[3];
                            return JsonUtil.toJson(store != null ? store.getRolloutStages(jid) : Collections.emptyList());
                        }
                        return JsonUtil.toJson(store != null ? store.getLatestRolloutStages(projectId) : Collections.emptyList());
                    }

                    if ("repairs".equalsIgnoreCase(sub)) {
                        if (tokens.length == 4) {
                            String jid = tokens[3];
                            return JsonUtil.toJson(store != null ? store.getRepairAttempts(jid) : Collections.emptyList());
                        }
                        return JsonUtil.toJson(store != null ? store.getRepairAttemptsForProject(projectId) : Collections.emptyList());
                    }

                    if ("benchmarks".equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.getBenchmarkSamplesForProject(projectId) : Collections.emptyList());
                    }

                    if ("clarifications".equalsIgnoreCase(sub)) {
                        return JsonUtil.toJson(store != null ? store.getClarificationsForProject(projectId) : Collections.emptyList());
                    }
                }
            }

            if (resp != null) resp.setStatus(404);
            return "{\"error\":\"Route not found: " + path + "\"}";
        } catch (ModernizerException | RuntimeException e) {
            LOG.error("API error handling {} {}: {}", method, path, e.getMessage(), e);
            if (resp != null) resp.setStatus(500);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
