package com.adobe.aem.modernizer.rag.retrieval;

import com.adobe.aem.modernizer.rag.model.Citation;
import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Master Hybrid Retrieval Service (Sections 12, 13).
 * Enforces pre-retrieval project isolation, coordinates semantic, keyword, live JCR,
 * and migration history retrieval channels, and executes rank blending with confidence scoring.
 */
@Component(service = RetrievalService.class, immediate = true)
public class RetrievalService {

    private static final Logger LOG = LoggerFactory.getLogger(RetrievalService.class);

    @Reference
    private transient KeywordRetriever keywordRetriever;

    @Reference
    private transient SemanticRetriever semanticRetriever;

    @Reference
    private transient AemRepositoryRetriever aemRepositoryRetriever;

    @Reference
    private transient MigrationHistoryRetriever migrationHistoryRetriever;

    @Reference
    private transient Reranker reranker;

    @Reference
    private transient ConfidenceCalculator confidenceCalculator;

    @Reference
    private transient CitationService citationService;

    public RetrievalService() {
    }

    public RetrievalService(KeywordRetriever keywordRetriever,
                            SemanticRetriever semanticRetriever,
                            AemRepositoryRetriever aemRepositoryRetriever,
                            MigrationHistoryRetriever migrationHistoryRetriever,
                            Reranker reranker,
                            ConfidenceCalculator confidenceCalculator,
                            CitationService citationService) {
        this.keywordRetriever = keywordRetriever;
        this.semanticRetriever = semanticRetriever;
        this.aemRepositoryRetriever = aemRepositoryRetriever;
        this.migrationHistoryRetriever = migrationHistoryRetriever;
        this.reranker = reranker;
        this.confidenceCalculator = confidenceCalculator;
        this.citationService = citationService;
    }

    public RetrievalResponse retrieve(RetrievalRequest request) {
        long startTime = System.currentTimeMillis();
        String projectId = request != null && request.getProjectId() != null ? request.getProjectId() : "default";
        String query = request != null ? request.getQuery() : "";

        RetrievalResponse response = new RetrievalResponse(query, projectId);
        if (request == null || query == null || query.isBlank()) {
            return response;
        }

        List<RetrievalResult> candidateResults = new ArrayList<>();

        // 1. Live AEM Repository structured facts (highest authority)
        if (request.isIncludeAemFacts() && aemRepositoryRetriever != null) {
            try {
                List<RetrievalResult> aemFacts = aemRepositoryRetriever.retrieve(request);
                candidateResults.addAll(aemFacts);
            } catch (Exception e) {
                LOG.warn("Error during AemRepositoryRetriever execution: {}", e.getMessage());
            }
        }

        // 2. Migration History reuse
        if (request.isIncludeMigrationHistory() && migrationHistoryRetriever != null) {
            try {
                List<RetrievalResult> history = migrationHistoryRetriever.retrieve(request);
                candidateResults.addAll(history);
            } catch (Exception e) {
                LOG.warn("Error during MigrationHistoryRetriever execution: {}", e.getMessage());
            }
        }

        // 3. Keyword / BM25 Search
        if (request.isIncludeEdsKnowledge() && keywordRetriever != null) {
            try {
                List<RetrievalResult> kw = keywordRetriever.retrieve(request);
                candidateResults.addAll(kw);
            } catch (Exception e) {
                LOG.warn("Error during KeywordRetriever execution: {}", e.getMessage());
            }
        }

        // 4. Semantic Dense Vector Search
        if (request.isIncludeEdsKnowledge() && semanticRetriever != null) {
            try {
                List<RetrievalResult> sem = semanticRetriever.retrieve(request);
                candidateResults.addAll(sem);
            } catch (Exception e) {
                LOG.warn("Error during SemanticRetriever execution: {}", e.getMessage());
            }
        }

        response.setTotalDiscovered(candidateResults.size());

        // 5. Rerank via RRF + Authority Weights
        List<RetrievalResult> ranked = (reranker != null)
                ? reranker.rerank(candidateResults, request.getTopK())
                : candidateResults;
        response.setResults(ranked);

        // 6. Calculate Confidence
        double conf = (confidenceCalculator != null) ? confidenceCalculator.calculateScore(ranked) : 0.50;
        String level = (confidenceCalculator != null) ? confidenceCalculator.calculateLevel(conf) : "MEDIUM";
        response.setConfidenceScore(conf);
        response.setConfidenceLevel(level);

        // 7. Generate Citations
        if (citationService != null) {
            List<Citation> citations = citationService.buildCitations(ranked);
            response.setCitations(citations);
        }

        response.setExecutionDurationMs(System.currentTimeMillis() - startTime);
        LOG.debug("Retrieval completed in {}ms: discovered {}, returned {} top results with confidence {}",
                response.getExecutionDurationMs(), response.getTotalDiscovered(), ranked.size(), level);

        return response;
    }
}
