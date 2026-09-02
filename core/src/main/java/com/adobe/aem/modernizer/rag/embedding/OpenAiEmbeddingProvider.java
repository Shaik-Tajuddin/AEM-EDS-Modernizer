package com.adobe.aem.modernizer.rag.embedding;

import com.adobe.aem.modernizer.ai.secret.EnvSecretProvider;
import com.adobe.aem.modernizer.ai.secret.SecretProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Production OpenAI Embedding Provider (text-embedding-3-small, 1536 dimension).
 */
@Component(service = {EmbeddingProvider.class, OpenAiEmbeddingProvider.class}, immediate = true)
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiEmbeddingProvider.class);
    private static final String DEFAULT_MODEL = "text-embedding-3-small";
    private static final int DIMENSION = 1536;

    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient SecretProvider secretProvider = new EnvSecretProvider();

    private String apiKey;
    private String model = DEFAULT_MODEL;

    public OpenAiEmbeddingProvider() {
    }

    public OpenAiEmbeddingProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Activate
    public void activate() {
        LOG.info("OpenAiEmbeddingProvider activated (model={})", model);
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        String key = resolveApiKey();
        if (key == null || key.isBlank()) {
            LOG.warn("No OpenAI API key found for embeddings. Falling back to deterministic pseudo-embeddings.");
            return generateFallbackEmbeddings(texts);
        }

        try {
            List<float[]> allVectors = new ArrayList<>();
            // Batch by 50 to respect OpenAI token limits
            int batchSize = 50;
            for (int i = 0; i < texts.size(); i += batchSize) {
                List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
                String requestJson = mapper.writeValueAsString(mapper.createObjectNode()
                        .put("model", model)
                        .set("input", mapper.valueToTree(batch)));

                Request request = new Request.Builder()
                        .url("https://api.openai.com/v1/embeddings")
                        .addHeader("Authorization", "Bearer " + key)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        LOG.warn("OpenAI embedding API call failed with code: {}", response.code());
                        return generateFallbackEmbeddings(texts);
                    }
                    JsonNode root = mapper.readTree(response.body().string());
                    JsonNode data = root.get("data");
                    if (data != null && data.isArray()) {
                        for (JsonNode item : data) {
                            JsonNode emb = item.get("embedding");
                            float[] vec = new float[emb.size()];
                            for (int j = 0; j < emb.size(); j++) {
                                vec[j] = (float) emb.get(j).asDouble();
                            }
                            allVectors.add(vec);
                        }
                    }
                }
            }
            return allVectors;
        } catch (Exception e) {
            LOG.error("Exception invoking OpenAI embedding API: {}", e.getMessage(), e);
            return generateFallbackEmbeddings(texts);
        }
    }

    private String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) return apiKey;
        if (secretProvider != null) {
            String s = secretProvider.resolve("env:OPENAI_API_KEY");
            if (s != null && !s.isBlank()) return s;
        }
        return System.getenv("OPENAI_API_KEY");
    }

    private List<float[]> generateFallbackEmbeddings(List<String> texts) {
        List<float[]> fallbacks = new ArrayList<>();
        for (String text : texts) {
            fallbacks.add(createDeterministicVector(text, DIMENSION));
        }
        return fallbacks;
    }

    public static float[] createDeterministicVector(String text, int dim) {
        float[] v = new float[dim];
        if (text == null) return v;
        int hash = text.hashCode();
        Random r = new Random(hash);
        float norm = 0f;
        for (int i = 0; i < dim; i++) {
            v[i] = r.nextFloat() - 0.5f;
            norm += v[i] * v[i];
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dim; i++) {
                v[i] /= norm;
            }
        }
        return v;
    }
}
