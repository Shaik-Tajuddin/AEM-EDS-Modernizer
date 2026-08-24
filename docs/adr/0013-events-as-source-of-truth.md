# ADR 0013 — Events as the Dashboard's Source of Truth

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Dashboard, observability

## Context

The dashboard shows a live view of a running migration: current
state, progress percentage, recent AI decisions, recent events,
recent issues. The question is: where does the dashboard get
this data?

Two patterns:

1. **Pull from the orchestrator's in-memory state.** The
   dashboard calls a `/state` endpoint that returns the current
   snapshot. Simple, but the in-memory state is lost on
   restart, and a new dashboard load shows nothing.
2. **Reconstruct from persisted events.** Every state change
   emits a `JobEventRecord`; the dashboard reads the events
   since the last refresh and reconstructs the view.

## Decision

We chose **option 2**. The dashboard polls
`GET /api/projects/{id}/events?since={lastEventId}` and
reconstructs its state from the returned events. There is no
other source of truth.

The benefits are:

- **A refresh always recovers the correct view.** The dashboard
  has no in-memory state; it reads the events and re-derives
  everything.
- **Events are append-only and idempotent.** The same event
  read twice produces the same view.
- **Events work across instances.** A dashboard connected to
  Author-A sees the same events as a dashboard connected to
  Author-B (assuming both have access to the same JCR).
- **The audit log is the event log.** Every state change is
  recorded; the operator can replay the migration step by step
  in the dashboard's timeline view.

## Consequences

### Positive

- **Stateless SPA.** The dashboard is a single HTML + CSS + JS
  file with no local state. A refresh always recovers.
- **Audit-friendly.** The events are persisted for the life of
  the job (and beyond, for completed jobs). The operator can
  scroll back to see what happened.
- **Multi-instance safe.** A migration that is paused and
  resumed on a different AEM instance resumes the same event
  stream.

### Negative

- **Polling cost.** The dashboard polls every 1-2 seconds
  during an active migration. For 1000 concurrent dashboards
  this is 30k req/min, which is acceptable for AEM Author
  (which handles 100k+ req/min easily) but not free.
- **Event growth.** For a long migration the event log can
  reach thousands of records. Mitigated by the
  `since={lastEventId}` cursor; the dashboard never reads
  events it has already seen.

## Alternatives considered

- **WebSocket push** (instead of polling): rejected for the
  MVP because it complicates the dispatcher config. A
  follow-up can add WebSocket support.
- **Server-sent events (SSE)**: same trade-off as WebSocket;
  deferred.
- **Polling + cache headers**: the current implementation
  returns `Cache-Control: no-store` on the events endpoint;
  this is correct but means the CDN cannot cache the response.

## Related

- [../architecture/STATE_MACHINE.md](../architecture/STATE_MACHINE.md) —
  the state machine that emits the events.
- [ADR 0012](0012-checkpoints-for-resumability.md) — the
  checkpoints that work alongside events.
