package com.adobe.aem.modernizer.persistence;

import com.adobe.aem.modernizer.persistence.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * High-performance, concurrent, in-memory implementation of the Store facade.
 */
public class InMemoryStore implements Store {

    private final Map<String, ProjectRecord> projects = new ConcurrentHashMap<>();
    private final Map<String, JobRecord> jobs = new ConcurrentHashMap<>();
    private final Map<String, SiteInventory> inventories = new ConcurrentHashMap<>();
    private final Map<String, MigrationPlan> plans = new ConcurrentHashMap<>();    private final Map<String, List<GeneratedFileRecord>> generatedFiles = new ConcurrentHashMap<>();
    private final Map<String, List<ValidationResultRecord>> validationResults = new ConcurrentHashMap<>();
    private final Map<String, List<RepairAttemptRecord>> repairAttempts = new ConcurrentHashMap<>();
    private final Map<String, List<UrlRedirectRecord>> urlRedirects = new ConcurrentHashMap<>();
    private final Map<String, List<DependencyEdgeRecord>> dependencyEdges = new ConcurrentHashMap<>();
    private final Map<String, List<RolloutStageRecord>> rolloutStages = new ConcurrentHashMap<>();
    private final Map<String, List<BenchmarkSampleRecord>> benchmarkSamples = new ConcurrentHashMap<>();
    private final Map<String, List<JobEventRecord>> events = new ConcurrentHashMap<>();
    private final Map<String, List<ClarificationRecord>> clarifications = new ConcurrentHashMap<>();
    private final Map<String, List<CheckpointRecord>> checkpoints = new ConcurrentHashMap<>();
    private final Map<String, MigrationContractRecord> contracts = new ConcurrentHashMap<>();

    protected final Map<String, JobRecord> jobsRef = this.jobs;
    protected final Map<String, SiteInventory> inventoriesRef = this.inventories;
    protected final Map<String, List<GeneratedFileRecord>> generatedFilesRef = this.generatedFiles;
    protected final Map<String, List<JobEventRecord>> eventsRef = this.events;

    // Projects
    @Override
    public void saveProject(ProjectRecord project) {
        if (project != null && project.getId() != null) {
            project.setUpdatedAt(System.currentTimeMillis());
            projects.put(project.getId(), project);
        }
    }

    @Override
    public Optional<ProjectRecord> getProject(String id) {
        return Optional.ofNullable(projects.get(id));
    }

    @Override
    public List<ProjectRecord> listProjects() {
        return new ArrayList<>(projects.values());
    }

    @Override
    public void deleteProject(String id) {
        // 1. Delete generated block directories from local disk using inventory of the latest job
        try {
            getLatestJob(id).ifPresent(job -> {
                getInventory(job.getId()).ifPresent(inv -> {
                    if (inv.getComponents() != null) {
                        for (SiteInventory.ComponentInfo comp : inv.getComponents()) {
                            String blockName = comp.getProposedEdsBlock() != null
                                    ? comp.getProposedEdsBlock().toLowerCase().replace(' ', '-')
                                    : comp.getResourceType().substring(comp.getResourceType().lastIndexOf('/') + 1).toLowerCase();
                            deleteLocalBlockFolder(blockName);
                        }
                    }
                });
            });
        } catch (Exception e) {
            // Ignore to ensure JCR/Store deletion completes
        }

        // 2. Remove the project itself
        projects.remove(id);

        // 3. Find and remove all jobs belonging to this project
        List<String> jobIds = jobs.values().stream()
                .filter(j -> id.equals(j.getProjectId()))
                .map(JobRecord::getId)
                .collect(Collectors.toList());

        for (String jId : jobIds) {
            jobs.remove(jId);
            inventories.remove(jId);
            plans.remove(jId);
            generatedFiles.remove(jId);
            validationResults.remove(jId);
            repairAttempts.remove(jId);
            urlRedirects.remove(jId);
            dependencyEdges.remove(jId);
            rolloutStages.remove(jId);
            benchmarkSamples.remove(jId);
            events.remove(jId);
            clarifications.remove(jId);
            checkpoints.remove(jId);
        }
    }

    private void deleteLocalBlockFolder(String blockName) {
        String[] candidateRoots = {
            "D:/eds personal/AEM-EDS-Modernizer",
            "d:/eds personal/AEM-EDS-Modernizer",
            System.getProperty("user.dir")
        };
        for (String root : candidateRoots) {
            java.io.File dir = new java.io.File(root, "blocks/" + blockName);
            if (dir.exists() && dir.isDirectory()) {
                deleteDirRecursive(dir);
            }
        }
    }

    private void deleteDirRecursive(java.io.File file) {
        java.io.File[] children = file.listFiles();
        if (children != null) {
            for (java.io.File child : children) {
                deleteDirRecursive(child);
            }
        }
        file.delete();
    }

    // Jobs
    @Override
    public void saveJob(JobRecord job) {
        if (job != null && job.getId() != null) {
            jobs.put(job.getId(), job);
        }
    }

    @Override
    public Optional<JobRecord> getJob(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    @Override
    public List<JobRecord> listJobs(String projectId) {
        return jobs.values().stream()
                .filter(j -> projectId == null || projectId.equals(j.getProjectId()))
                .sorted(Comparator.comparingLong(JobRecord::getStartedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<JobRecord> getLatestJob(String projectId) {
        return listJobs(projectId).stream().findFirst();
    }

    // Site Inventory
    @Override
    public void saveInventory(SiteInventory inventory) {
        if (inventory != null && inventory.getJobId() != null) {
            inventories.put(inventory.getJobId(), inventory);
        }
    }

    @Override
    public Optional<SiteInventory> getInventory(String jobId) {
        return Optional.ofNullable(inventories.get(jobId));
    }

    // Migration Plan
    @Override
    public void savePlan(MigrationPlan plan) {
        if (plan != null && plan.getJobId() != null) {
            plans.put(plan.getJobId(), plan);
        }
    }

    @Override
    public Optional<MigrationPlan> getPlan(String jobId) {
        return Optional.ofNullable(plans.get(jobId));
    }

    @Override
    public Optional<MigrationPlan> getLatestPlan(String projectId) {
        return getLatestJob(projectId)
                .flatMap(j -> getPlan(j.getId()));
    }

    // Generated Files
    @Override
    public void saveGeneratedFile(GeneratedFileRecord file) {
        if (file != null && file.getJobId() != null) {
            generatedFiles.computeIfAbsent(file.getJobId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(file);
        }
    }

    @Override
    public List<GeneratedFileRecord> getGeneratedFiles(String jobId) {
        return generatedFiles.getOrDefault(jobId, Collections.emptyList());
    }

    @Override
    public Optional<GeneratedFileRecord> getGeneratedFile(String jobId, String path) {
        return getGeneratedFiles(jobId).stream()
                .filter(f -> f.getPath().equals(path))
                .findFirst();
    }

    @Override
    public boolean deleteGeneratedFile(String jobId, String path) {
        if (jobId == null || path == null) return false;
        List<GeneratedFileRecord> files = generatedFiles.get(jobId);
        if (files != null) {
            return files.removeIf(f -> path.equals(f.getPath()));
        }
        return false;
    }

    // Validation Results
    @Override
    public void saveValidationResult(ValidationResultRecord result) {
        if (result != null && result.getJobId() != null) {
            validationResults.computeIfAbsent(result.getJobId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(result);
        }
    }

    @Override
    public List<ValidationResultRecord> getValidationResults(String jobId) {
        return validationResults.getOrDefault(jobId, Collections.emptyList());
    }

    // Repair Attempts
    @Override
    public void saveRepairAttempt(RepairAttemptRecord attempt) {
        if (attempt != null && attempt.getJobId() != null) {
            repairAttempts.computeIfAbsent(attempt.getJobId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(attempt);
        }
    }

    @Override
    public List<RepairAttemptRecord> getRepairAttempts(String jobId) {
        return repairAttempts.getOrDefault(jobId, Collections.emptyList());
    }

    @Override
    public List<RepairAttemptRecord> getRepairAttemptsForProject(String projectId) {
        return repairAttempts.values().stream()
                .flatMap(Collection::stream)
                .filter(r -> projectId == null || projectId.equals(r.getProjectId()))
                .sorted(Comparator.comparingLong(RepairAttemptRecord::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    // URL Redirects
    @Override
    public void saveUrlRedirect(UrlRedirectRecord redirect) {
        if (redirect != null && redirect.getJobId() != null) {
            urlRedirects.computeIfAbsent(redirect.getJobId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(redirect);
        }
    }

    @Override
    public List<UrlRedirectRecord> getUrlRedirects(String jobId) {
        return urlRedirects.getOrDefault(jobId, Collections.emptyList());
    }

    @Override
    public List<UrlRedirectRecord> getUrlRedirectsForProject(String projectId) {
        return urlRedirects.values().stream()
                .flatMap(Collection::stream)
                .filter(r -> projectId == null || projectId.equals(r.getProjectId()))
                .collect(Collectors.toList());
    }

    // Dependency Edges
    @Override
    public void saveDependencyEdge(DependencyEdgeRecord edge) {
        if (edge != null && edge.getJobId() != null) {
            dependencyEdges.computeIfAbsent(edge.getJobId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(edge);
        }
    }

    @Override
    public List<DependencyEdgeRecord> getDependencyEdges(String jobId) {
        return dependencyEdges.getOrDefault(jobId, Collections.emptyList());
    }

    @Override
    public List<DependencyEdgeRecord> getDependencyEdgesForProject(String projectId) {
        return dependencyEdges.values().stream()
                .flatMap(Collection::stream)
                .filter(r -> projectId == null || projectId.equals(r.getProjectId()))
                .collect(Collectors.toList());
    }

    // Rollout Stages
    @Override
    public void saveRolloutStage(RolloutStageRecord stage) {
        if (stage != null && stage.getJobId() != null) {
            rolloutStages.computeIfAbsent(stage.getJobId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(stage);
        }
    }

    @Override
    public List<RolloutStageRecord> getRolloutStages(String jobId) {
        return rolloutStages.getOrDefault(jobId, Collections.emptyList());
    }

    @Override
    public List<RolloutStageRecord> getLatestRolloutStages(String projectId) {
        return getLatestJob(projectId)
                .map(j -> getRolloutStages(j.getId()))
                .orElse(Collections.emptyList());
    }

    // Benchmark Samples
    @Override
    public void saveBenchmarkSample(BenchmarkSampleRecord sample) {
        if (sample != null && sample.getJobId() != null) {
            benchmarkSamples.computeIfAbsent(sample.getJobId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(sample);
        }
    }

    @Override
    public List<BenchmarkSampleRecord> getBenchmarkSamples(String jobId) {
        return benchmarkSamples.getOrDefault(jobId, Collections.emptyList());
    }

    @Override
    public List<BenchmarkSampleRecord> getBenchmarkSamplesForProject(String projectId) {
        return benchmarkSamples.values().stream()
                .flatMap(Collection::stream)
                .filter(r -> projectId == null || projectId.equals(r.getProjectId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<BenchmarkSampleRecord> getBenchmarkSamplesForAgent(String agent) {
        return benchmarkSamples.values().stream()
                .flatMap(Collection::stream)
                .filter(r -> agent == null || agent.equals(r.getAgent()))
                .collect(Collectors.toList());
    }

    // Job Events
    @Override
    public void recordEvent(JobEventRecord event) {
        if (event != null && event.getJobId() != null) {
            events.computeIfAbsent(event.getJobId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(event);
        }
    }

    @Override
    public List<JobEventRecord> getEvents(String jobId) {
        return events.getOrDefault(jobId, Collections.emptyList());
    }

    @Override
    public List<JobEventRecord> getEventsForProject(String projectId) {
        return events.values().stream()
                .flatMap(Collection::stream)
                .filter(r -> projectId == null || projectId.equals(r.getProjectId()))
                .sorted(Comparator.comparingLong(JobEventRecord::getTimestamp))
                .collect(Collectors.toList());
    }

    // Clarifications
    @Override
    public void saveClarification(ClarificationRecord clarification) {
        if (clarification != null && clarification.getJobId() != null) {
            clarifications.computeIfAbsent(clarification.getJobId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(clarification);
        }
    }

    @Override
    public List<ClarificationRecord> getClarifications(String jobId) {
        return clarifications.getOrDefault(jobId, Collections.emptyList());
    }

    @Override
    public List<ClarificationRecord> getClarificationsForProject(String projectId) {
        return clarifications.values().stream()
                .flatMap(Collection::stream)
                .filter(r -> projectId == null || projectId.equals(r.getProjectId()))
                .collect(Collectors.toList());
    }

    // Checkpoints
    @Override
    public void saveCheckpoint(CheckpointRecord checkpoint) {
        if (checkpoint != null && checkpoint.getJobId() != null) {
            checkpoints.computeIfAbsent(checkpoint.getJobId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(checkpoint);
        }
    }

    @Override
    public Optional<CheckpointRecord> getLatestCheckpoint(String jobId) {
        List<CheckpointRecord> list = checkpoints.get(jobId);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(list.size() - 1));
    }

    // Migration Contract
    @Override
    public void saveContract(MigrationContractRecord contract) {
        if (contract != null && contract.getJobId() != null) {
            contracts.put(contract.getJobId(), contract);
        }
    }

    @Override
    public Optional<MigrationContractRecord> getContract(String jobId) {
        return Optional.ofNullable(contracts.get(jobId));
    }
}
