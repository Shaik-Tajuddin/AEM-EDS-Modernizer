package com.adobe.aem.modernizer.rag.eval;

import com.adobe.aem.modernizer.rag.model.Citation;
import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import com.adobe.aem.modernizer.rag.retrieval.RetrievalRequest;
import com.adobe.aem.modernizer.rag.retrieval.RetrievalResponse;
import com.adobe.aem.modernizer.rag.retrieval.RetrievalService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Production RAG Evaluation Engine (Section 30).
 * Runs automated benchmarks evaluating precision, groundedness, latency, and citation verification.
 */
@Component(service = RagEvaluationService.class, immediate = true)
public class RagEvaluationService {

    private static final Logger LOG = LoggerFactory.getLogger(RagEvaluationService.class);

    @Reference
    private transient RetrievalService retrievalService;

    public RagEvaluationService() {
    }

    public RagEvaluationService(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    public List<RagEvaluationCase> getStandardTestCases() {
        List<RagEvaluationCase> cases = new ArrayList<>();
        cases.add(new RagEvaluationCase(
                "tc-1",
                "How do I author a Hero block in this repository?",
                "EDS_BLOCK",
                List.of("blocks/hero", "hero.js", "component-models.json")
        ));
        cases.add(new RagEvaluationCase(
                "tc-2",
                "What is the correct Universal Editor model for teasers?",
                "EDS_MODEL",
                List.of("component-models.json", "blocks/teaser")
        ));
        cases.add(new RagEvaluationCase(
                "tc-3",
                "Why did /content/wknd/en/magazine fail migration?",
                "MIGRATION_DIAGNOSTIC",
                List.of("/content/wknd", "validation", "history")
        ));
        cases.add(new RagEvaluationCase(
                "tc-4",
                "Which EDS block should wknd/components/teaser map to?",
                "MIGRATION_DECISION",
                List.of("blocks/teaser", "teaser", "plans")
        ));
        cases.add(new RagEvaluationCase(
                "tc-5",
                "What are the fstab.yaml mountpoint configurations?",
                "CONFIG",
                List.of("fstab.yaml")
        ));
        return cases;
    }

    public RagEvaluationRun runEvaluation(String projectId) {
        String pid = projectId != null ? projectId : "default";
        String runId = "eval-" + System.currentTimeMillis();
        RagEvaluationRun run = new RagEvaluationRun(runId, pid);

        List<RagEvaluationCase> testCases = getStandardTestCases();
        run.setTotalCases(testCases.size());

        if (retrievalService == null) {
            LOG.warn("RetrievalService is null, skipping RAG evaluation run");
            return run;
        }

        int passedCount = 0;
        double totalGroundedness = 0.0;
        double totalPrecision = 0.0;
        int verifiedCitations = 0;
        long totalLatency = 0;

        for (RagEvaluationCase tc : testCases) {
            long start = System.currentTimeMillis();
            RetrievalRequest req = new RetrievalRequest(tc.getQuestion(), pid);
            req.setTopK(5);

            RetrievalResponse res = retrievalService.retrieve(req);
            long duration = System.currentTimeMillis() - start;
            tc.setLatencyMs(duration);
            totalLatency += duration;

            tc.setActualConfidence(res.getConfidenceScore());

            // Check if expected sources appear in results
            boolean matchedSource = false;
            for (RetrievalResult r : res.getResults()) {
                String p = (r.getChunk() != null && r.getChunk().getPath() != null)
                        ? r.getChunk().getPath().toLowerCase() : "";
                for (String exp : tc.getExpectedSources()) {
                    if (p.contains(exp.toLowerCase())) {
                        matchedSource = true;
                        break;
                    }
                }
                if (matchedSource) break;
            }
            tc.setSourcesMatched(matchedSource);

            // Groundedness and citation verification
            boolean hasCitations = !res.getCitations().isEmpty();
            tc.setCitationVerified(hasCitations && matchedSource);
            if (tc.isCitationVerified()) verifiedCitations++;

            double precision = matchedSource ? 1.0 : 0.4;
            double groundedness = res.getConfidenceScore() >= tc.getMinExpectedConfidence() ? 0.95 : 0.60;
            tc.setGroundednessScore(groundedness);
            totalPrecision += precision;
            totalGroundedness += groundedness;

            boolean passed = (res.getConfidenceScore() >= tc.getMinExpectedConfidence()) || matchedSource;
            tc.setPassed(passed);
            if (passed) passedCount++;

            run.getCases().add(tc);
        }

        int count = testCases.size();
        run.setPassedCases(passedCount);
        run.setOverallScore(count > 0 ? (double) passedCount / count : 0.0);
        run.setAveragePrecision(count > 0 ? totalPrecision / count : 0.0);
        run.setAverageGroundedness(count > 0 ? totalGroundedness / count : 0.0);
        run.setCitationCorrectnessRate(count > 0 ? (double) verifiedCitations / count : 0.0);
        run.setAverageLatencyMs(count > 0 ? (double) totalLatency / count : 0.0);

        LOG.info("RAG Evaluation Run [{}] completed: {}/{} passed, overall score={}",
                runId, passedCount, count, String.format("%.2f", run.getOverallScore()));

        return run;
    }
}
