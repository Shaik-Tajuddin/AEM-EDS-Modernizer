package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.agents.Orchestrator;
import com.adobe.aem.modernizer.dashboard.ApiRouter;
import com.adobe.aem.modernizer.dashboard.StaticDashboard;
import com.adobe.aem.modernizer.dashboard.servlets.DashboardApi;
import com.adobe.aem.modernizer.dashboard.servlets.DryRunServlet;
import com.adobe.aem.modernizer.dashboard.servlets.MigrationServlet;
import com.adobe.aem.modernizer.dashboard.servlets.ModernizerHomeServlet;
import com.adobe.aem.modernizer.dashboard.servlets.ProjectServlet;
import com.adobe.aem.modernizer.mock.MockDataFactory;
import com.adobe.aem.modernizer.mock.MockGitHubClient;
import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.BenchmarkSampleRecord;
import com.adobe.aem.modernizer.persistence.model.JobRecord;
import com.adobe.aem.modernizer.persistence.model.MigrationPlan;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DashboardServletsAndApiTest {

    private Store store;
    private Orchestrator orchestrator;
    private ApiRouter router;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        orchestrator = new Orchestrator(store);
        router = new ApiRouter(store, orchestrator);
    }

    @Test
    void testApiRouterEndpoints() throws Exception {
        // GET /health
        String health = router.route("GET", "/health", null, null);
        assertThat(health).contains("UP");

        // GET /projects (empty)
        String projects = router.route("GET", "/projects", null, null);
        assertThat(projects).isEqualTo("[]");

        // POST /projects (create project)
        ProjectRecord p = new ProjectRecord("proj-1", "WKND Site", "http://localhost:4502", "/content/wknd", "https://github.com/org/wknd");
        String createProj = router.route("POST", "/projects", JsonUtil.toJson(p), null);
        assertThat(createProj).contains("proj-1");

        // GET /projects/proj-1
        String getProj = router.route("GET", "/projects/proj-1", null, null);
        assertThat(getProj).contains("proj-1");

        // GET /projects/proj-1/inventory
        JobRecord j1 = new JobRecord("job-1", "proj-1", "DRY_RUN");
        store.saveJob(j1);
        SiteInventory invObj = MockDataFactory.createWkndInventory("/content/wknd", null, 5);
        invObj.setJobId("job-1");
        store.saveInventory(invObj);
        String inv = router.route("GET", "/projects/proj-1/inventory", null, null);
        assertThat(inv).contains("/content/wknd");

        // POST /projects/proj-1/dryrun
        String dryrun = router.route("POST", "/projects/proj-1/dryrun", null, null);
        assertThat(dryrun).contains("DRY_RUN");

        // POST /projects/proj-1/migrate
        String migrate = router.route("POST", "/projects/proj-1/migrate", null, null);
        assertThat(migrate).contains("MIGRATE");

        // GET /projects/proj-1/jobs
        String jobs = router.route("GET", "/projects/proj-1/jobs", null, null);
        assertThat(jobs).contains("DRY_RUN");

        // GET /projects/proj-1/events
        String events = router.route("GET", "/projects/proj-1/events", null, null);
        assertThat(events).isNotNull();

        // GET /projects/proj-1/checkpoints
        String checkpoints = router.route("GET", "/projects/proj-1/checkpoints", null, null);
        assertThat(checkpoints).isNotNull();

        // GET /projects/proj-1/plan
        String plan = router.route("GET", "/projects/proj-1/plan", null, null);
        assertThat(plan).isNotNull();

        // GET /projects/proj-1/validations
        String validations = router.route("GET", "/projects/proj-1/validations", null, null);
        assertThat(validations).isNotNull();

        // GET /projects/proj-1/redirects
        String redirects = router.route("GET", "/projects/proj-1/redirects", null, null);
        assertThat(redirects).isNotNull();

        // GET /projects/proj-1/dependencies
        String deps = router.route("GET", "/projects/proj-1/dependencies", null, null);
        assertThat(deps).isNotNull();

        // GET /projects/proj-1/rollout-stages
        String rollout = router.route("GET", "/projects/proj-1/rollout-stages", null, null);
        assertThat(rollout).isNotNull();

        // GET /projects/proj-1/repairs
        String repairs = router.route("GET", "/projects/proj-1/repairs", null, null);
        assertThat(repairs).isNotNull();

        // GET /projects/proj-1/benchmarks
        store.saveBenchmarkSample(new BenchmarkSampleRecord("s1", "proj-1", "job-1", "connection", "test", 100L, 0.0));
        String benchmarks = router.route("GET", "/projects/proj-1/benchmarks", null, null);
        assertThat(benchmarks).contains("connection");

        // GET /projects/proj-1/clarifications
        String clarifications = router.route("GET", "/projects/proj-1/clarifications", null, null);
        assertThat(clarifications).isNotNull();

        // DELETE /projects/proj-1
        String delete = router.route("DELETE", "/projects/proj-1", null, null);
        assertThat(delete).contains("DELETED");

        // 404 Route Not Found
        String notFound = router.route("GET", "/unknown/route", null, null);
        assertThat(notFound).contains("error");
    }

    @Test
    void testDashboardApiServlet() throws Exception {
        DashboardApi servlet = new DashboardApi(router);

        SlingHttpServletRequest req = Mockito.mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse resp = Mockito.mock(SlingHttpServletResponse.class);

        when(req.getRequestURI()).thenReturn("/bin/aem-eds-modernizer/api/health");
        when(req.getContextPath()).thenReturn("");
        when(req.getServletPath()).thenReturn("/bin/aem-eds-modernizer/api");
        when(req.getPathInfo()).thenReturn("/health");
        when(req.getMethod()).thenReturn("GET");
        when(req.getProtocol()).thenReturn("HTTP/1.1");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("")));

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.doGet(req, resp);
        assertThat(sw.toString()).contains("UP");
    }

    @Test
    void testModernizerHomeServlet() throws Exception {
        ModernizerHomeServlet servlet = new ModernizerHomeServlet();

        SlingHttpServletRequest req = Mockito.mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse resp = Mockito.mock(SlingHttpServletResponse.class);
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);

        when(resp.getWriter()).thenReturn(pw);
        when(req.getMethod()).thenReturn("GET");

        servlet.service(req, resp);
        assertThat(sw.toString()).contains("AEM → EDS Modernizer");
    }

    @Test
    void testProjectServlet() throws Exception {
        ProjectServlet servlet = new ProjectServlet(router, store);

        // POST project
        SlingHttpServletRequest req = Mockito.mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse resp = Mockito.mock(SlingHttpServletResponse.class);
        when(req.getMethod()).thenReturn("POST");
        org.apache.sling.api.request.RequestPathInfo pathInfo = Mockito.mock(org.apache.sling.api.request.RequestPathInfo.class);
        when(req.getRequestPathInfo()).thenReturn(pathInfo);

        ProjectRecord p = new ProjectRecord("proj-custom", "Custom Project", "http://localhost:4502", "/content/custom", "https://github.com/custom/eds");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(JsonUtil.toJson(p))));
        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.doPost(req, resp);
        assertThat(sw.toString()).contains("proj-custom");

        // POST project without ID or Name
        when(req.getMethod()).thenReturn("POST");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{}")));
        StringWriter swEmpty = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(swEmpty));
        servlet.doPost(req, resp);
        assertThat(swEmpty.toString()).contains("proj-");

        // GET all projects
        SlingHttpServletRequest getReq = Mockito.mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse getResp = Mockito.mock(SlingHttpServletResponse.class);
        when(getReq.getMethod()).thenReturn("GET");
        when(getReq.getRequestPathInfo()).thenReturn(pathInfo);
        when(pathInfo.getSuffix()).thenReturn(null);
        when(getReq.getReader()).thenAnswer(invocation -> new BufferedReader(new StringReader("")));
        StringWriter getSw = new StringWriter();
        when(getResp.getWriter()).thenReturn(new PrintWriter(getSw));

        servlet.doGet(getReq, getResp);
        assertThat(getSw.toString()).contains("proj-custom");

        // GET project by suffix
        when(getReq.getMethod()).thenReturn("GET");
        when(pathInfo.getSuffix()).thenReturn("/proj-custom");
        StringWriter singleSw = new StringWriter();
        when(getResp.getWriter()).thenReturn(new PrintWriter(singleSw));
        servlet.doGet(getReq, getResp);
        assertThat(singleSw.toString()).contains("Custom Project");

        // GET non-existent project
        when(getReq.getMethod()).thenReturn("GET");
        when(pathInfo.getSuffix()).thenReturn("/non-existent");
        StringWriter notFoundSw = new StringWriter();
        when(getResp.getWriter()).thenReturn(new PrintWriter(notFoundSw));
        servlet.doGet(getReq, getResp);
        assertThat(notFoundSw.toString()).contains("error");

        // DELETE project
        when(getReq.getMethod()).thenReturn("DELETE");
        when(pathInfo.getSuffix()).thenReturn("/proj-custom");
        servlet.doDelete(getReq, getResp);
        assertThat(store.getProject("proj-custom")).isEmpty();

        // DELETE without id
        when(getReq.getMethod()).thenReturn("DELETE");
        when(pathInfo.getSuffix()).thenReturn(null);
        when(getReq.getParameter("id")).thenReturn(null);
        servlet.doDelete(getReq, getResp);

        // Test null store servlet
        ProjectServlet emptyServlet = new ProjectServlet();
        emptyServlet.doGet(getReq, getResp);
    }

    @Test
    void testDryRunAndMigrationServlets() throws Exception {
        DryRunServlet dryRunServlet = new DryRunServlet(store, orchestrator);
        MigrationServlet migrationServlet = new MigrationServlet(store, orchestrator);

        SlingHttpServletRequest req = Mockito.mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse resp = Mockito.mock(SlingHttpServletResponse.class);
        when(req.getParameter("projectId")).thenReturn("wknd-site");

        StringWriter sw1 = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw1));
        dryRunServlet.doPost(req, resp);
        assertThat(sw1.toString()).contains("DRY_RUN");

        StringWriter sw2 = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw2));
        migrationServlet.doPost(req, resp);
        assertThat(sw2.toString()).contains("MIGRATE");

        // Null constructors
        new DryRunServlet().doPost(req, resp);
        new MigrationServlet().doPost(req, resp);
    }

    @Test
    void testModernizerDashboardModel() {
        com.adobe.aem.modernizer.dashboard.models.ModernizerDashboardModel defaultModel =
                new com.adobe.aem.modernizer.dashboard.models.ModernizerDashboardModel();
        assertThat(defaultModel.getActiveProjectId()).isEqualTo("wknd-site");

        com.adobe.aem.modernizer.dashboard.models.ModernizerDashboardModel model =
                new com.adobe.aem.modernizer.dashboard.models.ModernizerDashboardModel(store, orchestrator);

        assertThat(model.getApiBaseUrl()).contains("/bin/aem-eds-modernizer/api");
        assertThat(model.getActiveProjectId()).isEqualTo("wknd-site");
        assertThat(model.getActiveProject()).isNotNull();
        assertThat(model.isConfigured()).isTrue();
        assertThat(model.getPagesCount()).isGreaterThanOrEqualTo(0);
        assertThat(model.getComponentsCount()).isGreaterThanOrEqualTo(0);
        assertThat(model.getExpectedCost()).isGreaterThanOrEqualTo(0.0);
        assertThat(model.getExpectedTimeSec()).isGreaterThanOrEqualTo(0.0);
        assertThat(model.getProjects()).isNotNull();
        assertThat(model.getRolloutStages()).isNotNull();
        assertThat(model.getRepairs()).isNotNull();
        assertThat(model.getBenchmarks()).isNotNull();

        model.setApiBaseUrl("http://localhost:4502/api");
        assertThat(model.getApiBaseUrl()).isEqualTo("http://localhost:4502/api");
        model.setActiveProjectId("custom-id");
        assertThat(model.getActiveProjectId()).isEqualTo("custom-id");

        SiteInventory inv = MockDataFactory.createWkndInventory("/content/wknd", null, 3);
        model.setInventory(inv);
        assertThat(model.getInventory()).isNotNull();
        assertThat(model.getPagesCount()).isGreaterThan(0);
        assertThat(model.getComponentsCount()).isGreaterThan(0);

        MigrationPlan plan = new MigrationPlan();
        plan.setProjectId("proj-1");
        plan.setJobId("job-1");
        plan.setCostExpected(45.5);
        plan.setTimeExpectedSec(120L);
        model.setPlan(plan);
        assertThat(model.getPlan()).isNotNull();
        assertThat(model.getExpectedCost()).isEqualTo(45.5);
        assertThat(model.getExpectedTimeSec()).isEqualTo(120.0);

        ProjectRecord p = new ProjectRecord("p-test", "Test", "http://author", "/content/test", "http://git");
        model.setActiveProject(p);
        assertThat(model.getActiveProject()).isEqualTo(p);
    }

    @Test
    void testStaticDashboardHtml() {
        String html = StaticDashboard.html("http://localhost:4502/bin/aem-eds-modernizer/api");
        assertThat(html).contains("AEM → EDS Modernizer");
        assertThat(html).contains("http://localhost:4502/bin/aem-eds-modernizer/api");
        assertThat(html).contains("VS Code Web Workspace");
        assertThat(html).contains("runNpmScript('lint:fix,build:json')");
        assertThat(html).contains("Heal CI");
        assertThat(html).contains("runNpmScript('heal')");
        assertThat(html).contains("chk-vscode-reviewed");
        assertThat(html).contains("ws-file-tree");
        assertThat(html).contains("workspace/save");
        assertThat(html).contains("workspace/delete");
        assertThat(html).contains("deleteCurrentProject");
        assertThat(html).contains("projects/' + encodeURIComponent(currentProjectId) + '/delete");
        assertThat(html).contains("ws-line-numbers");
        assertThat(html).contains("HTML view");
        assertThat(html).contains("DA compatible");
        assertThat(html).contains("Copy for Document Authoring");
        assertThat(html).contains("UE authoring");
        assertThat(html).contains("loadPreviewArtifacts");
        assertThat(html).contains("new RegExp");
        assertThat(html).contains("btn-migrate').disabled = false");
    }

    @Test
    void previewPushDoesNotOpenPrAndNpmDispatches() {
        MockGitHubClient gh = new MockGitHubClient("https://github.com/company/wknd-eds");
        Orchestrator orch = new Orchestrator(store);
        orch.register(new com.adobe.aem.modernizer.agents.PreviewAgent(
                gh, new com.adobe.aem.modernizer.mock.MockEdsClient("https://eds-mock.local"), store, null));
        orch.register(new com.adobe.aem.modernizer.agents.PublishingAgent(gh, store));
        ApiRouter wired = new ApiRouter(store, orch, gh);
        store.saveProject(new ProjectRecord("proj-1", "WKND Site", "http://localhost:4502", "/content/wknd", "https://github.com/company/wknd-eds"));
        store.saveJob(new com.adobe.aem.modernizer.persistence.model.JobRecord("job-1", "proj-1", "MIGRATE"));
        store.saveGeneratedFile(new com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord("f1", "proj-1", "job-1", "blocks/hero/hero.js", "BLOCK_JS", "export default function decorate() {}"));

        String preview = wired.route("POST", "/projects/proj-1/preview", null, null);
        assertThat(preview).contains("PREVIEWING");
        assertThat(preview).contains("vscodeUrl");
        assertThat(gh.getPrCount()).isEqualTo(0);
        assertThat(gh.getCommitCount()).isGreaterThan(0);

        String publish = wired.route("POST", "/projects/proj-1/publish", null, null);
        assertThat(publish).contains("COMPLETED");
        assertThat(gh.getPrCount()).isEqualTo(1);
        int commitsAfterPr = gh.getCommitCount();

        String heal = wired.route("POST", "/projects/proj-1/npm", "{\"command\":\"heal\"}", null);
        assertThat(heal).contains("heal");
        assertThat(heal).contains("feat/proj-1");

        String npm = wired.route("POST", "/projects/proj-1/npm", "{\"command\":\"lint:fix\"}", null);
        assertThat(npm).contains("runId");
        assertThat(npm).contains("lint:fix");
        assertThat(gh.getCommitCount()).isEqualTo(commitsAfterPr);

        @SuppressWarnings("unchecked")
        Map<String, Object> npmJson = JsonUtil.fromJson(npm, Map.class);
        String runId = String.valueOf(npmJson.get("runId"));
        String npmGet = wired.route("GET", "/projects/proj-1/npm/" + runId, null, null);
        assertThat(npmGet).contains("completed");
        assertThat(npmGet).contains("npm run lint:fix");

        String workspace = wired.route("POST", "/projects/proj-1/workspace", "{\"branch\":\"feat/proj-1\"}", null);
        assertThat(workspace).doesNotContain("fstab.yaml");
        assertThat(workspace).contains("/tree/feat/proj-1").doesNotContain("%2F");

        gh.commitFiles("feat/proj-1", java.util.List.of(new com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord(
                "legacy-md", "proj-1", "preview", "language-masters/en/about-us.md", "SECTION_MD", "# old")), "legacy");
        String listed = wired.route("POST", "/projects/proj-1/workspace", "{\"branch\":\"feat/proj-1\"}", null);
        assertThat(listed).contains("language-masters/en/about-us.md");
        String deleted = wired.route("POST", "/projects/proj-1/workspace/delete",
                "{\"branch\":\"feat/proj-1\",\"path\":\"language-masters/en/about-us.md\"}", null);
        assertThat(deleted).contains("\"deleted\":true");
        String afterDelete = wired.route("POST", "/projects/proj-1/workspace", "{\"branch\":\"feat/proj-1\"}", null);
        assertThat(afterDelete).doesNotContain("language-masters/en/about-us.md");

        String saved = wired.route("POST", "/projects/proj-1/workspace/save",
                "{\"branch\":\"feat/proj-1\",\"path\":\"docs/migrated-pages/language-masters/en/about-us.md\",\"content\":\"# About\"}", null);
        assertThat(saved).contains("\"ok\":true");
        assertThat(saved).contains("# About");

        String removed = wired.route("POST", "/projects/proj-1/delete", null, null);
        assertThat(removed).contains("DELETED");
        assertThat(wired.route("GET", "/projects/proj-1", null, null)).contains("not found");
    }
}

