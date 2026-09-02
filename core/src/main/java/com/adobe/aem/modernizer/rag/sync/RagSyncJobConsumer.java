package com.adobe.aem.modernizer.rag.sync;

import com.adobe.aem.modernizer.rag.chunking.SemanticChunker;
import com.adobe.aem.modernizer.rag.embedding.EmbeddingProvider;
import com.adobe.aem.modernizer.rag.embedding.VectorStore;
import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.KnowledgeDocument;
import com.adobe.aem.modernizer.rag.model.KnowledgeSyncContext;
import com.adobe.aem.modernizer.rag.model.KnowledgeSyncRun;
import com.adobe.aem.modernizer.rag.persistence.RagStore;
import com.adobe.aem.modernizer.rag.source.EDSRepositoryKnowledgeSource;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Production-ready Sling JobConsumer processing RAG repository synchronization in the background.
 * Topic: {@code com/adobe/aem/modernizer/rag/sync}.
 * Supports incremental fingerprint comparison, batch checkpointing, and cluster-safe resumption.
 */
@Component(
        service = {JobConsumer.class, RagSyncJobConsumer.class},
        property = {
                JobConsumer.PROPERTY_TOPICS + "=com/adobe/aem/modernizer/rag/sync"
        },
        immediate = true
)
public class RagSyncJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(RagSyncJobConsumer.class);
    public static final String JOB_TOPIC = "com/adobe/aem/modernizer/rag/sync";

    @Reference
    private transient RagStore ragStore;

    @Reference
    private transient EDSRepositoryKnowledgeSource edsSource;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient EmbeddingProvider embeddingProvider;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient VectorStore vectorStore;

    private final SemanticChunker chunker = new SemanticChunker();

    public RagSyncJobConsumer() {
    }

    public RagSyncJobConsumer(RagStore ragStore, EDSRepositoryKnowledgeSource edsSource) {
        this.ragStore = ragStore;
        this.edsSource = edsSource;
    }

    @Override
    public JobResult process(Job job) {
        String syncId = (String) job.getProperty("syncId");
        String projectId = (String) job.getProperty("projectId");
        boolean forceReindex = Boolean.TRUE.equals(job.getProperty("forceReindex"));
        String localPath = (String) job.getProperty("localPath");
        String repoUrl = (String) job.getProperty("repoUrl");
        String branch = (String) job.getProperty("branch");

        boolean ok = runSync(syncId, projectId, forceReindex, repoUrl, branch, localPath);
        return ok ? JobResult.OK : JobResult.FAILED;
    }

    public boolean runSync(String syncId, String projectId, boolean forceReindex, String repoUrl, String branch, String localPath) {
        if (syncId == null || syncId.isBlank()) {
            syncId = "sync-" + System.currentTimeMillis();
        }
        if (projectId == null || projectId.isBlank()) {
            projectId = "default";
        }

        LOG.info("Starting RAG Sync Job [{}] for project '{}' (forceReindex={})", syncId, projectId, forceReindex);

        KnowledgeSyncRun run = ragStore.getSyncRun(syncId).orElse(new KnowledgeSyncRun(syncId, projectId, EDSRepositoryKnowledgeSource.SOURCE_ID));
        run.setStatus("RUNNING");
        ragStore.saveSyncRun(run);

        try {
            KnowledgeSyncContext ctx = new KnowledgeSyncContext(syncId, projectId, EDSRepositoryKnowledgeSource.SOURCE_ID);
            ctx.setForceReindex(forceReindex);
            ctx.setLocalPath(localPath);
            ctx.setRepoUrl(repoUrl);
            ctx.setBranch(branch != null ? branch : "main");

            List<KnowledgeDocument> discovered = edsSource.scan(ctx);
            run.setDocumentsDiscovered(discovered.size());
            ragStore.saveSyncRun(run);

            LOG.info("Discovered {} documents in EDS repository for project '{}'", discovered.size(), projectId);

            int batchCounter = 0;
            for (KnowledgeDocument doc : discovered) {
                run.setCurrentDocument(doc.getPath());

                // Check for incremental skip
                Optional<KnowledgeDocument> existing = ragStore.getDocument(doc.getId());
                if (!forceReindex && existing.isPresent()) {
                    String existingFp = existing.get().getFingerprint();
                    if (existingFp != null && existingFp.equals(doc.getFingerprint())) {
                        run.setDocumentsSkipped(run.getDocumentsSkipped() + 1);
                        batchCounter++;
                        if (batchCounter % 10 == 0) {
                            run.setLastCheckpoint("Processed " + (run.getDocumentsProcessed() + run.getDocumentsSkipped()) + " documents");
                            ragStore.saveSyncRun(run);
                        }
                        continue;
                    }
                }

                // Changed or new document: Chunk
                List<KnowledgeChunk> chunks = chunker.chunk(doc);
                doc.setChunks(chunks);
                doc.setIndexedAt(Instant.now().toString());
                doc.setStatus("INDEXED");

                // Generate vector embeddings if provider is available
                if (embeddingProvider != null && !chunks.isEmpty()) {
                    try {
                        List<String> texts = new ArrayList<>();
                        for (KnowledgeChunk c : chunks) {
                            texts.add(c.getContent());
                        }
                        List<float[]> vectors = embeddingProvider.embed(texts);
                        if (vectorStore != null && vectors.size() == chunks.size()) {
                            vectorStore.upsert(chunks, vectors);
                            run.setEmbeddingsCreated(run.getEmbeddingsCreated() + vectors.size());
                        }
                    } catch (Exception e) {
                        LOG.warn("Failed embedding chunks for doc: {}", doc.getId(), e);
                    }
                }

                // Persist document and its chunks
                ragStore.saveDocument(doc);
                ragStore.saveChunks(chunks);

                run.setDocumentsProcessed(run.getDocumentsProcessed() + 1);
                run.setChunksCreated(run.getChunksCreated() + chunks.size());
                batchCounter++;

                if (batchCounter % 10 == 0) {
                    run.setLastCheckpoint("Processed " + (run.getDocumentsProcessed() + run.getDocumentsSkipped()) + " documents");
                    ragStore.saveSyncRun(run);
                }
            }

            run.setStatus("COMPLETED");
            run.setEndTime(Instant.now().toString());
            run.setLastCheckpoint("Completed processing " + discovered.size() + " documents");
            ragStore.saveSyncRun(run);

            LOG.info("RAG Sync Job [{}] COMPLETED successfully. Processed: {}, Skipped: {}, Chunks: {}",
                    syncId, run.getDocumentsProcessed(), run.getDocumentsSkipped(), run.getChunksCreated());
            return true;

        } catch (Exception e) {
            LOG.error("RAG Sync Job [{}] FAILED: {}", syncId, e.getMessage(), e);
            run.setStatus("FAILED");
            run.setEndTime(Instant.now().toString());
            run.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.toString());
            ragStore.saveSyncRun(run);
            return false;
        }
    }
}
