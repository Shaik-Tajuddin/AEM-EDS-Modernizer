package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.ModelCapability;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.DependencyEdgeRecord;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import com.adobe.aem.modernizer.persistence.model.UrlRedirectRecord;
import com.adobe.aem.modernizer.services.DependencyGraphService;
import com.adobe.aem.modernizer.services.UrlRedirectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Transforms AEM pages to EDS Markdown section models, builds redirects and dependencies (Stage: MIGRATING).
 */
public class ContentMigrationAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(ContentMigrationAgent.class);

    private final Store store;
    private final AiGateway ai;
    private final UrlRedirectService urlRedirectService = new UrlRedirectService();
    private final DependencyGraphService dependencyGraphService = new DependencyGraphService();

    public ContentMigrationAgent(Store store, AiGateway ai) {
        this.store = store;
        this.ai = ai;
    }

    @Override
    public String getName() {
        return "content-migration";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.MIGRATING;
    }

    @Override
    public void execute(AgentContext ctx) throws Exception {
        SiteInventory inv = ctx.getInventory();
        if (inv == null || inv.getPages() == null) return;

        LOG.info("ContentMigrationAgent migrating {} pages to EDS Markdown", inv.getPages().size());

        for (SiteInventory.PageInfo page : inv.getPages()) {
            if (!page.isEligible()) continue;

            String edsPath = UrlRedirectService.transformToEdsPath(page.getPath());
            String filePath = edsPath.startsWith("/") ? edsPath.substring(1) : edsPath;
            if (filePath.isEmpty()) filePath = "index";
            filePath += ".md";

            String markdown = "# " + page.getTitle() + "\n\n"
                    + "Welcome to " + page.getTitle() + " on Edge Delivery Services.\n\n"
                    + "### Hero\n| Image | Heading | Text |\n| --- | --- | --- |\n"
                    + "| /content/dam/wknd/hero.jpg | " + page.getTitle() + " | Explore the story |\n";

            if (ai != null) {
                ChatRequest req = new ChatRequest(getName(), "Convert AEM page " + page.getPath() + " to EDS Section Markdown");
                req.setTargetCapability(ModelCapability.CAP_CODE);
                ChatResponse resp = ai.dispatch(req);
                if (resp.getContent() != null && !resp.getContent().trim().isEmpty()) {
                    markdown = resp.getContent();
                }
            }

            GeneratedFileRecord file = new GeneratedFileRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    filePath,
                    "SECTION_MD",
                    markdown
            );
            file.setSourcePath(page.getPath());
            file.setVirtualDiffOnly(ctx.isDryRun());

            if (store != null) {
                store.saveGeneratedFile(file);
            }
        }

        // Build URL Redirects & Dependency Graph
        List<UrlRedirectRecord> redirects = urlRedirectService.buildRedirects(ctx.getProject().getId(), ctx.getJob().getId(), inv);
        List<DependencyEdgeRecord> edges = dependencyGraphService.buildGraph(ctx.getProject().getId(), ctx.getJob().getId(), inv);

        if (store != null) {
            for (UrlRedirectRecord r : redirects) store.saveUrlRedirect(r);
            for (DependencyEdgeRecord e : edges) store.saveDependencyEdge(e);

            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Migrated " + inv.getPages().size() + " pages to Markdown; recorded "
                            + redirects.size() + " URL redirects and " + edges.size() + " dependency edges."
            ));
        }
    }
}
