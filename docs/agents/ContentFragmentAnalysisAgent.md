# ContentFragmentAnalysisAgent

> Walks AEM content fragments (CFs) in the scope, identifies
> their model, and records their structured fields.

- **Stage:** `ANALYZING`
- **Phase:** 1
- **Agent name:** `content-fragment-analysis`
- **Task type:** `CONTENT_FRAGMENT_ANALYSIS`

## Inputs

- CFs in `/content/dam/{site}/content-fragments/...`.
- The CF model definitions.

## Outputs

- `ContentFragmentAnalysisEvent`s with: `fragmentPath`,
  `modelPath`, `fieldCount`, `referencedFragments`.

## AI usage

None. Pure database walk.

## Failure modes

- **CF model not found:** the agent records a `HIGH` issue and
  falls back to a generic model.
- **CF has no fields:** the agent records a `MEDIUM` issue and
  the CF is marked as "empty".

## Note

This is a sibling agent to `ContentAnalysisAgent`. The split
keeps the "find references" work separate from the "extract
fields" work; both run in the `ANALYZING` stage.
