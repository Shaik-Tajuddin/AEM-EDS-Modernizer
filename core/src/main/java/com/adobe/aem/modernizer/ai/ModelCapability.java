package com.adobe.aem.modernizer.ai;

import java.util.HashSet;
import java.util.Set;

/**
 * Model capability descriptor (ADR 0007).
 */
public class ModelCapability {

    public static final String CAP_CHAT = "chat";
    public static final String CAP_STRUCTURED = "structured";
    public static final String CAP_CODE = "code";
    public static final String CAP_VISION = "vision";
    public static final String CAP_LOCAL = "local";

    private String provider;
    private String modelName;
    private int maxContextTokens;
    private Set<String> capabilities = new HashSet<>();

    public ModelCapability() {}

    public ModelCapability(String provider, String modelName, int maxContextTokens) {
        this.provider = provider;
        this.modelName = modelName;
        this.maxContextTokens = maxContextTokens;
    }

    public ModelCapability add(String capability) {
        this.capabilities.add(capability.toLowerCase());
        return this;
    }

    public boolean has(String capability) {
        return capabilities.contains(capability.toLowerCase());
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public int getMaxContextTokens() { return maxContextTokens; }
    public void setMaxContextTokens(int maxContextTokens) { this.maxContextTokens = maxContextTokens; }

    public Set<String> getCapabilities() { return capabilities != null ? new HashSet<>(capabilities) : new HashSet<>(); }
    public void setCapabilities(Set<String> capabilities) { this.capabilities = capabilities != null ? new HashSet<>(capabilities) : new HashSet<>(); }
}
