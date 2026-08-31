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
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import com.adobe.aem.modernizer.persistence.model.JobRecord;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.services.EstimatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrchestratorTest {

    private Store store;
    private Orchestrator orchestrator;
    private ProjectRecord project;
    private MockGitHubClient gitHub;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
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

        orchestrator = new Orchestrator(store);

        AemClient aemAuthor = new MockAemClient("https://mock-aem.local", "author", 42, true);
        gitHub = new MockGitHubClient("https://github.com/company/wknd-eds");
        FigmaClient figma = new MockFigmaClient("https://www.figma.com/design/abcdef/WKND");
        EdsClient eds = new MockEdsClient("https://eds-mock.local");
        BrowserClient browser = new MockBrowserClient();

        orchestrator.registerCoreAgents(
                new ConnectionAgent(aemAuthor, gitHub, eds, browser, store),
                new DiscoveryAgent(aemAuthor, store),
                new ComponentIntelligenceAgent(store, ai),
                new ComponentMappingAgent(store, ai),
                new TemplateAnalysisAgent(store),
                new ContentAnalysisAgent(store),
                new AssetAnalysisAgent(store),
                new ContentFragmentAnalysisAgent(store),
                new MsmAnalysisAgent(store),
                new FigmaAnalysisAgent(figma, store),
                new MigrationPlannerAgent(store, estimator),
                new BlockGenerationAgent(store, ai),
                new CodeGenerationAgent(store, ai),
                new ContentMigrationAgent(store, ai),
                new AuthoringAgent(store),
                new PreviewAgent(gitHub, eds, store, ai),
                new ValidationAgent(browser, store),
                new VisualValidationAgent(store),
                new SelfRepairAgent(store, ai),
                new PublishingAgent(gitHub, store),
                new VerificationAgent(store)
        );

        orchestrator.register(new AdvancedFigmaIntelligenceAgent(store, ai));
        orchestrator.register(new AdvancedVisualValidationAgent(store, ai));
        orchestrator.register(new AdvancedRepairAgent(store, ai, 5));
        orchestrator.register(new AdvancedRolloutAgent(store, RolloutPolicy.defaultPolicy()));

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
        assertThat(job.getState()).isEqualTo("READY_TO_PUBLISH");
        assertThat(job.isDryRun()).isFalse();

        assertThat(store.getGeneratedFiles(job.getId())).isNotEmpty();
        assertThat(store.getGeneratedFiles(job.getId()))
                .extracting(GeneratedFileRecord::getPath)
                .anyMatch(path -> path != null && path.startsWith("docs/migrated-pages/"));
        assertThat(store.getGeneratedFiles(job.getId()))
                .extracting(GeneratedFileRecord::getPath)
                .noneMatch(path -> path != null && path.replace('\\', '/').endsWith("fstab.yaml"));
        assertThat(store.getValidationResults(job.getId())).isNotEmpty();
        assertThat(store.getRepairAttempts(job.getId())).isNotEmpty();
        assertThat(store.getUrlRedirects(job.getId())).isNotEmpty();
        assertThat(store.getDependencyEdges(job.getId())).isNotEmpty();
        assertThat(store.getRolloutStages(job.getId())).isNotEmpty();

        int commitsAfterMigrate = gitHub.getCommitCount();
        int prsAfterMigrate = gitHub.getPrCount();

        JobRecord previewJob = orchestrator.pushToPreviewBranch(project, "tester");
        assertThat(previewJob.getState()).isEqualTo("PREVIEWING");
        assertThat(previewJob.getMetadata()).containsKey("vscodeUrl");
        assertThat(String.valueOf(previewJob.getMetadata().get("vscodeUrl")))
                .contains("/tree/feat/test-wknd")
                .doesNotContain("%2F");
        assertThat(previewJob.getMetadata()).containsEntry("branch", "feat/test-wknd");
        assertThat(gitHub.listChangedFiles("main", "feat/test-wknd"))
                .extracting(row -> String.valueOf(row.get("filename")))
                .noneMatch(path -> path.endsWith("fstab.yaml"));
        assertThat(gitHub.getCommitCount()).isGreaterThan(commitsAfterMigrate);
        assertThat(gitHub.getPrCount()).isEqualTo(prsAfterMigrate);

        JobRecord pushJob = orchestrator.pushToGitHub(project, "tester");
        assertThat(pushJob).isNotNull();
        assertThat(pushJob.getState()).isEqualTo("COMPLETED");
        assertThat(pushJob.getMetadata()).containsKey("prUrl");
        assertThat(gitHub.getPrCount()).isEqualTo(prsAfterMigrate + 1);
        assertThat(gitHub.getCommitCount()).isGreaterThan(commitsAfterMigrate);
    }

    @Test
    void openPullRequestDoesNotCommitAgain() throws Exception {
        orchestrator.runMigration(project, "tester");
        orchestrator.pushToPreviewBranch(project, "tester");
        int commits = gitHub.getCommitCount();
        JobRecord pr = orchestrator.openPullRequest(project, "tester");
        assertThat(pr.getState()).isEqualTo("COMPLETED");
        assertThat(gitHub.getCommitCount()).isEqualTo(commits);
        assertThat(gitHub.getPrCount()).isEqualTo(1);
    }
}
