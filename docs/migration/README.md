# Migration Semantics

This section collects the migration-specific documentation:
the state machine, the dry run, the asset policy, the
estimate, and the scope.

## Documents in this section

- [STATE_MACHINE.md](STATE_MACHINE.md) — the full migration
  state machine.
- [DRY_RUN.md](DRY_RUN.md) — what the dry run does and does
  not do.
- [ASSET_POLICY.md](ASSET_POLICY.md) — the asset metadata-only
  policy.
- [ESTIMATE.md](ESTIMATE.md) — the pre-implementation
  estimate model.
- [SCOPE.md](SCOPE.md) — how scope is determined.
- [CHECKPOINTS.md](CHECKPOINTS.md) — checkpoint and recovery.
- [REPORT.md](REPORT.md) — the migration report format.

## How this fits in the architecture

These documents are the *semantic* layer: the rules that
govern what the modernizer does in each state. The
*implementation* layer lives in
[../architecture/](../architecture/) and the *agent* layer
lives in [../agents/](../agents/).

## See also

- [../architecture/STATE_MACHINE.md](../architecture/STATE_MACHINE.md) —
  the architectural view of the state machine.
- [../adr/0005-dry-run-is-mandatory.md](../adr/0005-dry-run-is-mandatory.md) —
  the dry run decision record.
