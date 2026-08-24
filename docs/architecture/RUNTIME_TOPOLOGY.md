# Runtime Topology

AEM Cloud deployment and the standalone runtime, side-by-side.

## AEM Cloud (production)

```
                ┌───────────────────────────────────────┐
                │  AEM Author (Cloud Service, runmode   │
                │  author, large, prod)                 │
                │                                       │
                │  ┌─────────────────────────────────┐  │
                │  │  OSGi container                 │  │
                │  │  ├── core bundle                │  │
                │  │  │   (com.adobe.aem.modernizer) │  │
                │  │  ├── ui.apps (HTL)              │  │
                │  │  ├── ui.config (OSGi cfg)       │  │
                │  │  └── ui.content (sample data)   │  │
                │  └─────────────────────────────────┘  │
                │                                       │
                │  Sling Servlet at:                    │
                │  /bin/aem-eds-modernizer/api/...      │
                │                                       │
                │  JCR / Oak                            │
                │  /content/aem-eds-modernizer/...      │
                │                                       │
                └──┬────────┬────────┬────────┬────────┘
                   │        │        │        │
                   ▼        ▼        ▼        ▼
                AEM      GitHub   Figma   EDS Preview
                APIs     API      API     (via Git)
```

### What runs where

| Component | Location |
|---|---|
| Dashboard SPA | `ui.apps` HTL at `/bin/aem-eds-modernizer/index.html` |
| API endpoints | Sling Servlet (`DashboardApi`) at `/bin/aem-eds-modernizer/api/*` |
| Orchestrator | `core` bundle (in-process) |
| State machine | `core` bundle (in-process) |
| AI gateway | `core` bundle (in-process) |
| Connectors | `core` bundle (in-process, calls AEM Author over HTTP) |
| State store | JCR (Oak) at `/content/aem-eds-modernizer/...` |
| Sample data | `ui.content` (mutable baseline) |
| Dispatcher | `dispatcher` content-package |

### What survives a restart?

Everything. JCR is the authoritative store. The dispatcher caches
the SPA HTML and proxies the API back to AEM Author.

### Sizing

- AEM Author: 1× small instance handles 10 concurrent migrations.
- For higher concurrency, scale to 2-4 instances.
- The state machine is single-writer per job (`MigrationJobRecord`
  has a `lockOwner`); multiple instances coordinate via JCR
  observations.

## Standalone (development / CI)

```
                ┌───────────────────────────────────────┐
                │  Java process                        │
                │  java -jar core-0.1.0-SNAPSHOT-      │
                │       standalone.jar                 │
                │                                       │
                │  ┌─────────────────────────────────┐  │
                │  │  JDK HttpServer (port 8080)     │  │
                │  │  ├── StandaloneServer (routes)  │  │
                │  │  └── StaticDashboard (SPA)      │  │
                │  │                                 │  │
                │  │  ApiRouter (same code)          │  │
                │  │  Orchestrator (same code)       │  │
                │  │  AiGateway (same code)          │  │
                │  │  Connectors (MOCK impls)        │  │
                │  │                                 │  │
                │  │  InMemoryStore                  │  │
                │  │  (ConcurrentHashMap)            │  │
                │  │                                 │  │
                │  │  File-backed mirror of          │  │
                │  │  generated files in ~/.modernizer│ │
                │  └─────────────────────────────────┘  │
                │                                       │
                └──┬────────────────────────────────────┘
                   │
                   ▼
              localhost:8080
              (browser opens dashboard directly)
```

### What runs where

| Component | Location |
|---|---|
| Dashboard SPA | `static/index.html` (in jar) |
| API endpoints | `ApiRouter` (in jar) at `/api/*` |
| Orchestrator | (in jar, in-process) |
| AI gateway | (in jar, in-process) |
| Connectors | `MockAemClient`, `MockGitHubClient`, `MockFigmaClient`, `MockEdsClient`, `MockBrowserClient` |
| State store | `InMemoryStore` (lost on restart unless file-mirrored) |
| Generated files | `~/.modernizer/modernizer-data/files/...` (file-backed mirror) |

### What survives a restart?

- Generated files: yes (file-backed).
- Everything else: **no**. The `InMemoryStore` is a
  `ConcurrentHashMap`. The mock seeds a project on startup, but
  all per-job state is lost.

This is intentional for the MVP. The AEM Cloud deployment uses
JCR for authoritative state.

## Differences

| Concern | AEM Cloud | Standalone |
|---|---|---|
| HTTP server | Sling Servlet + Dispatcher | JDK `HttpServer` |
| State | JCR / Oak | `ConcurrentHashMap` (+ file mirror for generated files) |
| Auth | AEM ACLs (inherits user) | None (loopback) |
| Connectors | Real AEM/GitHub/Figma/EDS API clients | Mock implementations |
| Browser client | Real Playwright | Mock that produces deterministic 16×16 PNGs |
| AI provider | Per `AiRoutingPolicy` (Anthropic/OpenAI/Gemini/Ollama) | Mock by default |
| Observability | Sling events, JCR audit, AEM logs | SLF4J to stdout/stderr |
| Scheduling | Sling Jobs (per AEM Cloud best practice) | Thread pool inside the standalone JVM |

## Why both?

- **AEM Cloud is the production intent.** It is what the operator
  deploys, what the customer sees, and where the SLA applies.
- **Standalone is the developer experience.** No AEM tenant, no
  cloud account, no IMS, no API keys — just `java -jar` and
  `http://localhost:8080`. CI uses the same path to run the e2e
  on every commit.
- **Same code path.** Both runtimes go through the same
  `ApiRouter`, the same `Orchestrator`, the same `AiGateway`, the
  same `Store` interface, and the same agent implementations.
  There is no "different code for dev vs prod" footgun.

## Migration path

When a developer wants to test against a real AEM Author:

1. Set `MOCK_MODE=false` (or omit the env var, since the default is
   real).
2. Configure the `AemAuthor` / `AemPublish` connector URLs and
   credentials.
3. Run `java -jar core-...-standalone.jar`.
4. The same dashboard, the same e2e, but every connector call hits
   the real APIs.
