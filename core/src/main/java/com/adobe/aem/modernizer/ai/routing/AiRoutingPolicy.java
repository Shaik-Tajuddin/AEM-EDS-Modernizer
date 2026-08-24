package com.adobe.aem.modernizer.ai.routing;

import java.util.HashMap;
import java.util.Map;

/**
 * Routing policy directing agent requests to providers and models (ADR 0003).
 */
public class AiRoutingPolicy {

    private String strategy = "MULTI_PROVIDER";
    private String defaultProvider = "mock";
    private String defaultModel = "mock-general-1";
    private final Map<String, String> agentProviderMap = new HashMap<>();
    private final Map<String, String> agentModelMap = new HashMap<>();

    public AiRoutingPolicy() {}

    public String resolveProvider(String agentName) {
        if ("LOCAL_ONLY".equalsIgnoreCase(strategy)) {
            return "ollama";
        }
        if (agentName != null && agentProviderMap.containsKey(agentName)) {
            return agentProviderMap.get(agentName);
        }
        return defaultProvider;
    }

    public String resolveModel(String agentName) {
        if ("LOCAL_ONLY".equalsIgnoreCase(strategy)) {
            return "llama3";
        }
        if (agentName != null && agentModelMap.containsKey(agentName)) {
            return agentModelMap.get(agentName);
        }
        return defaultModel;
    }

    public void setAgentRouting(String agentName, String provider, String model) {
        if (agentName != null) {
            if (provider != null) agentProviderMap.put(agentName, provider);
            if (model != null) agentModelMap.put(agentName, model);
        }
    }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getDefaultProvider() { return defaultProvider; }
    public void setDefaultProvider(String defaultProvider) { this.defaultProvider = defaultProvider; }

    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }

    public Map<String, String> getAgentProviderMap() { return new HashMap<>(agentProviderMap); }
    public Map<String, String> getAgentModelMap() { return new HashMap<>(agentModelMap); }
}
