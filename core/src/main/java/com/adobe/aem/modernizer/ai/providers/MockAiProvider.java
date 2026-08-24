package com.adobe.aem.modernizer.ai.providers;

import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.TokenUsage;

/**
 * Deterministic in-memory AI provider for offline testing, CI, and zero-cost verification.
 */
public class MockAiProvider implements AiProvider {

    private final String providerName;
    private final String defaultModel;

    public MockAiProvider() {
        this("mock", "mock-general-1");
    }

    public MockAiProvider(String providerName, String defaultModel) {
        this.providerName = providerName;
        this.defaultModel = defaultModel;
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ChatResponse chat(ChatRequest request, String model, String apiKey) {
        String agent = request.getAgentName() != null ? request.getAgentName() : "general";
        String chosenModel = model != null ? model : defaultModel;

        String content;

        if ("component-intelligence".equalsIgnoreCase(agent) || "component-mapping".equalsIgnoreCase(agent)) {
            content = "{\"proposedBlock\":\"hero\",\"variants\":[\"dark\",\"compact\"],\"confidence\":0.95,\"classification\":\"SUPPORTED\"}";
        } else if ("block-generation".equalsIgnoreCase(agent)) {
            content = "export default function decorate(block) {\n  const cols = [...block.firstElementChild.children];\n  block.classList.add(`hero-${cols.length}-cols`);\n}\n";
        } else if ("code-generation".equalsIgnoreCase(agent)) {
            content = ".hero {\n  display: flex;\n  padding: 40px 20px;\n  background: var(--hero-bg, #f4f4f4);\n}\n";
        } else if ("content-migration".equalsIgnoreCase(agent)) {
            content = "# Welcome to WKND Adventures\n\n## Experience the Unexplored\n\nExplore breathtaking expeditions across mountain ranges and pristine waters.\n\n### Hero\n| Image | Heading | Text |\n| --- | --- | --- |\n| /content/dam/wknd/hero.jpg | WKND 2026 | Discover more |\n";
        } else if ("visual-validation".equalsIgnoreCase(agent) || "advanced-visual-validation".equalsIgnoreCase(agent)) {
            content = "{\"visualScore\":0.96,\"a11yScore\":0.98,\"passed\":true,\"issues\":[]}";
        } else if ("self-repair".equalsIgnoreCase(agent) || "advanced-repair".equalsIgnoreCase(agent)) {
            content = "{\"successful\":true,\"patch\":\"/* repaired padding alignment */\\n.hero { margin: 0 auto; }\",\"explanation\":\"Adjusted hero margins to fix visual spacing differential.\"}";
        } else if ("figma-intelligence".equalsIgnoreCase(agent) || "figma-analysis".equalsIgnoreCase(agent)) {
            content = "{\"tokens\":{\"--color-primary\":\"#eb1000\",\"--font-heading\":\"'Source Sans Pro', sans-serif\"},\"componentPairs\":[{\"figma\":\"Card/Adventure\",\"edsBlock\":\"cards\"}]}";
        } else {
            content = "{\"status\":\"OK\",\"message\":\"Processed by Mock AI Provider\",\"confidence\":0.92}";
        }

        ChatResponse response = new ChatResponse(content, providerName, chosenModel);
        response.setTokenUsage(new TokenUsage(128, 64));
        response.setCostUsd(0.0);
        response.setFinishReason("stop");
        return response;
    }
}
