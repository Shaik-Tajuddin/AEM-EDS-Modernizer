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
 * OpenAI Provider implementation (GPT-4o, GPT-4o-mini).
 */
public class OpenAiProvider implements AiProvider {

    private final HttpClient httpClient;
    private final String baseUrl;

    public OpenAiProvider() {
        this("https://api.openai.com/v1");
    }

    public OpenAiProvider(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ChatResponse chat(ChatRequest request, String model, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key is missing");
        }

        String targetModel = (model != null && !model.isEmpty()) ? model : "gpt-4o-mini";

        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", request.getSystemPrompt());
            messages.add(sysMsg);
        }

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", request.getPrompt());
        messages.add(userMsg);

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("model", targetModel);
        bodyMap.put("messages", messages);
        bodyMap.put("temperature", request.getTemperature());
        bodyMap.put("max_tokens", request.getMaxTokens());

        String jsonBody = JsonUtil.toJson(bodyMap);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                String errBody = httpResponse.body() != null ? httpResponse.body() : "";
                throw new AiProviderException("OpenAI HTTP " + httpResponse.statusCode() + ": " + errBody);
            }
            String respBody = httpResponse.body() != null ? httpResponse.body() : "{}";
            Map<?, ?> parsed = JsonUtil.fromJson(respBody, Map.class);
            List<?> choices = (parsed != null && parsed.get("choices") instanceof List)
                    ? (List<?>) parsed.get("choices") : null;
            String content = "";
            String finishReason = "stop";
            if (choices != null && !choices.isEmpty()) {
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (firstChoice != null && firstChoice.get("message") instanceof Map)
                        ? (Map<?, ?>) firstChoice.get("message") : null;
                if (message != null) {
                    content = (String) message.get("content");
                }
                if (firstChoice != null && firstChoice.get("finish_reason") instanceof String) {
                    finishReason = (String) firstChoice.get("finish_reason");
                }
            }

            Map<?, ?> usage = (parsed != null && parsed.get("usage") instanceof Map)
                    ? (Map<?, ?>) parsed.get("usage") : null;
            int promptTokens = (usage != null && usage.get("prompt_tokens") instanceof Number)
                    ? ((Number) usage.get("prompt_tokens")).intValue() : 0;
            int completionTokens = (usage != null && usage.get("completion_tokens") instanceof Number)
                    ? ((Number) usage.get("completion_tokens")).intValue() : 0;

            ChatResponse response = new ChatResponse(content, "openai", targetModel);
            response.setTokenUsage(new TokenUsage(promptTokens, completionTokens));
            response.setFinishReason(finishReason);
            return response;
        } catch (Exception e) {
            throw new AiProviderException("OpenAI request failed: " + e.getMessage(), e);
        }
    }
}
