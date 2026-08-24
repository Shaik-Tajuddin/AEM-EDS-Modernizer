# ContentAnalysisAgent

> Analyses AEM content fragments (CFs) and their references,
> building a reference graph that's used by the dependency
> service.

- **Stage:** `ANALYZING`
- **Phase:** 1
- **Agent name:** `content-analysis`
- **Task type:** `CONTENT_ANALYSIS`

## Inputs

- AEM content fragments in the scope
  (`/content/dam/{site}/content-fragments/...`).
- The AEM pages that reference them (via `AemPageRecord`).

## Outputs

- `ContentAnalysisEvent`s (per CF) with:
  - `fragmentPath`
  - `referencedBy` (page paths that reference this CF)
  - `referencesTo` (other CFs / assets this CF references)
  - `complexity` (number of fields, depth)

## AI usage

None. Pure database walk.

## Failure modes

- **CF unreachable:** the agent records a `HIGH` issue and
  continues.
- **CF references a broken asset:** the agent records a
  `CRITICAL` issue; the asset is added to the migration's
  broken-reference list.
