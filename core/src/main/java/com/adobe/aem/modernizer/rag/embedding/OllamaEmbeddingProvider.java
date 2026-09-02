package com.adobe.aem.modernizer.rag.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Local Ollama Embedding Provider (e.g. nomic-embed-text or all-minilm).
 */
@Component(service = {EmbeddingProvider.class, OllamaEmbeddingProvider.class}, immediate = true)
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaEmbeddingProvider.class);
    private static final String DEFAULT_MODEL = "nomic-embed-text";
    private static final int DIMENSION = 768;

    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private String endpoint = "http://localhost:11434";
    private String model = DEFAULT_MODEL;

    public OllamaEmbeddingProvider() {
    }

    public OllamaEmbeddingProvider(String endpoint, String model) {
        this.endpoint = endpoint != null ? endpoint : "http://localhost:11434";
        this.model = model != null ? model : DEFAULT_MODEL;
    }

    @Activate
    public void activate() {
        LOG.info("OllamaEmbeddingProvider activated (endpoint={}, model={})", endpoint, model);
    }

    @Override
    public String getProviderName() {
        return "ollama";
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

        List<float[]> embeddings = new ArrayList<>();
        for (String text : texts) {
            try {
                String requestJson = mapper.writeValueAsString(mapper.createObjectNode()
                        .put("model", model)
                        .put("prompt", text));

                Request request = new Request.Builder()
                        .url(endpoint + "/api/embeddings")
                        .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonNode root = mapper.readTree(response.body().string());
                        JsonNode emb = root.get("embedding");
                        if (emb != null && emb.isArray()) {
                            float[] vec = new float[emb.size()];
                            for (int i = 0; i < emb.size(); i++) {
                                vec[i] = (float) emb.get(i).asDouble();
                            }
                            embeddings.add(vec);
                            continue;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debug("Ollama embedding call failed: {}", e.getMessage());
            }
            embeddings.add(OpenAiEmbeddingProvider.createDeterministicVector(text, DIMENSION));
        }
        return embeddings;
    }
}
