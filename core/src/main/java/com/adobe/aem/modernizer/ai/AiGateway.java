package com.adobe.aem.modernizer.ai;

import com.adobe.aem.modernizer.ai.providers.AiProvider;
import com.adobe.aem.modernizer.ai.providers.AiProviderException;
import com.adobe.aem.modernizer.ai.providers.AnthropicProvider;
import com.adobe.aem.modernizer.ai.providers.GeminiProvider;
import com.adobe.aem.modernizer.ai.providers.MockAiProvider;
import com.adobe.aem.modernizer.ai.providers.OllamaProvider;
import com.adobe.aem.modernizer.ai.providers.OpenAiProvider;
import com.adobe.aem.modernizer.ai.routing.AiRoutingPolicy;
import com.adobe.aem.modernizer.ai.secret.EnvSecretProvider;
import com.adobe.aem.modernizer.ai.secret.SecretProvider;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.security.Redactor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
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
    private final AiEndpointSettings endpointSettings = new AiEndpointSettings();
    private AiRoutingPolicy routingPolicy = new AiRoutingPolicy();
    private SecretProvider secretProvider = new EnvSecretProvider();
    private Store store;
    private boolean localOnly = false;
    private int maxRepairAttempts = 5;

    @Reference
    private transient Store storeRef;

    public AiGateway() {
        initBuiltinCapabilities();
        register(new MockAiProvider("mock", "mock-general-1"));
        // Defaults until OSGi factories bind (standalone / tests)
        registerCloudProvider(new AnthropicProvider(), "anthropic", "https://api.anthropic.com/v1",
                "env:ANTHROPIC_API_KEY", "", true);
        registerCloudProvider(new OpenAiProvider(), "openai", "https://api.openai.com/v1",
                "env:OPENAI_API_KEY", "", true);
        registerCloudProvider(new GeminiProvider(), "gemini", "https://generativelanguage.googleapis.com/v1beta",
                "env:GEMINI_API_KEY", "", true);
        registerCloudProvider(new OllamaProvider("http://localhost:11434"), "ollama", "http://localhost:11434",
                "", "qwen3:8b", true);
    }

    private void initBuiltinCapabilities() {
        capabilityRegistry.add(new ModelCapability("mock", "mock-general-1", 8192)
                .add(ModelCapability.CAP_CHAT)
                .add(ModelCapability.CAP_STRUCTURED)
                .add(ModelCapability.CAP_CODE)
                .add(ModelCapability.CAP_VISION)
                .add(ModelCapability.CAP_LOCAL));
        capabilityRegistry.add(new ModelCapability("ollama", "qwen3:8b", 8192)
                .add(ModelCapability.CAP_CHAT).add(ModelCapability.CAP_STRUCTURED)
                .add(ModelCapability.CAP_CODE).add(ModelCapability.CAP_LOCAL));
        capabilityRegistry.add(new ModelCapability("anthropic", "claude-3-5-sonnet-20241022", 200000)
                .add(ModelCapability.CAP_CHAT).add(ModelCapability.CAP_STRUCTURED)
                .add(ModelCapability.CAP_CODE).add(ModelCapability.CAP_VISION));
        capabilityRegistry.add(new ModelCapability("openai", "gpt-4o", 128000)
                .add(ModelCapability.CAP_CHAT).add(ModelCapability.CAP_STRUCTURED)
                .add(ModelCapability.CAP_CODE).add(ModelCapability.CAP_VISION));
        capabilityRegistry.add(new ModelCapability("gemini", "gemini-1.5-pro", 1000000)
                .add(ModelCapability.CAP_CHAT).add(ModelCapability.CAP_STRUCTURED)
                .add(ModelCapability.CAP_CODE).add(ModelCapability.CAP_VISION));
    }

    private void registerCloudProvider(AiProvider provider, String name, String baseUrl,
                                       String apiKeyRef, String defaultModel, boolean enabled) {
        register(provider);
        endpointSettings.put(new AiProviderEndpoint(name, baseUrl, apiKeyRef, defaultModel, enabled));
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

    @Reference(
            service = AiProviderEndpointConfig.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            bind = "bindEndpoint",
            unbind = "unbindEndpoint"
    )
    protected void bindEndpoint(AiProviderEndpointConfig config) {
        if (config == null || config.getEndpoint() == null) {
            return;
        }
        AiProviderEndpoint ep = config.getEndpoint();
        endpointSettings.put(ep);
        String name = ep.getProviderName() != null ? ep.getProviderName().toLowerCase(Locale.ROOT) : "";
        String base = ep.getBaseUrl();
        if (base == null || base.isBlank()) {
            return;
        }
        switch (name) {
            case "anthropic":
                register(new AnthropicProvider(base));
                break;
            case "openai":
                register(new OpenAiProvider(base));
                break;
            case "gemini":
                register(new GeminiProvider(base));
                break;
            case "ollama":
                register(new OllamaProvider(base));
                break;
            default:
                LOG.warn("Unknown AI endpoint provider '{}'", name);
        }
        LOG.info("Bound OSGi AI endpoint for {}", name);
    }

    protected void unbindEndpoint(AiProviderEndpointConfig config) {
        if (config != null && config.getEndpoint() != null) {
            endpointSettings.remove(config.getEndpoint().getProviderName());
        }
    }

    public void register(AiProvider provider) {
        if (provider != null) {
            providers.put(provider.getProviderName().toLowerCase(Locale.ROOT), provider);
        }
    }

    public AiEndpointSettings endpoints() {
        return endpointSettings;
    }

    public ChatResponse dispatch(ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ChatRequest cannot be null");
        }

        String agentName = request.getAgentName() != null ? request.getAgentName() : "general";
        String providerName = (request.getPreferredProvider() != null && !request.getPreferredProvider().trim().isEmpty())
                ? request.getPreferredProvider().trim().toLowerCase(Locale.ROOT)
                : routingPolicy.resolveProvider(agentName);
        String modelName = (request.getPreferredModel() != null && !request.getPreferredModel().trim().isEmpty())
                ? request.getPreferredModel().trim()
                : routingPolicy.resolveModel(agentName);

        if (IdeAgentProviders.isLocalOnlyProvider(providerName)) {
            // IDE agents are handoff-only (not LLM transports). Pipeline agents that still
            // call dispatch get mock scaffold enrichment; Agent Chat never reaches here for IDE.
            LOG.info("IDE provider '{}' requested for agent '{}'; using mock for scaffold generation",
                    providerName, agentName);
            providerName = "mock";
            modelName = "mock-general-1";
        }

        if (localOnly && !"ollama".equalsIgnoreCase(providerName) && !"mock".equalsIgnoreCase(providerName)) {
            LOG.warn("Local-only mode active: overriding provider {} to ollama", providerName);
            providerName = "ollama";
            modelName = modelName != null && !modelName.isBlank() ? modelName : "qwen3:8b";
        }

        boolean explicitMock = "mock".equalsIgnoreCase(providerName);

        // Prefer form model via project when available
        if (store != null && request.getProjectId() != null) {
            ProjectRecord pr = store.getProject(request.getProjectId()).orElse(null);
            String formModel = endpointSettings.resolveModel(pr, providerName);
            if (formModel != null && !formModel.isBlank()
                    && (request.getPreferredModel() == null || request.getPreferredModel().isBlank())) {
                modelName = formModel;
            }
        }

        AiProvider provider = providers.get(providerName.toLowerCase(Locale.ROOT));
        if (provider == null) {
            if (explicitMock) {
                provider = new MockAiProvider();
                register(provider);
            } else {
                throw new AiProviderException("AI provider '" + providerName + "' is not registered. "
                        + "Configure an OSGi AiProviderEndpointConfig or select a known provider.");
            }
        }

        String apiKey = null;
        if (!explicitMock && !"ollama".equalsIgnoreCase(providerName)) {
            String secretRef = endpointSettings.resolveApiKeyRef(providerName);
            apiKey = secretProvider.resolve(secretRef);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new AiProviderException("No API key resolved for provider '" + providerName
                        + "' (ref=" + secretRef + "). Set the environment secret or choose mock for tests.");
            }
        }

        long start = System.currentTimeMillis();
        ChatResponse response;
        try {
            response = provider.chat(request, modelName, apiKey);
        } catch (AiProviderException e) {
            if (localOnly || explicitMock) {
                LOG.warn("AI dispatch failed in local/mock mode ({}): {}. Using mock.", providerName, e.getMessage());
                AiProvider mockProv = providers.get("mock");
                if (mockProv == null) {
                    mockProv = new MockAiProvider();
                    register(mockProv);
                }
                response = mockProv.chat(request, "mock-general-1", null);
                providerName = "mock";
                modelName = "mock-general-1";
            } else {
                throw e;
            }
        } catch (Exception e) {
            if (localOnly || explicitMock) {
                LOG.warn("AI dispatch failed in local/mock mode ({}): {}. Using mock.", providerName, e.getMessage());
                AiProvider mockProv = providers.get("mock");
                if (mockProv == null) {
                    mockProv = new MockAiProvider();
                    register(mockProv);
                }
                response = mockProv.chat(request, "mock-general-1", null);
                providerName = "mock";
                modelName = "mock-general-1";
            } else {
                throw new AiProviderException("AI dispatch to " + providerName + " (" + modelName + ") failed: "
                        + e.getMessage(), e);
            }
        }
        if (response.getProvider() == null || response.getProvider().isBlank()) {
            response.setProvider(providerName);
        }
        if (response.getModelName() == null || response.getModelName().isBlank()) {
            response.setModelName(modelName);
        }
        long duration = System.currentTimeMillis() - start;

        double cost = pricingService.calculateCostUsd(
                providerName,
                modelName,
                response.getTokenUsage().getPromptTokens(),
                response.getTokenUsage().getCompletionTokens()
        );
        response.setCostUsd(cost);

        if (response.getContent() != null) {
            response.setContent(Redactor.redact(response.getContent()));
        }

        LOG.info("[AI:{}] Dispatched to {} model '{}': promptTokens={} compTokens={} dur={}ms",
                providerName, providerName, modelName,
                response.getTokenUsage().getPromptTokens(),
                response.getTokenUsage().getCompletionTokens(),
                duration);

        if (store != null && request.getProjectId() != null && request.getJobId() != null) {
            String promptText = request.getPrompt() != null ? request.getPrompt() : "";
            String responseText = response.getContent() != null ? response.getContent() : "";

            store.recordEvent(new com.adobe.aem.modernizer.persistence.model.JobEventRecord(
                    java.util.UUID.randomUUID().toString(),
                    request.getProjectId(),
                    request.getJobId(),
                    "ai-request",
                    "🤖 [AI:" + providerName + ":" + modelName + "] 📤 REQUEST:\n" + promptText
            ));

            store.recordEvent(new com.adobe.aem.modernizer.persistence.model.JobEventRecord(
                    java.util.UUID.randomUUID().toString(),
                    request.getProjectId(),
                    request.getJobId(),
                    "ai-response",
                    "🤖 [AI:" + providerName + ":" + modelName + "] 📥 RESPONSE (" + duration + "ms | "
                            + response.getTokenUsage().getPromptTokens() + " in / "
                            + response.getTokenUsage().getCompletionTokens() + " out tokens):\n" + responseText
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
