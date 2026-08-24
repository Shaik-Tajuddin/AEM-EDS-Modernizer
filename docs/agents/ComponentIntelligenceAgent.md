# ComponentIntelligenceAgent

> Analyses every AEM component used in the scope, extracting field
> descriptions, variant groups, and complexity metrics.

- **Stage:** `ANALYZING`
- **Phase:** 1
- **Agent name:** `component-intelligence`
- **Task type:** `COMPONENT_INTELLIGENCE`

## Inputs

- The `ComponentRecord`s for the project's components (from the
  `AssetAnalysisAgent` and the `DiscoveryAgent` cross-product).

## Outputs

- Updated `ComponentRecord`s with: `fieldDescriptions`,
  `variantGroup`, `complexity`, `analysisSource` (`DETERMINISTIC`
  or `AI`).

## AI usage

For each unique component type, the agent calls
`AiGateway.dispatch(...)` with `taskType=COMPONENT_INTELLIGENCE`
and the component's resource type and example fields. The AI
returns a JSON object with the field descriptions, the
suggested variant group, and a complexity score.

## Failure modes

- **AI returns invalid JSON:** the agent retries up to 2 times;
  if still failing, falls back to deterministic analysis
  (`analysisSource=DETERMINISTIC`).
- **Capability gate refusal** (e.g. `CAP_CODE` missing): the
  agent falls back to deterministic analysis; the AI is
  reconfigured for next time.

## Performance

- One AI call per unique component type (typically 10-30 calls
  per project).
- Mock mode produces a deterministic response in < 1 ms.
- Real mode typically 1-3 seconds per call.
