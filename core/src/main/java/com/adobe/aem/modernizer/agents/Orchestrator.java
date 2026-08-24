package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.*;
import com.adobe.aem.modernizer.services.EstimatorService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic Orchestrator executing the migration state machine and agent graph (Master §5, §33, ADR 0013).
 */
@Component(service = Orchestrator.class, immediate = true)
public class Orchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(Orchestrator.class);

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private Store store;
    private AiGateway ai;
    private EstimatorService estimator;

    @Reference private transient Store storeRef;
    @Reference private transient AiGateway aiRef;
    @Reference private transient EstimatorService estimatorRef;

    public Orchestrator() {}

    public Orchestrator(Store store, AiGateway ai, EstimatorService estimator) {
        this.store = store;
        this.ai = ai;
        this.estimator = estimator;
    }

    @Activate
    public void activate() {
        if (this.store == null && this.storeRef != null) this.store = this.storeRef;
        if (this.ai == null && this.aiRef != null) this.ai = this.aiRef;
        if (this.estimator == null && this.estimatorRef != null) this.estimator = this.estimatorRef;
        LOG.info("Orchestrator activated with {} registered agents", agents.size());
    }

    public void register(Agent agent) {
        if (agent != null) {
            agents.put(agent.getName().toLowerCase(), agent);
        }
    }

    public void registerCoreAgents(Agent... coreAgents) {
        if (coreAgents != null) {
            for (Agent a : coreAgents) {
                register(a);
            }
        }
    }

    public Optional<Agent> getAgent(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(agents.get(name.toLowerCase()));
    }

    public void invokeIfRegistered(String name, AgentContext ctx) throws Exception {
        Optional<Agent> agentOpt = getAgent(name);
        if (agentOpt.isPresent()) {
            agentOpt.get().execute(ctx);
        }
    }

    public JobRecord runDryRun(ProjectRecord project, String actor) throws Exception {
        String jobId = "job-" + UUID.randomUUID().toString().substring(0, 8);
        JobRecord job = new JobRecord(jobId, project.getId(), "DRY_RUN");
        job.setActor(actor != null ? actor : "admin");
        if (store != null) {
            store.saveJob(job);
        }

        AgentContext ctx = new AgentContext(project, job);
        ctx.setDryRun(true);

        try {
            transition(ctx, MigrationState.CONNECTING);
            invokeAgent("connection", ctx);

            transition(ctx, MigrationState.DISCOVERING);
            invokeAgent("discovery", ctx);

            transition(ctx, MigrationState.ANALYZING);
            invokeAgent("component-intelligence", ctx);
            invokeAgent("component-mapping", ctx);
            invokeAgent("template-analysis", ctx);
            invokeAgent("content-analysis", ctx);
            invokeAgent("asset-analysis", ctx);
            invokeAgent("content-fragment-analysis", ctx);
            invokeAgent("msm-analysis", ctx);

            transition(ctx, MigrationState.DESIGN_ANALYSIS);
            invokeAgent("figma-analysis", ctx);
            invokeIfRegistered("figma-intelligence", ctx);

            transition(ctx, MigrationState.PLANNING);
            invokeAgent("migration-planner", ctx);

            transition(ctx, MigrationState.BUILDING);
            invokeAgent("block-generation", ctx);
            invokeAgent("code-generation", ctx);

            transition(ctx, MigrationState.MIGRATING);
            invokeAgent("content-migration", ctx);

            transition(ctx, MigrationState.READY_TO_PUBLISH);
            invokeIfRegistered("advanced-rollout", ctx);

            transition(ctx, MigrationState.COMPLETED);
            job.setFinishedAt(System.currentTimeMillis());
            if (store != null) {
                store.saveJob(job);
            }
        } catch (Exception e) {
            LOG.error("Dry run execution failed for job {}: {}", jobId, e.getMessage(), e);
            job.setState(MigrationState.FAILED.name());
            job.setLastError(e.getMessage());
            job.setFinishedAt(System.currentTimeMillis());
            if (store != null) {
                store.saveJob(job);
            }
            throw e;
        }

        return job;
    }

    public JobRecord runMigration(ProjectRecord project, String actor) throws Exception {
        String jobId = "job-" + UUID.randomUUID().toString().substring(0, 8);
        JobRecord job = new JobRecord(jobId, project.getId(), "MIGRATE");
        job.setActor(actor != null ? actor : "admin");
        if (store != null) {
            store.saveJob(job);
        }

        AgentContext ctx = new AgentContext(project, job);
        ctx.setDryRun(false);

        try {
            transition(ctx, MigrationState.CONNECTING);
            invokeAgent("connection", ctx);

            transition(ctx, MigrationState.DISCOVERING);
            invokeAgent("discovery", ctx);

            transition(ctx, MigrationState.ANALYZING);
            invokeAgent("component-intelligence", ctx);
            invokeAgent("component-mapping", ctx);
            invokeAgent("template-analysis", ctx);
            invokeAgent("content-analysis", ctx);
            invokeAgent("asset-analysis", ctx);
            invokeAgent("content-fragment-analysis", ctx);
            invokeAgent("msm-analysis", ctx);

            transition(ctx, MigrationState.DESIGN_ANALYSIS);
            invokeAgent("figma-analysis", ctx);
            invokeIfRegistered("figma-intelligence", ctx);

            transition(ctx, MigrationState.PLANNING);
            invokeAgent("migration-planner", ctx);

            transition(ctx, MigrationState.BUILDING);
            invokeAgent("block-generation", ctx);
            invokeAgent("code-generation", ctx);

            transition(ctx, MigrationState.MIGRATING);
            invokeAgent("content-migration", ctx);

            transition(ctx, MigrationState.AUTHORING);
            invokeAgent("authoring", ctx);

            transition(ctx, MigrationState.PREVIEWING);
            invokeAgent("preview", ctx);

            transition(ctx, MigrationState.VALIDATING);
            invokeAgent("validation", ctx);
            invokeAgent("visual-validation", ctx);
            invokeIfRegistered("advanced-visual-validation", ctx);

            transition(ctx, MigrationState.REPAIRING);
            invokeAgent("self-repair", ctx);
            invokeIfRegistered("advanced-repair", ctx);

            transition(ctx, MigrationState.READY_TO_PUBLISH);
            invokeIfRegistered("advanced-rollout", ctx);

            transition(ctx, MigrationState.PUBLISHING);
            invokeAgent("publishing", ctx);

            transition(ctx, MigrationState.VERIFYING);
            invokeAgent("verification", ctx);

            transition(ctx, MigrationState.COMPLETED);
            job.setFinishedAt(System.currentTimeMillis());
            if (store != null) {
                store.saveJob(job);
            }
        } catch (Exception e) {
            LOG.error("Migration execution failed for job {}: {}", jobId, e.getMessage(), e);
            job.setState(MigrationState.FAILED.name());
            job.setLastError(e.getMessage());
            job.setFinishedAt(System.currentTimeMillis());
            if (store != null) {
                store.saveJob(job);
            }
            throw e;
        }

        return job;
    }

    private void invokeAgent(String name, AgentContext ctx) throws Exception {
        Agent agent = agents.get(name.toLowerCase());
        if (agent != null) {
            agent.execute(ctx);
        } else {
            LOG.warn("Agent '{}' not registered in orchestrator", name);
        }
    }

    private void transition(AgentContext ctx, MigrationState nextState) {
        String fromState = ctx.getJob().getState();
        ctx.getJob().setState(nextState.name());
        LOG.info("[STATE] Job {} transition: {} -> {}", ctx.getJob().getId(), fromState, nextState);

        if (store != null) {
            store.saveJob(ctx.getJob());
            store.saveCheckpoint(new CheckpointRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    nextState.name(),
                    "Transitioned to " + nextState.name()
            ));
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    "orchestrator",
                    "State transition: " + fromState + " -> " + nextState.name()
            ));
        }
    }

    public Map<String, Agent> getAgents() { return agents; }
    public Store getStore() { return store; }
    public AiGateway getAi() { return ai; }
    public EstimatorService getEstimator() { return estimator; }
}
