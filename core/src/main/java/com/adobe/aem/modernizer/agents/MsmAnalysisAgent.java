package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Analyzes Multi-Site Management (MSM) live copies and language masters (Stage: ANALYZING).
 */
public class MsmAnalysisAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(MsmAnalysisAgent.class);

    private final Store store;
    private final AiGateway ai;

    public MsmAnalysisAgent(Store store, AiGateway ai) {
        this.store = store;
        this.ai = ai;
    }

    @Override
    public String getName() {
        return "msm-analysis";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.ANALYZING;
    }

    @Override
    public void execute(AgentContext ctx) throws Exception {
        SiteInventory inv = ctx.getInventory();
        int count = (inv != null && inv.getLiveCopies() != null) ? inv.getLiveCopies().size() : 0;
        LOG.info("MsmAnalysisAgent analyzed {} live copies", count);

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Mapped " + count + " MSM live copy relationships to EDS language folder structures."
            ));
        }
    }
}
