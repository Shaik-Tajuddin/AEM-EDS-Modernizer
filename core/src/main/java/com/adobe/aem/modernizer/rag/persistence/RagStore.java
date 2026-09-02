package com.adobe.aem.modernizer.rag.persistence;

import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.KnowledgeDocument;
import com.adobe.aem.modernizer.rag.model.KnowledgeSyncRun;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for RAG knowledge documents, chunks, and sync runs.
 */
public interface RagStore {

    // Documents
    void saveDocument(KnowledgeDocument doc);
    Optional<KnowledgeDocument> getDocument(String id);
    List<KnowledgeDocument> listDocuments(String projectId);
    boolean deleteDocument(String id);

    // Chunks
    void saveChunk(KnowledgeChunk chunk);
    void saveChunks(List<KnowledgeChunk> chunks);
    Optional<KnowledgeChunk> getChunk(String chunkId);
    List<KnowledgeChunk> listChunksForDocument(String documentId);
    List<KnowledgeChunk> listChunksForProject(String projectId);
    boolean deleteChunk(String chunkId);

    // Sync Runs & Checkpoints
    void saveSyncRun(KnowledgeSyncRun run);
    Optional<KnowledgeSyncRun> getSyncRun(String syncId);
    List<KnowledgeSyncRun> listSyncRuns(String projectId);
    Optional<KnowledgeSyncRun> getLatestSyncRun(String projectId);

    // Repository counts
    long getDocumentCount(String projectId);
    long getChunkCount(String projectId);
}
