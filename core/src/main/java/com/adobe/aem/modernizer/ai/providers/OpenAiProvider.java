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
 * OpenAI Provider implementation (GPT-4o, GPT-4o-mini).
 */
public class OpenAiProvider implements AiProvider {

    private final OkHttpClient httpClient;
    private final String baseUrl;

    public OpenAiProvider() {
        this("https://api.openai.com/v1");
    }

    public OpenAiProvider(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
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

        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();

        try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
            if (!httpResponse.isSuccessful()) {
                String errBody = httpResponse.body() != null ? httpResponse.body().string() : "";
                throw new RuntimeException("OpenAI HTTP " + httpResponse.code() + ": " + errBody);
            }
            String respBody = httpResponse.body() != null ? httpResponse.body().string() : "{}";
            Map<?, ?> parsed = JsonUtil.fromJson(respBody, Map.class);
            List<?> choices = (List<?>) parsed.get("choices");
            String content = "";
            String finishReason = "stop";
            if (choices != null && !choices.isEmpty()) {
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                if (message != null) {
                    content = (String) message.get("content");
                }
                finishReason = (String) firstChoice.get("finish_reason");
            }

            Map<?, ?> usage = (Map<?, ?>) parsed.get("usage");
            int promptTokens = usage != null && usage.get("prompt_tokens") instanceof Number
                    ? ((Number) usage.get("prompt_tokens")).intValue() : 0;
            int completionTokens = usage != null && usage.get("completion_tokens") instanceof Number
                    ? ((Number) usage.get("completion_tokens")).intValue() : 0;

            ChatResponse response = new ChatResponse(content, "openai", targetModel);
            response.setTokenUsage(new TokenUsage(promptTokens, completionTokens));
            response.setFinishReason(finishReason);
            return response;
        } catch (IOException e) {
            throw new RuntimeException("OpenAI request failed: " + e.getMessage(), e);
        }
    }
}
