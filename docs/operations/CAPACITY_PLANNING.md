# Capacity Planning

Capacity planning for AEM Cloud deployments of the AEM →
EDS Modernizer.

## Baseline (AEM Cloud small instance)

| Resource | Baseline |
|---|---|
| AEM Author instance | 1× small |
| Heap size | 8 GB |
| Concurrent migrations | 1-5 |
| Concurrent AI calls per migration | 5 |
| Concurrent connector calls per migration | 10 |
| Storage (JCR at `/var/aem-eds-modernizer/`) | 10 GB (per project) |

## Throughput

A single AEM Author small instance can handle:

- 1-5 concurrent migrations (the bottleneck is the
  orchestrator's single-writer-per-job model).
- 10-20 connector requests per second (per migration).
- 5-10 AI requests per second (per migration).
- ~30,000 dashboard requests per minute (the dashboard
  polls /events every 1-2 seconds).

## Scaling up

To handle more concurrent migrations:

| Concurrent migrations | Recommended AEM Author |
|---|---|
| 1-5 | 1× small |
| 6-20 | 1× medium |
| 21-50 | 1× large |
| 51+ | 2× large (cluster) |

To handle more AI calls per second:

- Increase the `AemEdsModernizerService.aemConcurrency`
  setting (max concurrent AEM API calls).
- Increase the `AemEdsModernizerService.aiMaxConcurrency`
  setting (max concurrent AI calls per provider).

## Storage

Each project consumes ~10 GB of JCR storage:

- The migration job state (~1 MB per job).
- The generated files (the virtual diff; ~100 KB per
  page).
- The audit log (~10 KB per event, ~30 events per job).
- The benchmark samples (~1 KB per sample, ~50 samples
  per job).

For a long-running program with 100 migrations per month,
expect ~100 GB of JCR storage after 1 year. The default
AEM Cloud storage is 100 GB; plan accordingly.

## AI cost

The AI cost is the dominant cost for most migrations. The
cost depends on:

- The number of pages in scope (one AI call per page for
  content migration).
- The number of distinct blocks (one AI call per block).
- The AI provider (Anthropic Sonnet is ~$3 per million
  output tokens; Ollama is free but slower).

A typical 100-page migration costs $1-5 in AI calls.

## Network

The modernizer makes outbound calls to:

- AEM Author / Publish (HTTPS, port 443).
- GitHub API (HTTPS, port 443).
- Figma API (HTTPS, port 443).
- AI provider APIs (HTTPS, port 443; varies).

A single AEM Author small instance can sustain ~50
outbound requests per second without saturating the
network.

## See also

- [OBSERVABILITY.md](OBSERVABILITY.md) — the metrics to
  monitor.
- [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) — the
  pre-deployment checklist.
