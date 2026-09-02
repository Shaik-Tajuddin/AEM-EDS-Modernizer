package com.adobe.aem.modernizer.rag;

import com.adobe.aem.modernizer.rag.embedding.AemVectorStore;
import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.KnowledgeMetadata;
import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AemVectorStoreTest {

    @Test
    void testCosineSimilarityAndTopKRetrieval() {
        AemVectorStore store = new AemVectorStore();

        KnowledgeChunk chunk1 = new KnowledgeChunk();
        chunk1.setChunkId("c1");
        chunk1.setContent("Hero block banner image");
        chunk1.setMetadata(new KnowledgeMetadata("proj-1", "DOCS", "MARKDOWN"));

        KnowledgeChunk chunk2 = new KnowledgeChunk();
        chunk2.setChunkId("c2");
        chunk2.setContent("Footer copyright navigation");
        chunk2.setMetadata(new KnowledgeMetadata("proj-1", "DOCS", "MARKDOWN"));

        float[] v1 = new float[]{1.0f, 0.0f, 0.0f};
        float[] v2 = new float[]{0.0f, 1.0f, 0.0f};

        store.upsert(List.of(chunk1, chunk2), List.of(v1, v2));

        // Query vector close to chunk1
        float[] query = new float[]{0.95f, 0.05f, 0.0f};
        List<RetrievalResult> results = store.search("proj-1", query, 2, 0.0);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getChunk().getChunkId()).isEqualTo("c1");
        assertThat(results.get(0).getScore()).isGreaterThan(0.90);
        assertThat(results.get(1).getChunk().getChunkId()).isEqualTo("c2");
        assertThat(results.get(1).getScore()).isLessThan(0.20);
    }
}
