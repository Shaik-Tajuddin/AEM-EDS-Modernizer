package com.adobe.aem.modernizer.osgi;

import com.adobe.aem.modernizer.agents.*;
import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ModelCapability;
import com.adobe.aem.modernizer.ai.providers.MockAiProvider;
import com.adobe.aem.modernizer.ai.routing.AiRoutingPolicy;
import com.adobe.aem.modernizer.connectors.*;
import com.adobe.aem.modernizer.mock.*;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.scopes.MarkerEvaluator;
import com.adobe.aem.modernizer.services.ClarificationService;
import com.adobe.aem.modernizer.services.EstimatorService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi Declarative Services Bundle Activator wiring mock clients, AI routing, and the agent graph (Master §3).
 */
@Component(service = ModernizerBundleActivator.class, immediate = true)
public class ModernizerBundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(ModernizerBundleActivator.class);

    @Reference private transient Store store;
    @Reference private transient MarkerEvaluator marker;
    @Reference private transient EstimatorService estimator;
    @Reference private transient ClarificationService clarifications;
    @Reference private transient Orchestrator orchestrator;
    @Reference private transient AiGateway ai;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL)
    private transient GitHubClient gitHubClient;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL)
    private transient AemClient realAemClient;

    @Activate
    public void activate() {
        LOG.info("ModernizerBundleActivator activating (mockMode=true)");

        // Configure the Sling-provided AiGateway
        if (ai != null) {
            AiRoutingPolicy routing = ai.routingPolicy();
            if (routing == null) {
                routing = new AiRoutingPolicy();
            }
            routing.setStrategy("MULTI_PROVIDER");
            routing.setDefaultProvider("mock");
            routing.setDefaultModel("mock-general-1");

            ai.capabilities().add(new ModelCapability("mock", "mock-general-1", 8192)
                    .add(ModelCapability.CAP_CHAT)
                    .add(ModelCapability.CAP_STRUCTURED)
                    .add(ModelCapability.CAP_CODE)
                    .add(ModelCapability.CAP_VISION)
                    .add(ModelCapability.CAP_LOCAL));
            ai.register(new MockAiProvider("mock", "mock-general-1"));
        }

        // Build connectors — use the injected OSGi GitHubClient when available,
        // otherwise fall back to MockGitHubClient.
        AemClient aemAuthor = (realAemClient != null) ? realAemClient
                : new MockAemClient("https://mock-aem.local", "author", 42, true);
        AemClient aemPublish = new MockAemClient("https://mock-aem.local", "publish", 42, true);
        GitHubClient gh = (gitHubClient != null) ? gitHubClient : new MockGitHubClient();
        FigmaClient figma = new MockFigmaClient("https://www.figma.com/design/abcdef/WKND");
        EdsClient eds = new MockEdsClient("https://eds-mock.local");
        BrowserClient browser = new MockBrowserClient();

        if (orchestrator != null) {
            // Register Core Phase 1 Agents
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

            // Register Phase 2 Advanced Agents
            orchestrator.register(new AdvancedFigmaIntelligenceAgent(figma, store, ai));
            orchestrator.register(new AdvancedVisualValidationAgent(browser, store, ai));
            orchestrator.register(new AdvancedRepairAgent(store, ai, 5));
            orchestrator.register(new AdvancedRolloutAgent(store, ai, RolloutPolicy.defaultPolicy()));

            LOG.info("Modernizer OSGi agent graph wired: {} agents ready.", orchestrator.getAgents().size());
        }
    }
}
