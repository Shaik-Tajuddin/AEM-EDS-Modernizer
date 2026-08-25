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
        initProviders();
    }

    private void initProviders() {
        try {
            // Register Mock capability
            capabilityRegistry.add(new ModelCapability("mock", "mock-general-1", 8192)
                    .add(ModelCapability.CAP_CHAT)
                    .add(ModelCapability.CAP_STRUCTURED)
                    .add(ModelCapability.CAP_CODE)
                    .add(ModelCapability.CAP_VISION)
                    .add(ModelCapability.CAP_LOCAL));
            register(new MockAiProvider("mock", "mock-general-1"));

            // Register Ollama local capabilities & provider
            capabilityRegistry.add(new ModelCapability("ollama", "llama3", 8192)
                    .add(ModelCapability.CAP_CHAT).add(ModelCapability.CAP_STRUCTURED).add(ModelCapability.CAP_CODE).add(ModelCapability.CAP_LOCAL));
            capabilityRegistry.add(new ModelCapability("ollama", "qwen3:8b", 8192)
                    .add(ModelCapability.CAP_CHAT).add(ModelCapability.CAP_STRUCTURED).add(ModelCapability.CAP_CODE).add(ModelCapability.CAP_LOCAL));
            capabilityRegistry.add(new ModelCapability("ollama", "llama3:8b", 8192)
                    .add(ModelCapability.CAP_CHAT).add(ModelCapability.CAP_STRUCTURED).add(ModelCapability.CAP_CODE).add(ModelCapability.CAP_LOCAL));
            register(new com.adobe.aem.modernizer.ai.providers.OllamaProvider("http://localhost:11434"));

            // Register Cloud AI Providers
            capabilityRegistry.add(new ModelCapability("anthropic", "claude-3-5-sonnet-20241022", 200000)
                    .add(ModelCapability.CAP_CHAT).add(ModelCapability.CAP_STRUCTURED).add(ModelCapability.CAP_CODE).add(ModelCapability.CAP_VISION));
            capabilityRegistry.add(new ModelCapability("openai", "gpt-4o", 128000)
                    .add(ModelCapability.CAP_CHAT).add(ModelCapability.CAP_STRUCTURED).add(ModelCapability.CAP_CODE).add(ModelCapability.CAP_VISION));
            capabilityRegistry.add(new ModelCapability("gemini", "gemini-1.5-pro", 1000000)
                    .add(ModelCapability.CAP_CHAT).add(ModelCapability.CAP_STRUCTURED).add(ModelCapability.CAP_CODE).add(ModelCapability.CAP_VISION));
            register(new com.adobe.aem.modernizer.ai.providers.AnthropicProvider());
            register(new com.adobe.aem.modernizer.ai.providers.OpenAiProvider());
            register(new com.adobe.aem.modernizer.ai.providers.GeminiProvider());
        } catch (Throwable t) {
            LOG.error("Failed to initialize AI providers: {}", t.getMessage(), t);
        }
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
        LOG.info("AiGateway activated with {} providers: {}", providers.size(), providers.keySet());
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
        String providerName = (request.getPreferredProvider() != null && !request.getPreferredProvider().trim().isEmpty())
                ? request.getPreferredProvider().trim().toLowerCase()
                : routingPolicy.resolveProvider(agentName);
        String modelName = (request.getPreferredModel() != null && !request.getPreferredModel().trim().isEmpty())
                ? request.getPreferredModel().trim()
                : routingPolicy.resolveModel(agentName);

        if (localOnly && !"ollama".equalsIgnoreCase(providerName) && !"mock".equalsIgnoreCase(providerName)) {
            LOG.warn("Local-only mode active: overriding provider {} to ollama/mock", providerName);
            providerName = "ollama";
            modelName = "qwen3:8b";
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

        // Resolve secret reference for cloud providers
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
        ChatResponse response;
        try {
            response = provider.chat(request, modelName, apiKey);
        } catch (Exception e) {
            LOG.warn("AI dispatch to {} ({}) failed: {}. Falling back to mock", providerName, modelName, e.getMessage());
            AiProvider mockProv = providers.get("mock");
            if (mockProv == null) mockProv = new MockAiProvider();
            response = mockProv.chat(request, "mock-general-1", null);
            providerName = "mock";
            modelName = "mock-general-1";
        }
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

        LOG.info("[AI:{}] Dispatched to {} model '{}': promptTokens={} compTokens={} dur={}ms",
                providerName, providerName, modelName,
                response.getTokenUsage().getPromptTokens(),
                response.getTokenUsage().getCompletionTokens(),
                duration);

        if (store != null && request.getProjectId() != null && request.getJobId() != null) {
            String snippet = (request.getPrompt() != null && request.getPrompt().length() > 60)
                    ? request.getPrompt().substring(0, 60) + "..." : request.getPrompt();
            store.recordEvent(new com.adobe.aem.modernizer.persistence.model.JobEventRecord(
                    java.util.UUID.randomUUID().toString(),
                    request.getProjectId(),
                    request.getJobId(),
                    "ai-" + providerName,
                    "🤖 [AI:" + providerName + "] (model: " + modelName + ") " + snippet + " -> Completed in " + duration + "ms"
            ));
        }

        return response;
    }

    public CapabilityRegistry capabilities() { return capabilityRegistry; }
    public AiRoutingPolicy routingPolicy() { return routingPolicy; }
    public PricingService pricing() { return pricingService; }
    public SecretProvider secrets() { return secretProvider; }
    public boolean isLocalOnly() { return localOnly; }
    public int getMaxRepairAttempts() { return maxRepairAttempts; }
}
