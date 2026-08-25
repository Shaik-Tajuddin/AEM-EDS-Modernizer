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
 * Maps AEM component resource types to EDS block definitions (Stage: ANALYZING).
 */
public class ComponentMappingAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentMappingAgent.class);

    private final Store store;
    private final AiGateway ai;

    public ComponentMappingAgent(Store store, AiGateway ai) {
        this.store = store;
        this.ai = ai;
    }

    @Override
    public String getName() {
        return "component-mapping";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.ANALYZING;
    }

    @Override
    public void execute(AgentContext ctx) throws com.adobe.aem.modernizer.ModernizerException {
        SiteInventory inv = ctx.getInventory();
        if (inv == null || inv.getComponents() == null) return;

        LOG.info("ComponentMappingAgent mapping {} components to EDS blocks", inv.getComponents().size());

        for (SiteInventory.ComponentInfo comp : inv.getComponents()) {
            String blockName = comp.getResourceType().substring(comp.getResourceType().lastIndexOf('/') + 1);
            comp.setProposedEdsBlock(blockName);

            if (ai != null) {
                ChatRequest req = new ChatRequest(getName(), "Map AEM resourceType " + comp.getResourceType() + " to EDS block format");
                req.setTargetCapability(ModelCapability.CAP_STRUCTURED);
                req.setProjectId(ctx.getProject().getId());
                req.setJobId(ctx.getJob().getId());
                req.setPreferredProvider(ctx.getProject().getAiProvider());
                req.setPreferredModel(ctx.getProject().getAiModel());
                ChatResponse resp = ai.dispatch(req);
                LOG.debug("Mapping decision for {}: {}", comp.getResourceType(), resp.getContent());
            }
        }

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Mapped " + inv.getComponents().size() + " AEM components to EDS blocks and variants."
            ));
        }
    }
}
