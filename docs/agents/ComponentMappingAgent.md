# ComponentMappingAgent

> Maps every AEM component to one or more EDS blocks, with
> confidence, reasoning, and evidence.

- **Stage:** `ANALYZING`
- **Phase:** 1
- **Agent name:** `mapping`
- **Task type:** `MAPPING`

## Inputs

- The `ComponentRecord`s (after `ComponentIntelligenceAgent`).

## Outputs

- `ComponentMappingRecord`s with: `sourceComponent`,
  `targetBlock`, `confidence`, `reason`, `decisionSource`.

## AI usage

For each unique component type, the agent calls
`AiGateway.dispatch(...)` with `taskType=MAPPING` and the
component's resource type, field descriptions, and the EDS
block catalogue. The AI returns a JSON object with the
suggested block, the confidence, and the reasoning.

The mock provider produces a confidence between 0.5 and 0.99
deterministically per component.

## Failure modes

- **Confidence below 0.7:** the mapping is recorded with
  `decisionSource=AI` but the page is flagged as "high-risk"
  for visual validation (Phase 2).
- **No suitable block found:** the mapping is recorded with
  `targetBlock=generic-block` and a `CRITICAL` issue is
  created.

## Performance

- One AI call per unique component type.
- Mock mode produces ~15 mappings in < 100 ms.
- Real mode typically 1-3 seconds per call.

## Related

- [Master §17](../README.md) — the mapping decision model.
