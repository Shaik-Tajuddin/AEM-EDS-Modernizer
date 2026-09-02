package com.adobe.aem.modernizer.rag.retrieval;

import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import org.osgi.service.component.annotations.Component;

import java.util.*;

/**
 * Reciprocal Rank Fusion (RRF) & Source Authority Reranker (Sections 12, 26).
 * Blends multi-channel retrieval results and weights by source reliability.
 */
@Component(service = Reranker.class, immediate = true)
public class Reranker {

    private static final double RRF_K = 60.0;

    public List<RetrievalResult> rerank(List<RetrievalResult> rawResults, int topK) {
        if (rawResults == null || rawResults.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, RetrievalResult> deduplicated = new LinkedHashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();

        // 1. Group by channel and calculate rank
        Map<String, List<RetrievalResult>> byChannel = new HashMap<>();
        for (RetrievalResult res : rawResults) {
            String ch = res.getRetrievalChannel() != null ? res.getRetrievalChannel() : "GENERAL";
            byChannel.computeIfAbsent(ch, k -> new ArrayList<>()).add(res);
        }

        for (Map.Entry<String, List<RetrievalResult>> entry : byChannel.entrySet()) {
            List<RetrievalResult> list = entry.getValue();
            list.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            for (int rank = 0; rank < list.size(); rank++) {
                RetrievalResult item = list.get(rank);
                String id = item.getChunk() != null ? item.getChunk().getChunkId() : UUID.randomUUID().toString();
                deduplicated.putIfAbsent(id, item);

                double rrf = 1.0 / (RRF_K + (rank + 1));
                rrfScores.merge(id, rrf, Double::sum);
            }
        }

        // 2. Apply Source Authority Weights
        List<RetrievalResult> finalRanked = new ArrayList<>();
        for (Map.Entry<String, RetrievalResult> entry : deduplicated.entrySet()) {
            String id = entry.getKey();
            RetrievalResult res = entry.getValue();
            double rrf = rrfScores.getOrDefault(id, 0.01);
            double authority = calculateAuthority(res);

            double finalScore = Math.min(1.0, (rrf * 100.0 * 0.5) + (res.getScore() * 0.5));
            res.setAuthorityScore(authority);
            res.setCombinedScore(finalScore * authority);
            finalRanked.add(res);
        }

        finalRanked.sort((a, b) -> Double.compare(b.getCombinedScore(), a.getCombinedScore()));
        return finalRanked.size() > topK ? finalRanked.subList(0, topK) : finalRanked;
    }

    private double calculateAuthority(RetrievalResult res) {
        if (res == null || res.getChunk() == null) return 1.0;
        String type = res.getChunk().getChunkType() != null ? res.getChunk().getChunkType() : "";
        String path = res.getChunk().getPath() != null ? res.getChunk().getPath() : "";

        if (type.equals("AEM_JCR_STRUCTURED")) {
            return 1.6; // Live JCR facts
        }
        if (path.contains("AGENTS.md") || path.contains("AEM_EDS_RULES.md")) {
            return 1.7; // Approved project rules
        }
        if (path.contains("component-models.json") || path.contains("component-definition.json")) {
            return 1.5; // Approved Universal Editor Models
        }
        if (type.equals("EDS_BLOCK_JS") || path.contains("blocks/")) {
            return 1.35; // EDS block decorator code
        }
        if (type.equals("MIGRATION_DECISION")) {
            return 1.25; // Historical migration decisions
        }
        if (path.contains("adr/") || path.contains("docs/adr")) {
            return 1.2; // Approved ADRs
        }
        return 1.0;
    }
}
