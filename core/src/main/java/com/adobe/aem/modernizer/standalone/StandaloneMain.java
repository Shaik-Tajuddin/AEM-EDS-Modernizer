package com.adobe.aem.modernizer.standalone;

import com.adobe.aem.modernizer.agents.*;
import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ModelCapability;
import com.adobe.aem.modernizer.ai.providers.MockAiProvider;
import com.adobe.aem.modernizer.ai.routing.AiRoutingPolicy;
import com.adobe.aem.modernizer.ai.secret.EnvSecretProvider;
import com.adobe.aem.modernizer.connectors.*;
import com.adobe.aem.modernizer.dashboard.ApiRouter;
import com.adobe.aem.modernizer.dashboard.StaticDashboard;
import com.adobe.aem.modernizer.mock.*;
import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.scopes.MarkerEvaluator;
import com.adobe.aem.modernizer.services.EstimatorService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Standalone executable runner launching embedded HTTP server on port 8080 for offline/local testing.
 */
public class StandaloneMain {

    private static final Logger LOG = LoggerFactory.getLogger(StandaloneMain.class);

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        String envPort = System.getenv("PORT");
        if (envPort != null) {
            try {
                port = Integer.parseInt(envPort);
            } catch (NumberFormatException ignored) {}
        }

        // Initialize components
        Store store = new InMemoryStore();
        MarkerEvaluator marker = new MarkerEvaluator("edsModernize", "true");
        EstimatorService estimator = new EstimatorService();

        AiRoutingPolicy routing = new AiRoutingPolicy();
        routing.setStrategy("MULTI_PROVIDER");
        routing.setDefaultProvider("mock");
        routing.setDefaultModel("mock-general-1");

        AiGateway ai = new AiGateway(routing, new EnvSecretProvider(), store, false, 5);
        ai.capabilities().add(new ModelCapability("mock", "mock-general-1", 8192)
                .add(ModelCapability.CAP_CHAT)
                .add(ModelCapability.CAP_STRUCTURED)
                .add(ModelCapability.CAP_CODE)
                .add(ModelCapability.CAP_VISION)
                .add(ModelCapability.CAP_LOCAL));
        ai.register(new MockAiProvider("mock", "mock-general-1"));

        Orchestrator orchestrator = new Orchestrator(store, ai, estimator);

        AemClient aemAuthor = new MockAemClient("https://mock-aem.local", "author", 42, true);
        AemClient aemPublish = new MockAemClient("https://mock-aem.local", "publish", 42, true);
        GitHubClient gh = new MockGitHubClient("https://github.com/company/wknd-eds");
        FigmaClient figma = new MockFigmaClient("https://www.figma.com/design/abcdef/WKND");
        EdsClient eds = new MockEdsClient("https://eds-mock.local");
        BrowserClient browser = new MockBrowserClient();

        // Register Phase 1 Agents
        orchestrator.registerCoreAgents(
                new ConnectionAgent(aemAuthor, aemPublish, gh, figma, eds, browser, store, ai),
                new DiscoveryAgent(aemAuthor, store, ai, marker),
                new ComponentIntelligenceAgent(store, ai),
                new ComponentMappingAgent(store, ai),
                new TemplateAnalysisAgent(store, ai),
                new ContentAnalysisAgent(store, ai),
                new AssetAnalysisAgent(aemAuthor, store, ai),
                new ContentFragmentAnalysisAgent(store, ai),
                new MsmAnalysisAgent(store, ai),
                new FigmaAnalysisAgent(figma, store, ai),
                new MigrationPlannerAgent(store, ai, estimator),
                new BlockGenerationAgent(store, ai),
                new CodeGenerationAgent(store, ai),
                new ContentMigrationAgent(store, ai),
                new AuthoringAgent(aemAuthor, store, ai),
                new PreviewAgent(gh, eds, store, ai),
                new ValidationAgent(browser, store, ai),
                new VisualValidationAgent(browser, store, ai),
                new SelfRepairAgent(store, ai),
                new PublishingAgent(gh, store, ai),
                new VerificationAgent(browser, store, ai)
        );

        // Register Phase 2 Agents
        orchestrator.register(new AdvancedFigmaIntelligenceAgent(figma, store, ai));
        orchestrator.register(new AdvancedVisualValidationAgent(browser, store, ai));
        orchestrator.register(new AdvancedRepairAgent(store, ai, 5));
        orchestrator.register(new AdvancedRolloutAgent(store, ai, RolloutPolicy.defaultPolicy()));

        ApiRouter router = new ApiRouter(store, orchestrator);

        // Start embedded HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Serve Static SPA
        final int finalPort = port;
        HttpHandler spaHandler = exchange -> {
            byte[] response = StaticDashboard.html("http://localhost:" + finalPort + "/api").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        };

        server.createContext("/", spaHandler);
        server.createContext("/aem-eds-modernizer", spaHandler);

        // Serve API
        server.createContext("/api", exchange -> {
            String method = exchange.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api")) {
                path = path.substring(4);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = exchange.getRequestBody()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
            }
            String body = baos.toString(StandardCharsets.UTF_8.name());

            String json = router.route(method, path, body, null);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.setExecutor(null);
        server.start();

        LOG.info("===============================================================");
        LOG.info(" AEM -> EDS Modernizer Standalone Server Started!");
        LOG.info(" URL: http://localhost:{}", port);
        LOG.info(" API: http://localhost:{}/api/health", port);
        LOG.info("===============================================================");
    }
}
