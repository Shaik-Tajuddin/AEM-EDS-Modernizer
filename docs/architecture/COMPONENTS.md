# Components

Every module in the AEM → EDS Modernizer, its responsibility, its
public surface, and its dependencies.

## Reactor modules

| Module | Packaging | Purpose |
|---|---|---|
| `core` | `bundle` (OSGi) | All agents, connectors, AI gateway, dashboard |
| `ui.apps` | `content-package` | Immutable content: HTL templates, clientlibs, OSGi install hook |
| `ui.config` | `content-package` | OSGi configurations and Repo Init |
| `ui.content` | `content-package` | Mutable baseline content: sample projects, dashboard pages |
| `dispatcher` | `content-package` | Apache HTTP Server + Dispatcher vhost config |
| `all` | `content-package` | Single deployable container; depends on the four above |

## `core` package layout

| Package | Responsibility | Public surface |
|---|---|---|
| `agents/` | 20 specialised migration agents (per Master §2) | `Agent`, `AgentContext`, `AgentResult` |
| `ai/` | AI gateway, provider adapters, capability registry, routing policy | `AiGateway`, `ChatRequest`, `ChatResponse`, `ModelCapability` |
| `connectors/` | External-system adapters (AEM, GitHub, Figma, EDS, browser) | `AemClient`, `GitHubClient`, `FigmaClient`, `EdsClient`, `BrowserClient` |
| `persistence/` | Store interface + InMemory binding + record types | `Store`, `InMemoryStore`, `DashboardSnapshot` |
| `dashboard/` | HTTP API and SPA | `ApiRouter`, `StaticDashboard`, `DashboardApi` |
| `state/` | Migration state machine | `MigrationState` |
| `estimate/` | Pre-implementation estimate | `EstimatorService` |
| `dryrun/` | Dry Run service | `DryRunService` |
| `report/` | Migration report generation | `MigrationReportService` |
| `clarification/` | Clarification engine | `ClarificationService` |
| `redirect/` | URL redirect map (Phase 2) | `UrlRedirectService` |
| `dependency/` | Dependency graph (Phase 2) | `DependencyGraphService` |
| `authoring/` | Authoring strategy registry (Phase 2) | `AuthoringStrategy`, `AuthoringStrategyRegistry` |
| `rollout/` | Rollout policy (Phase 2) | `RolloutPolicy` |
| `benchmark/` | Historical optimization (Phase 2) | `BenchmarkService` |
| `diff/` | Image diff engine (Phase 2) | `ImageDiffEngine` |
| `security/` | Redactor, URL guard, exception types | `Redactor`, `UrlGuard`, `ConnectorException` |
| `ssrf/` | SSRF protection | (see `security/UrlGuard`) |
| `events/` | Job event records | `JobEventRecord` |
| `checkpoint/` | Checkpoint records | `CheckpointRecord` |
| `repair/` | Repair policy | (per-attempt records live in `persistence/`) |
| `mcp/` | MCP adapter placeholders | (interfaces only in MVP) |
| `mock/` | Mock connectors for standalone mode | `MockAemClient`, `MockGitHubClient`, `MockFigmaClient`, `MockEdsClient`, `MockBrowserClient` |
| `standalone/` | Non-OSGi `main()` | `StandaloneMain` |
| `util/` | JSON, ID gen, duration formatting | `Json`, `IdGen`, `DurationFormat` |

## Agents (20 specialised, per Master §2)

| Agent | Stage | Reads | Writes |
|---|---|---|---|
| `ConnectionAgent` | `CONNECTING` | AEM/GitHub/Figma/EDS endpoints | connection cards |
| `DiscoveryAgent` | `DISCOVERING` | AEM content tree | `AemPageRecord` |
| `ComponentIntelligenceAgent` | `ANALYZING` | pages, components | `ComponentRecord` |
| `ComponentMappingAgent` | `ANALYZING` | `ComponentRecord` | `ComponentMappingRecord` |
| `TemplateAnalysisAgent` | `ANALYZING` | AEM templates | (analysis events) |
| `ContentAnalysisAgent` | `ANALYZING` | AEM content fragments | (analysis events) |
| `AssetAnalysisAgent` | `ANALYZING` | AEM DAM metadata | `AssetRecord` |
| `ContentFragmentAnalysisAgent` | `ANALYZING` | AEM content fragments | (analysis events) |
| `MsmAnalysisAgent` | `ANALYZING` | AEM live copies | (analysis events) |
| `FigmaAnalysisAgent` | `DESIGN_ANALYSIS` | Figma file | design tokens |
| `AdvancedFigmaIntelligenceAgent` | `DESIGN_ANALYSIS` | Figma file | `themes.css`, `figma-tokens.json`, `figma-component-map.json` |
| `MigrationPlannerAgent` | `PLANNING` | pages, components, mappings | plan, estimate |
| `BlockGenerationAgent` | `BUILDING` | mappings, templates | EDS block JS + CSS |
| `CodeGenerationAgent` | `BUILDING` | project, mappings | repo scaffold (`fstab.yaml`, `README.md`, `scripts.js`, `styles.css`) |
| `ContentMigrationAgent` | `MIGRATING` | AEM pages | EDS section models (`.md`) |
| `AuthoringAgent` | `AUTHORING` | migrated content | AEM UE-compatible page structures |
| `PreviewAgent` | `PREVIEWING` | generated files | EDS preview deploy |
| `ValidationAgent` | `VALIDATING` | preview site | `ValidationResultRecord` |
| `VisualValidationAgent` | `VALIDATING` | preview site | `ValidationResultRecord` (kind=VISUAL) |
| `AdvancedVisualValidationAgent` | `VALIDATING` | preview site, image diff | `ValidationResultRecord` with representative sampling |
| `SelfRepairAgent` | `REPAIRING` | failed validations | repair records |
| `AdvancedRepairAgent` | `REPAIRING` | failed validations | `RepairAttemptRecord` with evidence |
| `PublishingAgent` | `READY_TO_PUBLISH` | preview site | Git PR |
| `AdvancedRolloutAgent` | `READY_TO_PUBLISH` | validations, pages | `RolloutStageRecord` (6 stages) |
| `VerificationAgent` | `VERIFYING` | production site | final report |

## Phase 2 services

| Service | Endpoint | Records |
|---|---|---|
| `UrlRedirectService` | `GET /api/projects/{id}/redirects` | `UrlRedirectRecord` |
| `DependencyGraphService` | `GET /api/projects/{id}/dependencies` | `DependencyEdgeRecord` |
| `BenchmarkService` | `GET /api/projects/{id}/benchmarks` | `BenchmarkSampleRecord` |
| `AuthoringStrategyRegistry` | (consulted by `AuthoringAgent`) | (no records; selects strategy) |

## External integrations

| Integration | Connector | Endpoint / contract |
|---|---|---|
| AEM Author | `AemClient` | `/api/assets`, `/content/{path}.json`, `/content/{path}` |
| AEM Publish | `AemClient` | read-only |
| GitHub | `GitHubClient` | REST API v3, PAT or GitHub App |
| Figma | `FigmaClient` | REST API v1, `figma.com/v1/files/{key}` |
| EDS | `EdsClient` | preview URL + Git-based deploy |
| Browser | `BrowserClient` | Playwright-shaped API |

## Dependencies

- `core` depends on public OSGi APIs only (`org.osgi.framework`,
  `org.osgi.service.component`, `org.apache.sling.api`,
  `org.apache.sling.servlets.annotations`, `javax.jcr`).
- Third-party libs are all compile-scope:
  - Jackson (`com.fasterxml.jackson.core:jackson-databind`)
  - OkHttp (`com.squareup.okhttp3:okhttp`) for HTTP connectors
  - SLF4J (`org.slf4j:slf4j-api`) for logging
  - The standalone runtime bundles them into a fat jar via
    `maven-shade-plugin`.
- The OSGi bundle does **not** import any third-party libs; the
  import-package policy is `*; resolution:=optional` to keep the
  AEM-Cloud-recommended pattern (compile against public APIs only).
