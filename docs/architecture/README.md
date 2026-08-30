# Architecture

This section collects the high-level architectural documentation for the
AEM → EDS Modernizer: component diagrams, request flows, data flow, and
the runtime topology that ties the OSGi bundle, the standalone server,
and the external system adapters together.

## Documents in this section

- [OVERVIEW.md](OVERVIEW.md) — single-page architecture summary
- [COMPONENTS.md](COMPONENTS.md) — every module, its responsibility, its
  public surface, and its inbound / outbound dependencies
- [REQUEST_FLOW.md](REQUEST_FLOW.md) — request lifecycle from the
  dashboard to the orchestrator to the agents to the AI gateway
- [DATA_FLOW.md](DATA_FLOW.md) — how data moves through the system
  during a migration (pages, components, mappings, generated files,
  repairs, benchmarks)
- [RUNTIME_TOPOLOGY.md](RUNTIME_TOPOLOGY.md) — AEM Cloud deployment vs
  the standalone runtime, side-by-side
- [STATE_MACHINE.md](STATE_MACHINE.md) — the migration state machine,
  its guards, its failure paths

## How this fits with the rest of the docs

- See [../adr/](../adr/) for the *why* behind the architecture (the
  decision records that produced this layout).
- See [../agents/](../agents/) for the *who* — one doc per agent.
- See [../aem/](../aem/), [../eds/](../eds/), [../figma/](../figma/),
  [../github/](../github/) for the *how* of the external-system
  integrations.
- See [../security/](../security/) and [../operations/](../operations/)
  for the cross-cutting concerns.

## Architectural principles

1. **Control plane inside AEM.** Per Master §4 the dashboard, the
   orchestrator, and the agents are packaged as an OSGi bundle and
   served from AEM Author at `/bin/aem-eds-modernizer/*`. The
   standalone runtime is a development convenience, not a separate
   product.
2. **The state machine is the single source of truth.** Every agent
   runs inside a `MigrationState` transition; the state machine guards
   ordering, retries, clarifications, and human-approval gates.
3. **Agents never instantiate a provider SDK directly.** They call
   `AiGateway.dispatch(request)` which routes through the
   `AiRoutingPolicy` and the `CapabilityRegistry`.
4. **The `Store` interface is a narrow CRUD facade.** Three
   implementations exist: `JcrStore` (highest ranking, persists to
   `/var/aem-eds-modernizer/projects/{yyyy}/{MM}/` under the `eds:` JCR namespace),
   `JsonFileStore` (JSON-file snapshot for local dev), and
   `InMemoryStore` (ConcurrentHashMap, standalone fallback). The AEM
   Cloud runtime binds `JcrStore`; the standalone runtime uses
   `JsonFileStore` or `InMemoryStore`. Same interface, no call-site
   changes.
5. **The dashboard is a stateless SPA.** It polls
   `/api/projects/{id}/events` and reconstructs its state from
   persisted `JobEventRecord`s. A refresh always recovers the
   correct view.
6. **Dry Run is mandatory.** The `MIGRATE` button is only enabled
   after a complete Dry Run with no critical blockers (Master §0A).
