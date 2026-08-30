# ADR 0002 — Control Plane Inside AEM (Master §4)

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Deployment topology

## Context

The AEM → EDS Modernizer is a stateful, long-running service: it
holds migration jobs, drives agents, dispatches AI calls, and
streams events to a dashboard. Where should this code run?

Three options were on the table:

1. **A standalone microservice** (Spring Boot / Quarkus, deployed
   to AEM's side-car infrastructure).
2. **A Sling Servlet inside AEM Author** (an OSGi bundle).
3. **A serverless function** (Cloud Run / Lambda) called by an
   AEM package.

## Decision

We chose **option 2: Sling Servlet inside AEM Author**. The
control plane is part of the AEM application; the dashboard lives
at `/bin/aem-eds-modernizer/*`; the API at
`/bin/aem-eds-modernizer/api/*`.

A **standalone Java runtime** (fat-jar with JDK `HttpServer`)
exists for development and CI, but it is a deployment convenience,
not a separate product. Both runtimes share the same `ApiRouter`,
`Orchestrator`, `AiGateway`, and `Store` interface.

## Consequences

### Positive

- **Identity and ACLs for free.** The dashboard inherits AEM's
  authentication and per-user permissions. No separate auth layer
  to design, implement, or test.
- **JCR is the canonical store.** No separate database; no ETL
  between the migration engine and the production data model.
  `JcrStore` persists project records under
  `/conf/aem-eds-modernizer/` with `eds:*` namespaced
  properties.
- **Operational simplicity.** One deployment, one set of metrics,
  one alert rule. No side-car to monitor.
- **In-process AEM connector access.** Inside AEM, the
  `ResourceResolver` is available; the connector can use it
  instead of going over HTTP for every read.
- **The dispatcher already serves the SPA.** The existing
  Dispatcher config (`/dispatcher/src/conf.dispatcher.d/`) caches
  the dashboard HTML at the edge with no extra work.

### Negative

- **AEM Author is a single point of failure.** A failure of the
  Author instance takes the dashboard down. Mitigated by AEM
  Cloud's 99.9% SLA and by the standalone fallback for
  development.
- **Scaling is tied to AEM Author scaling.** A spike of 50
  concurrent migrations would need an AEM Author scale-up. For
  the MVP workload (1-5 concurrent migrations) this is fine; a
  follow-up could move the orchestrator to a separate Sling Job
  consumer in the publish tier.
- **AEM Cloud's deployment cadence is slower than a microservice.**
  Each migration engine change requires a full Cloud Manager
  pipeline. Mitigated by the standalone runtime for fast iteration
  in dev.

## Alternatives considered

- **Standalone microservice** (option 1): rejected because of the
  double-deployment cost (AEM + microservice) and the loss of
  in-process AEM access.
- **Serverless function** (option 3): rejected because state
  (jobs, checkpoints, generated files) is first-class and
  functions are ephemeral; we would need a separate database,
  defeating the simplicity argument.

## Related

- [RUNTIME_TOPOLOGY.md](../architecture/RUNTIME_TOPOLOGY.md) — the
  full deployment topology, both runtimes.
- [ADR 0015](0015-phase-1-and-phase-2-coexist.md) — the
  agent-naming rule that lets Phase 1 and Phase 2 coexist.
