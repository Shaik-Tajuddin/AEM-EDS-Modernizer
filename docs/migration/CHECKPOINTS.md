# Checkpoints

Checkpoint and recovery. Per Master §40, every state
transition persists a `CheckpointRecord` so a migration can
resume from the last successful state.

## What a checkpoint contains

```java
public class CheckpointRecord {
    String jobId;
    MigrationState fromState;
    MigrationState toState;
    Instant createdAt;
    String resumptionHint;
}
```

| Field | Description |
|---|---|
| `jobId` | The migration job id |
| `fromState` | The state we were leaving |
| `toState` | The state we entered |
| `createdAt` | The timestamp |
| `resumptionHint` | An optional string the agent uses to pick up where it left off (e.g. "resume from page 247 of 1000") |

## When checkpoints are written

Every state transition writes a `CheckpointRecord` before
the next state's work begins. For a typical migration this
is ~30 checkpoints (one per state transition, not per
page).

## When checkpoints are read

When the operator clicks "Resume" on a `FAILED` (or
paused) job, the orchestrator:

1. Loads the `MigrationJobRecord`.
2. Reads the last `CheckpointRecord` for the job.
3. Re-enters the state machine at `toState`.
4. Passes the `resumptionHint` to the relevant agent.

## Idempotency

Every agent is idempotent: re-running it on the same input
produces the same output. This is what makes resumption
safe. The `Store` interface uses `upsert(map, record, keyFn)`
for every collection, so a second run overwrites the first
rather than duplicating.

## Resumption hints

Some agents have a meaningful `resumptionHint`:

| Agent | Resumption hint |
|---|---|
| `DiscoveryAgent` | The path of the last successfully processed page |
| `ContentMigrationAgent` | The index of the last successfully processed page |
| `ValidationAgent` | The URL of the last successfully validated page |

Other agents have `resumptionHint=null` (the agent re-runs
from the start of its state; the rest of the state is
idempotent).

## Storage

Checkpoints are persisted in the same `Store` as the rest
of the job state, so `JcrStore` (AEM Cloud) or
`JsonFileStore` / `InMemoryStore` (standalone) handle the
lifecycle identically.

## Storage growth

For a 1000-page migration, the orchestrator writes ~30
checkpoints (one per state transition, not per page).
Storage growth is negligible.

## See also

- [../adr/0012-checkpoints-for-resumability.md](../adr/0012-checkpoints-for-resumability.md) —
  the decision record.
- [../operations/RECOVERY.md](../operations/RECOVERY.md) —
  the operator's recovery runbook.
