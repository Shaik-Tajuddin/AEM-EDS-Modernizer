# PublishingAgent

> Phase 1 basic publishing. Creates a single Git branch,
> pushes the generated files as commits, and opens a PR.

- **Stage:** `READY_TO_PUBLISH` (after the operator clicks
  `MIGRATE`)
- **Phase:** 1
- **Agent name:** `publishing`
- **Task type:** `PUBLISHING`

## Inputs

- Every `GeneratedFileRecord` from the building + migrating
  stages.
- The project's GitHub configuration.

## Outputs

- A Git branch with all the generated files.
- A PR against the configured default branch.
- A `JobEventRecord` with the PR URL.

## Branch policy

- The branch name is `modernizer/{projectId}/{jobId}`.
- The PR title is `Modernize {projectName} to EDS ({jobId})`.
- The PR body includes a summary of the changes (page count,
  block count, AI cost) and a link to the dashboard.

## AI usage

None. Pure Git operations.

## Failure modes

- **GitHub API rate-limited:** the agent backs off and retries.
- **Branch already exists:** the agent appends a timestamp
  and retries.
- **PR creation fails** (e.g. branch protection): the agent
  records a `CRITICAL` issue with the GitHub response.

## Related

- [AdvancedRolloutAgent](AdvancedRolloutAgent.md) — the
  Phase 2 version with a 6-stage rollout and stop conditions.
- [ADR 0011](../adr/0011-virtual-diff-not-real-git.md) — the
  virtual diff (no real Git ops during dry run).
