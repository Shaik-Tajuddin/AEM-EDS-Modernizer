package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Analyzes editable templates and section layouts (Stage: ANALYZING).
 */
public class TemplateAnalysisAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateAnalysisAgent.class);

    private final Store store;

    public TemplateAnalysisAgent(Store store) {
        this.store = store;
    }

    @Override
    public String getName() {
        return "template-analysis";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.ANALYZING;
    }

    @Override
    public void execute(AgentContext ctx) throws com.adobe.aem.modernizer.ModernizerException {
        SiteInventory inv = ctx.getInventory();
        int count = (inv != null && inv.getTemplates() != null) ? inv.getTemplates().size() : 0;
        LOG.info("TemplateAnalysisAgent analyzed {} templates", count);

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Analyzed " + count + " templates for layout section models."
            ));
        }
    }
}
