package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.agents.*;
import com.adobe.aem.modernizer.ai.*;
import com.adobe.aem.modernizer.ai.providers.*;
import com.adobe.aem.modernizer.ai.routing.AiRoutingPolicy;
import com.adobe.aem.modernizer.ai.secret.EnvSecretProvider;
import com.adobe.aem.modernizer.connectors.*;
import com.adobe.aem.modernizer.dashboard.ApiRouter;
import com.adobe.aem.modernizer.mock.*;
import com.adobe.aem.modernizer.osgi.ModernizerBundleActivator;
import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.*;
import com.adobe.aem.modernizer.scopes.MarkerEvaluator;
import com.adobe.aem.modernizer.services.ClarificationService;
import com.adobe.aem.modernizer.services.EstimatorService;
import com.adobe.aem.modernizer.util.JsonUtil;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProvidersAndComprehensiveCoverageTest {

    private static HttpServer mockServer;
    private static int mockPort;

    @BeforeAll
    static void startMockHttpServer() throws Exception {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockPort = mockServer.getAddress().getPort();

        // OpenAI mock endpoint
        mockServer.createContext("/chat/completions", exchange -> {
            String resp = "{\"choices\":[{\"message\":{\"content\":\"OpenAI output\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":20}}";
            byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        // Anthropic mock endpoint
        mockServer.createContext("/messages", exchange -> {
            String resp = "{\"content\":[{\"type\":\"text\",\"text\":\"Anthropic output\"}],\"stop_reason\":\"end_turn\",\"usage\":{\"input_tokens\":15,\"output_tokens\":25}}";
            byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        // Gemini mock endpoint
        mockServer.createContext("/models/gemini-1.5-pro:generateContent", exchange -> {
            String resp = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Gemini output\"}]},\"finishReason\":\"STOP\"}],\"usageMetadata\":{\"promptTokenCount\":12,\"candidatesTokenCount\":22}}";
            byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        // Ollama mock endpoint
        mockServer.createContext("/api/chat", exchange -> {
            String resp = "{\"message\":{\"content\":\"Ollama output\"},\"prompt_eval_count\":8,\"eval_count\":18}";
            byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        // Error endpoint
        mockServer.createContext("/error-endpoint", exchange -> {
            String err = "{\"error\":\"Internal error\"}";
            byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        mockServer.start();
    }

    @AfterAll
    static void stopMockHttpServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    void testRealProvidersWithMockServer() {
        String baseUrl = "http://localhost:" + mockPort;
        ChatRequest req = new ChatRequest("test-agent", "Prompt text");
        req.setSystemPrompt("System prompt text");

        // OpenAI Provider
        OpenAiProvider openAi = new OpenAiProvider(baseUrl);
        assertThat(openAi.getProviderName()).isEqualTo("openai");
        assertThat(openAi.isAvailable()).isTrue();
        ChatResponse openAiResp = openAi.chat(req, "gpt-4o", "test-key");
        assertThat(openAiResp.getContent()).isEqualTo("OpenAI output");
        assertThat(openAiResp.getTokenUsage().getTotalTokens()).isEqualTo(30);

        // Anthropic Provider
        AnthropicProvider anthropic = new AnthropicProvider(baseUrl);
        assertThat(anthropic.getProviderName()).isEqualTo("anthropic");
        assertThat(anthropic.isAvailable()).isTrue();
        ChatResponse anthropicResp = anthropic.chat(req, "claude-3-5-sonnet", "test-key");
        assertThat(anthropicResp.getContent()).isEqualTo("Anthropic output");
        assertThat(anthropicResp.getTokenUsage().getTotalTokens()).isEqualTo(40);

        // Gemini Provider
        GeminiProvider gemini = new GeminiProvider(baseUrl);
        assertThat(gemini.getProviderName()).isEqualTo("gemini");
        assertThat(gemini.isAvailable()).isTrue();
        ChatResponse geminiResp = gemini.chat(req, "gemini-1.5-pro", "test-key");
        assertThat(geminiResp.getContent()).isEqualTo("Gemini output");
        assertThat(geminiResp.getTokenUsage().getTotalTokens()).isEqualTo(128);

        // Ollama Provider
        OllamaProvider ollama = new OllamaProvider(baseUrl);
        assertThat(ollama.getProviderName()).isEqualTo("ollama");
        assertThat(ollama.isAvailable()).isTrue();
        ChatResponse ollamaResp = ollama.chat(req, "llama3", null);
        assertThat(ollamaResp.getContent()).isEqualTo("Ollama output");
        assertThat(ollamaResp.getTokenUsage().getTotalTokens()).isEqualTo(26);

        // Provider error handling with bad URL
        OpenAiProvider badOpenAi = new OpenAiProvider(baseUrl + "/error-endpoint");
        assertThatThrownBy(() -> badOpenAi.chat(req, "gpt-4o", "key"))
                .isInstanceOf(AiProviderException.class);

        AnthropicProvider badAnthropic = new AnthropicProvider(baseUrl + "/error-endpoint");
        assertThatThrownBy(() -> badAnthropic.chat(req, "claude", "key"))
                .isInstanceOf(AiProviderException.class);

        GeminiProvider badGemini = new GeminiProvider(baseUrl + "/error-endpoint");
        assertThatThrownBy(() -> badGemini.chat(req, "gemini", "key"))
                .isInstanceOf(AiProviderException.class);

        OllamaProvider badOllama = new OllamaProvider(baseUrl + "/error-endpoint");
        assertThatThrownBy(() -> badOllama.chat(req, "llama3", null))
                .isInstanceOf(AiProviderException.class);
    }

    @Test
    void testModernizerBundleActivatorFullWiring() throws Exception {
        ModernizerBundleActivator activator = new ModernizerBundleActivator();
        Store store = new InMemoryStore();
        MarkerEvaluator marker = new MarkerEvaluator("edsModernize", "true");
        EstimatorService estimator = new EstimatorService();
        ClarificationService clar = new ClarificationService(store);
        AiGateway ai = new AiGateway();
        ai.activate();
        Orchestrator orchestrator = new Orchestrator(store, ai, estimator);
        orchestrator.activate();

        setField(activator, "store", store);
        setField(activator, "marker", marker);
        setField(activator, "estimator", estimator);
        setField(activator, "clarifications", clar);
        setField(activator, "orchestrator", orchestrator);
        setField(activator, "ai", ai);

        activator.activate();
        assertThat(orchestrator.getAgents()).isNotEmpty();
    }

    @Test
    void testMigrationStateTransitionsExhaustive() {
        MigrationState[] allStates = MigrationState.values();
        for (MigrationState from : allStates) {
            for (MigrationState to : allStates) {
                from.canTransitionTo(to);
            }
            from.canTransitionTo(null);
        }
    }

    @Test
    void testAiGatewayAdvanced() {
        AiRoutingPolicy routing = new AiRoutingPolicy();
        EnvSecretProvider secrets = new EnvSecretProvider();
        Store store = new InMemoryStore();

        AiGateway gateway = new AiGateway(routing, secrets, store, true, 3);
        gateway.activate();

        assertThat(gateway.routingPolicy()).isNotNull();
        assertThat(gateway.secrets()).isNotNull();
        assertThat(gateway.capabilities()).isNotNull();
        assertThat(gateway.pricing()).isNotNull();
        assertThat(gateway.isLocalOnly()).isTrue();
        assertThat(gateway.getMaxRepairAttempts()).isEqualTo(3);

        ChatRequest req = new ChatRequest("code-agent", "Generate code");
        ChatResponse res = gateway.dispatch(req);
        assertThat(res).isNotNull();

        // Dispatch with routing policy override
        routing.setStrategy("LOCAL_ONLY");
        ChatResponse resLocal = gateway.dispatch(req);
        assertThat(resLocal).isNotNull();
    }

    @Test
    void testOrchestratorAdvanced() throws Exception {
        Store store = new InMemoryStore();
        AiGateway ai = new AiGateway();
        ai.activate();
        EstimatorService estimator = new EstimatorService();
        Orchestrator orchestrator = new Orchestrator(store, ai, estimator);
        orchestrator.activate();

        ProjectRecord proj = new ProjectRecord("p-adv", "Adv Project", "http://author", "/content/wknd", "https://github.com/repo");
        store.saveProject(proj);

        // Dry Run
        JobRecord dryJob = orchestrator.runDryRun(proj, "operator");
        assertThat(dryJob.getState()).isIn("READY_TO_PUBLISH", "COMPLETED");

        // Migration Run
        JobRecord migJob = orchestrator.runMigration(proj, "operator");
        assertThat(migJob.getState()).isEqualTo("COMPLETED");

        // Null checks
        assertThatThrownBy(() -> orchestrator.runDryRun(null, "admin"))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> orchestrator.runMigration(null, "admin"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testApiRouterAdvancedEndpoints() throws Exception {
        Store store = new InMemoryStore();
        Orchestrator orchestrator = new Orchestrator(store, null, null);
        ApiRouter router = new ApiRouter(store, orchestrator);

        ProjectRecord proj = new ProjectRecord("p1", "Test", "http://author", "/content/wknd", "https://github.com/repo");
        store.saveProject(proj);

        JobRecord job = new JobRecord("j1", "p1", "MIGRATE");
        store.saveJob(job);

        // Clarifications
        ClarificationRecord clar = new ClarificationRecord("cl1", "p1", "j1", "Q?", "Opt1");
        store.saveClarification(clar);
        String getClar = router.route("GET", "/projects/p1/clarifications", null, null);
        assertThat(getClar).contains("cl1");

        // Files
        GeneratedFileRecord file = new GeneratedFileRecord("f1", "p1", "j1", "blocks/hero/hero.js", "BLOCK_JS", "export default () => {}");
        store.saveGeneratedFile(file);
        String getFiles = router.route("GET", "/projects/p1/files", null, null);
        assertThat(getFiles).contains("blocks/hero/hero.js");

        // Rollout stages by job
        RolloutStageRecord rs = new RolloutStageRecord("rs1", "p1", "j1", 1, "Canary", 10);
        store.saveRolloutStage(rs);
        String getStages = router.route("GET", "/projects/p1/rollout-stages/j1", null, null);
        assertThat(getStages).contains("Canary");

        // Repairs by job
        RepairAttemptRecord rep = new RepairAttemptRecord("rep1", "p1", "j1", "hero", 1, "CSS");
        store.saveRepairAttempt(rep);
        String getRepairs = router.route("GET", "/projects/p1/repairs/j1", null, null);
        assertThat(getRepairs).contains("hero");

        // Error routing
        String errMethod = router.route("PUT", "/projects/p1", "{}", null);
        assertThat(errMethod).contains("error");

        String invalidSub = router.route("GET", "/projects/p1/nonexistent", null, null);
        assertThat(invalidSub).contains("error");
    }

    @Test
    void testAllModelsGettersSettersConstructors() {
        // ProjectRecord
        ProjectRecord pr = new ProjectRecord();
        pr.setId("id");
        pr.setName("name");
        pr.setAemAuthorUrl("authorUrl");
        pr.setAemPublishUrl("publishUrl");
        pr.setContentRoot("/root");
        pr.setPageScope("/scope");
        pr.setEdsGitRepoUrl("gitUrl");
        pr.setEdsBranch("main");
        pr.setFigmaUrl("figmaUrl");
        pr.setMarkerProperty("prop");
        pr.setMarkerValue("val");
        pr.setAuthoringStrategy("DOC");
        pr.setAiProvider("openai");
        pr.setAiModel("gpt-4o");
        pr.setMaxBudgetUsd(500.0);
        pr.setMaxRepairAttempts(3);
        pr.setCreatedAt(100L);
        pr.setUpdatedAt(200L);
        pr.setProperties(new HashMap<>());

        assertThat(pr.getId()).isEqualTo("id");
        assertThat(pr.getName()).isEqualTo("name");
        assertThat(pr.getAemAuthorUrl()).isEqualTo("authorUrl");
        assertThat(pr.getAemPublishUrl()).isEqualTo("publishUrl");
        assertThat(pr.getContentRoot()).isEqualTo("/root");
        assertThat(pr.getPageScope()).isEqualTo("/scope");
        assertThat(pr.getEdsGitRepoUrl()).isEqualTo("gitUrl");
        assertThat(pr.getEdsBranch()).isEqualTo("main");
        assertThat(pr.getFigmaUrl()).isEqualTo("figmaUrl");
        assertThat(pr.getMarkerProperty()).isEqualTo("prop");
        assertThat(pr.getMarkerValue()).isEqualTo("val");
        assertThat(pr.getAuthoringStrategy()).isEqualTo("DOC");
        assertThat(pr.getAiProvider()).isEqualTo("openai");
        assertThat(pr.getAiModel()).isEqualTo("gpt-4o");
        assertThat(pr.getMaxBudgetUsd()).isEqualTo(500.0);
        assertThat(pr.getMaxRepairAttempts()).isEqualTo(3);
        assertThat(pr.getCreatedAt()).isEqualTo(100L);
        assertThat(pr.getUpdatedAt()).isEqualTo(200L);
        assertThat(pr.getProperties()).isNotNull();

        // JobRecord
        JobRecord jr = new JobRecord();
        jr.setId("jId");
        jr.setProjectId("pId");
        jr.setState("COMPLETED");
        jr.setMode("DRY_RUN");
        jr.setDryRun(true);
        jr.setStartedAt(10L);
        jr.setFinishedAt(20L);
        jr.setActualAiCostUsd(1.5);
        jr.setAiCallsMade(5);
        jr.setLastError("err");
        jr.setActor("admin");
        jr.setMetadata(new HashMap<>());

        assertThat(jr.getId()).isEqualTo("jId");
        assertThat(jr.getProjectId()).isEqualTo("pId");
        assertThat(jr.getState()).isEqualTo("COMPLETED");
        assertThat(jr.getMode()).isEqualTo("DRY_RUN");
        assertThat(jr.isDryRun()).isTrue();
        assertThat(jr.getStartedAt()).isEqualTo(10L);
        assertThat(jr.getFinishedAt()).isEqualTo(20L);
        assertThat(jr.getActualAiCostUsd()).isEqualTo(1.5);
        assertThat(jr.getAiCallsMade()).isEqualTo(5);
        assertThat(jr.getLastError()).isEqualTo("err");
        assertThat(jr.getActor()).isEqualTo("admin");
        assertThat(jr.getMetadata()).isNotNull();

        // MigrationPlan
        MigrationPlan mp = new MigrationPlan();
        mp.setProjectId("pId");
        mp.setJobId("jId");
        mp.setVersion("2.0");
        mp.setPagesEligible(10);
        mp.setEdsBlocksNew(5);
        mp.setAiRequestsExpected(8);
        mp.setCostExpected(10.0);
        mp.setCostLo(5.0);
        mp.setCostHi(15.0);
        mp.setTimeExpectedSec(60);
        mp.setTimeLoSec(30);
        mp.setTimeHiSec(90);
        mp.setValidationsExpected(10);
        mp.setRepairsExpected(2);
        mp.setAutomationConfidence(0.95);
        mp.setStatus("CURRENT");
        mp.setDerivationTrail(new ArrayList<>());
        mp.setBlockers(new ArrayList<>());
        mp.setWarnings(new ArrayList<>());
        mp.setDetails(new HashMap<>());
        mp.setGeneratedAt(100L);

        assertThat(mp.getProjectId()).isEqualTo("pId");
        assertThat(mp.getJobId()).isEqualTo("jId");
        assertThat(mp.getVersion()).isEqualTo("2.0");
        assertThat(mp.getPagesEligible()).isEqualTo(10);
        assertThat(mp.getEdsBlocksNew()).isEqualTo(5);
        assertThat(mp.getAiRequestsExpected()).isEqualTo(8);
        assertThat(mp.getCostExpected()).isEqualTo(10.0);
        assertThat(mp.getCostLo()).isEqualTo(5.0);
        assertThat(mp.getCostHi()).isEqualTo(15.0);
        assertThat(mp.getTimeExpectedSec()).isEqualTo(60);
        assertThat(mp.getTimeLoSec()).isEqualTo(30);
        assertThat(mp.getTimeHiSec()).isEqualTo(90);
        assertThat(mp.getValidationsExpected()).isEqualTo(10);
        assertThat(mp.getRepairsExpected()).isEqualTo(2);
        assertThat(mp.getAutomationConfidence()).isEqualTo(0.95);
        assertThat(mp.getStatus()).isEqualTo("CURRENT");
        assertThat(mp.getDerivationTrail()).isNotNull();
        assertThat(mp.getBlockers()).isNotNull();
        assertThat(mp.getWarnings()).isNotNull();
        assertThat(mp.getDetails()).isNotNull();
        assertThat(mp.getGeneratedAt()).isEqualTo(100L);

        // RolloutStageRecord
        RolloutStageRecord rsr = new RolloutStageRecord();
        rsr.setId("rs1");
        rsr.setProjectId("p1");
        rsr.setJobId("j1");
        rsr.setStageIndex(1);
        rsr.setStageName("CANARY");
        rsr.setTargetTrafficPercent(10);
        rsr.setPagesIncluded(5);
        rsr.setStatus("PASSED");
        rsr.setStopConditionTriggered(null);
        rsr.setStartedAt(100L);
        rsr.setCompletedAt(200L);

        assertThat(rsr.getId()).isEqualTo("rs1");
        assertThat(rsr.getProjectId()).isEqualTo("p1");
        assertThat(rsr.getJobId()).isEqualTo("j1");
        assertThat(rsr.getStageIndex()).isEqualTo(1);
        assertThat(rsr.getStageName()).isEqualTo("CANARY");
        assertThat(rsr.getTargetTrafficPercent()).isEqualTo(10);
        assertThat(rsr.getPagesIncluded()).isEqualTo(5);
        assertThat(rsr.getStatus()).isEqualTo("PASSED");
        assertThat(rsr.getStopConditionTriggered()).isNull();
        assertThat(rsr.getStartedAt()).isEqualTo(100L);
        assertThat(rsr.getCompletedAt()).isEqualTo(200L);

        // RepairAttemptRecord
        RepairAttemptRecord rar = new RepairAttemptRecord();
        rar.setId("ra1");
        rar.setProjectId("p1");
        rar.setJobId("j1");
        rar.setTargetPath("block/hero");
        rar.setAttemptNumber(1);
        rar.setIssueCategory("CSS");
        rar.setIssueDescription("Color mismatch");
        rar.setProposedFix("fix css");
        rar.setPatchDiff("diff");
        rar.setSuccessful(true);
        rar.setAiCostMicros(100.0);
        rar.setDurationMs(200L);
        rar.setTimestamp(300L);

        assertThat(rar.getId()).isEqualTo("ra1");
        assertThat(rar.getProjectId()).isEqualTo("p1");
        assertThat(rar.getJobId()).isEqualTo("j1");
        assertThat(rar.getTargetPath()).isEqualTo("block/hero");
        assertThat(rar.getAttemptNumber()).isEqualTo(1);
        assertThat(rar.getIssueCategory()).isEqualTo("CSS");
        assertThat(rar.getIssueDescription()).isEqualTo("Color mismatch");
        assertThat(rar.getProposedFix()).isEqualTo("fix css");
        assertThat(rar.getPatchDiff()).isEqualTo("diff");
        assertThat(rar.isSuccessful()).isTrue();
        assertThat(rar.getAiCostMicros()).isEqualTo(100.0);
        assertThat(rar.getDurationMs()).isEqualTo(200L);
        assertThat(rar.getTimestamp()).isEqualTo(300L);

        // UrlRedirectRecord
        UrlRedirectRecord urr = new UrlRedirectRecord();
        urr.setId("ur1");
        urr.setProjectId("p1");
        urr.setJobId("j1");
        urr.setSourceUrl("/src.html");
        urr.setTargetUrl("/tgt");
        urr.setStatusCode(301);
        urr.setRedirectType("PAGE");
        urr.setConflict(false);
        urr.setConflictReason(null);

        assertThat(urr.getId()).isEqualTo("ur1");
        assertThat(urr.getProjectId()).isEqualTo("p1");
        assertThat(urr.getJobId()).isEqualTo("j1");
        assertThat(urr.getSourceUrl()).isEqualTo("/src.html");
        assertThat(urr.getTargetUrl()).isEqualTo("/tgt");
        assertThat(urr.getStatusCode()).isEqualTo(301);
        assertThat(urr.getRedirectType()).isEqualTo("PAGE");
        assertThat(urr.isConflict()).isFalse();
        assertThat(urr.getConflictReason()).isNull();

        // GeneratedFileRecord
        GeneratedFileRecord gfr = new GeneratedFileRecord();
        gfr.setId("gf1");
        gfr.setProjectId("p1");
        gfr.setJobId("j1");
        gfr.setPath("blocks/hero/hero.js");
        gfr.setFileType("BLOCK_JS");
        gfr.setContent("export default () => {}");
        gfr.setSourcePath("/apps/hero");
        gfr.setVirtualDiffOnly(false);
        gfr.setCreatedAt(100L);

        assertThat(gfr.getId()).isEqualTo("gf1");
        assertThat(gfr.getProjectId()).isEqualTo("p1");
        assertThat(gfr.getJobId()).isEqualTo("j1");
        assertThat(gfr.getPath()).isEqualTo("blocks/hero/hero.js");
        assertThat(gfr.getFileType()).isEqualTo("BLOCK_JS");
        assertThat(gfr.getContent()).isEqualTo("export default () => {}");
        assertThat(gfr.getSourcePath()).isEqualTo("/apps/hero");
        assertThat(gfr.isVirtualDiffOnly()).isFalse();
        assertThat(gfr.getCreatedAt()).isEqualTo(100L);

        // DependencyEdgeRecord
        DependencyEdgeRecord der = new DependencyEdgeRecord();
        der.setId("de1");
        der.setProjectId("p1");
        der.setJobId("j1");
        der.setSource("page:1");
        der.setTarget("block:hero");
        der.setEdgeType("PAGE_TO_BLOCK");
        der.setImpactLevel("HIGH");

        assertThat(der.getId()).isEqualTo("de1");
        assertThat(der.getProjectId()).isEqualTo("p1");
        assertThat(der.getJobId()).isEqualTo("j1");
        assertThat(der.getSource()).isEqualTo("page:1");
        assertThat(der.getTarget()).isEqualTo("block:hero");
        assertThat(der.getEdgeType()).isEqualTo("PAGE_TO_BLOCK");
        assertThat(der.getImpactLevel()).isEqualTo("HIGH");

        // CheckpointRecord
        CheckpointRecord cpr = new CheckpointRecord();
        cpr.setId("cp1");
        cpr.setProjectId("p1");
        cpr.setJobId("j1");
        cpr.setState("ANALYZING");
        cpr.setResumeHint("hint");
        cpr.setStateData(new HashMap<>());
        cpr.setTimestamp(100L);

        assertThat(cpr.getId()).isEqualTo("cp1");
        assertThat(cpr.getProjectId()).isEqualTo("p1");
        assertThat(cpr.getJobId()).isEqualTo("j1");
        assertThat(cpr.getState()).isEqualTo("ANALYZING");
        assertThat(cpr.getResumeHint()).isEqualTo("hint");
        assertThat(cpr.getStateData()).isNotNull();
        assertThat(cpr.getTimestamp()).isEqualTo(100L);

        // BenchmarkSampleRecord
        BenchmarkSampleRecord bsr = new BenchmarkSampleRecord();
        bsr.setId("bs1");
        bsr.setProjectId("p1");
        bsr.setJobId("j1");
        bsr.setAgent("connection");
        bsr.setOperation("test");
        bsr.setPromptTokens(5);
        bsr.setCompletionTokens(10);
        bsr.setDurationMs(100L);
        bsr.setCostMicros(10.0);
        bsr.setTimestamp(200L);

        assertThat(bsr.getId()).isEqualTo("bs1");
        assertThat(bsr.getProjectId()).isEqualTo("p1");
        assertThat(bsr.getJobId()).isEqualTo("j1");
        assertThat(bsr.getAgent()).isEqualTo("connection");
        assertThat(bsr.getOperation()).isEqualTo("test");
        assertThat(bsr.getPromptTokens()).isEqualTo(5);
        assertThat(bsr.getCompletionTokens()).isEqualTo(10);
        assertThat(bsr.getDurationMs()).isEqualTo(100L);
        assertThat(bsr.getCostMicros()).isEqualTo(10.0);
        assertThat(bsr.getTimestamp()).isEqualTo(200L);

        // ClarificationRecord
        ClarificationRecord clr = new ClarificationRecord();
        clr.setId("cl1");
        clr.setProjectId("p1");
        clr.setJobId("j1");
        clr.setQuestion("Q");
        clr.setRationale("R");
        clr.setOptions(Arrays.asList("OptA"));
        clr.setDefaultOption("OptA");
        clr.setSelectedOption("OptA");
        clr.setStatus("RESOLVED");
        clr.setAffectedPages(Arrays.asList("/page"));
        clr.setCreatedAt(100L);
        clr.setResolvedAt(200L);

        assertThat(clr.getId()).isEqualTo("cl1");
        assertThat(clr.getProjectId()).isEqualTo("p1");
        assertThat(clr.getJobId()).isEqualTo("j1");
        assertThat(clr.getQuestion()).isEqualTo("Q");
        assertThat(clr.getRationale()).isEqualTo("R");
        assertThat(clr.getOptions()).contains("OptA");
        assertThat(clr.getDefaultOption()).isEqualTo("OptA");
        assertThat(clr.getSelectedOption()).isEqualTo("OptA");
        assertThat(clr.getStatus()).isEqualTo("RESOLVED");
        assertThat(clr.getAffectedPages()).contains("/page");
        assertThat(clr.getCreatedAt()).isEqualTo(100L);
        assertThat(clr.getResolvedAt()).isEqualTo(200L);

        // JobEventRecord
        JobEventRecord jer = new JobEventRecord();
        jer.setId("je1");
        jer.setProjectId("p1");
        jer.setJobId("j1");
        jer.setLevel("WARN");
        jer.setAgent("connection");
        jer.setFromState("CREATED");
        jer.setToState("CONNECTING");
        jer.setMessage("Connecting");
        jer.setActor("admin");
        jer.setTimestamp(100L);

        assertThat(jer.getId()).isEqualTo("je1");
        assertThat(jer.getProjectId()).isEqualTo("p1");
        assertThat(jer.getJobId()).isEqualTo("j1");
        assertThat(jer.getLevel()).isEqualTo("WARN");
        assertThat(jer.getAgent()).isEqualTo("connection");
        assertThat(jer.getFromState()).isEqualTo("CREATED");
        assertThat(jer.getToState()).isEqualTo("CONNECTING");
        assertThat(jer.getMessage()).isEqualTo("Connecting");
        assertThat(jer.getActor()).isEqualTo("admin");
        assertThat(jer.getTimestamp()).isEqualTo(100L);

        // ValidationResultRecord
        ValidationResultRecord vrr = new ValidationResultRecord();
        vrr.setId("vr1");
        vrr.setProjectId("p1");
        vrr.setJobId("j1");
        vrr.setTargetPath("/content/wknd/us/en");
        vrr.setValidationType("VISUAL");
        vrr.setPassed(true);
        vrr.setVisualScore(0.98);
        vrr.setA11yScore(1.0);
        vrr.setIssues(Arrays.asList("None"));
        vrr.setScreenshotBase64("data:image/png;base64,...");
        vrr.setTimestamp(100L);

        assertThat(vrr.getId()).isEqualTo("vr1");
        assertThat(vrr.getProjectId()).isEqualTo("p1");
        assertThat(vrr.getJobId()).isEqualTo("j1");
        assertThat(vrr.getTargetPath()).isEqualTo("/content/wknd/us/en");
        assertThat(vrr.getValidationType()).isEqualTo("VISUAL");
        assertThat(vrr.isPassed()).isTrue();
        assertThat(vrr.getVisualScore()).isEqualTo(0.98);
        assertThat(vrr.getA11yScore()).isEqualTo(1.0);
        assertThat(vrr.getIssues()).contains("None");
        assertThat(vrr.getScreenshotBase64()).isNotNull();
        assertThat(vrr.getTimestamp()).isEqualTo(100L);

        // MigrationContractRecord
        MigrationContractRecord mcr = new MigrationContractRecord();
        mcr.setId("mc1");
        mcr.setProjectId("p1");
        mcr.setJobId("j1");
        mcr.setApprovedScope("/content/wknd");
        mcr.setAuthoringStrategy("UNIVERSAL_EDITOR");
        mcr.setAssetPolicy("METADATA_ONLY");
        mcr.setAiProvider("anthropic");
        mcr.setAiModel("claude-3-5-sonnet");
        mcr.setApprovedBudgetUsd(50.0);
        mcr.setMaxRepairAttempts(5);
        mcr.setApprovedByOperator(true);
        mcr.setApprovedBy("admin");
        mcr.setApprovedAt(100L);
        mcr.setAcceptedRisks(Arrays.asList("RiskA"));

        assertThat(mcr.getId()).isEqualTo("mc1");
        assertThat(mcr.getProjectId()).isEqualTo("p1");
        assertThat(mcr.getJobId()).isEqualTo("j1");
        assertThat(mcr.getApprovedScope()).isEqualTo("/content/wknd");
        assertThat(mcr.getAuthoringStrategy()).isEqualTo("UNIVERSAL_EDITOR");
        assertThat(mcr.getAssetPolicy()).isEqualTo("METADATA_ONLY");
        assertThat(mcr.getAiProvider()).isEqualTo("anthropic");
        assertThat(mcr.getAiModel()).isEqualTo("claude-3-5-sonnet");
        assertThat(mcr.getApprovedBudgetUsd()).isEqualTo(50.0);
        assertThat(mcr.getMaxRepairAttempts()).isEqualTo(5);
        assertThat(mcr.isApprovedByOperator()).isTrue();
        assertThat(mcr.getApprovedBy()).isEqualTo("admin");
        assertThat(mcr.getApprovedAt()).isEqualTo(100L);
        assertThat(mcr.getAcceptedRisks()).contains("RiskA");
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
