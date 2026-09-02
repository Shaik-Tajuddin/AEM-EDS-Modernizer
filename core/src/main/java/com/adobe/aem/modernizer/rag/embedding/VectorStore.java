package com.adobe.aem.modernizer.rag.embedding;

import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.RetrievalResult;

import java.util.List;

/**
 * Common abstraction for Vector Storage and similarity search (AEM native JCR store, OpenSearch, pgvector).
 */
public interface VectorStore {

    /** Upserts chunks with their corresponding dense float vectors. */
    void upsert(List<KnowledgeChunk> chunks, List<float[]> vectors);

    /** Searches for top-k chunks matching the query vector within the authorized project context. */
    List<RetrievalResult> search(String projectId, float[] queryVector, int topK, double minSimilarity);

    /** Removes vector for a specific chunk. */
    void delete(String chunkId);

    /** Clears all vectors for a project. */
    void clearProject(String projectId);
}
