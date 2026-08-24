package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.BenchmarkSampleRecord;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.MigrationPlan;
import com.adobe.aem.modernizer.services.EstimatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Builds the Migration Plan, derivation trail, and pre-implementation estimate (Stage: PLANNING).
 */
public class MigrationPlannerAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationPlannerAgent.class);

    private final Store store;
    private final AiGateway ai;
    private final EstimatorService estimator;

    public MigrationPlannerAgent(Store store, AiGateway ai, EstimatorService estimator) {
        this.store = store;
        this.ai = ai;
        this.estimator = estimator;
    }

    @Override
    public String getName() {
        return "migration-planner";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.PLANNING;
    }

    @Override
    public void execute(AgentContext ctx) throws Exception {
        LOG.info("MigrationPlannerAgent computing estimate and migration plan");

        List<BenchmarkSampleRecord> samples = (store != null)
                ? store.getBenchmarkSamplesForProject(ctx.getProject().getId()) : null;

        MigrationPlan plan = estimator.estimate(
                ctx.getProject().getId(),
                ctx.getJob().getId(),
                ctx.getInventory(),
                samples
        );

        ctx.setPlan(plan);
        if (store != null) {
            store.savePlan(plan);
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Built Migration Plan: " + plan.getPagesEligible() + " pages, "
                            + plan.getEdsBlocksNew() + " blocks, " + plan.getAiRequestsExpected()
                            + " AI calls, Expected cost: $" + plan.getCostExpected()
                            + ", Expected duration: " + plan.getTimeExpectedSec() + "s."
            ));
        }
    }
}
