package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.connectors.AemClient;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import com.adobe.aem.modernizer.scopes.MarkerEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Crawls AEM to create the immutable Site Inventory snapshot (Stage: DISCOVERING).
 */
public class DiscoveryAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryAgent.class);

    private final AemClient aemClient;
    private final Store store;
    private final AiGateway ai;
    private final MarkerEvaluator markerEvaluator;

    public DiscoveryAgent(AemClient aemClient, Store store, AiGateway ai, MarkerEvaluator markerEvaluator) {
        this.aemClient = aemClient;
        this.store = store;
        this.ai = ai;
        this.markerEvaluator = markerEvaluator;
    }

    @Override
    public String getName() {
        return "discovery";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.DISCOVERING;
    }

    @Override
    public void execute(AgentContext ctx) throws Exception {
        LOG.info("DiscoveryAgent crawling AEM at {}", ctx.getProject().getContentRoot());

        SiteInventory inventory = null;
        if (aemClient != null) {
            inventory = aemClient.crawl(ctx.getProject().getContentRoot(), ctx.getProject().getPageScope());
        }

        if (inventory == null) {
            inventory = new SiteInventory();
        }

        inventory.setProjectId(ctx.getProject().getId());
        inventory.setJobId(ctx.getJob().getId());

        ctx.setInventory(inventory);
        if (store != null) {
            store.saveInventory(inventory);
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Discovered " + inventory.getTotalPages() + " pages (" + inventory.getEligiblePages()
                            + " eligible), " + inventory.getComponents().size() + " components, "
                            + inventory.getTemplates().size() + " templates."
            ));
        }
    }
}
