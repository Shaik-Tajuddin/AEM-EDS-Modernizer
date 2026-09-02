package com.adobe.aem.modernizer.rag.retrieval;

import com.adobe.aem.modernizer.rag.embedding.EmbeddingProvider;
import com.adobe.aem.modernizer.rag.embedding.VectorStore;
import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Dense vector retriever leveraging {@link EmbeddingProvider} and {@link VectorStore}
 * to surface semantically similar knowledge chunks.
 */
@Component(service = SemanticRetriever.class, immediate = true)
public class SemanticRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(SemanticRetriever.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient EmbeddingProvider embeddingProvider;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient VectorStore vectorStore;

    public SemanticRetriever() {
    }

    public SemanticRetriever(EmbeddingProvider embeddingProvider, VectorStore vectorStore) {
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
    }

    public List<RetrievalResult> retrieve(RetrievalRequest request) {
        if (embeddingProvider == null || vectorStore == null || request == null) {
            return Collections.emptyList();
        }

        try {
            float[] queryVec = embeddingProvider.embedQuery(request.getQuery());
            if (queryVec == null || queryVec.length == 0) {
                return Collections.emptyList();
            }

            return vectorStore.search(
                    request.getProjectId(),
                    queryVec,
                    request.getTopK(),
                    request.getMinSimilarity()
            );
        } catch (Exception e) {
            LOG.warn("Semantic retrieval failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
