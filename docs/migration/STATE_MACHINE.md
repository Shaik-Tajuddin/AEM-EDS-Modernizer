# Migration State Machine

The full migration state machine, with per-state semantics,
invariants, and recovery rules.

See [../architecture/STATE_MACHINE.md](../architecture/STATE_MACHINE.md)
for the architectural view. This document focuses on the
*semantics* of each state.

## State semantics

| State | What's true | What's persisted | What's allowed |
|---|---|---|---|
| `CREATED` | Job record exists, no work done | `MigrationJobRecord` | operator cancel |
| `CONNECTING` | External systems are being tested | connection cards | operator cancel |
| `DISCOVERING` | AEM content tree is being walked | `AemPageRecord`s | operator cancel, scope change |
| `ANALYZING` | Components, templates, content, assets, MSM are being analysed | `ComponentRecord`s, `AssetRecord`s, analysis events | operator cancel |
| `DESIGN_ANALYSIS` | Figma file is being read | `GeneratedFileRecord`s (themes.css, figma-tokens.json) | operator cancel |
| `PLANNING` | Plan and estimate are being built | `MigrationPlan`, `Estimate` | operator cancel |
| `BUILDING` | EDS blocks and repo scaffold are being generated | `GeneratedFileRecord`s (blocks, scaffold) | operator cancel |
| `MIGRATING` | Content is being converted to section models; redirects + dependencies being built | `GeneratedFileRecord`s (section models), `UrlRedirectRecord`s, `DependencyEdgeRecord`s | operator cancel |
| `AUTHORING` | AEM UE-compatible page structures being created | `AemPageRecord`s (status=AUTHORED) | operator cancel |
| `PREVIEWING` | Files being deployed to EDS preview | preview URL | operator cancel |
| `VALIDATING` | Browser-based validation running | `ValidationResultRecord`s | operator cancel |
| `REPAIRING` | Failed validations being repaired | `RepairAttemptRecord`s | operator cancel |
| `READY_TO_PUBLISH` | Human gate; dashboard's `MIGRATE` button is enabled | (no new records) | operator click `MIGRATE` |
| `PUBLISHING` | Git branch being created, PR being opened | `JobEventRecord` (PR URL) | (no operator action) |
| `VERIFYING` | Production crawl running | `ValidationResultRecord`s, `MigrationReport` | operator cancel |
| `COMPLETED` | Terminal success | (no new records) | (terminal) |
| `FAILED` | Terminal failure | `JobEventRecord` (lastError) | operator retry |
| `CANCELLED` | Terminal cancel | `JobEventRecord` (cancelledBy) | (terminal) |
| `WAITING_FOR_CLARIFICATION` | Human input needed | `ClarificationRequest` | operator answer |

## Invariants

1. **Every transition emits a `JobEventRecord`.** The
   dashboard reconstructs its state from events.
2. **Every state has a `CheckpointRecord`.** A migration can
   resume from any checkpoint.
3. **The orchestrator never skips a state.** Transitions are
   guarded by `MigrationState.canTransitionTo(next)`.
4. **Critical issues block `READY_TO_PUBLISH`.** A
   `CRITICAL` `IssueRecord` disables the `MIGRATE` button.

## Recovery

If the orchestrator JVM is killed mid-migration:

1. The `MigrationJobRecord` is in the last persisted state.
2. The operator opens the dashboard and sees the job in
   `FAILED` (or the last state, if the kill was before the
   failure transition).
3. The operator clicks "Resume". The orchestrator reads
   the last `CheckpointRecord` and re-enters the state
   machine at the `toState`.
4. The orchestrator re-runs the state. Idempotency ensures
   that the second run produces the same result.

If the operator wants to start over, they click "Cancel"
and then "Start new migration".

## See also

- [../architecture/STATE_MACHINE.md](../architecture/STATE_MACHINE.md) —
  the architectural view.
- [DRY_RUN.md](DRY_RUN.md) — the dry run semantics.
- [CHECKPOINTS.md](CHECKPOINTS.md) — checkpoint and recovery.
