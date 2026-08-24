# Architecture Overview

The AEM → EDS Modernizer is a real AEM as a Cloud Service project
(Java 11 / OSGi / Sling) plus a standalone runtime that exercises the
same code path end-to-end without an AEM instance. The product is an
**autonomous migration platform**: a user provides connection details
and an AEM content root, and the platform crawls, analyses, maps,
generates, migrates, author-rewrites, previews, validates, repairs,
publishes, and verifies the migration with minimal human intervention.

## High-level component diagram

```
                       ┌──────────────────────────────────────┐
                       │       AEM Author (Cloud Service)      │
                       │                                      │
   USER ── HTTPS ───▶  │  Sling Servlet                       │
                       │  /bin/aem-eds-modernizer/*            │
                       │  ├── StaticDashboard (SPA)           │
                       │  └── DashboardApi (REST)              │
                       │           │                          │
                       │           ▼                          │
                       │      ApiRouter  (24 routes)          │
                       │           │                          │
                       │           ▼                          │
                       │      Orchestrator  (state machine)   │
                       │           │                          │
                       │           ▼                          │
                       │   ┌── 20 specialised agents ──┐      │
                       │   │  connection  discovery    │      │
                       │   │  component   template     │      │
                       │   │  content     asset        │      │
                       │   │  figma       planning     │      │
                       │   │  block       code         │      │
                       │   │  migration   authoring    │      │
                       │   │  preview     validation   │      │
                       │   │  visual      repair       │      │
                       │   │  publishing  verification │      │
                       │   └─────────────┬─────────────┘      │
                       │                 │                    │
                       │                 ▼                    │
                       │           AiGateway                  │
                       │                 │                    │
                       │                 ▼                    │
                       │   ┌── Provider Adapters ──┐          │
                       │   │ Anthropic  OpenAI     │          │
                       │   │ Gemini     Ollama     │          │
                       │   │ Mock (mock-mode only) │          │
                       │   └───────────────────────┘          │
                       │                 │                    │
                       │                 ▼                    │
                       │      Store  (JCR / InMemory)         │
                       │                                      │
                       └────┬──────────┬──────────┬───────────┘
                            │          │          │
                            ▼          ▼          ▼
                        AEM APIs   GitHub API  Figma API
                                    EDS Preview
```

## Runtime topologies

### AEM Cloud (production)

- The control plane runs as a Sling Servlet inside AEM Author
  (`/bin/aem-eds-modernizer/*`).
- The dispatcher serves the dashboard UI and proxies API calls back
  to the same author instance.
- State is persisted in JCR (Oak).
- Connectors call AEM Author/Publish via the documented APIs, EDS via
  the GitHub API, Figma via the Figma REST API.

### Standalone (development / CI)

- The same OSGi bundle is run as a fat jar with a `Main-Class`
  (`StandaloneMain`).
- A `com.sun.net.httpserver.HttpServer` mounts the dashboard at `/`
  and the API at `/api/...`.
- State lives in `ConcurrentHashMap`s (with file-backed mirroring of
  generated files).
- Connectors use the mock implementations (`MockAemClient`,
  `MockGitHubClient`, `MockFigmaClient`, `MockEdsClient`,
  `MockBrowserClient`).

The two share the same `ApiRouter`, `Orchestrator`, `AiGateway`,
`Store` interface, and every agent. The only differences are the
binding for `Store` and the connector implementations.

## Why an OSGi bundle, not a microservice?

Per Master §4 the control plane lives inside AEM. Concretely:

- **Operational simplicity**: no extra service to deploy, monitor,
  scale, or version.
- **Identity and ACLs**: the dashboard inherits AEM's authentication
  and per-user permissions automatically.
- **Connector access**: the AEM Author connector runs in the same
  JVM and can use the Sling `ResourceResolver` rather than going over
  HTTP, which avoids the IMS round trip for in-process queries.
- **State locality**: JCR is the canonical store, so the dashboard
  can read state with a single `ResourceResolver` call, no API
  request needed.

The standalone runtime exists so developers without an AEM tenant
can run the full state machine locally and CI can run the e2e test
on every commit.
