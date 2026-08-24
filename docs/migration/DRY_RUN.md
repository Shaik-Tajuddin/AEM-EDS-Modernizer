# Dry Run

What the dry run does and does not do.

## What the dry run does

A dry run is a complete `MigrationJobRecord` with
`dryRun=true`. The orchestrator runs the same state
machine, the same agents, and the same `Store` writes. The
dry run produces:

- The full inventory (pages, components, mappings).
- The full pre-implementation estimate.
- The full virtual diff (every `GeneratedFileRecord`).
- The full set of validations (against the preview).
- The full set of repair attempts.
- The full set of issues, validations, repairs, and
  benchmarks.

The dry run is **identical to a real migration** in every
way except the three "does not" items below.

## What the dry run does not do

1. **No source AEM is modified.** No `createPage`, no
   `updatePage`, no publish to source. The
   `AuthoringAgent` checks the `dryRun` flag and no-ops.
2. **No asset binaries are downloaded.** The
   `AssetAnalysisAgent` does HEAD requests only. No bytes
   are moved.
3. **No real Git commit / PR / merge.** The
   `PublishingAgent` produces a virtual diff only. No
   branch, no PR, no merge. The dry run's
   `GeneratedFileRecord`s are persisted in JCR (or
   in-memory) and surfaced in the dashboard's `#/diff`
   view, but no real Git operations are performed.

## The dry-run flag

The `dryRun` flag is plumbed through every agent and
connector. Every side-effecting call checks the flag and
no-ops. The flag is recorded on every `JobEventRecord` and
`AIDecision`.

## The mandatory gate

Per Master §0A, the dashboard's `MIGRATE` button is only
enabled when:

1. The Dry Run has completed.
2. No critical blockers exist (no `IssueRecord` with
   `severity=CRITICAL`).
3. The target connection is valid.
4. The estimate is `CURRENT` (not stale).
5. Project policy allows the target environment.

## Cost

The dry run is **not free**: it makes every AI call and
produces every record. For a 1000-page site it can take
10-30 minutes and cost a few dollars in AI calls.

The mock mode is faster (4 seconds in the e2e) and free.

## Failure modes

- **Dry run fails at any state:** the migration is in
  `FAILED`. The operator can fix the issue and re-run the
  dry run. The cost is the cost of one dry run.
- **Dry run succeeds with `CRITICAL` issues:** the
  `MIGRATE` button is disabled. The operator must resolve
  the critical issues (e.g. broken asset references) and
  re-run the dry run.

## See also

- [../adr/0005-dry-run-is-mandatory.md](../adr/0005-dry-run-is-mandatory.md) —
  the decision record.
- [../adr/0011-virtual-diff-not-real-git.md](../adr/0011-virtual-diff-not-real-git.md) —
  the virtual diff decision record.
