# ADR 0015 — Phase 1 and Phase 2 Agents Coexist in the Same Map

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Orchestrator, agent registration

## Context

The Master Prompt's Phase 1 ships a baseline set of agents
(`ValidationAgent`, `VisualValidationAgent`, `SelfRepairAgent`,
`PublishingAgent`, `FigmaAnalysisAgent`). Phase 2 adds advanced
versions (`AdvancedVisualValidationAgent`,
`AdvancedRepairAgent`, `AdvancedRolloutAgent`,
`AdvancedFigmaIntelligenceAgent`).

The orchestrator's `agents` map is keyed by agent name
(`agent.name()`). If two agents register the same name, the
later registration overwrites the earlier one. This is a silent
bug: the earlier agent is "gone" from the orchestrator's view,
but the dashboard still lists it in the available agents, and
its tests still pass.

## Decision

The Phase 1 and Phase 2 agents use **distinct names** in the
`agents` map:

| Phase 1 | Phase 2 |
|---|---|
| `visual-validation` | `advanced-visual-validation` |
| `self-repair` | `advanced-repair` |
| `publishing` | `advanced-rollout` |
| `figma-analysis` | `figma-intelligence` |

The orchestrator's `runStage` method calls both
(`invokeIfRegistered("advanced-visual-validation", ctx)` and
`invokeIfRegistered("visual-validation", ctx)`) so both can
coexist. If only one is registered, the other is silently
skipped.

## Bug history

The first e2e run after Phase 2 wiring returned `Repairs = 0`.
The root cause was that `AdvancedVisualValidationAgent` was
registering as `"visual-validation"`, colliding with the basic
`VisualValidationAgent`. The basic one (registered later in
`StandaloneMain`) overwrote the advanced one. The orchestrator's
`invokeIfRegistered("advanced-visual-validation", ...)` never
matched, so the advanced visual validation never ran, no
validations were produced, and the repair agent had nothing
to repair.

The fix was renaming the advanced agent's `super(...)` to
`"advanced-visual-validation"`. The e2e then produced 23
repair records, all successful.

## Consequences

### Positive

- **Both layers work in the same deployment.** A customer that
  upgrades from Phase 1 to Phase 2 sees the advanced agents
  activate automatically.
- **Backwards compatible.** A customer that stays on Phase 1
  (no advanced agents registered) sees the baseline
  behaviour. The orchestrator skips the missing advanced
  agents with a debug log.
- **The naming convention is enforceable.** A linter / code
  review rule can check that any new `Advanced*` agent
  registers as `advanced-{base-name}`.

### Negative

- **Two names for similar things.** New developers may be
  confused why there are two `visual-validation` agents. The
  code comment in `Orchestrator.runStage` explains the
  pattern.
- **The advanced agents must implement the same `Agent`
  contract.** They don't get a free pass to change the
  interface; they must coexist with the basic agents'
  lifecycle.

## Alternatives considered

- **Replace Phase 1 with Phase 2** (use the advanced agents
  only): rejected because the Master Prompt asks for
  backwards compatibility, and some deployments may need the
  simpler Phase 1 behaviour.
- **Use a different map key** (e.g. versioned:
  `visual-validation@1.0.0` vs `visual-validation@2.0.0`):
  rejected as over-engineered; the `advanced-` prefix is
  enough.

## Related

- [../agents/](../agents/) — one doc per agent; the advanced
  agents cross-reference their basic counterparts.
- [ADR 0001](0001-phase2-advanced-features.md) — the Phase 2
  decision record.
- [Orchestrator.java](../../core/src/main/java/com/adobe/aem/modernizer/agents/Orchestrator.java)
  — the `invokeIfRegistered` method.
