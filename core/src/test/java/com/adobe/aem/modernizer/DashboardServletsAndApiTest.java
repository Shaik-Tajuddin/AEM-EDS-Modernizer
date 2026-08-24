package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.agents.Orchestrator;
import com.adobe.aem.modernizer.dashboard.ApiRouter;
import com.adobe.aem.modernizer.dashboard.StaticDashboard;
import com.adobe.aem.modernizer.dashboard.servlets.DashboardApi;
import com.adobe.aem.modernizer.dashboard.servlets.ModernizerHomeServlet;
import com.adobe.aem.modernizer.mock.MockDataFactory;
import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.BenchmarkSampleRecord;
import com.adobe.aem.modernizer.persistence.model.JobRecord;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DashboardServletsAndApiTest {

    private Store store;
    private Orchestrator orchestrator;
    private ApiRouter router;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        orchestrator = new Orchestrator(store, null, null);
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

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);

        when(req.getRequestURI()).thenReturn("/bin/aem-eds-modernizer/api/health");
        when(req.getContextPath()).thenReturn("");
        when(req.getServletPath()).thenReturn("/bin/aem-eds-modernizer/api");
        when(req.getPathInfo()).thenReturn("/health");
        when(req.getMethod()).thenReturn("GET");
        when(req.getProtocol()).thenReturn("HTTP/1.1");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("")));

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.service(req, resp);
        assertThat(sw.toString()).contains("UP");
    }

    @Test
    void testModernizerHomeServlet() throws Exception {
        ModernizerHomeServlet servlet = new ModernizerHomeServlet();

        SlingHttpServletRequest req = Mockito.mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse resp = Mockito.mock(SlingHttpServletResponse.class);

        when(req.getScheme()).thenReturn("http");
        when(req.getServerName()).thenReturn("localhost");
        when(req.getServerPort()).thenReturn(4502);
        when(req.getMethod()).thenReturn("GET");
        when(req.getProtocol()).thenReturn("HTTP/1.1");

        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.service(req, resp);
        assertThat(sw.toString()).contains("AEM → EDS Modernizer");
    }

    @Test
    void testStaticDashboardHtml() {
        String html = StaticDashboard.html("http://localhost:4502/bin/aem-eds-modernizer/api");
        assertThat(html).contains("AEM → EDS Modernizer");
        assertThat(html).contains("http://localhost:4502/bin/aem-eds-modernizer/api");
    }
}
