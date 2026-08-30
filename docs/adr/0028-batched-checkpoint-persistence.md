# ADR-0028 — Batched checkpoint persistence; JCR is the checkpoint store, not the journal

- **Status:** Accepted
- **Date:** 2026-08-30
- **Scope:** `core/.../persistence/JcrStore.java`, `core/.../orchestrator/Orchestrator.java`

## Context

The current `JcrStore` persists **every** project mutation synchronously
to JCR. For a migration of 25,000 pages with 25 agents, this produces
hundreds of thousands of JCR commits:

- Oak commit contention under concurrent writes.
- Observation-listener load (every save fires Sling observation).
- Revision-GC pressure on the author repository.
- The author pod's session refresh cost scales linearly.

A migration should produce **hundreds** of JCR commits, not hundreds of
thousands.

## Decision

Stop persisting every mutation synchronously to JCR. Instead:

### Hot state stays in memory

The running migration's current state, per-unit progress, and
intermediate results live in the runner's memory (or an embedded store
like Chronicle Map for crash-safety). This is the "journal" — the
append-only event log that the dashboard streams via SSE.

### JCR receives checkpoints

JCR receives **checkpoints**: state transitions, per-unit terminal
status, final artifacts, and cost totals. These are the durable
records that survive a restart and are browsable in CRX/DE.

### Batched `session.save()`

Batch JCR writes every N units (500 is a reasonable start) rather than
per mutation. The batch size is configurable via OSGi config
(`AemEdsModernizerService.checkpointBatchSize`, default 500).

### Event log batching

The append-only event log gets a monotonic sequence number and is
written in batches too. This is also what makes resumable SSE possible
(see ADR-0018) — the dashboard requests events since `lastSeq` and
receives a batch, not a single event.

### Checkpoint granularity

| What | When persisted | Why |
|------|---------------|-----|
| State transition | Immediately | Determines resumption point |
| Per-unit terminal status | Batched (every N units) | Progress tracking, restart recovery |
| Final artifacts (JS/CSS/MD) | Batched | Content-addressed; can be regenerated |
| Cost totals | At state transition | Budget enforcement |
| Event log entries | Batched | SSE, audit trail |

## Consequences

### Positive

- JCR commits per 10k-page run drop from ~100,000 to < 500.
- Oak commit contention eliminated.
- Observation listener load drops by orders of magnitude.
- Revision-GC pressure reduced.
- SSE becomes naturally batched (better for dashboard polling).

### Negative

- A crash between checkpoints loses up to N units of progress.
  Mitigated by (a) N is configurable, (b) the content-addressed
  artifact cache (ADR-0022) means re-computation is cheap, and
  (c) unit-level checkpointing on state transitions ensures the
  resumption point is always current.
- Slightly more complex `JcrStore` implementation (batch flush
  logic, periodic timer or threshold trigger).

## Related

- [ADR-0012](0012-checkpoints-for-resumability.md) —
  the checkpoint model (this ADR changes the write frequency, not
  the model).
- [ADR-0022](0022-content-addressed-artifacts.md) —
  content-addressed artifacts make re-computation cheap, which
  makes larger batch sizes safe.
- [ADR-0029](0029-projectstore-port-and-conformance-suite.md) —
  the conformance test suite verifies batch behaviour.
