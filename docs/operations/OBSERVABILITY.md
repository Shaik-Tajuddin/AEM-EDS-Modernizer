# Observability

The metrics, logs, and traces the modernizer emits.

## Metrics

The modernizer emits metrics via Micrometer. The metrics
are scraped by the AEM Cloud Prometheus integration and
displayed in Cloud Manager → Observability.

### Job metrics

| Metric | Tags | Description |
|---|---|---|
| `modernizer.jobs.created` | `projectId`, `dryRun` | Counter of jobs created |
| `modernizer.jobs.completed` | `projectId`, `dryRun`, `state` | Counter of jobs completed |
| `modernizer.jobs.failed` | `projectId`, `dryRun`, `state` | Counter of jobs failed |
| `modernizer.jobs.duration` | `projectId`, `dryRun` | Histogram of job duration (seconds) |

### Agent metrics

| Metric | Tags | Description |
|---|---|---|
| `modernizer.agents.duration` | `agent`, `state` | Histogram of agent duration (milliseconds) |
| `modernizer.agents.errors` | `agent`, `state`, `error` | Counter of agent errors |
| `modernizer.agents.retries` | `agent` | Counter of agent retries |

### AI metrics

| Metric | Tags | Description |
|---|---|---|
| `modernizer.ai.requests` | `provider`, `model`, `agent` | Counter of AI requests |
| `modernizer.ai.tokens_in` | `provider`, `model`, `agent` | Counter of input tokens |
| `modernizer.ai.tokens_out` | `provider`, `model`, `agent` | Counter of output tokens |
| `modernizer.ai.cost` | `provider`, `model`, `agent` | Counter of cost (USD) |
| `modernizer.ai.errors` | `provider`, `model`, `agent`, `error` | Counter of AI errors |
| `modernizer.ai.latency` | `provider`, `model`, `agent` | Histogram of AI latency (milliseconds) |

### Connector metrics

| Metric | Tags | Description |
|---|---|---|
| `modernizer.connectors.requests` | `connector`, `endpoint` | Counter of connector requests |
| `modernizer.connectors.errors` | `connector`, `endpoint`, `error` | Counter of connector errors |
| `modernizer.connectors.latency` | `connector`, `endpoint` | Histogram of connector latency (milliseconds) |

## Logs

The modernizer uses SLF4J. The log level is controlled by
the OSGi config (`AemEdsModernizerService.logLevel`).

The log format is the default AEM Cloud format:
`{timestamp} {level} {logger} - {message} {mdc}`.

The MDC includes:

- `jobId`
- `projectId`
- `agent`
- `actor` (when running under Sling)

The `Redactor` strips secret patterns from every log line.
See [../security/REDACTOR.md](../security/REDACTOR.md).

## Traces

The modernizer emits OpenTelemetry traces for:

- The full state machine (one trace per job).
- Each agent invocation (a child span).
- Each AI call (a child span).
- Each connector call (a child span).

The traces are exported to the AEM Cloud Jaeger
integration.

## Audit log

The audit log is a stream of `JobEventRecord`s, persisted
in JCR under `/var/aem-eds-modernizer/` by `JcrStore`.
See [../security/AUDIT.md](../security/AUDIT.md).

## Dashboards

The AEM Cloud observability stack includes pre-built
dashboards for the modernizer:

- **Job overview** — jobs created / completed / failed
  over time, with breakdowns by project and dry-run.
- **AI cost** — AI requests, cost, and tokens per
  provider / model / agent.
- **Connector health** — connector latency and error rates
  per connector / endpoint.
- **Agent performance** — agent duration and error rates
  per agent.

## Alerts

The AEM Cloud observability stack includes pre-built
alerts for the modernizer:

- **High AI cost** — fires when the daily AI cost exceeds
  the configured budget.
- **High job failure rate** — fires when the job failure
  rate exceeds 10% over a 1-hour window.
- **High connector error rate** — fires when the connector
  error rate exceeds 5% over a 5-minute window.
- **Slow AI calls** — fires when the AI P95 latency
  exceeds 30 seconds.

## See also

- [RUNBOOK.md](RUNBOOK.md) — the daily runbook.
- [INCIDENT_RESPONSE.md](INCIDENT_RESPONSE.md) — the
  incident response procedure.
