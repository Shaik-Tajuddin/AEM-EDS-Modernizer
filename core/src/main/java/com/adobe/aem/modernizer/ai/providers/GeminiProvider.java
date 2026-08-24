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
 * Google Gemini Provider implementation.
 */
public class GeminiProvider implements AiProvider {

    private final OkHttpClient httpClient;
    private final String baseUrl;

    public GeminiProvider() {
        this("https://generativelanguage.googleapis.com/v1beta");
    }

    public GeminiProvider(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ChatResponse chat(ChatRequest request, String model, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini API key is missing");
        }

        String targetModel = (model != null && !model.isEmpty()) ? model : "gemini-1.5-pro";

        Map<String, Object> part = new HashMap<>();
        part.put("text", request.getPrompt());

        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", Collections.singletonList(part));

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("contents", Collections.singletonList(content));

        String jsonBody = JsonUtil.toJson(bodyMap);
        String url = baseUrl + "/models/" + targetModel + ":generateContent?key=" + apiKey;

        Request httpRequest = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();

        try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
            if (!httpResponse.isSuccessful()) {
                String errBody = httpResponse.body() != null ? httpResponse.body().string() : "";
                throw new AiProviderException("Gemini HTTP " + httpResponse.code() + ": " + errBody);
            }
            String respBody = httpResponse.body() != null ? httpResponse.body().string() : "{}";
            Map<?, ?> parsed = JsonUtil.fromJson(respBody, Map.class);
            List<?> candidates = (List<?>) parsed.get("candidates");
            String resultText = "";
            if (candidates != null && !candidates.isEmpty()) {
                Map<?, ?> firstCand = (Map<?, ?>) candidates.get(0);
                Map<?, ?> c = (Map<?, ?>) firstCand.get("content");
                if (c != null) {
                    List<?> parts = (List<?>) c.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map<?, ?> p = (Map<?, ?>) parts.get(0);
                        resultText = (String) p.get("text");
                    }
                }
            }

            ChatResponse response = new ChatResponse(resultText, "gemini", targetModel);
            response.setTokenUsage(new TokenUsage(64, 64));
            return response;
        } catch (IOException e) {
            throw new AiProviderException("Gemini request failed: " + e.getMessage(), e);
        }
    }
}
