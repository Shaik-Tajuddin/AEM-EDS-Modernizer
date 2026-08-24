package com.adobe.aem.modernizer.ai.providers;

import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.TokenUsage;
import com.adobe.aem.modernizer.util.JsonUtil;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Anthropic Provider implementation (Claude 3.5 Sonnet, Claude 3 Haiku).
 */
public class AnthropicProvider implements AiProvider {

    private final OkHttpClient httpClient;
    private final String baseUrl;

    public AnthropicProvider() {
        this("https://api.anthropic.com/v1");
    }

    public AnthropicProvider(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
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

        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();

        try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
            if (!httpResponse.isSuccessful()) {
                String errBody = httpResponse.body() != null ? httpResponse.body().string() : "";
                throw new RuntimeException("Anthropic HTTP " + httpResponse.code() + ": " + errBody);
            }
            String respBody = httpResponse.body() != null ? httpResponse.body().string() : "{}";
            Map<?, ?> parsed = JsonUtil.fromJson(respBody, Map.class);
            List<?> contentList = (List<?>) parsed.get("content");
            StringBuilder sb = new StringBuilder();
            if (contentList != null) {
                for (Object item : contentList) {
                    if (item instanceof Map) {
                        Object text = ((Map<?, ?>) item).get("text");
                        if (text != null) sb.append(text);
                    }
                }
            }

            Map<?, ?> usage = (Map<?, ?>) parsed.get("usage");
            int promptTokens = usage != null && usage.get("input_tokens") instanceof Number
                    ? ((Number) usage.get("input_tokens")).intValue() : 0;
            int completionTokens = usage != null && usage.get("output_tokens") instanceof Number
                    ? ((Number) usage.get("output_tokens")).intValue() : 0;

            ChatResponse response = new ChatResponse(sb.toString(), "anthropic", targetModel);
            response.setTokenUsage(new TokenUsage(promptTokens, completionTokens));
            return response;
        } catch (IOException e) {
            throw new RuntimeException("Anthropic request failed: " + e.getMessage(), e);
        }
    }
}
