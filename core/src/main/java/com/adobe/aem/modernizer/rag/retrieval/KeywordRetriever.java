package com.adobe.aem.modernizer.rag.retrieval;

import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import com.adobe.aem.modernizer.rag.persistence.RagStore;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Keyword & BM25-based retriever scoring chunks based on exact term matches,
 * title relevance, and file path proximity.
 */
@Component(service = KeywordRetriever.class, immediate = true)
public class KeywordRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(KeywordRetriever.class);

    @Reference
    private transient RagStore ragStore;

    public KeywordRetriever() {
    }

    public KeywordRetriever(RagStore ragStore) {
        this.ragStore = ragStore;
    }

    public List<RetrievalResult> retrieve(RetrievalRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            return Collections.emptyList();
        }

        String projectId = request.getProjectId() != null ? request.getProjectId() : "default";
        List<KnowledgeChunk> chunks = ragStore.listChunksForProject(projectId);
        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }

        String[] rawTokens = request.getQuery().toLowerCase(Locale.ROOT).split("[\\s,;:.!?()/\\[\\]]+");
        Set<String> queryTokens = new HashSet<>();
        for (String t : rawTokens) {
            if (t.length() >= 2) queryTokens.add(t);
        }

        if (queryTokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<RetrievalResult> matches = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            double score = scoreChunk(chunk, queryTokens);
            if (score > 0.05) {
                matches.add(new RetrievalResult(chunk, score, "KEYWORD"));
            }
        }

        matches.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return matches.size() > request.getTopK() ? matches.subList(0, request.getTopK()) : matches;
    }

    private double scoreChunk(KnowledgeChunk chunk, Set<String> queryTokens) {
        String content = chunk.getContent() != null ? chunk.getContent().toLowerCase(Locale.ROOT) : "";
        String heading = chunk.getHeading() != null ? chunk.getHeading().toLowerCase(Locale.ROOT) : "";
        String path = chunk.getPath() != null ? chunk.getPath().toLowerCase(Locale.ROOT) : "";

        double score = 0.0;
        for (String token : queryTokens) {
            // Path match (strongest signal for block / file specific queries)
            if (path.contains(token)) {
                score += 0.40;
            }
            // Heading match
            if (heading.contains(token)) {
                score += 0.35;
            }
            // Exact content frequency
            if (content.contains(token)) {
                int count = countOccurrences(content, token);
                score += Math.min(0.30, count * 0.05);
            }
        }
        return Math.min(1.0, score);
    }

    private static int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
            if (count >= 10) break;
        }
        return count;
    }
}
