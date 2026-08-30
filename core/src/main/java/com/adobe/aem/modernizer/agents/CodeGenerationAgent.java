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
                writeLocalFile("blocks/" + blockName + "/" + blockName + ".css", cssContent);
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

    private void writeLocalFile(String relPath, String content) {
        try {
            java.io.File target = null;
            String[] candidateRoots = new String[] {
                "D:/eds personal/AEM-EDS-Modernizer",
                "d:/eds personal/AEM-EDS-Modernizer",
                System.getProperty("user.dir")
            };
            for (String root : candidateRoots) {
                java.io.File dir = new java.io.File(root);
                if (new java.io.File(dir, "pom.xml").exists() || new java.io.File(dir, "blocks").exists()) {
                    target = new java.io.File(dir, relPath);
                    break;
                }
            }
            if (target == null) {
                target = new java.io.File("D:/eds personal/AEM-EDS-Modernizer", relPath);
            }
            java.io.File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            java.nio.file.Files.writeString(target.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
            LOG.info("Wrote local block file: {}", target.getAbsolutePath());
        } catch (Exception e) {
            LOG.warn("Could not write local block file {}: {}", relPath, e.getMessage());
        }
    }
}
