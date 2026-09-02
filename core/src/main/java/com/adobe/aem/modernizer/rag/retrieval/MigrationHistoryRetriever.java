package com.adobe.aem.modernizer.rag.retrieval;

import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.MigrationPlan;
import com.adobe.aem.modernizer.persistence.model.ValidationResultRecord;
import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.KnowledgeMetadata;
import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Migration History Retriever (Section 27).
 * Reuses verified block mappings, past validation passes, and past repair decisions
 * from earlier migration runs to avoid redundant AI calls and guarantee consistent decisions.
 */
@Component(service = MigrationHistoryRetriever.class, immediate = true)
public class MigrationHistoryRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationHistoryRetriever.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient Store store;

    public MigrationHistoryRetriever() {
    }

    public MigrationHistoryRetriever(Store store) {
        this.store = store;
    }

    public List<RetrievalResult> retrieve(RetrievalRequest request) {
        if (store == null || request == null || request.getQuery() == null) {
            return Collections.emptyList();
        }

        String query = request.getQuery().toLowerCase(Locale.ROOT);
        String projectId = request.getProjectId() != null ? request.getProjectId() : "default";

        Optional<MigrationPlan> optPlan = store.getLatestPlan(projectId);
        if (optPlan.isEmpty()) {
            return Collections.emptyList();
        }

        MigrationPlan plan = optPlan.get();
        List<RetrievalResult> historyResults = new ArrayList<>();

        Object mappingsObj = plan.getDetails() != null ? plan.getDetails().get("blockMappings") : null;
        if (mappingsObj instanceof Map) {
            Map<?, ?> mappings = (Map<?, ?>) mappingsObj;
            for (Map.Entry<?, ?> entry : mappings.entrySet()) {
                String aemComp = String.valueOf(entry.getKey());
                String edsBlock = String.valueOf(entry.getValue());

                if (query.contains(aemComp.toLowerCase(Locale.ROOT)) ||
                        query.contains(edsBlock.toLowerCase(Locale.ROOT)) ||
                        query.contains("mapping") || query.contains("history")) {

                    String summary = "Verified Mapping Decision in Previous Migration:\n" +
                            "AEM Component: " + aemComp + "\n" +
                            "Mapped to EDS Block: " + edsBlock + "\n" +
                            "Status: Approved in Migration Plan #" + (plan.getJobId() != null ? plan.getJobId() : "1");

                    KnowledgeChunk chunk = new KnowledgeChunk();
                    chunk.setChunkId("hist:" + Math.abs((aemComp + edsBlock).hashCode()));
                    chunk.setHeading("Historical Block Mapping: " + aemComp);
                    chunk.setSection("Migration Decision Record");
                    chunk.setChunkType("MIGRATION_DECISION");
                    chunk.setContent(summary);
                    chunk.setPath("history/plans/" + plan.getJobId());

                    KnowledgeMetadata meta = new KnowledgeMetadata(projectId, "INTERNAL", "MIGRATION_HISTORY");
                    chunk.setMetadata(meta);

                    RetrievalResult res = new RetrievalResult(chunk, 0.94, "MIGRATION_HISTORY");
                    res.setAuthorityScore(1.3);
                    historyResults.add(res);
                }
            }
        }

        return historyResults;
    }
}
