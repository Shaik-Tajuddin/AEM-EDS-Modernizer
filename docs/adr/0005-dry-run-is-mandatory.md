# ADR 0005 — Dry Run is Mandatory (Master §0A)

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** User experience, product behaviour

## Context

A migration to Edge Delivery Services touches every page in the
AEM content tree. Mistakes are expensive: a misrouted URL breaks
SEO, a missing redirect loses traffic, a wrong block mapping
rebuilds the site wrong.

Per Master §0A, the system must complete a full **Dry Run** before
the operator is allowed to click `MIGRATE`. The Dry Run is
identical to a real migration except that:

- **No source AEM is modified** (no `createPage`, no `updatePage`,
  no publish to source).
- **No asset binaries are downloaded** (the Dry Run records
  references, not bytes).
- **No real Git commit / PR / merge** is created (the
  `GeneratedFileRecord`s are written but the publishing agent
  produces a virtual diff only).

The Dry Run produces a complete dashboard, a virtual diff, and a
pre-implementation estimate.

## Decision

The dashboard's `MIGRATE` button is **disabled** until all of the
following are true:

1. The Dry Run has completed.
2. No critical blockers exist (no `IssueRecord` with
   `severity=CRITICAL`).
3. The target connection is valid (the
   `AemClient`/`EdsClient`/`GitHubClient` reachability check
   passed).
4. The estimate is `CURRENT` (not stale, i.e. the
   `MigrationPlannerAgent` ran in this same job or a fresher
   one).
5. Project policy allows the target environment
   (per `ProjectRecord.policy`).

The Dry Run itself is a normal `MigrationJobRecord` with
`dryRun=true`. It uses the same `Orchestrator`, the same agents,
and the same `Store`. The `dryRun` flag is plumbed through to
every connector and every AI call; any side-effecting call must
check the flag and no-op.

## Consequences

### Positive

- **Operators see the full impact before committing.** A Dry Run
  produces 60-100 generated files, 700+ dependency edges, 177+
  URL redirects, and a 6-stage rollout plan. The operator reviews
  these in the dashboard before they become real.
- **Estimates are accurate.** The estimate is derived from the
  same data the real migration will use, not a separate
  approximation.
- **Rollback is automatic if the operator declines.** A Dry Run
  leaves no trace in AEM; the operator just closes the browser
  tab.
- **The system is auditable.** Every `MigrationJobRecord` records
  the `dryRun` flag; the report distinguishes dry runs from real
  migrations.

### Negative

- **The Dry Run is not free.** It runs every agent, makes every
  AI call, and produces every record. For a large site (1000+
  pages) it can take 10-30 minutes. The e2e mock run takes 4
  seconds.
- **Operators may want a "cheap" estimate** (e.g. just the page
  count and the cost). The MVP does not offer this; the estimate
  is always derived from a complete Dry Run. A follow-up could
  add a `--quick` flag that skips the heavy agents.

## Alternatives considered

- **No Dry Run, just a confirmation dialog** (rejected): too
  easy to click through; the Master Prompt is explicit that the
  Dry Run is a hard gate.
- **Dry Run is optional but recommended** (rejected): the
  Master Prompt's "must" wording is unambiguous.

## Related

- [../migration/DRY_RUN.md](../migration/DRY_RUN.md) — the full
  Dry Run semantics.
- [ADR 0011](0011-virtual-diff-not-real-git.md) — the virtual
  diff the Dry Run produces.
