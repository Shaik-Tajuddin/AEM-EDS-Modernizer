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
 * Google Gemini Provider implementation.
 */
public class GeminiProvider implements AiProvider {

    private final HttpClient httpClient;
    private final String baseUrl;

    public GeminiProvider() {
        this("https://generativelanguage.googleapis.com/v1beta");
    }

    public GeminiProvider(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
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

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                String errBody = httpResponse.body() != null ? httpResponse.body() : "";
                throw new AiProviderException("Gemini HTTP " + httpResponse.statusCode() + ": " + errBody);
            }
            String respBody = httpResponse.body() != null ? httpResponse.body() : "{}";
            Map<?, ?> parsed = JsonUtil.fromJson(respBody, Map.class);
            List<?> candidates = (parsed != null && parsed.get("candidates") instanceof List)
                    ? (List<?>) parsed.get("candidates") : null;
            String resultText = "";
            if (candidates != null && !candidates.isEmpty()) {
                Map<?, ?> firstCand = (Map<?, ?>) candidates.get(0);
                Map<?, ?> c = (firstCand != null && firstCand.get("content") instanceof Map)
                        ? (Map<?, ?>) firstCand.get("content") : null;
                if (c != null) {
                    List<?> parts = (c.get("parts") instanceof List) ? (List<?>) c.get("parts") : null;
                    if (parts != null && !parts.isEmpty()) {
                        Map<?, ?> p = (parts.get(0) instanceof Map) ? (Map<?, ?>) parts.get(0) : null;
                        if (p != null && p.get("text") instanceof String) {
                            resultText = (String) p.get("text");
                        }
                    }
                }
            }

            ChatResponse response = new ChatResponse(resultText, "gemini", targetModel);
            response.setTokenUsage(new TokenUsage(64, 64));
            return response;
        } catch (Exception e) {
            throw new AiProviderException("Gemini request failed: " + e.getMessage(), e);
        }
    }
}
