# ADR 0012 — Checkpoints for Resumability (Master §40)

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Reliability, recovery

## Context

A migration of a large site (1000+ pages) can take hours. During
that time:

- The AEM Author instance may be redeployed.
- The orchestrator JVM may be killed.
- The network may partition.
- The operator may need to cancel and resume.

Without checkpoints, every interruption means restarting from
the top. With checkpoints, the migration resumes from the last
successful state.

## Decision

Every state transition persists a `CheckpointRecord` with:

- `jobId` — the migration job id
- `fromState` — the state we were leaving
- `toState` — the state we entered
- `createdAt` — the timestamp
- `resumptionHint` — an optional string the agent uses to pick
  up where it left off (e.g. "resume from page 247 of 1000")

When a `MigrationJobRecord` is in a non-terminal state and the
operator clicks `Resume`, the orchestrator reads the last
`CheckpointRecord` and:

1. Loads the `MigrationJobRecord`.
2. Re-enters the state machine at `toState`.
3. Passes the `resumptionHint` to the relevant agent.
4. Continues from there.

Checkpoints are persisted in the same `Store` as the rest of the
job state, so the JCR / InMemory store handles the lifecycle
identically.

## Consequences

### Positive

- **Long migrations can survive interruptions.** A 4-hour
  migration that is interrupted after 2 hours resumes in
  minutes.
- **Cancellation is safe.** An operator who cancels at
  page 247 can later resume from page 247 instead of starting
  over.
- **The dashboard surfaces checkpoints.** The `#/dryrun` view
  shows the latest checkpoint for each job, so the operator can
  see exactly where the migration would resume.

### Negative

- **Not all agents are fully resumable.** Some agents (e.g.
  `AuthoringAgent`, which creates AEM pages) are easier to
  re-run from scratch than to resume mid-way. Those agents
  record a `resumptionHint` of `null`, and the orchestrator
  re-runs them from the beginning of their state.
- **Checkpoint storage grows with job size.** For a 1000-page
  migration the orchestrator writes ~30 checkpoints
  (one per state transition, not per page). Negligible.

## Alternatives considered

- **No checkpoints** (always restart from `CREATED`): rejected
  because a 4-hour migration that is interrupted after 3:59 is
  operator-hostile.
- **Per-page checkpoints** (one per page): rejected as too
  expensive in storage and I/O.

## Related

- [../operations/RECOVERY.md](../operations/RECOVERY.md) —
  operational runbook for resuming a migration.
- [ADR 0013](0013-events-as-source-of-truth.md) — events are
  the *log* of what happened; checkpoints are the *snapshot*
  of where to resume.
