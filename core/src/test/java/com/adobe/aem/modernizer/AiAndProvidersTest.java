package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.ai.*;
import com.adobe.aem.modernizer.ai.providers.*;
import com.adobe.aem.modernizer.ai.routing.AiRoutingPolicy;
import com.adobe.aem.modernizer.ai.secret.EnvSecretProvider;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiAndProvidersTest {

    @Test
    void testModelCapabilityAndTokenUsageAndChatModels() {
        ModelCapability cap = new ModelCapability("anthropic", "claude-3-5-sonnet", 200000);
        cap.add(ModelCapability.CAP_CHAT)
           .add(ModelCapability.CAP_VISION)
           .add(ModelCapability.CAP_CODE)
           .add(ModelCapability.CAP_STRUCTURED);

        assertThat(cap.getModelName()).isEqualTo("claude-3-5-sonnet");
        assertThat(cap.getProvider()).isEqualTo("anthropic");
        assertThat(cap.getMaxContextTokens()).isEqualTo(200000);
        assertThat(cap.has(ModelCapability.CAP_CHAT)).isTrue();
        assertThat(cap.has(ModelCapability.CAP_VISION)).isTrue();
        assertThat(cap.getCapabilities()).isNotEmpty();

        cap.setCapabilities(new HashSet<>(Collections.singletonList("code")));
        assertThat(cap.getCapabilities()).contains("code");

        TokenUsage usage = new TokenUsage(100, 50);
        assertThat(usage.getPromptTokens()).isEqualTo(100);
        assertThat(usage.getCompletionTokens()).isEqualTo(50);
        assertThat(usage.getTotalTokens()).isEqualTo(150);

        ChatRequest req = new ChatRequest("code-agent", "Generate a teaser component block");
        req.setSystemPrompt("You are an expert EDS developer");
        req.setTargetCapability(ModelCapability.CAP_CODE);
        req.setResponseSchema("json");
        req.setTemperature(0.2);
        req.setMaxTokens(2048);
        req.setImageBase64List(Collections.singletonList("base64image"));

        assertThat(req.getAgentName()).isEqualTo("code-agent");
        assertThat(req.getPrompt()).isEqualTo("Generate a teaser component block");
        assertThat(req.getSystemPrompt()).isEqualTo("You are an expert EDS developer");
        assertThat(req.getTargetCapability()).isEqualTo(ModelCapability.CAP_CODE);
        assertThat(req.getResponseSchema()).isEqualTo("json");
        assertThat(req.getTemperature()).isEqualTo(0.2);
        assertThat(req.getMaxTokens()).isEqualTo(2048);
        assertThat(req.getImageBase64List()).hasSize(1);

        ChatResponse res = new ChatResponse("content-response", "anthropic", "claude-3-5-sonnet");
        res.setTokenUsage(usage);
        res.setCostUsd(0.005);
        res.setFinishReason("stop");
        assertThat(res.getContent()).isEqualTo("content-response");
        assertThat(res.getModelName()).isEqualTo("claude-3-5-sonnet");
        assertThat(res.getProvider()).isEqualTo("anthropic");
        assertThat(res.getCostUsd()).isEqualTo(0.005);
        assertThat(res.getFinishReason()).isEqualTo("stop");
        assertThat(res.getTokenUsage()).isNotNull();
    }

    @Test
    void testAiRoutingPolicy() {
        AiRoutingPolicy policy = new AiRoutingPolicy();
        policy.setStrategy("cost_optimized");
        assertThat(policy.getStrategy()).isEqualTo("cost_optimized");

        policy.setDefaultProvider("anthropic");
        assertThat(policy.getDefaultProvider()).isEqualTo("anthropic");

        policy.setDefaultModel("claude-3-5-sonnet");
        assertThat(policy.getDefaultModel()).isEqualTo("claude-3-5-sonnet");

        policy.setAgentRouting("connection", "mock", "mock-fast");
        assertThat(policy.resolveProvider("connection")).isEqualTo("mock");
        assertThat(policy.resolveProvider("unknown")).isEqualTo("anthropic");

        assertThat(policy.resolveModel("connection")).isEqualTo("mock-fast");
        assertThat(policy.resolveModel("unknown")).isEqualTo("claude-3-5-sonnet");

        policy.setStrategy("LOCAL_ONLY");
        assertThat(policy.resolveProvider("connection")).isEqualTo("ollama");
        assertThat(policy.resolveModel("connection")).isEqualTo("llama3");

        assertThat(policy.getAgentProviderMap()).isNotEmpty();
        assertThat(policy.getAgentModelMap()).isNotEmpty();
    }

    @Test
    void testCapabilityRegistryAndPricingService() {
        CapabilityRegistry reg = new CapabilityRegistry();
        ModelCapability cap = new ModelCapability("anthropic", "claude-3-5-sonnet", 200000).add(ModelCapability.CAP_CHAT);
        reg.add(cap);

        assertThat(reg.list()).isNotEmpty();
        assertThat(reg.find("anthropic", "claude-3-5-sonnet")).isPresent();
        assertThat(reg.find("nonexistent", "dummy")).isEmpty();
        assertThat(reg.supports("anthropic", "claude-3-5-sonnet", ModelCapability.CAP_CHAT)).isTrue();
        assertThat(reg.supports("anthropic", "claude-3-5-sonnet", "UNKNOWN_CAP")).isFalse();
        assertThat(reg.supports("anthropic", "claude-3-5-sonnet", null)).isTrue();

        PricingService pricing = new PricingService();
        double costAnthropic = pricing.calculateCostUsd("anthropic", "claude-3-5-sonnet", 1000, 1000);
        assertThat(costAnthropic).isGreaterThan(0.0);

        double costOpenAi = pricing.calculateCostUsd("openai", "gpt-4o", 1000, 1000);
        assertThat(costOpenAi).isGreaterThan(0.0);

        double costGemini = pricing.calculateCostUsd("gemini", "gemini-1.5-pro", 1000, 1000);
        assertThat(costGemini).isGreaterThan(0.0);

        double costOther = pricing.calculateCostUsd("custom", "custom-model", 1000, 1000);
        assertThat(costOther).isGreaterThan(0.0);

        double costMock = pricing.calculateCostUsd("mock", "mock-model", 1000, 1000);
        assertThat(costMock).isEqualTo(0.0);

        double costOllama = pricing.calculateCostUsd("ollama", "llama3", 1000, 1000);
        assertThat(costOllama).isEqualTo(0.0);
    }

    @Test
    void testEnvSecretProvider() {
        EnvSecretProvider secrets = new EnvSecretProvider();
        assertThat(secrets.resolve("env:NON_EXISTENT_VAR_12345")).isNull();
        assertThat(secrets.resolve("direct-key-value")).isEqualTo("direct-key-value");
        assertThat(secrets.resolve(null)).isNull();
        assertThat(secrets.resolve("")).isNull();
    }

    @Test
    void testAiProviderException() {
        AiProviderException ex1 = new AiProviderException("error message");
        assertThat(ex1.getMessage()).isEqualTo("error message");
        AiProviderException ex2 = new AiProviderException("error message", new RuntimeException("cause"));
        assertThat(ex2.getCause()).isNotNull();
    }

    @Test
    void testMockAiProvider() {
        MockAiProvider mock = new MockAiProvider("mock", "mock-model");
        assertThat(mock.getProviderName()).isEqualTo("mock");
        assertThat(mock.isAvailable()).isTrue();

        ChatRequest req = new ChatRequest("mapping", "map component to block");
        ChatResponse res = mock.chat(req, "mock-model", null);
        assertThat(res.getContent()).isNotNull();

        MockAiProvider defaultMock = new MockAiProvider();
        assertThat(defaultMock.getProviderName()).isEqualTo("mock");
    }

    @Test
    void testRealProvidersMissingApiKeyBehavior() {
        ChatRequest req = new ChatRequest("test", "test prompt");

        AnthropicProvider anthropic = new AnthropicProvider();
        assertThat(anthropic.getProviderName()).isEqualTo("anthropic");
        assertThat(anthropic.isAvailable()).isTrue();
        assertThatThrownBy(() -> anthropic.chat(req, "claude-3-5-sonnet", null))
                .isInstanceOf(RuntimeException.class);

        GeminiProvider gemini = new GeminiProvider();
        assertThat(gemini.getProviderName()).isEqualTo("gemini");
        assertThat(gemini.isAvailable()).isTrue();
        assertThatThrownBy(() -> gemini.chat(req, "gemini-1.5-pro", null))
                .isInstanceOf(RuntimeException.class);

        OpenAiProvider openai = new OpenAiProvider();
        assertThat(openai.getProviderName()).isEqualTo("openai");
        assertThat(openai.isAvailable()).isTrue();
        assertThatThrownBy(() -> openai.chat(req, "gpt-4o", null))
                .isInstanceOf(RuntimeException.class);

        OllamaProvider ollama = new OllamaProvider();
        assertThat(ollama.getProviderName()).isEqualTo("ollama");
        assertThat(ollama.isAvailable()).isTrue();
    }

    @Test
    void testAiGatewayDispatch() {
        AiGateway gateway = new AiGateway();
        gateway.activate();

        ChatRequest req = new ChatRequest("code-agent", "Generate a teaser component");
        ChatResponse res = gateway.dispatch(req);
        assertThat(res).isNotNull();
        assertThat(res.getContent()).isNotNull();

        assertThatThrownBy(() -> gateway.dispatch(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
