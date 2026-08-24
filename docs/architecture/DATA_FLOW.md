# Data Flow

This document describes how data moves through the system during a
migration. It is the companion to [REQUEST_FLOW.md](REQUEST_FLOW.md):
that document shows the *control* flow, this one shows the *data*
flow.

## Inputs

| Input | Where it comes from | Where it lands |
|---|---|---|
| AEM content tree | `AemClient.listPages(contentRoot)` | `AemPageRecord` (one per page) |
| AEM components | `AemClient.getComponentUsages()` | `ComponentRecord` (one per component resource type) |
| AEM assets (metadata) | `AemClient.getAssetMetadata()` | `AssetRecord` (one per referenced asset) |
| AEM live copies | `AemClient.getMsmInfo()` | analysis events (no persistent record) |
| AEM templates | `AemClient.getTemplateInfo()` | analysis events |
| Figma file | `FigmaClient.getFile()` | `GeneratedFileRecord` (themes.css, figma-tokens.json, figma-component-map.json) |
| GitHub repo | `GitHubClient.getRepo()` | repository context (used by CodeGeneration) |
| EDS preview URL | `EdsClient.getPreviewUrl()` | (read by `PreviewAgent` to know where to deploy) |

## Processing

### Discovery

The `DiscoveryAgent` walks the AEM content tree from the configured
`contentRoot`. For each page it:

1. Reads the page's properties via `AemClient.getPage(path)`.
2. Evaluates the `MarkerEvaluator` to decide eligibility.
3. Persists an `AemPageRecord` with the path, template, components,
   eligibility status, and a list of `componentTypes`.

### Analysis

The analysis stage fans out to five agents:

- `ComponentIntelligenceAgent` reads the `ComponentRecord`s, calls
  the AI gateway to extract field descriptions and detect variants.
- `TemplateAnalysisAgent` reads AEM templates, extracts structure
  rules.
- `ContentAnalysisAgent` reads content fragments and their
  references.
- `AssetAnalysisAgent` validates each asset reference and persists
  an `AssetRecord`.
- `MsmAnalysisAgent` walks live copies, records dependencies.

### Design analysis (Phase 2)

`AdvancedFigmaIntelligenceAgent` reads the Figma file, uses the AI
gateway to pair components with blocks, and writes
`themes.css`, `figma-tokens.json`, and `figma-component-map.json`
as `GeneratedFileRecord`s.

### Planning

`MigrationPlannerAgent` aggregates the analysis output and produces:

- A `MigrationPlan` (sequence, agent, stage, method).
- A pre-implementation estimate (`AIRequestsExpected`,
  `CostExpected`, `TimeExpectedSec`, `PagesEligible`,
  `EdsBlocksNew`).
- A task plan with derivation trail.

### Building

`BlockGenerationAgent` and `CodeGenerationAgent` write
`GeneratedFileRecord`s for the EDS repo scaffold:

- `blocks/{name}/{name}.js` and `{name}.css` (one per EDS block)
- `scripts.js` and `styles.css` (theme)
- `fstab.yaml`, `README.md`, `package.json`
- `tokens.css` (the design tokens)

### Migrating

`ContentMigrationAgent` walks every eligible page, converts the
AEM content to EDS section models (`.md` files), and persists each
as a `GeneratedFileRecord` with `operation=CREATE`,
`stage=CONTENT_MIGRATION`, and `path={eds-path}.md`.

`UrlRedirectService` (Phase 2) is invoked at the same stage to
build the AEM → EDS redirect map.

`DependencyGraphService` (Phase 2) is invoked at the same stage to
build the dependency graph.

### Authoring

`AuthoringAgent` creates AEM Universal Editor–compatible page
structures in the target. Each authored page is recorded as an
`AemPageRecord` with `migrationStatus=AUTHORED`.

### Previewing

`PreviewAgent` deploys the generated files to the EDS preview URL
and records the deploy event.

### Validating

`ValidationAgent` and `AdvancedVisualValidationAgent` (Phase 2) read
the deployed site via the `BrowserClient` and persist
`ValidationResultRecord`s for each page, broken down by kind
(`CONTENT`, `VISUAL`, `FUNCTIONAL`, `SEO`, `ACCESSIBILITY`, `PERF`).

### Repairing

`SelfRepairAgent` and `AdvancedRepairAgent` (Phase 2) read the
failed validations, propose patches, and persist
`RepairAttemptRecord`s with evidence, proposed fix, actual change,
and validation result.

### Rollout (Phase 2)

`AdvancedRolloutAgent` reads the validations, builds the rollout
stages (`PREVIEW`, `INTERNAL_VALIDATION`, `CANARY`, `BATCH`,
`BROAD`, `FULL`), and persists a `RolloutStageRecord` per stage
with status (`COMPLETED` / `HALTED`), percentage, and the
included pages.

### Verifying

`VerificationAgent` reads the production site, runs the full
validation suite again, and produces the migration report.

## Outputs

| Output | Where it's persisted | Where it's surfaced |
|---|---|---|
| EDS block JS / CSS | `GeneratedFileRecord` | `#/diff` (virtual Git diff) |
| Section model `.md` | `GeneratedFileRecord` | `#/diff` |
| Repo scaffold (`fstab.yaml`, etc.) | `GeneratedFileRecord` | `#/diff` |
| AEM UE page structures | AEM Author | (external to the modernizer) |
| URL redirects | `UrlRedirectRecord` | `#/redirects` |
| Dependency edges | `DependencyEdgeRecord` | `#/dependencies` |
| Repair attempts | `RepairAttemptRecord` | `#/repairs` |
| Rollout stages | `RolloutStageRecord` | `#/rollout` |
| Benchmark samples | `BenchmarkSampleRecord` | `#/benchmarks` |
| Migration report | `MigrationReport` JSON | `#/report` (download) |

## Data invariants

1. **Every persisted record carries a `projectId` and (where
   relevant) a `jobId`.** A query for "what happened on this
   project" always works.
2. **Every agent run emits a `JobEventRecord` before and after the
   work.** The dashboard reconstructs its state from events; there
   is no other source of truth.
3. **`GeneratedFileRecord.path` is always EDS-style** (e.g.
   `blocks/cards/cards.css`, never `/content/dam/...`).
4. **`ValidationResultRecord.score` is always in [0.0, 1.0].** The
   Advanced Repair agent bumps the score by 0.05 per successful
   attempt, capped at 1.0.
5. **AI decisions are never deleted.** They accumulate so the
   cost/token dashboard can show historical trends.
6. **Generated files are addressable by `path` within a job.** Two
   records with the same path in the same job are updates, not
   duplicates.
