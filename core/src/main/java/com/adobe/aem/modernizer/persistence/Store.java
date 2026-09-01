package com.adobe.aem.modernizer.persistence;

import com.adobe.aem.modernizer.persistence.model.*;

import java.util.List;
import java.util.Optional;

/**
 * Persistence facade for migration state, inventory, generated files, and events.
 */
public interface Store {

    // Projects
    void saveProject(ProjectRecord project);
    Optional<ProjectRecord> getProject(String id);
    List<ProjectRecord> listProjects();
    void deleteProject(String id);

    // Jobs
    void saveJob(JobRecord job);
    Optional<JobRecord> getJob(String id);
    List<JobRecord> listJobs(String projectId);
    Optional<JobRecord> getLatestJob(String projectId);

    // Site Inventory
    void saveInventory(SiteInventory inventory);
    Optional<SiteInventory> getInventory(String jobId);

    // Migration Plan
    void savePlan(MigrationPlan plan);
    Optional<MigrationPlan> getPlan(String jobId);
    Optional<MigrationPlan> getLatestPlan(String projectId);

    // Generated Files
    void saveGeneratedFile(GeneratedFileRecord file);
    List<GeneratedFileRecord> getGeneratedFiles(String jobId);
    Optional<GeneratedFileRecord> getGeneratedFile(String jobId, String path);
    boolean deleteGeneratedFile(String jobId, String path);

    // Validation Results
    void saveValidationResult(ValidationResultRecord result);
    List<ValidationResultRecord> getValidationResults(String jobId);

    // Repair Attempts
    void saveRepairAttempt(RepairAttemptRecord attempt);
    List<RepairAttemptRecord> getRepairAttempts(String jobId);
    List<RepairAttemptRecord> getRepairAttemptsForProject(String projectId);

    // URL Redirects
    void saveUrlRedirect(UrlRedirectRecord redirect);
    List<UrlRedirectRecord> getUrlRedirects(String jobId);
    List<UrlRedirectRecord> getUrlRedirectsForProject(String projectId);

    // Dependency Edges
    void saveDependencyEdge(DependencyEdgeRecord edge);
    List<DependencyEdgeRecord> getDependencyEdges(String jobId);
    List<DependencyEdgeRecord> getDependencyEdgesForProject(String projectId);

    // Rollout Stages
    void saveRolloutStage(RolloutStageRecord stage);
    List<RolloutStageRecord> getRolloutStages(String jobId);
    List<RolloutStageRecord> getLatestRolloutStages(String projectId);

    // Benchmark Samples
    void saveBenchmarkSample(BenchmarkSampleRecord sample);
    List<BenchmarkSampleRecord> getBenchmarkSamples(String jobId);
    List<BenchmarkSampleRecord> getBenchmarkSamplesForProject(String projectId);
    List<BenchmarkSampleRecord> getBenchmarkSamplesForAgent(String agent);

    // Job Events
    void recordEvent(JobEventRecord event);
    List<JobEventRecord> getEvents(String jobId);
    List<JobEventRecord> getEventsForProject(String projectId);

    // Clarifications
    void saveClarification(ClarificationRecord clarification);
    List<ClarificationRecord> getClarifications(String jobId);
    List<ClarificationRecord> getClarificationsForProject(String projectId);

    // Checkpoints
    void saveCheckpoint(CheckpointRecord checkpoint);
    Optional<CheckpointRecord> getLatestCheckpoint(String jobId);

    // Migration Contract
    void saveContract(MigrationContractRecord contract);
    Optional<MigrationContractRecord> getContract(String jobId);
}
