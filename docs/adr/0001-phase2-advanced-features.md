# ADR 0001 — Phase 2: Advanced Features (Master Prompt §33)

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** `aem-eds-modernizer` core module

## Context

The Master Prompt §33 mandates a Phase 2 feature set that takes the
baseline migration flow (Phase 1) and adds the intelligence, validation,
repair, dependency, and rollout capabilities a real AEM → EDS
modernization requires. Without Phase 2 the platform can only do a
"best-effort" dump-and-write; with it, the platform becomes an
operator-grade system that:

1. Reasons about the design system (Figma) instead of guessing.
2. Validates visual + a11y quality after migration instead of trusting
   the build.
3. Self-heals when validations fail, with bounded retries and
   per-attempt evidence.
4. Understands the dependency graph so changes don't silently break
   other pages.
5. Uses a configurable rollout policy (PREVIEW → INTERNAL → CANARY →
   BATCH → BROAD → FULL) instead of a single publish.
6. Records benchmark data so the platform can learn over time.
7. Offers pluggable authoring strategies (Universal Editor, doc-based,
   existing-repo, custom adapter).

## Decision

Phase 2 is implemented as **opt-in agents** that the existing
`MigrationState` machine calls via `Orchestrator.invokeIfRegistered(...)`.
The state machine is **not changed**; the new agents run alongside the
Phase 1 agents at the same stages. The principle is:

> *"Phase 1 must keep working unchanged. Phase 2 augments, it does not
> replace."*

The advanced agents' names are distinct from the basic agents
(`advanced-visual-validation`, `advanced-repair`,
`advanced-rollout`, `figma-intelligence`) so both can coexist in the
`Orchestrator.agents` map. (This is the rule that fixed the
"Repairs = 0" bug observed before this ADR was written — see the
test in `AdvancedRepairAgentTest`.)

## Components

### New agents

| Agent | Stage | Responsibility |
|---|---|---|
| `AdvancedFigmaIntelligenceAgent` | `DESIGN_ANALYSIS` | Component → block pairing, theme tokens, figma-component-map.json |
| `AdvancedVisualValidationAgent` | `VALIDATING` | Representative sampling per template + 5% random + high-risk; image-diff via `ImageDiffEngine` |
| `AdvancedRepairAgent` | `REPAIRING` | Failure-mode classification, per-attempt records, safe-retry (max 5) |
| `AdvancedRolloutAgent` | `READY_TO_PUBLISH` | 6-stage rollout with stop conditions |

### New services

- `UrlRedirectService` — AEM-path → EDS-path redirect map, conflict detection
- `DependencyGraphService` — PAGE / SHARED / PROJECT edges, `impactedPages(block)`
- `AuthoringStrategyRegistry` — 4 built-in strategies
- `RolloutPolicy` — data class for stage definitions and stop conditions
- `BenchmarkService` — rolling P50/P95 per agent, persisted as `BenchmarkSampleRecord`
- `ImageDiffEngine` — pure-Java 64×64 downsample + Euclidean RGB color distance

### New persistence models

- `UrlRedirectRecord`
- `DependencyEdgeRecord`
- `RolloutStageRecord`
- `RepairAttemptRecord`
- `BenchmarkSampleRecord`

### New persistence bindings

- **`JcrStore`** — `service.ranking=200`. Persists projects as
  `nt:unstructured` nodes under `/var/aem-eds-modernizer/projects/{yyyy}/{MM}/{projectId}`
  with `eds:*` namespaced properties. The `eds` namespace is
  registered via Repo Init. Node names are escaped with
  `Text.escapeIllegalJcrChars()`.
- **`JsonFileStore`** — `service.ranking=100`. JSON-file snapshot
  for local dev and standalone mode.
- **`InMemoryStore`** — no ranking (fallback). `ConcurrentHashMap`
  for standalone mode when no `SlingRepository` is available.

### New connector

- `PlaywrightBrowserClient` — uses reflection to detect Playwright on
  the classpath; falls back to a deterministic 16×16 PNG generator for
  the MVP / e2e flow.

### New dashboard endpoints

- `GET /api/projects/{id}/redirects` — URL redirect map
- `GET /api/projects/{id}/dependencies` — dependency graph
- `GET /api/projects/{id}/rollout-stages` — latest job's rollout
- `GET /api/projects/{id}/repairs` — cross-job repair history
- `GET /api/projects/{id}/benchmarks` — agent duration / cost samples

### Dashboard UI

A new "Phase 2" nav group in the static dashboard provides
`#/redirects`, `#/dependencies`, `#/rollout`, `#/repairs`,
`#/benchmarks` views.

## Migration strategy

Phase 1 stays the default. If the advanced agents are not registered
(e.g. a slimmed-down deployment), the basic agents still run. The
e2e script proves both layers work: Phase 1 endpoints return
non-zero values (pages, components, validations, generated files)
and Phase 2 endpoints return their own non-zero values
(redirects, edges, stages, repairs, benchmarks).

## Consequences

### Positive

- Visual + a11y regressions are detected automatically.
- Repairs happen with evidence and bounded retry, not silently.
- Rollout halts on a real stop condition (e.g. visual score drop),
  not a hard-coded "publish everything" call.
- Benchmark data accumulates so the platform can optimise over time.
- The same code path runs in mock mode (Playwright absent) and real
  mode (Playwright present), thanks to the reflection-based detection.

### Negative

- Phase 2 adds ~12 new files and 5 new endpoints; the surface area
  is wider, but each component is independently testable.
- The advanced repair agent re-validates after each patch; that costs
  one AI round-trip per attempt. The default `maxRepairAttempts=5`
  caps this, but operators should be aware.

## Bug history

The first e2e run after Phase 2 wiring returned `Repairs = 0`. The
root cause was that `AdvancedVisualValidationAgent` was registered
as `"visual-validation"`, colliding with the basic
`VisualValidationAgent` and being shadowed by the later registration
in `StandaloneMain`. The orchestrator's
`invokeIfRegistered("advanced-visual-validation", ...)` never matched.
Fix: the advanced agent now registers as
`"advanced-visual-validation"`, the basic one keeps
`"visual-validation"`, and the e2e proves 23 repair attempts across
23 successful patches.
