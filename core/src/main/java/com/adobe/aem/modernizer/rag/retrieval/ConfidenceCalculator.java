package com.adobe.aem.modernizer.rag.retrieval;

import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import org.osgi.service.component.annotations.Component;

import java.util.List;

/**
 * Calculates quantitative and categorical confidence (HIGH, MEDIUM, LOW)
 * based on retrieval relevance, source authority, and supporting evidence density (Section 25).
 */
@Component(service = ConfidenceCalculator.class, immediate = true)
public class ConfidenceCalculator {

    public double calculateScore(List<RetrievalResult> rankedResults) {
        if (rankedResults == null || rankedResults.isEmpty()) {
            return 0.10;
        }

        double topScore = rankedResults.get(0).getScore();
        double authority = rankedResults.get(0).getAuthorityScore();

        // Check diversity of supporting sources
        long distinctSources = rankedResults.stream()
                .map(r -> r.getChunk() != null ? r.getChunk().getPath() : "")
                .filter(p -> !p.isBlank())
                .distinct()
                .count();

        double sourceCoverageBonus = Math.min(0.15, (distinctSources - 1) * 0.05);
        double raw = (topScore * 0.65) + (authority * 0.20) + sourceCoverageBonus;

        return Math.min(0.99, Math.max(0.10, Math.round(raw * 100.0) / 100.0));
    }

    public String calculateLevel(double confidenceScore) {
        if (confidenceScore >= 0.80) {
            return "HIGH";
        } else if (confidenceScore >= 0.50) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
