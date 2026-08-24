package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.ModelCapability;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Generates EDS JavaScript block decoration logic (Stage: BUILDING).
 */
public class BlockGenerationAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(BlockGenerationAgent.class);

    private final Store store;
    private final AiGateway ai;

    public BlockGenerationAgent(Store store, AiGateway ai) {
        this.store = store;
        this.ai = ai;
    }

    @Override
    public String getName() {
        return "block-generation";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.BUILDING;
    }

    @Override
    public void execute(AgentContext ctx) throws com.adobe.aem.modernizer.ModernizerException {
        SiteInventory inv = ctx.getInventory();
        if (inv == null || inv.getComponents() == null) return;

        LOG.info("BlockGenerationAgent generating blocks for {} components", inv.getComponents().size());

        for (SiteInventory.ComponentInfo comp : inv.getComponents()) {
            String blockName = comp.getProposedEdsBlock() != null
                    ? comp.getProposedEdsBlock()
                    : comp.getResourceType().substring(comp.getResourceType().lastIndexOf('/') + 1);

            String jsContent = "export default function decorate(block) {\n"
                    + "  const cols = [...block.firstElementChild.children];\n"
                    + "  block.classList.add(`" + blockName + "-${cols.length}-cols`);\n"
                    + "}\n";

            if (ai != null) {
                ChatRequest req = new ChatRequest(getName(), "Generate EDS decorate() function for block: " + blockName);
                req.setTargetCapability(ModelCapability.CAP_CODE);
                ChatResponse resp = ai.dispatch(req);
                if (resp.getContent() != null && !resp.getContent().trim().isEmpty()) {
                    jsContent = resp.getContent();
                }
            }

            GeneratedFileRecord file = new GeneratedFileRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    "blocks/" + blockName + "/" + blockName + ".js",
                    "BLOCK_JS",
                    jsContent
            );
            file.setVirtualDiffOnly(ctx.isDryRun());

            if (store != null) {
                store.saveGeneratedFile(file);
            }
        }

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Generated JavaScript decoration logic for " + inv.getComponents().size() + " EDS blocks."
            ));
        }
    }
}
