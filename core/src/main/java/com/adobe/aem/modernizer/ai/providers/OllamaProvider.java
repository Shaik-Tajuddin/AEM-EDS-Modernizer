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
 * Local Ollama Provider implementation (ADR 0003, Master §16).
 */
public class OllamaProvider implements AiProvider {

    private final OkHttpClient httpClient;
    private final String baseUrl;

    public OllamaProvider() {
        this("http://localhost:11434");
    }

    public OllamaProvider(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
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
        String targetModel = (model != null && !model.isEmpty()) ? model : "llama3";

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
        bodyMap.put("stream", false);

        String jsonBody = JsonUtil.toJson(bodyMap);

        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();

        try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
            if (!httpResponse.isSuccessful()) {
                throw new RuntimeException("Ollama HTTP " + httpResponse.code());
            }
            String respBody = httpResponse.body() != null ? httpResponse.body().string() : "{}";
            Map<?, ?> parsed = JsonUtil.fromJson(respBody, Map.class);
            Map<?, ?> message = (Map<?, ?>) parsed.get("message");
            String content = message != null ? (String) message.get("content") : "";

            int promptTokens = parsed.get("prompt_eval_count") instanceof Number
                    ? ((Number) parsed.get("prompt_eval_count")).intValue() : 0;
            int completionTokens = parsed.get("eval_count") instanceof Number
                    ? ((Number) parsed.get("eval_count")).intValue() : 0;

            ChatResponse response = new ChatResponse(content, "ollama", targetModel);
            response.setTokenUsage(new TokenUsage(promptTokens, completionTokens));
            response.setCostUsd(0.0);
            return response;
        } catch (IOException e) {
            throw new RuntimeException("Ollama local connection failed: " + e.getMessage(), e);
        }
    }
}
