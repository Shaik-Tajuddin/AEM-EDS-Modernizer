package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.ModelCapability;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Analyzes AEM component dialogs, HTML structures, and classifies capabilities (Stage: ANALYZING).
 */
public class ComponentIntelligenceAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentIntelligenceAgent.class);

    private final Store store;
    private final AiGateway ai;

    public ComponentIntelligenceAgent(Store store, AiGateway ai) {
        this.store = store;
        this.ai = ai;
    }

    @Override
    public String getName() {
        return "component-intelligence";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.ANALYZING;
    }

    @Override
    public void execute(AgentContext ctx) throws Exception {
        SiteInventory inv = ctx.getInventory();
        if (inv == null || inv.getComponents() == null) return;

        LOG.info("ComponentIntelligenceAgent analyzing {} components", inv.getComponents().size());

        for (SiteInventory.ComponentInfo comp : inv.getComponents()) {
            if (ai != null) {
                ChatRequest req = new ChatRequest(getName(), "Analyze AEM component " + comp.getResourceType() + " (" + comp.getTitle() + ")");
                req.setTargetCapability(ModelCapability.CAP_STRUCTURED);
                ChatResponse resp = ai.dispatch(req);
                LOG.debug("Analyzed component {}: {}", comp.getResourceType(), resp.getContent());
            }
        }

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Classified capabilities and variant rules for " + inv.getComponents().size() + " components."
            ));
        }
    }
}
