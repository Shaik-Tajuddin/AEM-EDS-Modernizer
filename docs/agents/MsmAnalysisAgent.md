# MsmAnalysisAgent

> Analyses AEM Multi-Site Manager (MSM) structures: blueprints,
> live copies, and rollout configurations.

- **Stage:** `ANALYZING`
- **Phase:** 1
- **Agent name:** `msm-analysis`
- **Task type:** `MSM_ANALYSIS`

## Inputs

- MSM relationships via `AemClient.getMsmInfo(path)`.

## Outputs

- `MsmAnalysisEvent`s with: `blueprintPath`, `liveCopyPaths`,
  `rolloutConfig`, `inheritanceBroken`.

## Why it matters

A live copy in AEM is a site that inherits from a blueprint. If
the blueprint is migrated to EDS, the live copy needs to be
migrated too (and its inheritance broken so it doesn't try to
inherit from an AEM blueprint that no longer exists).

## AI usage

None. Pure database walk.

## Failure modes

- **MSM relationship broken:** the agent records a `HIGH` issue
  and the page is flagged as "MSM-orphaned".
- **Blueprint not in scope:** the agent records a `MEDIUM`
  issue and the live copy is migrated independently.
