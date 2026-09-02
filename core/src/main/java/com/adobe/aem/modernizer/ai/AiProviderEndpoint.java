package com.adobe.aem.modernizer.ai;

/**
 * Resolved cloud AI endpoint (OSGi factory + optional overrides).
 */
public final class AiProviderEndpoint {

    private final String providerName;
    private final String baseUrl;
    private final String apiKeyRef;
    private final String defaultModel;
    private final boolean enabled;

    public AiProviderEndpoint(String providerName, String baseUrl, String apiKeyRef,
                              String defaultModel, boolean enabled) {
        this.providerName = providerName;
        this.baseUrl = baseUrl;
        this.apiKeyRef = apiKeyRef;
        this.defaultModel = defaultModel;
        this.enabled = enabled;
    }

    public String getProviderName() { return providerName; }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKeyRef() { return apiKeyRef; }
    public String getDefaultModel() { return defaultModel; }
    public boolean isEnabled() { return enabled; }
}
