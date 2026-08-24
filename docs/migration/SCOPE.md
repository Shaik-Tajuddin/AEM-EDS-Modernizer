# Scope

How the scope of a migration is determined.

## The scope is a property of the `ProjectRecord`

The `ProjectRecord` carries:

- `contentRoot` — the AEM path to start the walk (e.g.
  `/content/wknd`).
- `markerProperty` — the marker property name (e.g.
  `modernizer.migrate`).
- `markerValue` — the marker property value (e.g. `true`).
- `markerPolicy` — `MARKED_ONLY` /
  `MARKED_AND_EXPLICIT_SELECTION` /
  `EXPLICIT_SELECTION_ONLY`.

The `MarkerEvaluator` reads these and decides per-page
eligibility.

## The marker policies

### `MARKED_ONLY` (production default)

A page is eligible only if it has the marker property
(`{markerProperty}={markerValue}`). The walk stops at the
content root; no inheritance is applied.

**Use when:** the operator wants explicit opt-in for every
page.

### `MARKED_AND_EXPLICIT_SELECTION` (standalone default)

A page is eligible if:

- The page has the marker property, OR
- A parent of the page has the marker property (cascading
  inheritance), OR
- The page is in the project's explicit selection
  (`ProjectRecord.explicitSelection`).

**Use when:** the operator wants to mark a subtree and have
all children included, or to use the mock fixture.

### `EXPLICIT_SELECTION_ONLY`

A page is eligible only if the page is in the project's
explicit selection.

**Use when:** the operator wants a hand-picked list of
pages.

## How the marker is read

The `MarkerEvaluator.evaluate(page)`:

1. Reads the page's `jcr:content` properties.
2. Checks if `{markerProperty}={markerValue}` is present.
3. If not, walks up the parent chain and checks each
   parent's `jcr:content` properties.
4. If still not found, checks the `explicitSelection`.
5. Returns `Eligible` (with a reason) or `NotEligible`
   (with a reason).

The result is recorded on the `AemPageRecord.eligible` and
`AemPageRecord.eligibilityReason` fields.

## Changing the scope mid-migration

The scope can be changed between the dry run and the real
migration. The operator:

1. Updates the `ProjectRecord` (e.g. adds a marker to a
   page).
2. Re-runs the dry run.
3. Reviews the new dry run.
4. Clicks `MIGRATE` (if approved).

The scope **cannot** be changed mid-migration. The
orchestrator reads the scope at `DISCOVERING` and does not
re-read it.

## See also

- [../adr/0006-marker-based-eligibility.md](../adr/0006-marker-based-eligibility.md) —
  the decision record.
- [../agents/DiscoveryAgent.md](../agents/DiscoveryAgent.md) —
  the agent.
