package com.adobe.aem.modernizer.ai;

import com.adobe.aem.modernizer.persistence.model.ProjectRecord;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of OSGi AI endpoints + helpers to resolve effective provider/model/key ref.
 */
public class AiEndpointSettings {

    private final Map<String, AiProviderEndpoint> byProvider = new ConcurrentHashMap<>();

    public void put(AiProviderEndpoint endpoint) {
        if (endpoint == null || endpoint.getProviderName() == null || endpoint.getProviderName().isBlank()) {
            return;
        }
        byProvider.put(endpoint.getProviderName().toLowerCase(Locale.ROOT), endpoint);
    }

    public void remove(String providerName) {
        if (providerName != null) {
            byProvider.remove(providerName.toLowerCase(Locale.ROOT));
        }
    }

    public AiProviderEndpoint get(String providerName) {
        if (providerName == null) {
            return null;
        }
        return byProvider.get(providerName.toLowerCase(Locale.ROOT));
    }

    public Map<String, AiProviderEndpoint> all() {
        return Map.copyOf(byProvider);
    }

    /**
     * Effective cloud model: ProjectRecord.aiModel first; ollama may fall back to OSGi defaultModel.
     */
    public String resolveModel(ProjectRecord project, String providerName) {
        String formModel = project != null ? project.getAiModel() : null;
        if (formModel != null && !formModel.isBlank()) {
            return formModel.trim();
        }
        AiProviderEndpoint ep = get(providerName);
        if (ep != null && ep.getDefaultModel() != null && !ep.getDefaultModel().isBlank()) {
            return ep.getDefaultModel();
        }
        return null;
    }

    public String resolveApiKeyRef(String providerName) {
        AiProviderEndpoint ep = get(providerName);
        if (ep != null && ep.getApiKeyRef() != null && !ep.getApiKeyRef().isBlank()) {
            return ep.getApiKeyRef();
        }
        if (providerName == null || providerName.isBlank()) {
            return null;
        }
        return "env:" + providerName.trim().toUpperCase(Locale.ROOT) + "_API_KEY";
    }

    public String resolveBaseUrl(String providerName) {
        AiProviderEndpoint ep = get(providerName);
        return ep != null ? ep.getBaseUrl() : null;
    }

    /**
     * When an IDE provider is saved but operator switches to cloud, pick first enabled online endpoint.
     */
    public String firstEnabledCloudProvider() {
        for (String name : new String[]{"anthropic", "openai", "gemini", "ollama"}) {
            AiProviderEndpoint ep = byProvider.get(name);
            if (ep != null && ep.isEnabled()) {
                return name;
            }
        }
        return byProvider.keySet().stream().findFirst().orElse(null);
    }
}
