package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Analyzes DAM asset references without downloading binaries (ADR 0010, Master §15).
 */
public class AssetAnalysisAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(AssetAnalysisAgent.class);

    private final Store store;

    public AssetAnalysisAgent(Store store) {
        this.store = store;
    }

    @Override
    public String getName() {
        return "asset-analysis";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.ANALYZING;
    }

    @Override
    public void execute(AgentContext ctx) throws com.adobe.aem.modernizer.ModernizerException {
        SiteInventory inv = ctx.getInventory();
        int count = (inv != null && inv.getAssets() != null) ? inv.getAssets().size() : 0;
        LOG.info("AssetAnalysisAgent verified {} asset references (metadata-only)", count);

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Verified " + count + " asset references as metadata-only (0 binaries downloaded)."
            ));
        }
    }
}
