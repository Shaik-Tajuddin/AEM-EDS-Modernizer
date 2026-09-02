package com.adobe.aem.modernizer.ai;

/**
 * Distinguishes local IDE handoff from cloud LLM API execution.
 */
public enum AiExecutionMode {
    LOCAL,
    CLOUD;

    public static AiExecutionMode fromProvider(String provider) {
        if (IdeAgentProviders.isIdeAgent(provider)) {
            return LOCAL;
        }
        return CLOUD;
    }
}
