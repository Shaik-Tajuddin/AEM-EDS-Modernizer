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
 * Local Ollama Provider implementation (ADR 0003, Master §16).
 */
public class OllamaProvider implements AiProvider {

    private final HttpClient httpClient;
    private final String baseUrl;

    public OllamaProvider() {
        this("http://localhost:11434");
    }

    public OllamaProvider(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ChatResponse chat(ChatRequest request, String model, String apiKey) {
        String targetModel = (model != null && !model.isEmpty()) ? model : "qwen3:8b";

        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null) {
            Map<String, String> sys = new HashMap<>();
            sys.put("role", "system");
            sys.put("content", request.getSystemPrompt());
            messages.add(sys);
        }

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", request.getPrompt());
        messages.add(user);

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("model", targetModel);
        bodyMap.put("messages", messages);
        Map<String, Object> options = new HashMap<>();
        options.put("num_predict", 256);
        bodyMap.put("options", options);
        bodyMap.put("stream", false);

        String jsonBody = JsonUtil.toJson(bodyMap);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                throw new AiProviderException("Ollama HTTP " + httpResponse.statusCode());
            }
            String respBody = httpResponse.body() != null ? httpResponse.body() : "{}";
            Map<?, ?> parsed = JsonUtil.fromJson(respBody, Map.class);
            Map<?, ?> message = (parsed != null && parsed.get("message") instanceof Map)
                    ? (Map<?, ?>) parsed.get("message") : null;
            String content = message != null ? (String) message.get("content") : "";

            int promptTokens = (parsed != null && parsed.get("prompt_eval_count") instanceof Number)
                    ? ((Number) parsed.get("prompt_eval_count")).intValue() : 0;
            int completionTokens = (parsed != null && parsed.get("eval_count") instanceof Number)
                    ? ((Number) parsed.get("eval_count")).intValue() : 0;

            ChatResponse response = new ChatResponse(content, "ollama", targetModel);
            response.setTokenUsage(new TokenUsage(promptTokens, completionTokens));
            response.setCostUsd(0.0);
            return response;
        } catch (Exception e) {
            throw new AiProviderException("Ollama local connection failed: " + e.getMessage(), e);
        }
    }
}
