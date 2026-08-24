# ADR 0011 — Virtual Diff, Not Real Git Commits (During Dry Run)

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** User experience, dry run

## Context

A Dry Run must produce a complete view of what the real
migration would do, including the proposed Git changes. But the
Dry Run must not actually create commits, branches, PRs, or
merges — those are irreversible in practice.

A naive approach is to create a real branch, push commits, and
let the operator review the PR. This has problems:

- **Pollutes the repo.** A dry-run branch on every iteration
  clutters the GitHub UI.
- **Leaks information.** A real PR is visible to anyone with
  read access; the operator may want to keep the migration
  plan private until ready.
- **One-way door.** Pushing a commit and then deleting the
  branch still leaves the commit in the reflog; if someone
  fast-forwards `main` in the meantime, the dry-run commit
  reappears.

## Decision

The `PublishingAgent` (and the Phase 2 `AdvancedRolloutAgent`)
produces a **virtual diff** during the Dry Run:

- Every `GeneratedFileRecord` carries the proposed file path,
  content, and operation (`CREATE`, `UPDATE`, `DELETE`).
- The dashboard's `#/diff` view renders the virtual diff as a
  unified-diff view, with syntax highlighting per file type.
- No commit, no branch, no PR, no merge happens.
- The `MigrationReportService` includes the diff in the report
  JSON, so the operator can download it for offline review.

When the operator clicks `MIGRATE` (after the Dry Run is
accepted), a real `MigrationJobRecord` with `dryRun=false` is
created. That job's `PublishingAgent` creates a real branch,
pushes commits, and opens a PR. The same `GeneratedFileRecord`s
are used; the difference is only the side-effecting call.

## Consequences

### Positive

- **No repo pollution.** The Dry Run leaves no trace in Git.
- **The plan is private.** The diff is in the dashboard, behind
  AEM auth.
- **The dry run is cheap.** No Git operations means the dry run
  is just database writes.
- **The diff is reviewable in-place.** The dashboard's `#/diff`
  view is faster than a GitHub PR review for the operator.

### Negative

- **The diff is a "would be" not a "is".** If the repo state
  changes between the Dry Run and the real migration, the real
  PR may differ. Mitigated by the `MigrationPlannerAgent`
  re-running in the real job and by the operator's option to
  re-Dry-Run before the real migration.
- **The diff is per-job, not per-PR.** If the operator runs
  two migrations against the same repo, the dashboard shows the
  union, not the latest. Mitigated by the
  `MigrationJobRecord`'s `createdAt` and the dashboard's
  job-selector dropdown.

## Alternatives considered

- **Real branch, no PR** (push a branch but don't open a PR):
  rejected because the operator must go to GitHub to see the
  diff, which is more friction than the in-dashboard view.
- **Local clone and diff** (clone the repo, write the files,
  diff locally): rejected because it duplicates the GitHub
  state and creates cleanup questions.

## Related

- [../migration/DRY_RUN.md](../migration/DRY_RUN.md) — full Dry
  Run semantics.
- [ADR 0005](0005-dry-run-is-mandatory.md) — the dry-run gate.
- [ADR 0010](0010-assets-are-metadata-only.md) — the asset
  policy that pairs with the diff (the diff shows preserved
  references, not binaries).
