package com.adobe.aem.modernizer.rag;

import com.adobe.aem.modernizer.rag.eval.RagEvaluationCase;
import com.adobe.aem.modernizer.rag.eval.RagEvaluationRun;
import com.adobe.aem.modernizer.rag.eval.RagEvaluationService;
import com.adobe.aem.modernizer.rag.model.Citation;
import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.KnowledgeMetadata;
import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import com.adobe.aem.modernizer.rag.retrieval.RetrievalRequest;
import com.adobe.aem.modernizer.rag.retrieval.RetrievalResponse;
import com.adobe.aem.modernizer.rag.retrieval.RetrievalService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagEvaluationTest {

    @Test
    void testStandardEvaluationSuiteExecution() {
        RetrievalService retrievalService = new RetrievalService() {
            @Override
            public RetrievalResponse retrieve(RetrievalRequest request) {
                RetrievalResponse resp = new RetrievalResponse(request.getQuery(), request.getProjectId());
                KnowledgeChunk c = new KnowledgeChunk();
                c.setChunkId("chunk-1");
                c.setHeading("Hero Block");
                c.setPath("blocks/hero/hero.js");
                c.setContent("Hero block authored with banner image");
                c.setMetadata(new KnowledgeMetadata(request.getProjectId(), "DOCS", "EDS_CODE"));

                RetrievalResult res = new RetrievalResult(c, 0.95, "EDS_DOCS");
                resp.setResults(List.of(res));
                resp.setConfidenceScore(0.92);
                resp.setConfidenceLevel("HIGH");
                resp.setCitations(List.of(new Citation(1, "blocks/hero/hero.js", "Hero Block", 0.95)));
                return resp;
            }
        };

        RagEvaluationService evalService = new RagEvaluationService(retrievalService);

        RagEvaluationRun run = evalService.runEvaluation("wknd-site");

        assertThat(run).isNotNull();
        assertThat(run.getTotalCases()).isEqualTo(5);
        assertThat(run.getPassedCases()).isGreaterThan(0);
        assertThat(run.getAveragePrecision()).isGreaterThan(0.0);
        assertThat(run.getAverageGroundedness()).isGreaterThan(0.0);
        assertThat(run.getCases()).hasSize(5);

        for (RagEvaluationCase tc : run.getCases()) {
            assertThat(tc.getLatencyMs()).isGreaterThanOrEqualTo(0);
        }
    }
}
