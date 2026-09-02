package com.adobe.aem.modernizer.rag.embedding;

import java.util.List;

/**
 * Common abstraction for embedding generation providers (OpenAI, Ollama, Anthropic-compatible, or Mock).
 */
public interface EmbeddingProvider {

    /** Provider name (e.g., "openai", "ollama", "mock"). */
    String getProviderName();

    /** Dimension of the produced vectors (e.g. 1536 for OpenAI text-embedding-3-small). */
    int getDimension();

    /** Generates embeddings for a batch of text strings. */
    List<float[]> embed(List<String> texts);

    /** Generates a single embedding for query retrieval. */
    default float[] embedQuery(String text) {
        List<float[]> results = embed(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }
}
