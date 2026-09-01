package com.adobe.aem.modernizer.osgi;

import com.adobe.aem.modernizer.agents.*;
import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ModelCapability;
import com.adobe.aem.modernizer.ai.providers.MockAiProvider;
import com.adobe.aem.modernizer.ai.routing.AiRoutingPolicy;
import com.adobe.aem.modernizer.connectors.*;
import com.adobe.aem.modernizer.mock.*;
import com.adobe.aem.modernizer.persistence.Store;
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
    @Reference private transient EstimatorService estimator;
    @Reference private transient Orchestrator orchestrator;
    @Reference private transient AiGateway ai;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL)
    private transient GitHubClient gitHubClient;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL)
    private transient AemClient realAemClient;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL)
    private transient LocalEdsRepoManager localEdsRepo;

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
        GitHubClient gh = (gitHubClient != null) ? gitHubClient : new MockGitHubClient();
        FigmaClient figma = new MockFigmaClient("https://www.figma.com/design/abcdef/WKND");
        EdsClient eds = new MockEdsClient("https://eds-mock.local");
        BrowserClient browser = new MockBrowserClient();

        if (orchestrator != null) {
            // Register Core Phase 1 Agents
            orchestrator.registerCoreAgents(
                    new ConnectionAgent(aemAuthor, gh, eds, browser, store),
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
                    new BlockGenerationAgent(store, ai, localEdsRepo),
                    new CodeGenerationAgent(store, ai, localEdsRepo),
                    new ContentMigrationAgent(store, ai),
                    new AuthoringAgent(store),
                    new PreviewAgent(gh, eds, store, ai),
                    new ValidationAgent(browser, store),
                    new VisualValidationAgent(store),
                    new SelfRepairAgent(store, ai),
                    new PublishingAgent(gh, store),
                    new VerificationAgent(store)
            );

            // Register Phase 2 Advanced Agents
            orchestrator.register(new AdvancedFigmaIntelligenceAgent(store, ai));
            orchestrator.register(new AdvancedVisualValidationAgent(store, ai));
            orchestrator.register(new AdvancedRepairAgent(store, ai, 5));
            orchestrator.register(new AdvancedRolloutAgent(store, RolloutPolicy.defaultPolicy()));

            LOG.info("Modernizer OSGi agent graph wired: {} agents ready.", orchestrator.getAgents().size());
        }
    }
}
