package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.connectors.AemClient;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
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

    public DiscoveryAgent(AemClient aemClient, Store store) {
        this.aemClient = aemClient;
        this.store = store;
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
    public void execute(AgentContext ctx) throws com.adobe.aem.modernizer.ModernizerException {
        LOG.info("DiscoveryAgent crawling AEM at {}", ctx.getProject().getContentRoot());

        SiteInventory inventory = null;
        if (aemClient != null) {
            String scopeMode = ctx.getProject() != null ? ctx.getProject().getScopeMode() : "RECURSIVE";
            inventory = aemClient.crawl(ctx.getProject().getContentRoot(), ctx.getProject().getPageScope(), scopeMode);
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
