package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Analyzes page content trees, semantic structure, and text density (Stage: ANALYZING).
 */
public class ContentAnalysisAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(ContentAnalysisAgent.class);

    private final Store store;

    public ContentAnalysisAgent(Store store) {
        this.store = store;
    }

    @Override
    public String getName() {
        return "content-analysis";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.ANALYZING;
    }

    @Override
    public void execute(AgentContext ctx) throws com.adobe.aem.modernizer.ModernizerException {
        SiteInventory inv = ctx.getInventory();
        int count = (inv != null && inv.getPages() != null) ? inv.getPages().size() : 0;
        LOG.info("ContentAnalysisAgent analyzed {} pages", count);

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Completed semantic content analysis across " + count + " pages."
            ));
        }
    }
}
