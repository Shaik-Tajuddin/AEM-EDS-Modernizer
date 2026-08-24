package com.adobe.aem.modernizer.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of available model capabilities (ADR 0007).
 */
public class CapabilityRegistry {

    private final List<ModelCapability> capabilities = new CopyOnWriteArrayList<>();

    public CapabilityRegistry() {}

    public void add(ModelCapability capability) {
        if (capability != null) {
            capabilities.add(capability);
        }
    }

    public List<ModelCapability> list() {
        return new ArrayList<>(capabilities);
    }

    public Optional<ModelCapability> find(String provider, String modelName) {
        return capabilities.stream()
                .filter(c -> c.getProvider().equalsIgnoreCase(provider) && c.getModelName().equalsIgnoreCase(modelName))
                .findFirst();
    }

    public boolean supports(String provider, String modelName, String requiredCapability) {
        if (requiredCapability == null || requiredCapability.isEmpty()) {
            return true;
        }
        return find(provider, modelName)
                .map(c -> c.has(requiredCapability))
                .orElse(false);
    }
}
