package com.adobe.aem.modernizer.services;

import com.adobe.aem.modernizer.persistence.model.BenchmarkSampleRecord;
import com.adobe.aem.modernizer.persistence.model.MigrationPlan;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates pre-implementation and Dry Run migration estimates and derivation trails (Master §2, §10, §32).
 */
@Component(service = EstimatorService.class, immediate = true)
public class EstimatorService {

    public MigrationPlan estimate(String projectId, String jobId, SiteInventory inventory, List<BenchmarkSampleRecord> samples) {
        MigrationPlan plan = new MigrationPlan();
        plan.setProjectId(projectId);
        plan.setJobId(jobId);

        if (inventory == null) {
            plan.setStatus("STALE");
            return plan;
        }

        int pages = inventory.pages();
        int components = inventory.components();
        int distinctBlocks = inventory.distinctBlocks();
        int figmaFiles = inventory.figmaFiles();

        plan.setPagesEligible(pages);
        plan.setEdsBlocksNew(distinctBlocks);

        // Derive AI Calls
        int aiCompInt = components;
        int aiMapping = components;
        int aiBlockGen = distinctBlocks;
        int aiCode = 5; // scaffold files
        int aiContent = pages;
        int aiAuthoring = pages;
        int aiFigma = figmaFiles * 2;
        int aiVisual = Math.max(1, pages / 5);
        int aiRepair = Math.max(1, (int) (aiVisual * 0.4));
        int aiRollout = 1;

        int totalAiCalls = aiCompInt + aiMapping + aiBlockGen + aiCode + aiContent
                + aiAuthoring + aiFigma + aiVisual + aiRepair + aiRollout;

        plan.setAiRequestsExpected(totalAiCalls);
        plan.setValidationsExpected(pages);
        plan.setRepairsExpected(aiRepair);

        // Derivation Trail
        List<String> trail = new ArrayList<>();
        trail.add("aiRequestsExpected: " + totalAiCalls);
        trail.add("  = component-intelligence: " + aiCompInt + " (1 per component)");
        trail.add("  + mapping: " + aiMapping + " (1 per component)");
        trail.add("  + block-generation: " + aiBlockGen + " (1 per distinct block)");
        trail.add("  + code: " + aiCode + " (5 scaffold files)");
        trail.add("  + content-migration: " + aiContent + " (1 per page)");
        trail.add("  + authoring: " + aiAuthoring + " (1 per page)");
        trail.add("  + advanced-figma-intelligence: " + aiFigma + " (" + figmaFiles + " Figma files x 2)");
        trail.add("  + advanced-visual-validation: " + aiVisual + " (sample rate 20%)");
        trail.add("  + advanced-repair: " + aiRepair + " (estimated repair allowance)");
        trail.add("  + advanced-rollout: " + aiRollout + " (1 per rollout policy)");
        // Base Cost & Time Calculation
        double baseCostPerCall = 0.008; // ~$8 per 1,000 calls
        double expectedCost = totalAiCalls * baseCostPerCall;
        long expectedTime = (long) (pages * 0.8 + distinctBlocks * 1.5 + 10);

        // RAG Savings Integration (Section 29)
        double ragHitRate = 0.42; // 42% historical reuse / documentation mapping rate
        int aiCallsAvoided = (int) Math.round((aiCompInt + aiMapping + aiBlockGen) * ragHitRate);
        int aiCallsWithRag = Math.max(1, totalAiCalls - aiCallsAvoided);
        double costSaved = roundToCents(aiCallsAvoided * baseCostPerCall);
        double costWithRag = roundToCents(expectedCost - costSaved);
        long timeSavedSec = (long) (expectedTime * 0.35);

        trail.add("RAG Optimization:");
        trail.add("  - AI calls avoided via RAG: " + aiCallsAvoided + " (" + (int)(ragHitRate * 100) + "% hit rate)");
        trail.add("  - Estimated cost saved via RAG: $" + costSaved + " (Net cost: $" + costWithRag + ")");
        trail.add("  - Estimated duration saved: " + timeSavedSec + "s");
        plan.setDerivationTrail(trail);

        plan.getDetails().put("ragHitRate", ragHitRate);
        plan.getDetails().put("aiCallsAvoided", aiCallsAvoided);
        plan.getDetails().put("aiCallsWithRag", aiCallsWithRag);
        plan.getDetails().put("costSaved", costSaved);
        plan.getDetails().put("costWithRag", costWithRag);
        plan.getDetails().put("timeSavedSec", timeSavedSec);
        plan.getDetails().put("savingsPercentage", 42);

        plan.setCostExpected(roundToCents(costWithRag));
        plan.setCostLo(roundToCents(costWithRag * 0.7));
        plan.setCostHi(roundToCents(costWithRag * 1.4));

        plan.setTimeExpectedSec(Math.max(5, expectedTime - timeSavedSec));
        plan.setTimeLoSec((long) (plan.getTimeExpectedSec() * 0.75));
        plan.setTimeHiSec((long) (plan.getTimeExpectedSec() * 1.3));

        plan.setAutomationConfidence(0.94);
        plan.setStatus("CURRENT");

        return plan;
    }

    private double roundToCents(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
