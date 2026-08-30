# Operations

Operational documentation for the AEM → EDS Modernizer: the
runbook, observability, failure recovery, and incident
response.

## Documents in this section

- [RUNBOOK.md](RUNBOOK.md) — the daily operations runbook.
- [OBSERVABILITY.md](OBSERVABILITY.md) — the metrics, logs,
  and traces the modernizer emits.
- [RECOVERY.md](RECOVERY.md) — how to recover from common
  failure modes.
- [INCIDENT_RESPONSE.md](INCIDENT_RESPONSE.md) — the
  incident response procedure.
- [CAPACITY_PLANNING.md](CAPACITY_PLANNING.md) — capacity
  planning for AEM Cloud deployments.
- [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) — the
  pre-deployment checklist.

## How operations fits in the architecture

Operations is a **cross-cutting concern**: every layer of
the modernizer has operational responsibilities.

```
┌─────────────────────────────────────────┐
│  Dashboard                              │ Health check, status
├─────────────────────────────────────────┤
│  ApiRouter                              │ Request metrics
├─────────────────────────────────────────┤
│  Agent                                  │ Job metrics, agent metrics
├─────────────────────────────────────────┤
│  AiGateway                              │ AI cost metrics, error rates
├─────────────────────────────────────────┤
│  Connector (AEM, GitHub, Figma, EDS)    │ Latency, error rates
├─────────────────────────────────────────┤
│  Store (JcrStore / JsonFileStore / InMemory)│ Storage metrics
└─────────────────────────────────────────┘
```

## See also

- [../architecture/](../architecture/) — the architecture
  documentation.
- [../security/](../security/) — the security documentation.
- [../adr/](../adr/) — the decision records.
