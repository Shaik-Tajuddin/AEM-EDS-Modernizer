package com.adobe.aem.modernizer.ai;

import org.osgi.service.component.annotations.Component;

/**
 * Calculates estimated and actual AI cost in USD (Master §18).
 */
@Component(service = PricingService.class, immediate = true)
public class PricingService {

    public double calculateCostUsd(String provider, String modelName, int promptTokens, int completionTokens) {
        if (provider == null || "mock".equalsIgnoreCase(provider) || "ollama".equalsIgnoreCase(provider)) {
            return 0.0;
        }

        double promptRatePerMillion;
        double completionRatePerMillion;

        if ("anthropic".equalsIgnoreCase(provider)) {
            promptRatePerMillion = 3.0;
            completionRatePerMillion = 15.0;
        } else if ("openai".equalsIgnoreCase(provider)) {
            promptRatePerMillion = 2.5;
            completionRatePerMillion = 10.0;
        } else if ("gemini".equalsIgnoreCase(provider)) {
            promptRatePerMillion = 1.25;
            completionRatePerMillion = 5.0;
        } else {
            promptRatePerMillion = 2.0;
            completionRatePerMillion = 8.0;
        }

        double promptCost = (promptTokens / 1_000_000.0) * promptRatePerMillion;
        double completionCost = (completionTokens / 1_000_000.0) * completionRatePerMillion;
        return promptCost + completionCost;
    }

    public double calculateEmbeddingCostUsd(String provider, int embeddingTokens) {
        if (provider == null || "mock".equalsIgnoreCase(provider) || "ollama".equalsIgnoreCase(provider)) {
            return 0.0;
        }
        double ratePerMillion = 0.02; // OpenAI text-embedding-3-small default
        return (embeddingTokens / 1_000_000.0) * ratePerMillion;
    }
}
