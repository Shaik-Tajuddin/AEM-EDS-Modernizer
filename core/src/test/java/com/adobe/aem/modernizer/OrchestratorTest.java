package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.agents.*;
import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ModelCapability;
import com.adobe.aem.modernizer.ai.providers.MockAiProvider;
import com.adobe.aem.modernizer.ai.routing.AiRoutingPolicy;
import com.adobe.aem.modernizer.ai.secret.EnvSecretProvider;
import com.adobe.aem.modernizer.connectors.*;
import com.adobe.aem.modernizer.mock.*;
import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobRecord;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.scopes.MarkerEvaluator;
import com.adobe.aem.modernizer.services.EstimatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrchestratorTest {

    private Store store;
    private Orchestrator orchestrator;
    private ProjectRecord project;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
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

        orchestrator = new Orchestrator(store, ai, estimator);

        AemClient aemAuthor = new MockAemClient("https://mock-aem.local", "author", 42, true);
        AemClient aemPublish = new MockAemClient("https://mock-aem.local", "publish", 42, true);
        GitHubClient gh = new MockGitHubClient("https://github.com/company/wknd-eds");
        FigmaClient figma = new MockFigmaClient("https://www.figma.com/design/abcdef/WKND");
        EdsClient eds = new MockEdsClient("https://eds-mock.local");
        BrowserClient browser = new MockBrowserClient();

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

        orchestrator.register(new AdvancedFigmaIntelligenceAgent(figma, store, ai));
        orchestrator.register(new AdvancedVisualValidationAgent(browser, store, ai));
        orchestrator.register(new AdvancedRepairAgent(store, ai, 5));
        orchestrator.register(new AdvancedRolloutAgent(store, ai, RolloutPolicy.defaultPolicy()));

        project = new ProjectRecord("test-wknd", "WKND Test", "https://mock-aem.local", "/content/wknd", "https://github.com/company/wknd-eds");
        store.saveProject(project);
    }

    @Test
    void testDryRunCompletesSuccessfully() throws Exception {
        JobRecord job = orchestrator.runDryRun(project, "tester");

        assertThat(job).isNotNull();
        assertThat(job.getState()).isEqualTo("COMPLETED");
        assertThat(job.isDryRun()).isTrue();

        assertThat(store.getInventory(job.getId())).isPresent();
        assertThat(store.getPlan(job.getId())).isPresent();
        assertThat(store.getGeneratedFiles(job.getId())).isNotEmpty();
        assertThat(store.getEvents(job.getId())).isNotEmpty();
    }

    @Test
    void testMigrationCompletesSuccessfully() throws Exception {
        JobRecord job = orchestrator.runMigration(project, "tester");

        assertThat(job).isNotNull();
        assertThat(job.getState()).isEqualTo("COMPLETED");
        assertThat(job.isDryRun()).isFalse();

        assertThat(store.getGeneratedFiles(job.getId())).isNotEmpty();
        assertThat(store.getValidationResults(job.getId())).isNotEmpty();
        assertThat(store.getRepairAttempts(job.getId())).isNotEmpty();
        assertThat(store.getUrlRedirects(job.getId())).isNotEmpty();
        assertThat(store.getDependencyEdges(job.getId())).isNotEmpty();
        assertThat(store.getRolloutStages(job.getId())).isNotEmpty();
    }
}
