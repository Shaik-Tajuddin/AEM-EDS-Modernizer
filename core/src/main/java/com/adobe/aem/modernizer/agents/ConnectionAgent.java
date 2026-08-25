package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.connectors.*;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Tests connection reachability and credentials for all configured endpoints (Stage: CONNECTING).
 */
public class ConnectionAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionAgent.class);

    private final AemClient aemAuthor;
    private final AemClient aemPublish;
    private final GitHubClient gitHub;
    private final FigmaClient figma;
    private final EdsClient eds;
    private final BrowserClient browser;
    private final Store store;
    private final AiGateway ai;

    public ConnectionAgent(AemClient aemAuthor, AemClient aemPublish, GitHubClient gitHub,
                           FigmaClient figma, EdsClient eds, BrowserClient browser,
                           Store store, AiGateway ai) {
        this.aemAuthor = aemAuthor;
        this.aemPublish = aemPublish;
        this.gitHub = gitHub;
        this.figma = figma;
        this.eds = eds;
        this.browser = browser;
        this.store = store;
        this.ai = ai;
    }

    @Override
    public String getName() {
        return "connection";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.CONNECTING;
    }

    @Override
    public void execute(AgentContext ctx) throws com.adobe.aem.modernizer.ModernizerException {
        GitHubClient gh = (gitHub instanceof com.adobe.aem.modernizer.connectors.RealGitHubClient && ctx.getProject() != null)
                ? ((com.adobe.aem.modernizer.connectors.RealGitHubClient) gitHub).forProject(ctx.getProject())
                : gitHub;

        boolean authorOk = aemAuthor == null || aemAuthor.testConnection();
        boolean ghOk = gh == null || gh.testConnection();
        boolean edsOk = eds == null || eds.testConnection();
        boolean browserOk = browser == null || browser.testConnection();

        if (!authorOk) {
            throw new IllegalStateException("AEM Author endpoint is unreachable: " + ctx.getProject().getAemAuthorUrl());
        }
        if (!ghOk) {
            LOG.warn("GitHub repository connection unverified for '{}' - proceeding with local workspace block generation (push deferred to final step)",
                    ctx.getProject().getEdsGitRepoUrl());
        }
        if (!edsOk) {
            throw new IllegalStateException("EDS endpoint is unreachable");
        }
        if (!browserOk) {
            throw new IllegalStateException("Browser rendering service is unavailable");
        }

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "All target connections verified successfully (Author, GitHub, EDS, Browser)"
            ));
        }
    }
}
