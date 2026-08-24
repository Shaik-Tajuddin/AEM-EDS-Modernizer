# DiscoveryAgent

> Walks the AEM content tree from the project's `contentRoot` and
> records every page as an `AemPageRecord` with eligibility and
> status.

- **Stage:** `DISCOVERING`
- **Phase:** 1
- **Agent name:** `discovery`
- **Task type:** `DISCOVERY`

## Inputs

- The project's `contentRoot` (e.g. `/content/wknd`).
- The `MarkerEvaluator` config (property, value, policy).

## Outputs

- One `AemPageRecord` per page in the content tree, with:
  - `path` (AEM path)
  - `title` (from `jcr:title`)
  - `template` (the cq:template)
  - `componentTypes` (the resource types of the components on
    the page)
  - `eligible` (boolean from the `MarkerEvaluator`)
  - `eligibilityReason` (e.g. `MARKER_PRESENT`,
    `INHERITED_FROM_PARENT`, `EXCLUDED_BY_POLICY`)
  - `migrationStatus` (initially `DISCOVERED`)

## AI usage

None. The agent walks the tree via `AemClient.listPages(root)`
and `AemClient.getPage(path)`. It uses the indexed SQL2 query
where available; otherwise it falls back to resource traversal.

## Failure modes

- **AEM unreachable:** the agent fails; the orchestrator
  transitions the job to `FAILED`.
- **AEM returns a partial tree:** the agent records what it
  got and creates a `HIGH` issue ("Partial discovery").

## Eligibility

The `MarkerEvaluator` decides per-page:

- `MARKED_ONLY` — eligible only if the page has the marker
  property. **Production default.**
- `MARKED_AND_EXPLICIT_SELECTION` — eligible if the page has the
  marker, OR if a parent has the marker, OR if the page is in
  the mock fixture's explicit selection. **Standalone default.**
- `EXPLICIT_SELECTION_ONLY` — eligible only if the page is in an
  explicit allowlist.

## Related

- [ADR 0006](../adr/0006-marker-based-eligibility.md) — the
  marker-based eligibility decision record.
- [Scope](../migration/SCOPE.md) — full scope semantics.
