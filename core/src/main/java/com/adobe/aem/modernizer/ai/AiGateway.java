package com.adobe.aem.modernizer.ai;

import com.adobe.aem.modernizer.ai.providers.AiProvider;
import com.adobe.aem.modernizer.ai.providers.MockAiProvider;
import com.adobe.aem.modernizer.ai.routing.AiRoutingPolicy;
import com.adobe.aem.modernizer.ai.secret.EnvSecretProvider;
import com.adobe.aem.modernizer.ai.secret.SecretProvider;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.security.Redactor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative AI Gateway managing provider dispatch, capability gates, routing, and cost control (ADR 0003, ADR 0007).
 */
@Component(service = AiGateway.class, immediate = true)
public class AiGateway {

    private static final Logger LOG = LoggerFactory.getLogger(AiGateway.class);

    private final Map<String, AiProvider> providers = new ConcurrentHashMap<>();
    private final CapabilityRegistry capabilityRegistry = new CapabilityRegistry();
    private final PricingService pricingService = new PricingService();
    private AiRoutingPolicy routingPolicy = new AiRoutingPolicy();
    private SecretProvider secretProvider = new EnvSecretProvider();
    private Store store;
    private boolean localOnly = false;
    private int maxRepairAttempts = 5;

    @Reference
    private transient Store storeRef;

    public AiGateway() {
        // Register default Mock capability
        capabilityRegistry.add(new ModelCapability("mock", "mock-general-1", 8192)
                .add(ModelCapability.CAP_CHAT)
                .add(ModelCapability.CAP_STRUCTURED)
                .add(ModelCapability.CAP_CODE)
                .add(ModelCapability.CAP_VISION)
                .add(ModelCapability.CAP_LOCAL));
        register(new MockAiProvider("mock", "mock-general-1"));
    }

    public AiGateway(AiRoutingPolicy routing, SecretProvider secrets, Store store, boolean localOnly, int maxRepairAttempts) {
        this();
        this.routingPolicy = routing != null ? routing : this.routingPolicy;
        this.secretProvider = secrets != null ? secrets : this.secretProvider;
        this.store = store;
        this.localOnly = localOnly;
        this.maxRepairAttempts = maxRepairAttempts;
    }

    @Activate
    public void activate() {
        if (this.store == null && this.storeRef != null) {
            this.store = this.storeRef;
        }
        LOG.info("AiGateway activated with {} providers", providers.size());
    }

    public void register(AiProvider provider) {
        if (provider != null) {
            providers.put(provider.getProviderName().toLowerCase(), provider);
        }
    }

    public ChatResponse dispatch(ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ChatRequest cannot be null");
        }

        String agentName = request.getAgentName() != null ? request.getAgentName() : "general";
        String providerName = routingPolicy.resolveProvider(agentName);
        String modelName = routingPolicy.resolveModel(agentName);

        if (localOnly && !"ollama".equalsIgnoreCase(providerName) && !"mock".equalsIgnoreCase(providerName)) {
            LOG.warn("Local-only mode active: overriding provider {} to mock/ollama", providerName);
            providerName = "mock";
            modelName = "mock-general-1";
        }

        // Capability Gate Check
        if (request.getTargetCapability() != null) {
            boolean supported = capabilityRegistry.supports(providerName, modelName, request.getTargetCapability());
            if (!supported && !"mock".equalsIgnoreCase(providerName)) {
                LOG.warn("Model {}:{} lacks capability {}; falling back to mock provider", providerName, modelName, request.getTargetCapability());
                providerName = "mock";
                modelName = "mock-general-1";
            }
        }

        AiProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            LOG.warn("Provider '{}' not registered; falling back to mock", providerName);
            provider = providers.get("mock");
            if (provider == null) {
                provider = new MockAiProvider();
                register(provider);
            }
            providerName = "mock";
            modelName = "mock-general-1";
        }

        // Resolve secret reference
        String apiKey = null;
        if (!"mock".equalsIgnoreCase(providerName) && !"ollama".equalsIgnoreCase(providerName)) {
            String secretRef = "env:" + providerName.toUpperCase() + "_API_KEY";
            apiKey = secretProvider.resolve(secretRef);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                LOG.warn("No API key resolved for {}; falling back to mock provider", providerName);
                provider = providers.get("mock");
                providerName = "mock";
                modelName = "mock-general-1";
            }
        }

        long start = System.currentTimeMillis();
        ChatResponse response = provider.chat(request, modelName, apiKey);
        long duration = System.currentTimeMillis() - start;

        // Calculate Cost
        double cost = pricingService.calculateCostUsd(
                providerName,
                modelName,
                response.getTokenUsage().getPromptTokens(),
                response.getTokenUsage().getCompletionTokens()
        );
        response.setCostUsd(cost);

        // Sanitize / Redact response content
        if (response.getContent() != null) {
            response.setContent(Redactor.redact(response.getContent()));
        }

        LOG.debug("[AI] agent={} provider={} model={} promptTokens={} compTokens={} cost=${} dur={}ms",
                agentName, providerName, modelName,
                response.getTokenUsage().getPromptTokens(),
                response.getTokenUsage().getCompletionTokens(),
                cost, duration);

        return response;
    }

    public CapabilityRegistry capabilities() { return capabilityRegistry; }
    public AiRoutingPolicy routingPolicy() { return routingPolicy; }
    public PricingService pricing() { return pricingService; }
    public SecretProvider secrets() { return secretProvider; }
    public boolean isLocalOnly() { return localOnly; }
    public int getMaxRepairAttempts() { return maxRepairAttempts; }
}
