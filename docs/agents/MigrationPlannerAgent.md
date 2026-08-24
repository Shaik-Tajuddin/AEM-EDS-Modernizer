# MigrationPlannerAgent

> Builds the migration plan (sequence, agent, stage, method)
> and the pre-implementation estimate (AI requests, cost, time,
> pages, blocks).

- **Stage:** `PLANNING`
- **Phase:** 1
- **Agent name:** `migration-planner`
- **Task type:** `PLANNING`

## Inputs

- The `SiteInventory` (pages, components, mappings).
- The `EstimatorService` configuration.

## Outputs

- `MigrationPlan` with: `sequences[]`, `stages[]`, `methods[]`.
- `Estimate` with: `aiRequestsExpected`, `costExpected`,
  `timeExpectedSec`, `pagesEligible`, `edsBlocksNew`.
- A `taskPlan[]` with the derivation trail (so the user can
  see *why* each number was produced).

## AI usage

Optional. The agent can call `AiGateway.dispatch(...)` with
`taskType=PLANNING` to ask the AI for the optimal stage
ordering (e.g. "should we migrate content first or generate
blocks first?"). The mock provider returns a sensible default
ordering.

## Failure modes

- **No pages in scope:** the agent records a `CRITICAL` issue
  and the migration cannot proceed.
- **AI returns invalid plan:** the agent falls back to the
  default linear ordering (DISCOVER → ANALYZE → PLAN → BUILD
  → MIGRATE → AUTHOR → PREVIEW → VALIDATE → REPAIR → PUBLISH).

## Performance

- One (optional) AI call.
- Estimate derivation is pure arithmetic: < 1 ms.

## Related

- [Pre-implementation Estimate](../migration/ESTIMATE.md) —
  the full estimate model.
- [Master §0A.2A, §0A.2B](../README.md) — the estimate
  requirements.
