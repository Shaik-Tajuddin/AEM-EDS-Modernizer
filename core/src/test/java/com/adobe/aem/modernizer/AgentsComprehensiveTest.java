package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.agents.*;
import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.connectors.*;
import com.adobe.aem.modernizer.mock.*;
import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobRecord;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import com.adobe.aem.modernizer.scopes.MarkerEvaluator;
import com.adobe.aem.modernizer.services.EstimatorService;
import com.adobe.aem.modernizer.standalone.StandaloneMain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentsComprehensiveTest {

    private Store store;
    private ProjectRecord project;
    private JobRecord job;
    private AgentContext ctx;
    private AiGateway aiGateway;
    private AemClient aemAuthor;
    private AemClient aemPublish;
    private GitHubClient gitHubClient;
    private FigmaClient figmaClient;
    private EdsClient edsClient;
    private BrowserClient browserClient;
    private MarkerEvaluator marker;
    private EstimatorService estimator;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        project = new ProjectRecord("test-wknd", "WKND Site", "http://mock-author:4502", "/content/wknd", "https://github.com/company/wknd-eds");
        project.setFigmaUrl("https://figma.com/file/mock-wknd");
        store.saveProject(project);

        job = new JobRecord("test-job", project.getId(), "MIGRATE");
        store.saveJob(job);

        ctx = new AgentContext(project, job);
        ctx.setDryRun(false);

        aiGateway = new AiGateway();
        aiGateway.activate();

        aemAuthor = new MockAemClient("http://mock-author:4502", "author", 42, true);
        aemPublish = new MockAemClient("http://mock-publish:4503", "publish", 42, true);
        gitHubClient = new MockGitHubClient("https://github.com/company/wknd-eds");
        figmaClient = new MockFigmaClient("https://www.figma.com/design/abcdef/WKND");
        edsClient = new MockEdsClient("https://main--wknd--hlx.live");
        browserClient = new MockBrowserClient();
        marker = new MarkerEvaluator("edsModernize", "true");
        estimator = new EstimatorService();

        SiteInventory inventory = MockDataFactory.createWkndInventory(project.getContentRoot(), null, 10);
        store.saveInventory(inventory);
    }

    @Test
    void testAgentContext() {
        AgentContext context = new AgentContext(project, job);
        assertThat(context.getProject()).isNotNull();
        assertThat(context.getJob()).isNotNull();
        assertThat(context.getAttributes()).isNotNull();
        context.setDryRun(true);
        assertThat(context.isDryRun()).isTrue();
        context.incrementRepairAttempts();
        assertThat(context.getRepairAttempts()).isEqualTo(1);
        context.setLastGeneratedPrUrl("https://github.com/pr/1");
        assertThat(context.getLastGeneratedPrUrl()).isEqualTo("https://github.com/pr/1");
    }

    @Test
    void testMigrationStateEnum() {
        for (MigrationState state : MigrationState.values()) {
            assertThat(state.name()).isNotNull();
            assertThat(state.canTransitionTo(MigrationState.FAILED)).isTrue();
        }
        assertThat(MigrationState.CREATED.canTransitionTo(MigrationState.CONNECTING)).isTrue();
        assertThat(MigrationState.valueOf("COMPLETED")).isEqualTo(MigrationState.COMPLETED);
    }

    @Test
    void testAuthoringStrategyRegistry() {
        AuthoringStrategyRegistry registry = new AuthoringStrategyRegistry();
        assertThat(registry.listStrategies()).contains("UNIVERSAL_EDITOR");
        assertThat(registry.isValid("UNIVERSAL_EDITOR")).isTrue();
        assertThat(registry.isValid("INVALID")).isFalse();
    }

    @Test
    void testRolloutPolicy() {
        RolloutPolicy policy = RolloutPolicy.defaultPolicy();
        assertThat(policy.getStages()).isNotEmpty();

        RolloutPolicy.StageDefinition stage = new RolloutPolicy.StageDefinition("Canary", 10, 0.92);
        assertThat(stage.getName()).isEqualTo("Canary");
        assertThat(stage.getTrafficPercent()).isEqualTo(10);
        assertThat(stage.getMinVisualScore()).isEqualTo(0.92);
    }

    @Test
    void testAllAgentsIndividually() throws Exception {
        new ConnectionAgent(aemAuthor, aemPublish, gitHubClient, figmaClient, edsClient, browserClient, store, aiGateway).execute(ctx);
        new DiscoveryAgent(aemAuthor, store, aiGateway, marker).execute(ctx);
        new ComponentIntelligenceAgent(store, aiGateway).execute(ctx);
        new ComponentMappingAgent(store, aiGateway).execute(ctx);
        new TemplateAnalysisAgent(store, aiGateway).execute(ctx);
        new ContentAnalysisAgent(store, aiGateway).execute(ctx);
        new AssetAnalysisAgent(aemAuthor, store, aiGateway).execute(ctx);
        new ContentFragmentAnalysisAgent(store, aiGateway).execute(ctx);
        new MsmAnalysisAgent(store, aiGateway).execute(ctx);
        new FigmaAnalysisAgent(figmaClient, store, aiGateway).execute(ctx);
        new AdvancedFigmaIntelligenceAgent(figmaClient, store, aiGateway).execute(ctx);
        new MigrationPlannerAgent(store, aiGateway, estimator).execute(ctx);
        new BlockGenerationAgent(store, aiGateway).execute(ctx);
        new CodeGenerationAgent(store, aiGateway).execute(ctx);
        new ContentMigrationAgent(store, aiGateway).execute(ctx);
        new AuthoringAgent(aemAuthor, store, aiGateway).execute(ctx);
        new PreviewAgent(gitHubClient, edsClient, store, aiGateway).execute(ctx);
        new ValidationAgent(browserClient, store, aiGateway).execute(ctx);
        new VisualValidationAgent(browserClient, store, aiGateway).execute(ctx);
        new AdvancedVisualValidationAgent(browserClient, store, aiGateway).execute(ctx);
        new SelfRepairAgent(store, aiGateway).execute(ctx);
        new AdvancedRepairAgent(store, aiGateway, 5).execute(ctx);
        new AdvancedRolloutAgent(store, aiGateway, RolloutPolicy.defaultPolicy()).execute(ctx);
        new PublishingAgent(gitHubClient, store, aiGateway).execute(ctx);
        new VerificationAgent(browserClient, store, aiGateway).execute(ctx);
    }

    @Test
    void testStandaloneMain() throws Exception {
        com.sun.net.httpserver.HttpServer server = StandaloneMain.startServer(0);
        assertThat(server).isNotNull();
        server.stop(0);
    }
}
