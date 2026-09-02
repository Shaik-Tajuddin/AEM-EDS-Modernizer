package com.adobe.aem.modernizer.rag;

import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.KnowledgeDocument;
import com.adobe.aem.modernizer.rag.model.KnowledgeMetadata;
import com.adobe.aem.modernizer.rag.model.KnowledgeSyncRun;
import com.adobe.aem.modernizer.rag.persistence.RagStore;
import com.adobe.aem.modernizer.rag.retrieval.*;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalServiceTest {

    @Test
    void testHybridRetrievalAndConfidenceScoring() {
        RagStore dummyStore = new RagStore() {
            @Override public void saveDocument(KnowledgeDocument doc) {}
            @Override public Optional<KnowledgeDocument> getDocument(String id) { return Optional.empty(); }
            @Override public List<KnowledgeDocument> listDocuments(String projectId) { return Collections.emptyList(); }
            @Override public boolean deleteDocument(String id) { return false; }

            @Override public void saveChunk(KnowledgeChunk chunk) {}
            @Override public void saveChunks(List<KnowledgeChunk> chunks) {}
            @Override public Optional<KnowledgeChunk> getChunk(String chunkId) { return Optional.empty(); }
            @Override public List<KnowledgeChunk> listChunksForDocument(String documentId) { return Collections.emptyList(); }
            @Override public List<KnowledgeChunk> listChunksForProject(String projectId) {
                KnowledgeChunk c = new KnowledgeChunk();
                c.setChunkId("chunk-hero");
                c.setHeading("Hero Block Structure");
                c.setContent("The hero block renders a banner image and title.");
                c.setPath("blocks/hero/hero.js");
                c.setMetadata(new KnowledgeMetadata("proj-1", "DOCS", "EDS_CODE"));
                return List.of(c);
            }
            @Override public boolean deleteChunk(String chunkId) { return false; }

            @Override public void saveSyncRun(KnowledgeSyncRun run) {}
            @Override public Optional<KnowledgeSyncRun> getSyncRun(String syncId) { return Optional.empty(); }
            @Override public List<KnowledgeSyncRun> listSyncRuns(String projectId) { return Collections.emptyList(); }
            @Override public Optional<KnowledgeSyncRun> getLatestSyncRun(String projectId) { return Optional.empty(); }

            @Override public long getDocumentCount(String projectId) { return 1; }
            @Override public long getChunkCount(String projectId) { return 1; }
        };

        KeywordRetriever keywordRetriever = new KeywordRetriever(dummyStore);
        Reranker reranker = new Reranker();
        ConfidenceCalculator confidenceCalculator = new ConfidenceCalculator();
        CitationService citationService = new CitationService();

        RetrievalService service = new RetrievalService(
                keywordRetriever,
                null,
                null,
                null,
                reranker,
                confidenceCalculator,
                citationService
        );

        RetrievalRequest req = new RetrievalRequest("hero block", "proj-1");
        RetrievalResponse resp = service.retrieve(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getResults()).isNotEmpty();
        assertThat(resp.getResults().get(0).getChunk().getHeading()).isEqualTo("Hero Block Structure");
        assertThat(resp.getConfidenceScore()).isGreaterThan(0.5);
        assertThat(resp.getConfidenceLevel()).isIn("HIGH", "MEDIUM");
        assertThat(resp.getCitations()).isNotEmpty();
        assertThat(resp.getCitations().get(0).getPath()).isEqualTo("blocks/hero/hero.js");
    }
}
