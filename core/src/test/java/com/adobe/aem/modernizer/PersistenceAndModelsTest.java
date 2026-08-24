package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.mock.MockDataFactory;
import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.model.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceAndModelsTest {

    @Test
    void testInMemoryStoreAllOperations() {
        InMemoryStore store = new InMemoryStore();

        // Project
        ProjectRecord proj = new ProjectRecord("p-1", "WKND Site", "http://author", "/content/wknd", "https://github.com/org/repo");
        proj.setCreatedAt(1000L);
        proj.setUpdatedAt(2000L);
        proj.setFigmaUrl("https://figma.com/file/123");
        proj.setProperties(Collections.singletonMap("env", "prod"));
        assertThat(proj.getProperties()).containsEntry("env", "prod");

        store.saveProject(proj);
        assertThat(store.getProject("p-1")).isPresent();
        assertThat(store.listProjects()).hasSize(1);

        // Job
        JobRecord job = new JobRecord("j-1", "p-1", "MIGRATE");
        job.setState("PLANNING");
        job.setActor("admin");
        job.setStartedAt(1000L);
        job.setFinishedAt(2000L);
        job.setLastError(null);
        job.setMetadata(Collections.singletonMap("metaKey", "metaVal"));
        assertThat(job.getMetadata()).containsEntry("metaKey", "metaVal");

        store.saveJob(job);
        assertThat(store.getJob("j-1")).isPresent();
        assertThat(store.listJobs("p-1")).hasSize(1);
        assertThat(store.getLatestJob("p-1")).isPresent();

        // Inventory
        SiteInventory inv = MockDataFactory.createWkndInventory("/content/wknd", null, 5);
        inv.setJobId("j-1");
        store.saveInventory(inv);
        assertThat(store.getInventory(inv.getJobId())).isPresent();

        // MigrationPlan
        MigrationPlan plan = new MigrationPlan();
        plan.setProjectId("p-1");
        plan.setJobId("j-1");
        plan.setPagesEligible(10);
        plan.setEdsBlocksNew(5);
        plan.setCostExpected(120.0);
        plan.setDerivationTrail(Collections.singletonList("rule1"));
        plan.setBlockers(Collections.singletonList("blocker1"));
        plan.setWarnings(Collections.singletonList("warn1"));
        plan.setDetails(Collections.singletonMap("k", "v"));
        store.savePlan(plan);
        assertThat(store.getPlan("j-1")).isPresent();
        assertThat(store.getLatestPlan("p-1")).isPresent();
        assertThat(store.getPlan("j-1").get().getDerivationTrail()).contains("rule1");
        assertThat(store.getPlan("j-1").get().getBlockers()).contains("blocker1");
        assertThat(store.getPlan("j-1").get().getWarnings()).contains("warn1");
        assertThat(store.getPlan("j-1").get().getDetails()).containsEntry("k", "v");

        // Checkpoint
        CheckpointRecord cp = new CheckpointRecord("cp-1", "p-1", "j-1", "DISCOVERING", "resumeHint");
        cp.setStateData(Collections.singletonMap("pages", "5"));
        store.saveCheckpoint(cp);
        assertThat(store.getLatestCheckpoint("j-1")).isPresent();
        assertThat(cp.getStateData()).containsEntry("pages", "5");

        // JobEvent
        JobEventRecord event = new JobEventRecord("ev-1", "p-1", "j-1", "DISCOVERING", "Moved to analyzing");
        store.recordEvent(event);
        assertThat(store.getEvents("j-1")).hasSize(1);
        assertThat(store.getEventsForProject("p-1")).isNotEmpty();

        // GeneratedFile
        GeneratedFileRecord gen = new GeneratedFileRecord("f-1", "p-1", "j-1", "blocks/teaser/teaser.js", "BLOCK_JS", "export default function() {}");
        store.saveGeneratedFile(gen);
        assertThat(store.getGeneratedFiles("j-1")).hasSize(1);
        assertThat(store.getGeneratedFile("j-1", "blocks/teaser/teaser.js")).isPresent();

        // ValidationResult
        ValidationResultRecord vr = new ValidationResultRecord("vr-1", "p-1", "j-1", "/content/wknd/us/en", "VISUAL", true);
        vr.setIssues(Collections.singletonList("minor-issue"));
        store.saveValidationResult(vr);
        assertThat(store.getValidationResults("j-1")).hasSize(1);
        assertThat(vr.getIssues()).contains("minor-issue");

        // RepairAttempt
        RepairAttemptRecord rep = new RepairAttemptRecord("rep-1", "p-1", "j-1", "teaser", 1, "MISSING_CSS");
        store.saveRepairAttempt(rep);
        assertThat(store.getRepairAttempts("j-1")).hasSize(1);
        assertThat(store.getRepairAttemptsForProject("p-1")).hasSize(1);

        // RolloutStage
        RolloutStageRecord stage = new RolloutStageRecord("st-1", "p-1", "j-1", 1, "Canary", 10);
        store.saveRolloutStage(stage);
        assertThat(store.getRolloutStages("j-1")).hasSize(1);
        assertThat(store.getLatestRolloutStages("p-1")).hasSize(1);

        // Benchmark
        BenchmarkSampleRecord sample = new BenchmarkSampleRecord("bm-1", "p-1", "j-1", "connection", "test", 50L, 0.0);
        sample.setPromptTokens(10);
        sample.setCompletionTokens(20);
        store.saveBenchmarkSample(sample);
        assertThat(store.getBenchmarkSamples("j-1")).hasSize(1);
        assertThat(store.getBenchmarkSamplesForProject("p-1")).hasSize(1);
        assertThat(store.getBenchmarkSamplesForAgent("connection")).hasSize(1);

        // Clarification
        ClarificationRecord clar = new ClarificationRecord("cl-1", "p-1", "j-1", "Clarify block", "Opt1");
        clar.setRationale("Why");
        clar.setOptions(Collections.singletonList("Opt2"));
        clar.setAffectedPages(Collections.singletonList("/page2"));
        clar.setSelectedOption("Opt2");
        clar.setStatus("RESOLVED");
        clar.setResolvedAt(2000L);
        store.saveClarification(clar);
        assertThat(store.getClarifications("j-1")).hasSize(1);
        assertThat(store.getClarificationsForProject("p-1")).hasSize(1);

        // Contract
        MigrationContractRecord contract = new MigrationContractRecord("mc-1", "p-1", "j-1");
        contract.setApprovedScope("/content/wknd");
        contract.setAuthoringStrategy("UNIVERSAL_EDITOR");
        contract.setAssetPolicy("METADATA_ONLY");
        contract.setAiProvider("anthropic");
        contract.setAiModel("claude-3-5-sonnet");
        contract.setApprovedBudgetUsd(50.0);
        contract.setMaxRepairAttempts(5);
        contract.setApprovedByOperator(true);
        contract.setApprovedBy("admin");
        contract.setApprovedAt(1000L);
        contract.setAcceptedRisks(Collections.singletonList("Risk2"));
        store.saveContract(contract);
        assertThat(store.getContract("j-1")).isPresent();
        assertThat(contract.getAcceptedRisks()).contains("Risk2");

        // Redirects & Dependencies
        UrlRedirectRecord red = new UrlRedirectRecord("rd-1", "p-1", "j-1", "/content/wknd/us/en.html", "https://main--wknd--hlx.live/us/en");
        red.setStatusCode(301);
        red.setRedirectType("PAGE_PATH_CHANGE");
        red.setConflict(false);
        red.setConflictReason(null);
        store.saveUrlRedirect(red);
        assertThat(store.getUrlRedirects("j-1")).hasSize(1);
        assertThat(store.getUrlRedirectsForProject("p-1")).hasSize(1);

        DependencyEdgeRecord edge = new DependencyEdgeRecord("de-1", "p-1", "j-1", "/content/wknd/us/en", "/content/dam/wknd/hero.jpg", "ASSET_REFERENCE");
        edge.setImpactLevel("LOW");
        store.saveDependencyEdge(edge);
        assertThat(store.getDependencyEdges("j-1")).hasSize(1);
        assertThat(store.getDependencyEdgesForProject("p-1")).hasSize(1);

        // Delete project
        store.deleteProject("p-1");
        assertThat(store.getProject("p-1")).isEmpty();
    }

    @Test
    void testSiteInventoryInnerClasses() {
        SiteInventory.PageInfo page = new SiteInventory.PageInfo("/content/wknd/us/en", "Home", "/apps/wknd/template");
        page.setComponentResourceTypes(Arrays.asList("core/wcm/components/text/v1/text"));
        page.setAssetPaths(Arrays.asList("/content/dam/wknd/hero.jpg"));
        page.setEligible(true);
        page.setExclusionReason(null);
        assertThat(page.getPath()).isEqualTo("/content/wknd/us/en");
        assertThat(page.getTitle()).isEqualTo("Home");
        assertThat(page.getTemplate()).isEqualTo("/apps/wknd/template");
        assertThat(page.getComponentResourceTypes()).contains("core/wcm/components/text/v1/text");
        assertThat(page.getAssetPaths()).contains("/content/dam/wknd/hero.jpg");
        assertThat(page.isEligible()).isTrue();

        SiteInventory.ComponentInfo comp = new SiteInventory.ComponentInfo("core/wcm/components/teaser/v1/teaser", "Teaser", "General");
        comp.setOccurrenceCount(10);
        comp.setProposedEdsBlock("teaser");
        comp.setCapabilityClassification("SUPPORTED");
        assertThat(comp.getResourceType()).isEqualTo("core/wcm/components/teaser/v1/teaser");
        assertThat(comp.getTitle()).isEqualTo("Teaser");
        assertThat(comp.getGroup()).isEqualTo("General");
        assertThat(comp.getOccurrenceCount()).isEqualTo(10);
        assertThat(comp.getProposedEdsBlock()).isEqualTo("teaser");
        assertThat(comp.getCapabilityClassification()).isEqualTo("SUPPORTED");

        SiteInventory.TemplateInfo tpl = new SiteInventory.TemplateInfo("/conf/wknd/settings/wcm/templates/landing", "Landing Page");
        tpl.setAllowedComponents(Collections.singletonList("text"));
        assertThat(tpl.getPath()).isEqualTo("/conf/wknd/settings/wcm/templates/landing");
        assertThat(tpl.getTitle()).isEqualTo("Landing Page");
        assertThat(tpl.getAllowedComponents()).contains("text");

        SiteInventory.AssetInfo asset = new SiteInventory.AssetInfo("/content/dam/wknd/hero.jpg", "image/jpeg");
        asset.setResolvable(true);
        assertThat(asset.getPath()).isEqualTo("/content/dam/wknd/hero.jpg");
        assertThat(asset.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset.isResolvable()).isTrue();

        SiteInventory.ContentFragmentInfo cf = new SiteInventory.ContentFragmentInfo("/content/dam/wknd/cf/article", "/conf/wknd/models/article", "Article CF");
        assertThat(cf.getPath()).isEqualTo("/content/dam/wknd/cf/article");
        assertThat(cf.getModel()).isEqualTo("/conf/wknd/models/article");
        assertThat(cf.getTitle()).isEqualTo("Article CF");

        SiteInventory.MsmLiveCopyInfo msm = new SiteInventory.MsmLiveCopyInfo("/content/wknd/language-masters/en", "/content/wknd/ca/en");
        msm.setRollOutConfig("/etc/msm/rollout-config/standard");
        assertThat(msm.getSourcePath()).isEqualTo("/content/wknd/language-masters/en");
        assertThat(msm.getLiveCopyPath()).isEqualTo("/content/wknd/ca/en");
        assertThat(msm.getRollOutConfig()).isEqualTo("/etc/msm/rollout-config/standard");
    }
}
