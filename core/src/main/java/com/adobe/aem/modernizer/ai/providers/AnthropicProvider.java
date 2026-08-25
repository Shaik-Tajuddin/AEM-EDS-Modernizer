package com.adobe.aem.modernizer.ai.providers;

import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.TokenUsage;
import com.adobe.aem.modernizer.util.JsonUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Anthropic Provider implementation (Claude 3.5 Sonnet, Claude 3 Haiku).
 */
public class AnthropicProvider implements AiProvider {

    private final HttpClient httpClient;
    private final String baseUrl;

    public AnthropicProvider() {
        this("https://api.anthropic.com/v1");
    }

    public AnthropicProvider(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String getProviderName() {
        return "anthropic";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ChatResponse chat(ChatRequest request, String model, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Anthropic API key is missing");
        }

        String targetModel = (model != null && !model.isEmpty()) ? model : "claude-3-5-sonnet-20241022";

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", request.getPrompt());
        messages.add(userMsg);

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("model", targetModel);
        bodyMap.put("messages", messages);
        if (request.getSystemPrompt() != null) {
            bodyMap.put("system", request.getSystemPrompt());
        }
        bodyMap.put("max_tokens", request.getMaxTokens());
        bodyMap.put("temperature", request.getTemperature());

        String jsonBody = JsonUtil.toJson(bodyMap);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                String errBody = httpResponse.body() != null ? httpResponse.body() : "";
                throw new AiProviderException("Anthropic HTTP " + httpResponse.statusCode() + ": " + errBody);
            }
            String respBody = httpResponse.body() != null ? httpResponse.body() : "{}";
            Map<?, ?> parsed = JsonUtil.fromJson(respBody, Map.class);
            List<?> contentList = (parsed != null && parsed.get("content") instanceof List)
                    ? (List<?>) parsed.get("content") : null;
            StringBuilder sb = new StringBuilder();
            if (contentList != null) {
                for (Object item : contentList) {
                    if (item instanceof Map) {
                        Object text = ((Map<?, ?>) item).get("text");
                        if (text != null) sb.append(text);
                    }
                }
            }

            Map<?, ?> usage = (parsed != null && parsed.get("usage") instanceof Map)
                    ? (Map<?, ?>) parsed.get("usage") : null;
            int promptTokens = (usage != null && usage.get("input_tokens") instanceof Number)
                    ? ((Number) usage.get("input_tokens")).intValue() : 0;
            int completionTokens = (usage != null && usage.get("output_tokens") instanceof Number)
                    ? ((Number) usage.get("output_tokens")).intValue() : 0;

            ChatResponse response = new ChatResponse(sb.toString(), "anthropic", targetModel);
            response.setTokenUsage(new TokenUsage(promptTokens, completionTokens));
            return response;
        } catch (Exception e) {
            throw new AiProviderException("Anthropic request failed: " + e.getMessage(), e);
        }
    }
}
