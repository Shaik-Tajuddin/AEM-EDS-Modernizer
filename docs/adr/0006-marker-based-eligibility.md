# ADR 0006 — Marker-Based Eligibility (Master §33)

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Discovery, scope control

## Context

A migration to EDS is irreversible in practice. We need a way to
say "these 200 pages are in scope; the other 800 are not" without
forcing the operator to list every path by hand.

Three approaches were on the table:

1. **All pages by default, opt-out** (a property to mark a page
   as excluded). Simple, but a single forgotten exclusion
   migrates a page the operator wanted to keep.
2. **All pages by default, opt-in** (a property to mark a page
   as included). Safer, but requires touching every page in
   the scope.
3. **Marker-based, configurable policy**: the operator picks a
   policy (`MARKED_ONLY`, `MARKED_AND_EXPLICIT_SELECTION`,
   `EXPLICIT_SELECTION_ONLY`) and a property name/value. The
   default is the safest (most restrictive) policy.

## Decision

We chose **option 3** with a marker-based configuration:

- `MarkerEvaluator` reads three values from the project config:
  - `markerProperty` — e.g. `modernizer.migrate`
  - `markerValue` — e.g. `true`
  - `policy` — `MARKED_ONLY` / `MARKED_AND_EXPLICIT_SELECTION` /
    `EXPLICIT_SELECTION_ONLY`
- For every page the `DiscoveryAgent` walks, the
  `MarkerEvaluator.evaluate(page)` returns `Eligible` /
  `NotEligible` / `InheritedFromParent`.
- The `AemPageRecord` carries the eligibility verdict and the
  reason (`eligibilityReason`).
- The default in production is `MARKED_ONLY` (most restrictive):
  a page is eligible only if it has the marker property.
- The default in the standalone mock is
  `MARKED_AND_EXPLICIT_SELECTION`: a page is eligible if it has
  the marker, **or** if it's a child of a marked parent, **or**
  if it's in the mock fixture's explicit selection. This is
  intentional: it makes the mock dashboard non-empty.

## Consequences

### Positive

- **Safe default.** Production deployments use `MARKED_ONLY`;
  nothing is migrated without an explicit opt-in.
- **Hierarchical.** A marker on `/content/wknd` cascades to
  every child page; no per-page work needed.
- **Auditable.** The eligibility reason is recorded on every
  `AemPageRecord`; the dashboard's `#/pages` view shows it.
- **Reversible.** Removing the marker removes a page from the
  scope without touching the data.

### Negative

- **Operators must remember to mark pages.** A migration that
  finds zero pages is usually a missing marker. The
  `TestConnections` step warns if the marker is not set.
- **The default differs between mock and real mode.** A
  developer running the standalone runtime might be surprised
  that production behaviour is more restrictive. The
  `MarkerEvaluator`'s `policy` is logged on every
  `DiscoveryAgent` run; the warning is visible.

## Alternatives considered

- **Path-list based** (option 1, reject because too permissive).
- **Always on** (option 1 variant, rejected for the same
  reason).
- **Time-based** (a page is eligible if it was modified after a
  given date): rejected as the primary mechanism because the
  operator can't easily see *which* pages are in scope, but kept
  as a hint to the agent for the staleness check (Master §8).

## Related

- [../migration/SCOPE.md](../migration/SCOPE.md) — full scope
  semantics.
- [ADR 0005](0005-dry-run-is-mandatory.md) — the Dry Run gate
  that follows the scope.
