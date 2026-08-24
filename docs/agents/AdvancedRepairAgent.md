# AdvancedRepairAgent

> Phase 2 advanced self-repair. Extends the basic
> `SelfRepairAgent` with:
>
> - **Failure-mode classification** (VISUAL / A11Y / CONTENT /
>   FUNCTIONAL / STRUCTURAL).
> - **Per-repair records** with evidence, proposed fix, actual
>   change, and validation result.
> - **Safe-retry** (max 5 attempts, with rollback checkpoints).
> - **Budget enforcement** via the `AiGateway`.

- **Stage:** `REPAIRING`
- **Phase:** 2
- **Agent name:** `advanced-repair`
- **Task type:** `REPAIR`

## Inputs

- The failed `ValidationResultRecord`s for the current job
  (any validation with `score < 0.95`).

## Outputs

- `RepairAttemptRecord`s with: `pagePath`, `attemptNumber`,
  `issueId`, `evidence`, `proposedFix`, `actualChange`,
  `validationResult`, `validationScore`, `success`, `durationMs`,
  `estimatedCost`, `failureReason`, `diff{before, after}`.
- Updated `ValidationResultRecord`s (score bumped by 0.05 per
  successful attempt, capped at 1.0).
- Updated `IssueRecord`s (severity, status, repairAttempts).

## Failure-mode classification

For each failed validation:

| Validation `kind` | Failure mode |
|---|---|
| `VISUAL` | `VISUAL` |
| `ACCESSIBILITY` | `A11Y` |
| `SEO` or `CONTENT` | `CONTENT` |
| `VERIFICATION` | `FUNCTIONAL` |
| anything else | `STRUCTURAL` |

The failure mode is recorded on the corresponding
`IssueRecord` and influences the repair strategy.

## AI usage

One AI call per failed validation per attempt. The mock
provider returns a deterministic CSS comment patch.

## Performance

- 1 AI call per failed validation per attempt.
- Mock mode: ~5 ms per attempt.
- Real mode: 2-5 seconds per attempt.

## Cross-job query

The dashboard's `/api/projects/{id}/repairs` endpoint
returns the cross-job repair history (any attempt, any
job), so the operator can see the full repair picture.

## Related

- [SelfRepairAgent](SelfRepairAgent.md) — the Phase 1 basic
  version.
- [ADR 0001](../adr/0001-phase2-advanced-features.md) — the
  Phase 2 decision record.
