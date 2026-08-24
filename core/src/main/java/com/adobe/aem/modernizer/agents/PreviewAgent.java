package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.connectors.EdsClient;
import com.adobe.aem.modernizer.connectors.GitHubClient;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Pushes generated files to the preview branch and activates EDS preview (Stage: PREVIEWING).
 */
public class PreviewAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(PreviewAgent.class);

    private final GitHubClient gitHub;
    private final EdsClient eds;
    private final Store store;
    private final AiGateway ai;

    public PreviewAgent(GitHubClient gitHub, EdsClient eds, Store store, AiGateway ai) {
        this.gitHub = gitHub;
        this.eds = eds;
        this.store = store;
        this.ai = ai;
    }

    @Override
    public String getName() {
        return "preview";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.PREVIEWING;
    }

    @Override
    public void execute(AgentContext ctx) throws Exception {
        if (ctx.isDryRun()) {
            LOG.info("PreviewAgent skipped real deploy during Dry Run");
            if (store != null) {
                store.recordEvent(new JobEventRecord(
                        UUID.randomUUID().toString(),
                        ctx.getProject().getId(),
                        ctx.getJob().getId(),
                        getName(),
                        "Dry Run preview calculated: virtual branch prepared (0 commits pushed)."
                ));
            }
            return;
        }

        String branch = "modernizer/" + ctx.getProject().getId() + "/" + ctx.getJob().getId();
        LOG.info("PreviewAgent pushing generated files to branch: {}", branch);

        if (gitHub != null) {
            gitHub.createBranch(branch);
            List<GeneratedFileRecord> files = (store != null) ? store.getGeneratedFiles(ctx.getJob().getId()) : null;
            if (files != null && !files.isEmpty()) {
                gitHub.commitFiles(branch, files, "feat: modernizer automated migration preview");
            }
        }

        String previewUrl = (eds != null) ? eds.getPreviewUrl(branch, "/index") : "https://eds-mock.local/preview/" + branch;

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Activated EDS preview environment at: " + previewUrl
            ));
        }
    }
}
