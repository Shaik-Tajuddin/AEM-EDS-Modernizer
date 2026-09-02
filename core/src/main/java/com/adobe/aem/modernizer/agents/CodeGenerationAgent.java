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
    private final com.adobe.aem.modernizer.connectors.LocalEdsRepoManager edsRepo;

    public CodeGenerationAgent(Store store, AiGateway ai) {
        this(store, ai, null);
    }

    public CodeGenerationAgent(Store store, AiGateway ai,
            com.adobe.aem.modernizer.connectors.LocalEdsRepoManager edsRepo) {
        this.store = store;
        this.ai = ai;
        this.edsRepo = edsRepo;
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
    public void execute(AgentContext ctx) throws com.adobe.aem.modernizer.ModernizerException {
        SiteInventory inv = ctx.getInventory();
        if (inv == null || inv.getComponents() == null) return;

        LOG.info("CodeGenerationAgent generating styles and configuration");

        int aiCallCount = 0;
        for (SiteInventory.ComponentInfo comp : inv.getComponents()) {
            if (comp.getResourceType() != null && (comp.getResourceType().contains("/components/container")
                    || comp.getResourceType().contains("/components/page")
                    || comp.getResourceType().endsWith("/container")
                    || comp.getResourceType().endsWith("/page"))) {
                LOG.info("Skipping styles/code generation for container/page component: {}", comp.getResourceType());
                continue;
            }
            String blockName = comp.getProposedEdsBlock() != null
                    ? comp.getProposedEdsBlock().toLowerCase().replace(' ', '-')
                    : comp.getResourceType().substring(comp.getResourceType().lastIndexOf('/') + 1).toLowerCase().replace(' ', '-');

            String cssContent = "/* " + blockName + " — scoped mobile-first styles */\n"
                    + "." + blockName + " {\n"
                    + "  display: block;\n"
                    + "  padding: var(--space-s, 16px);\n"
                    + "  background: var(--color-base-background, #ffffff);\n"
                    + "  color: var(--color-base-text, #1e293b);\n"
                    + "  border-radius: 8px;\n"
                    + "}\n\n"
                    + "." + blockName + "-inner {\n"
                    + "  display: flex;\n"
                    + "  flex-direction: column;\n"
                    + "  gap: var(--space-s, 14px);\n"
                    + "}\n\n"
                    + "." + blockName + "-title h2,\n"
                    + "." + blockName + "-title p {\n"
                    + "  font-size: 1.5rem;\n"
                    + "  font-weight: 700;\n"
                    + "  color: var(--color-heading, #0f172a);\n"
                    + "  line-height: 1.25;\n"
                    + "}\n\n"
                    + "." + blockName + "-text p {\n"
                    + "  font-size: 1rem;\n"
                    + "  line-height: 1.6;\n"
                    + "  color: var(--color-text-secondary, #475569);\n"
                    + "}\n\n"
                    + "." + blockName + "-cta .brand-cta {\n"
                    + "  display: inline-flex;\n"
                    + "  align-items: center;\n"
                    + "  gap: 8px;\n"
                    + "  padding: 10px 20px;\n"
                    + "  background: var(--color-primary, #38bdf8);\n"
                    + "  color: #090d16;\n"
                    + "  text-decoration: none;\n"
                    + "  border-radius: 6px;\n"
                    + "  font-weight: 600;\n"
                    + "  font-size: 0.9rem;\n"
                    + "  transition: all 0.2s ease;\n"
                    + "}\n\n"
                    + "." + blockName + "-cta .brand-cta:hover {\n"
                    + "  background: var(--color-primary-hover, #0ea5e9);\n"
                    + "  transform: translateY(-1px);\n"
                    + "}\n\n"
                    + "." + blockName + "." + blockName + "-align-center {\n"
                    + "  text-align: center;\n"
                    + "}\n"
                    + "." + blockName + "." + blockName + "-align-center ." + blockName + "-cta {\n"
                    + "  display: flex;\n"
                    + "  justify-content: center;\n"
                    + "}\n\n"
                    + "." + blockName + "." + blockName + "-align-right {\n"
                    + "  text-align: right;\n"
                    + "}\n"
                    + "." + blockName + "." + blockName + "-align-right ." + blockName + "-cta {\n"
                    + "  display: flex;\n"
                    + "  justify-content: flex-end;\n"
                    + "}\n\n"
                    + "." + blockName + "." + blockName + "-tone-emphasis {\n"
                    + "  background-color: var(--color-surface-emphasis, #f0f9ff);\n"
                    + "  border: 1px solid rgba(56, 189, 248, 0.35);\n"
                    + "}\n\n"
                    // Authorable cssClass / blockId variant selectors — content-driven page styling
                    + "/* Authorable page-instance overrides (cssClass & blockId from UE model) */\n"
                    + "." + blockName + ".dark-tone {\n"
                    + "  background: #0f172a;\n"
                    + "  color: #e2e8f0;\n"
                    + "}\n\n"
                    + "." + blockName + ".compact {\n"
                    + "  padding: 8px;\n"
                    + "  margin: 12px auto;\n"
                    + "}\n\n"
                    + "/* #<blockId> page-specific anchor overrides */\n"
                    + "." + blockName + "[id] {\n"
                    + "  scroll-margin-top: 96px;\n"
                    + "}\n\n"
                    + "@media (min-width: 768px) {\n"
                    + "  ." + blockName + " {\n"
                    + "    padding: var(--space-m, 24px);\n"
                    + "  }\n"
                    + "}\n\n"
                    + "@media (prefers-reduced-motion: reduce) {\n"
                    + "  ." + blockName + " * {\n"
                    + "    transition: none !important;\n"
                    + "  }\n"
                    + "}\n\n"
                    + "@media print {\n"
                    + "  ." + blockName + " {\n"
                    + "    page-break-inside: avoid;\n"
                    + "  }\n"
                    + "}\n";

            if (ai != null && aiCallCount++ < 1) {
                ChatRequest req = new ChatRequest(getName(), "Generate CSS styles for EDS block: " + blockName);
                req.setTargetCapability(ModelCapability.CAP_CODE);
                req.setPreferredProvider(ctx.getProject().getAiProvider());
                req.setPreferredModel(ctx.getProject().getAiModel());
                req.setProjectId(ctx.getProject().getId());
                req.setJobId(ctx.getJob().getId());
                try {
                    ChatResponse resp = ai.dispatch(req);
                    if (resp.getContent() != null && resp.getContent().contains("." + blockName)) {
                        cssContent = resp.getContent();
                    }
                } catch (Exception e) {
                    LOG.debug("AI CSS generation fallback for {}: {}", blockName, e.getMessage());
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

            if (!ctx.isDryRun()) {
                if (edsRepo != null) {
                    // Direct output: eds/<projectId>/blocks/<blockName>/<blockName>.css
                    edsRepo.writeProjectFile(ctx.getProject().getId(),
                            "blocks/" + blockName + "/" + blockName + ".css", cssContent);
                } else {
                    writeLocalFile(ctx, "blocks/" + blockName + "/" + blockName + ".css", cssContent);
                }
            }
        }

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Generated CSS stylesheets. Existing fstab.yaml was left unchanged."
            ));
        }
    }

    private void writeLocalFile(AgentContext ctx, String relPath, String content) {
        String projectId = (ctx != null && ctx.getProject() != null) ? ctx.getProject().getId() : "project";
        java.io.File target = new java.io.File(new java.io.File("D:/eds personal/AEM-EDS-Modernizer/eds", projectId), relPath);
        try {
            target.getParentFile().mkdirs();
            java.nio.file.Files.writeString(target.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
            LOG.info("Wrote local block CSS file in workspace: {}", target.getAbsolutePath());
        } catch (Exception e) {
            LOG.warn("Could not write local block CSS file {}: {}", relPath, e.getMessage());
        }
    }
}
