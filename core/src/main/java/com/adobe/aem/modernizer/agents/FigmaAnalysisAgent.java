package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.connectors.FigmaClient;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Extracts design tokens (colors, typography, spacing) from Figma (Phase 1, Stage: DESIGN_ANALYSIS).
 */
public class FigmaAnalysisAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(FigmaAnalysisAgent.class);

    private final FigmaClient figmaClient;
    private final Store store;
    private final AiGateway ai;

    public FigmaAnalysisAgent(FigmaClient figmaClient, Store store, AiGateway ai) {
        this.figmaClient = figmaClient;
        this.store = store;
        this.ai = ai;
    }

    @Override
    public String getName() {
        return "figma-analysis";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.DESIGN_ANALYSIS;
    }

    @Override
    public void execute(AgentContext ctx) throws Exception {
        String figmaUrl = ctx.getProject().getFigmaUrl();
        if (figmaUrl == null || figmaUrl.trim().isEmpty()) {
            LOG.info("FigmaAnalysisAgent skipped: no Figma URL configured");
            return;
        }

        LOG.info("FigmaAnalysisAgent reading tokens from {}", figmaUrl);
        Map<String, String> tokens = (figmaClient != null) ? figmaClient.extractTokens(figmaUrl) : null;
        if (tokens != null && ctx.getInventory() != null) {
            ctx.getInventory().getFigmaTokens().putAll(tokens);
        }

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Extracted " + (tokens != null ? tokens.size() : 0) + " design tokens from Figma."
            ));
        }
    }
}
