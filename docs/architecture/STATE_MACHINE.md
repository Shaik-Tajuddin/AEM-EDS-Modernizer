# Migration State Machine

The orchestrator's state machine is the single source of truth for
"where is this migration right now?". Every agent runs inside a
state transition; the state machine guards ordering, retries,
clarifications, and human-approval gates.

## States

```
CREATED → CONNECTING → DISCOVERING → ANALYZING
       → DESIGN_ANALYSIS → PLANNING → BUILDING
       → MIGRATING → AUTHORING → PREVIEWING
       → VALIDATING → REPAIRING → READY_TO_PUBLISH
       → PUBLISHING → VERIFYING → COMPLETED
```

At any point the job can transition to:

- `FAILED` (with `lastError`)
- `CANCELLED` (by user)
- `WAITING_FOR_CLARIFICATION` (Master §38)

Every transition is guarded by `MigrationState.canTransitionTo(next)`.

## State descriptions

| State | What happens | Agent(s) |
|---|---|---|
| `CREATED` | Job is created, no work yet | (none) |
| `CONNECTING` | Test reachability + auth for every external system | `ConnectionAgent` |
| `DISCOVERING` | Walk the AEM content tree, evaluate eligibility | `DiscoveryAgent` |
| `ANALYZING` | Component, template, content, asset, MSM analysis | `ComponentIntelligenceAgent`, `ComponentMappingAgent`, `TemplateAnalysisAgent`, `ContentAnalysisAgent`, `AssetAnalysisAgent`, `ContentFragmentAnalysisAgent`, `MsmAnalysisAgent` |
| `DESIGN_ANALYSIS` | Figma design analysis (Phase 1 + Phase 2) | `FigmaAnalysisAgent`, `AdvancedFigmaIntelligenceAgent` |
| `PLANNING` | Build the migration plan and the estimate | `MigrationPlannerAgent` |
| `BUILDING` | Generate EDS blocks, code, styles | `BlockGenerationAgent`, `CodeGenerationAgent` |
| `MIGRATING` | Content → EDS section models; redirects + dependencies | `ContentMigrationAgent`, `UrlRedirectService`, `DependencyGraphService` |
| `AUTHORING` | AEM UE-compatible authoring | `AuthoringAgent` (+ `AuthoringStrategyRegistry`) |
| `PREVIEWING` | Deploy to EDS preview | `PreviewAgent` |
| `VALIDATING` | Functional + visual + a11y validation | `ValidationAgent`, `VisualValidationAgent` (+ `AdvancedVisualValidationAgent`) |
| `REPAIRING` | Self-repair on failed validations | `SelfRepairAgent` (+ `AdvancedRepairAgent`) |
| `READY_TO_PUBLISH` | Human gate; the dashboard's `MIGRATE` button | (no agent; user click) |
| `PUBLISHING` | Git PR + (in Phase 2) staged rollout | `PublishingAgent` (+ `AdvancedRolloutAgent`) |
| `VERIFYING` | Final production crawl | `VerificationAgent` |
| `COMPLETED` | Terminal success | (none) |

## Failure transitions

- `ANY_STATE → FAILED`: an unhandled exception in an agent, or an
  explicit operator cancel, or `BUDGET_EXCEEDED` from the AI
  gateway. The `lastError` field is populated and the dashboard
  shows the stack trace.
- `VALIDATING → WAITING_FOR_CLARIFICATION`: the AI gateway returns
  a low-confidence result that needs human input. The clarification
  service records the question; the operator answers on
  `#/clarifications`; the agent retries.
- `REPAIRING → FAILED`: the repair budget (5 attempts) is
  exhausted. The job moves to `FAILED` and the operator must
  intervene.

## Phase 1 vs Phase 2

The Phase 2 agents are **opt-in** via
`Orchestrator.invokeIfRegistered(name, ctx)`. The state machine
itself is unchanged. If the advanced agent is not registered, the
basic Phase 1 agent still runs.

The agent names are deliberately distinct so both can coexist in
the `agents` map:

| Phase 1 (basic) | Phase 2 (advanced) |
|---|---|
| `visual-validation` | `advanced-visual-validation` |
| `self-repair` | `advanced-repair` |
| `publishing` | `advanced-rollout` |
| `figma-analysis` | `figma-intelligence` |

This is the rule that fixed the "Repairs = 0" bug: the
`AdvancedVisualValidationAgent` was originally registered as
`"visual-validation"` (same as the basic one) and was shadowed by
the later registration. Renaming to `"advanced-visual-validation"`
made the orchestrator's lookup work and produced 23 repair records
in the e2e.

## Human-approval gate

`READY_TO_PUBLISH` is a terminal state that requires a user click.
The `MIGRATE` button in the dashboard is only enabled when:

- The Dry Run has completed.
- No critical blockers exist.
- The target connection is valid.
- The estimate is `CURRENT` (not stale).
- Project policy allows the target environment.

## Checkpoints

Per Master §40, every state transition persists a `CheckpointRecord`
with:

- The current state
- The next state
- A timestamp
- An optional resumption hint (e.g. "resume from this generated
  file index")

A migration that is interrupted (process restart, network
partition) can resume from the last checkpoint without redoing
upstream work.

## Idempotency

Every agent is idempotent: re-running it on the same input
produces the same output. This is what makes retries safe and
what makes resumption work. The `Store` interface uses
`upsert(map, record, keyFn)` for every collection, so a second
run overwrites the first rather than duplicating.

## Observability

- Every state transition emits a `JobEventRecord` with the
  `fromState`, `toState`, `agent`, and `message`.
- The dashboard polls `GET /api/projects/{id}/events` and
  reconstructs the run from these events.
- A refresh always recovers the correct view; there is no
  in-memory state on the browser side.
