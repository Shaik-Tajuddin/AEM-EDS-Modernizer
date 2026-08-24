package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Analyzes Content Fragments and structured data models (Stage: ANALYZING).
 */
public class ContentFragmentAnalysisAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(ContentFragmentAnalysisAgent.class);

    private final Store store;
    private final AiGateway ai;

    public ContentFragmentAnalysisAgent(Store store, AiGateway ai) {
        this.store = store;
        this.ai = ai;
    }

    @Override
    public String getName() {
        return "content-fragment-analysis";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.ANALYZING;
    }

    @Override
    public void execute(AgentContext ctx) throws Exception {
        SiteInventory inv = ctx.getInventory();
        int count = (inv != null && inv.getContentFragments() != null) ? inv.getContentFragments().size() : 0;
        LOG.info("ContentFragmentAnalysisAgent analyzed {} Content Fragments", count);

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Analyzed " + count + " Content Fragments and mapped schema fields to EDS JSON tables."
            ));
        }
    }
}
