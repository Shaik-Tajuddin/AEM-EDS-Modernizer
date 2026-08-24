# AdvancedRolloutAgent

> Phase 2 advanced rollout. Replaces the single-shot
> `PublishingAgent` with a stage-by-stage rollout:
> **PREVIEW → INTERNAL_VALIDATION → CANARY → BATCH → BROAD →
> FULL**. Each stage has an explicit stop condition; when the
> condition triggers, the rollout halts and the operator is
> notified.

- **Stage:** `READY_TO_PUBLISH`
- **Phase:** 2
- **Agent name:** `advanced-rollout`
- **Task type:** `ROLLOUT`

## Default rollout policy

| Stage | % pages | Stop condition |
|---|---|---|
| `PREVIEW` | 0% | `MANUAL_APPROVAL` (operator must approve) |
| `INTERNAL_VALIDATION` | 5% | `A11Y_SCORE_ABOVE_THRESHOLD` (≥ 0.95) |
| `CANARY` | 5% | `NO_BREAKING_ERRORS` (zero 5xx in 5 min) |
| `BATCH` | 25% | `PERF_LCP_BELOW_THRESHOLD` (LCP < 2.5s) |
| `BROAD` | 75% | `VISUAL_SCORE_ABOVE_THRESHOLD` (≥ 0.90) |
| `FULL` | 100% | `MANUAL_APPROVAL` (operator must approve) |

If a stop condition fails, the rollout halts at that stage;
the operator is notified; the dashboard shows the halt
reason and the measured metrics.

## Inputs

- The `RolloutPolicy` (default or custom).
- The validations for the current job.
- The list of pages to roll out.

## Outputs

- `RolloutStageRecord`s with: `stage`, `percentage`, `status`
  (`COMPLETED` / `HALTED`), `includedPages`,
  `haltReason`, `metrics`.

## AI usage

The agent can call `AiGateway.dispatch(...)` to ask the AI
for a risk assessment at each stage ("based on these
metrics, should we proceed?"). The mock provider returns
`proceed=true`.

## Failure modes

- **Stop condition triggers:** the rollout halts; the
  remaining stages are not executed. The job transitions to
  `WAITING_FOR_CLARIFICATION` if the operator must
  intervene.
- **No pages in scope:** the agent records a `MEDIUM` issue
  and completes immediately.

## Custom policies

Operators can register a custom `RolloutPolicy` by:

1. Implementing the `RolloutPolicy` data class with a
   different list of `Stage` objects.
2. Wiring it into `StandaloneMain` (or the OSGi component
   for AEM Cloud).

## Related

- [PublishingAgent](PublishingAgent.md) — the Phase 1 basic
  version.
- [RolloutPolicy](../architecture/COMPONENTS.md) — the
  policy data class.
- [ADR 0001](../adr/0001-phase2-advanced-features.md) — the
  Phase 2 decision record.
