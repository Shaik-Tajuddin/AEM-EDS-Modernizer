# AEM → EDS Experience Modernizer

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/Shaik-Tajuddin/AEM-EDS-Modernizer)
[![Java](https://img.shields.io/badge/Java-11%20%7C%2017-blue.svg)](https://www.oracle.com/java/)
[![AEM](https://img.shields.io/badge/AEM-Cloud%20Service%20%7C%206.5+-orange.svg)](https://experienceleague.adobe.com/)
[![Edge Delivery Services](https://img.shields.io/badge/Adobe-Edge%20Delivery%20Services-red.svg)](https://www.aem.live/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

> **Enterprise-grade, autonomous platform deployed inside Adobe Experience Manager (AEM) to assess, plan, migrate, validate, self-repair, and publish web experiences to Adobe Edge Delivery Services (EDS).**

---

## 📖 Table of Contents

- [Overview & Mission](#-overview--mission)
- [Architecture & Multi-Module Layout](#-architecture--multi-module-layout)
- [Core Features & Subsystems](#-core-features--subsystems)
- [The 25 Modernizer Agents](#-the-25-modernizer-agents)
- [Estimation & Safety Guardrails](#-estimation--safety-guardrails)
- [Root-Path ↔ Blocks ↔ Page Reference System](#-root-path--blocks--page-reference-system)
- [Dashboard Reference (Pages & Scope / Blocks)](#-dashboard-reference-pages--scope--blocks)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [1. Running as Standalone Server (Local / Offline)](#1-running-as-standalone-server-local--offline)
  - [2. Building and Deploying to AEM Cloud](#2-building-and-deploying-to-aem-cloud)
- [REST API Reference](#-rest-api-reference)
- [Testing & Validation](#-testing--validation)
- [Documentation Index](#-documentation-index)

---

## 🎯 Overview & Mission

Migrating enterprise content and components from traditional AEM (HTL, WCM Core Components, Dialogs, ClientLibs) to Adobe Edge Delivery Services (EDS Markdown section models, Vanilla JS/CSS blocks) requires rigorous planning, structural transformation, and regression safety.

The **AEM → EDS Experience Modernizer** provides an explainable, safe, measurable, resumable, and enterprise-ready control plane for this transformation with two distinct runtime modes:

1. **Inside AEM as a Cloud Service / 6.5+**: As an OSGi bundle and Content Package accessible at `/aem-eds-modernizer` and `/content/aem-eds-modernizer/home.html`.
2. **Standalone Executable**: As a self-contained shaded fat JAR with an embedded HTTP server on port `8080` for CI/CD pipelines and local experimentation.

---

## 🏗 Architecture & Multi-Module Layout

The project is structured as a standard Adobe Cloud Manager compliant Maven multi-module reactor:

```
AEM-EDS-Modernizer/
├── pom.xml                                      # Parent reactor POM (Java 11 source/target)
├── core/                                        # OSGi bundle + shaded standalone executable fat JAR
│   └── src/main/java/com/adobe/aem/modernizer/
│       ├── agents/                              # 25 Autonomous agents & Orchestrator state machine
│       ├── ai/                                  # Multi-provider AI Gateway (OpenAI, Anthropic, Gemini, Ollama, Mock)
│       ├── connectors/                          # AEM, EDS, GitHub, Figma, Playwright connectors
│       ├── dashboard/                           # Inlined SPA control center & JSON REST API router
│       ├── mock/                                # Deterministic mock fixtures & test factories
│       ├── osgi/                                # Declarative Services Bundle Activator
│       ├── persistence/                         # In-memory & JCR persistence stores
│       ├── scopes/                              # Marker-based page & component eligibility evaluators
│       ├── security/                            # RBAC, audit logger & regex-based secret redactor
│       ├── services/                            # Estimator, Redirects, Dependency Graph, ImageDiff, Benchmarks
│       ├── ssrf/                                # Strict SSRF UrlGuard with private IP blocking
│       └── standalone/                          # Standalone HTTP Server runner (port 8080)
├── ui.apps/                                     # AEM HTL components, clientlibs, and Granite UI nodes
├── ui.config/                                   # OSGi configurations & Repo Init system user setup
├── ui.content/                                  # Seed JCR content packages & Modernizer dashboard nodes
├── dispatcher/                                  # Apache Dispatcher vhost & farm routing rules
├── all/                                         # Adobe Cloud Manager container package (.zip)
├── docs/                                        # 100 architectural, ADR, and operational guides
│   ├── adr/                                     # Architecture Decision Records (ADR 0001 - 0015)
│   ├── aem/                                     # AEM Cloud deployment, dispatcher & security docs
│   ├── agents/                                  # Individual agent specifications
│   ├── architecture/                            # Data flow, state machine, runtime topologies
│   ├── eds/                                     # Section models, block generation & pipeline conventions
│   ├── figma/                                   # Figma token extraction and component pairing
│   ├── github/                                  # PR templates, branch policies & operations
│   ├── migration/                               # Checkpoints, estimation formulas & scoping rules
│   ├── operations/                              # Observability, capacity planning & runbooks
│   ├── prompts/                                 # Product master prompts and specifications
│   └── security/                                # Capability gates, SSRF, redactor & secrets
└── scripts/                                     # Automated test runners & end-to-end scripts (e2e.sh)
```

---

## ⚡ Core Features & Subsystems

### 1. Deterministic 16-State Orchestration

Transitions are strictly guarded and event-driven:
`CREATED` → `CONNECTING` → `DISCOVERING` → `ANALYZING` → `DESIGN_ANALYSIS` → `PLANNING` → `BUILDING` → `MIGRATING` → `AUTHORING` → `PREVIEWING` → `VALIDATING` → `REPAIRING` → `READY_TO_PUBLISH` → `PUBLISHING` → `VERIFYING` → `COMPLETED`.

### 2. Multi-Provider AI Gateway & Routing Policy

- Seamless fallback and capability gating (`CAP_CHAT`, `CAP_STRUCTURED`, `CAP_CODE`, `CAP_VISION`, `CAP_LOCAL`).
- Dynamic provider integration: **OpenAI**, **Anthropic**, **Google Gemini**, **Ollama**, and zero-cost **Mock**.
- Exact token usage tracking and micro-dollar pricing calculations.

### 3. Enterprise Security & SSRF Protection

- **UrlGuard**: Validates every outbound URL against RFC 1918, RFC 3927 (link-local), and RFC 4193 private ranges with allowlist overrides.
- **Redactor**: Automatically scrubs API keys (`sk-*`, `ghp_*`, `figd_*`), bearer tokens, and basic-auth credentials from logs, events, and API payloads.
- **Metadata-Only DAM Policy**: Verifies asset URLs and references without downloading or duplicating heavy media binaries.

### 4. Project Persistence (CRX/DE Visible)

- **`JcrStore`** (`core/.../persistence/JcrStore.java`) persists every project mutation as an `nt:unstructured` node under `/var/aem-eds-modernizer/projects/{yyyy}/{MM}/{projectId}` (date-sharded, visible and browsable in CRX/DE) — with `eds:*` properties (`eds:projectId`, `eds:name`, `eds:aemAuthorUrl`, etc.) and automatic restore from JCR on bundle restart.
- The `eds` JCR namespace is registered declaratively via Repo Init (not programmatically). The `modernizer-service` user holds least privilege: `jcr:read` on `/content`, `/conf`, `/apps`; `jcr:all` on `/var/aem-eds-modernizer`.
- Node names are escaped via `Text.escapeIllegalJcrChars()` so project IDs containing colons, slashes, or spaces are safe.
- Falls back to **`JsonFileStore`** (local JSON snapshot) when no `SlingRepository` is available, e.g. in the standalone runtime.

### 5. Phase 2 Advanced Capabilities

- **URL Redirect Mapping**: Converts `.html` paths to clean vanity URLs, preserving legacy SEO routes.
- **Dependency Graph Analyzer**: Builds DAG linking pages, editable templates, components, and Content Fragments to detect cascading migration risks.
- **Automated Bounded Self-Repair**: Multi-attempt repair loops with patch diff generation and regression validation.
- **6-Stage Progressive Rollout**: `PREVIEW` (0%) → `INTERNAL` (0%) → `CANARY` (5%) → `BATCH` (25%) → `BROAD` (50%) → `FULL` (100%) with automated visual quality stop gates.
- **Historical Benchmark Tracking**: Rolling P50/P95 durations and compute cost aggregations.

---

## 🤖 The 25 Modernizer Agents

| #   | Agent Name                       | Phase | Stage              | Description                                                                                                                                                                                                  |
| --- | -------------------------------- | ----- | ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 1   | `ConnectionAgent`                | 1     | `CONNECTING`       | Validates reachability to AEM, EDS, GitHub, Figma, and Headless Browser.                                                                                                                                     |
| 2   | `DiscoveryAgent`                 | 1     | `DISCOVERING`      | Crawls AEM page trees and builds the immutable `SiteInventory` snapshot.                                                                                                                                     |
| 3   | `ComponentIntelligenceAgent`     | 1     | `ANALYZING`        | Classifies component dialogs, HTML models, and capability requirements.                                                                                                                                      |
| 4   | `ComponentMappingAgent`          | 1     | `ANALYZING`        | Maps AEM resource types to EDS block definitions.                                                                                                                                                            |
| 5   | `TemplateAnalysisAgent`          | 1     | `ANALYZING`        | Analyzes editable templates and section layouts.                                                                                                                                                             |
| 6   | `ContentAnalysisAgent`           | 1     | `ANALYZING`        | Evaluates semantic tree depth, typography density, and formatting.                                                                                                                                           |
| 7   | `AssetAnalysisAgent`             | 1     | `ANALYZING`        | Inspects DAM references as metadata-only (zero binary downloads).                                                                                                                                            |
| 8   | `ContentFragmentAnalysisAgent`   | 1     | `ANALYZING`        | Maps structured Content Fragments to EDS JSON data tables.                                                                                                                                                   |
| 9   | `MsmAnalysisAgent`               | 1     | `ANALYZING`        | Mappings MSM live copies and language masters to EDS localization.                                                                                                                                           |
| 10  | `FigmaAnalysisAgent`             | 1     | `DESIGN_ANALYSIS`  | Extracts color palettes, typography, and spacing tokens from Figma.                                                                                                                                          |
| 11  | `AdvancedFigmaIntelligenceAgent` | 2     | `DESIGN_ANALYSIS`  | Deep component-to-block pairing and CSS custom property token maps.                                                                                                                                          |
| 12  | `MigrationPlannerAgent`          | 1     | `PLANNING`         | Generates the Migration Plan, derivation trail, and cost/time bounds.                                                                                                                                        |
| 13  | `BlockGenerationAgent`           | 1     | `BUILDING`         | Synthesizes the full Block Quad (JS `decorate(block)`, CSS, UE model JSON, example HTML, README) from **real JCR component properties**, tagging every generated file with the AEM root path (`sourcePath`). |
| 14  | `CodeGenerationAgent`            | 1     | `BUILDING`         | Generates CSS stylesheets, global styles, and `fstab.yaml` mounts.                                                                                                                                           |
| 15  | `ContentMigrationAgent`          | 1     | `MIGRATING`        | Converts AEM content trees into EDS Markdown section models from **real JCR page content** (`_jcr_content.infinity.json`); AI refinement may only refine — never replace — JCR-derived content.              |
| 16  | `AuthoringAgent`                 | 1     | `AUTHORING`        | Sets up Universal Editor and Document-Based authoring contracts.                                                                                                                                             |
| 17  | `PreviewAgent`                   | 1     | `PREVIEWING`       | Pushes virtual commits to GitHub preview branch and activates EDS preview.                                                                                                                                   |
| 18  | `ValidationAgent`                | 1     | `VALIDATING`       | Deterministic functional validation (broken links, SEO, a11y, schema).                                                                                                                                       |
| 19  | `VisualValidationAgent`          | 1     | `VALIDATING`       | Baseline visual regression checks against reference renders.                                                                                                                                                 |
| 20  | `AdvancedVisualValidationAgent`  | 2     | `VALIDATING`       | Representative template sampling and pure-Java visual diff engine.                                                                                                                                           |
| 21  | `SelfRepairAgent`                | 1     | `REPAIRING`        | Basic automated repair for minor styling discrepancies.                                                                                                                                                      |
| 22  | `AdvancedRepairAgent`            | 2     | `REPAIRING`        | Bounded multi-attempt repair loop with patch diff synthesis and re-verification.                                                                                                                             |
| 23  | `AdvancedRolloutAgent`           | 2     | `READY_TO_PUBLISH` | Schedules 6-stage progressive traffic rollout with stop gates.                                                                                                                                               |
| 24  | `PublishingAgent`                | 1     | `PUBLISHING`       | Creates production GitHub Pull Requests for human review.                                                                                                                                                    |
| 25  | `VerificationAgent`              | 1     | `VERIFYING`        | Performs live post-publish crawl to verify 0 regressions.                                                                                                                                                    |

---

## 📊 Estimation & Safety Guardrails

The Modernizer enforces a strict distinction between the two estimation phases:

1. **Build Estimate**: Pre-implementation effort, confidence rating (92%), and zero-cost mock baseline.
2. **Migration Dry Run Estimate**: Mandatory pre-migration dry run calculating:
   - $T_{\text{expected}} = T_{\text{base}} + (\text{pages} \times t_p) + (\text{blocks} \times t_b)$
   - $C_{\text{expected}} = \sum (\text{tokens}_{\text{prompt}} \times P_{\text{in}} + \text{tokens}_{\text{completion}} \times P_{\text{out}})$
   - Tri-point bounds ($\text{Lo} = 0.85 \times \text{Expected}$, $\text{Hi} = 1.35 \times \text{Expected}$)
   - Complete explainable **Derivation Trail** shown in the dashboard.

---

## 🔗 Root-Path ↔ Blocks ↔ Page Reference System

Every migrated page and every generated block is bound to the **same AEM JCR root path**, so pages and blocks always share one identical source reference. This is the core fidelity contract of the migration pipeline.

### How the reference flows

```
AEM Root Path (e.g. /content/wknd/language-masters/en/about-us)
        │
        ├── ContentMigrationAgent ──► page markdown (SECTION_MD)   → GeneratedFileRecord.sourcePath = root path
        │
        └── BlockGenerationAgent  ──► Block Quad per component     → all 5 records .setSourcePath(root path)
                ├── blocks/<name>/<name>.js        (BLOCK_JS)
                ├── blocks/<name>/<name>.css       (BLOCK_CSS)
                ├── blocks/<name>/_<name>.json     (BLOCK_MODEL_JSON)
                ├── blocks/<name>/<name>-example.html (BLOCK_EXAMPLE_HTML)
                └── blocks/<name>/README.md        (BLOCK_README)
```

### Key implementation points (agents context)

| Concern                                  | Where                                                        | Behavior                                                                                                                                                                                                                                                                                                                                                                   |
| ---------------------------------------- | ------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Block source tagging                     | `core/.../agents/BlockGenerationAgent.java`                  | Every generated block file record gets `setSourcePath(samplePagePath)` (the page containing the component), falling back to `ProjectRecord.getContentRoot()`.                                                                                                                                                                                                              |
| Real JCR content in blocks               | `BlockGenerationAgent.fetchComponentProperties()`            | Fetches `<page>/_jcr_content.infinity.json` from the AEM author and builds each block from the **actual component properties** (props fall back to sensible defaults only when the JCR node has none).                                                                                                                                                                     |
| Real JCR content in pages                | `core/.../agents/ContentMigrationAgent.buildPageMarkdown()`  | Fetches `<page>/_jcr_content.infinity.json`, collects all non-container components, and emits one DA-style `###` + `\| --- \|` markdown table per component with its real property values.                                                                                                                                                                                 |
| AI refine must never replace JCR content | `ContentMigrationAgent` + `ai/providers/MockAiProvider.java` | The mock/Antigravity provider **refines** the JCR-derived markdown passed in the prompt (marker: `Refine migrated Markdown structure and tables:\n\n`) and returns it unchanged — it must never return canned sample content. The agent additionally guards: a provider response is only accepted if it contains the page title, or real block tables (`### ` + `\| ---`). |
| Page ↔ block matching                    | `ui.apps/.../clientlib-dashboard/js/dashboard.js`            | `getBlocksForPagePath(pagePath)` matches blocks to pages by `sourcePath` (exact match or subtree containment in either direction).                                                                                                                                                                                                                                         |

> ⚠️ **Invariant for any agent modifying this code**: generated page markdown and generated blocks must always reflect the actual JCR content of the root path. Hardcoded/mocked sample content (e.g. legacy "Ski Touring Mont Blanc" fixtures) must never overwrite JCR-derived output.

---

## 🖥 Dashboard Reference (Pages & Scope / Blocks)

The dashboard (`ui.apps/.../components/page/home/home.html` + `clientlib-dashboard`) exposes the reference system in two tabs:

### Tab: Generated Blocks & Code (Components)

- Block list with per-block file quad inspector (UE HTML demo / model JSON / JS / CSS / README).
- **🔗 AEM Root Path** reference line under the block detail header (`#block-source-ref` / `#block-source-path`) showing the JCR source the block was authored from.

### Tab: Pages & Scope

- Page table (path + title) → page detail header with three file tabs:
  1. **👁️ DA Preview** — migrated markdown rendered as Document Authoring tables (`formatMarkdownToDA`).
  2. **📝 Markdown Source** — raw generated markdown.
  3. **🌐 Live HTML Page** — a complete page **composed entirely from the generated blocks** authored at the page's root path. No direct fetch of the AEM page happens; `buildPageHtmlDocument()` assembles each matching block's generated demo markup + styles into labeled sections (`🧱 blockname — authored from <root path>`), followed by the migrated DA content, inside a sandboxed `srcdoc` iframe. A header line above the iframe states: `🧱 Page composed from N generated block(s) authored at <root path>`.
- **`<eds-block-references>` tag** (`#page-block-references`) — a custom HTML element between the page header and the preview that appears once blocks are authored with content matching the page's root path; lists clickable block chips. Clicking a chip (`jumpToBlock(name)`) jumps to the Components tab with that block selected.

### Frontend helper functions (dashboard.js)

| Function                                    | Purpose                                                                                     |
| ------------------------------------------- | ------------------------------------------------------------------------------------------- |
| `processBlockFiles(files)`                  | Groups generated files into `blockFilesMap` and captures each block's `sourcePath`.         |
| `getBlocksForPagePath(pagePath)`            | Filters blocks whose `sourcePath` matches the page (exact or subtree).                      |
| `renderPageBlockReferences(pagePath)`       | Populates the `<eds-block-references>` tag with matching block chips.                       |
| `jumpToBlock(blockName)`                    | Navigates from a page's block reference to the block inspector.                             |
| `extractBlockDemoParts(demoHtml)`           | Splits a generated block demo HTML into its `<style>` and body markup for page composition. |
| `buildPageHtmlDocument(pagePath, markdown)` | Builds the full Live HTML page from generated blocks + DA content.                          |
| `renderPageHtmlView(markdown)`              | Renders the composed page into the Live HTML tab iframe.                                    |

---

## 🚀 Getting Started

### Prerequisites

- **Java**: JDK 11 or JDK 17
- **Maven**: Apache Maven 3.9+
- **Browser**: Modern web browser (Chrome, Edge, Firefox, Safari)

---

### 1. Running as Standalone Server (Local / Offline)

You can launch the complete Modernizer control plane locally without needing an AEM installation:

```bash
# 1. Clone the repository
git clone https://github.com/Shaik-Tajuddin/AEM-EDS-Modernizer.git
cd AEM-EDS-Modernizer

# 2. Build the shaded fat JAR
mvn clean package -pl core

# 3. Launch the standalone server on port 8080
java -jar core/target/aem-eds-modernizer.core-0.1.0-SNAPSHOT-standalone.jar 8080
```

Open your browser to **[http://localhost:8080](http://localhost:8080)**:

1. Click **⚡ Create Demo Project (WKND)**.
2. Click **🔍 Run Dry Run** to execute the 16-state dry-run pipeline.
3. Inspect pages, mapped blocks, URL redirects, dependencies, and cost estimates.
4. Click **🚀 Approve Contract & Migrate** to execute full migration.

---

### 2. Building and Deploying to AEM Cloud

Build all packages for Cloud Manager deployment or local AEM SDK:

```bash
# Build the entire reactor
mvn clean install

# Deploy to local running AEM Author instance (http://localhost:4502)
mvn clean install -Pdeploy -Daem.host=http://localhost:4502 -Daem.user=admin -Daem.password=admin
```

Access the control plane inside AEM:

- **Direct SPA Route**: `http://localhost:4502/aem-eds-modernizer`
- **AEM Page Route**: `http://localhost:4502/content/aem-eds-modernizer/home.html`

---

## 📡 REST API Reference

All API routes are served under `/api` (Standalone) and `/bin/aem-eds-modernizer/api` (AEM Author):

| Method | Endpoint                        | Description                                         |
| ------ | ------------------------------- | --------------------------------------------------- |
| `GET`  | `/health`                       | Server health check and version status.             |
| `GET`  | `/projects`                     | List all migration projects.                        |
| `POST` | `/projects`                     | Create or update a project record.                  |
| `GET`  | `/projects/{id}`                | Get project details by ID.                          |
| `POST` | `/projects/{id}/dryrun`         | Execute mandatory Dry Run on project.               |
| `POST` | `/projects/{id}/migrate`        | Execute approved full migration pipeline.           |
| `GET`  | `/projects/{id}/inventory`      | Retrieve immutable Site Inventory snapshot.         |
| `GET`  | `/projects/{id}/plan`           | Get Migration Plan, costs, and derivation trail.    |
| `GET`  | `/projects/{id}/files`          | Retrieve generated EDS JS, CSS, and Markdown files. |
| `GET`  | `/projects/{id}/redirects`      | Retrieve Phase 2 URL redirect records.              |
| `GET`  | `/projects/{id}/dependencies`   | Retrieve Phase 2 dependency DAG edges.              |
| `GET`  | `/projects/{id}/rollout-stages` | Retrieve 6-stage progressive rollout status.        |
| `GET`  | `/projects/{id}/repairs`        | Retrieve automated repair attempt history.          |
| `GET`  | `/projects/{id}/benchmarks`     | Retrieve performance and cost benchmark metrics.    |
| `GET`  | `/projects/{id}/events`         | Real-time event log for audit trail.                |

---

## 🧪 Testing & Validation

Run the JUnit 5 test suite:

```bash
mvn test
```

Run the automated End-to-End verification script:

```bash
# Start standalone server in background, then run:
bash scripts/e2e.sh
```

---

## 📚 Documentation Index

Detailed architectural, security, and operational documentation is located in the [`docs/`](docs/) directory:

- 🏛 **Architecture Decision Records**: [`docs/adr/`](docs/adr/) (ADR 0001 - ADR 0015)
- ⚙️ **AEM Cloud & Dispatcher Setup**: [`docs/aem/`](docs/aem/)
- 🤖 **Agent Specifications**: [`docs/agents/`](docs/agents/)
- 📐 **EDS Pipelines & Conventions**: [`docs/eds/`](docs/eds/)
- 🎨 **Figma Token Extraction**: [`docs/figma/`](docs/figma/)
- 🐙 **GitHub Operations & PRs**: [`docs/github/`](docs/github/)
- 📊 **Migration Policies & Scope**: [`docs/migration/`](docs/migration/)
- 🛡 **Security, SSRF & RBAC**: [`docs/security/`](docs/security/)
- 📋 **Master Prompts & Blueprints**: [`docs/prompts/`](docs/prompts/)

---

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
