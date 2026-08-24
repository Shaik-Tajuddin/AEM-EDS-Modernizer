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
 * Generates EDS CSS stylesheets and repository configuration files (Stage: BUILDING).
 */
public class CodeGenerationAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(CodeGenerationAgent.class);

    private final Store store;
    private final AiGateway ai;

    public CodeGenerationAgent(Store store, AiGateway ai) {
        this.store = store;
        this.ai = ai;
    }

    @Override
    public String getName() {
        return "code-generation";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.BUILDING;
    }

    @Override
    public void execute(AgentContext ctx) throws Exception {
        SiteInventory inv = ctx.getInventory();
        if (inv == null || inv.getComponents() == null) return;

        LOG.info("CodeGenerationAgent generating styles and configuration");

        for (SiteInventory.ComponentInfo comp : inv.getComponents()) {
            String blockName = comp.getProposedEdsBlock() != null
                    ? comp.getProposedEdsBlock()
                    : comp.getResourceType().substring(comp.getResourceType().lastIndexOf('/') + 1);

            String cssContent = "." + blockName + " {\n"
                    + "  display: block;\n"
                    + "  padding: var(--spacing-unit, 16px);\n"
                    + "  background: var(--color-bg, #ffffff);\n"
                    + "  color: var(--color-text, #222222);\n"
                    + "}\n";

            if (ai != null) {
                ChatRequest req = new ChatRequest(getName(), "Generate CSS styles for EDS block: " + blockName);
                req.setTargetCapability(ModelCapability.CAP_CODE);
                ChatResponse resp = ai.dispatch(req);
                if (resp.getContent() != null && !resp.getContent().trim().isEmpty()) {
                    cssContent = resp.getContent();
                }
            }

            GeneratedFileRecord file = new GeneratedFileRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    "blocks/" + blockName + "/" + blockName + ".css",
                    "BLOCK_CSS",
                    cssContent
            );
            file.setVirtualDiffOnly(ctx.isDryRun());

            if (store != null) {
                store.saveGeneratedFile(file);
            }
        }

        // Global styles & fstab.yaml
        String fstab = "mountpoints:\n  /: " + ctx.getProject().getAemAuthorUrl() + "/bin/aem-eds-modernizer/eds-delivery\n";
        GeneratedFileRecord fstabFile = new GeneratedFileRecord(
                UUID.randomUUID().toString(),
                ctx.getProject().getId(),
                ctx.getJob().getId(),
                "fstab.yaml",
                "CONFIG",
                fstab
        );
        fstabFile.setVirtualDiffOnly(ctx.isDryRun());
        if (store != null) {
            store.saveGeneratedFile(fstabFile);
        }

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Generated CSS stylesheets and fstab.yaml configuration files."
            ));
        }
    }
}
