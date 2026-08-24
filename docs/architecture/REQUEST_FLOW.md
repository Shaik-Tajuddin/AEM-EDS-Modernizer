# Request Flow

A user request — for example, "Run Dry Run" — flows through the system
in a predictable, well-defined sequence. This document traces the
lifecycle from the dashboard click to the AI dispatch and back.

## Lifecycle of a request

### 1. Dashboard click → Sling servlet (or HttpServer route)

The dashboard SPA issues `POST /api/projects/{id}/dry-run`. In AEM
Cloud this routes to the Sling Servlet `DashboardApi` at
`/bin/aem-eds-modernizer/api/projects/{id}/dry-run`; in the
standalone runtime the same path resolves to the `ApiRouter` mounted
on the JDK `HttpServer`.

### 2. `ApiRouter` dispatch

`ApiRouter.handle(path, method, body)` is a single entry point. It
parses the path, resolves the project id, and dispatches to the
correct handler:

```java
if (p.size() == 2 && "dry-run".equals(p.get("1")) && "POST".equalsIgnoreCase(method)) {
    return dryRun(id);
}
```

The handler is a thin method that calls the relevant service (here,
`DryRunService.run(projectId)`).

### 3. `DryRunService`

The dry run service is responsible for:

1. Creating a new `MigrationJobRecord` with `dryRun=true`.
2. Driving the `Orchestrator` from `CREATED` through the state
   machine to `COMPLETED`.
3. Returning the job id and a summary.

It does **not** know about the dashboard; it returns a plain Java
object that the API serialises.

### 4. `Orchestrator.runJob`

`runJob(job)` iterates over the state machine: `CREATED →
CONNECTING → DISCOVERING → ... → COMPLETED`. For each transition it:

1. Persists a `JobEventRecord` (so the dashboard can show progress).
2. Calls `runStage(job, nextState)`.
3. On failure, transitions the job to `FAILED` with the
   `lastError` field populated.

### 5. `Orchestrator.runStage`

`runStage(job, state)` switches on the state and calls the relevant
agent(s) via `invoke(name, ctx)` or `invokeIfRegistered(name, ctx)`.
Each `invoke`:

1. Looks up the agent in the `agents` map.
2. Records the start time.
3. Calls `agent.run(ctx)` and waits for the `AgentResult`.
4. Records the duration as a `BenchmarkSampleRecord` for historical
   optimization.
5. Throws on failure (or logs + continues for opt-in agents).

### 6. Agent → `AiGateway.dispatch`

Agents that need AI call `AiGateway.dispatch(ChatRequest)`. The
gateway:

1. Looks up the agent's routing in `AiRoutingPolicy`.
2. Verifies the target model has the required `ModelCapability`
   entries.
3. Records the request in `AIDecision` for observability.
4. Calls the provider adapter's `chat(request)`.
5. On failure, retries with exponential backoff (up to
   `maxRetries`).
6. Returns a `ChatResponse` with `content`, `structured` (parsed
   JSON if a `jsonSchema` was provided), `estimatedCost`, and
   `tokensIn`/`tokensOut`.

### 7. Agent → `Store` writes

Agents persist their output via the `Store` interface
(`store.saveX(rec)`). In AEM Cloud this writes to JCR; in the
standalone runtime it writes to `ConcurrentHashMap`s.

### 8. `ApiRouter` returns JSON

The API serialises the response with Jackson. Headers are set to
`Content-Type: application/json; charset=utf-8` and
`X-Content-Type-Options: nosniff`.

### 9. Dashboard updates

The dashboard polls `GET /api/projects/{id}/events` and reconstructs
its state from the new `JobEventRecord`s. The
`#/dryrun` view animates the new state, the `#/ai-activity` view
shows the new AI decision, and the `#/pages` view updates with the
newly discovered pages.

## End-to-end example: "Run Dry Run"

```
USER
  │ click "Run Dry Run" on #/dryrun
  ▼
DASHBOARD
  │ POST /api/projects/{id}/dry-run
  ▼
SlingServlet (or HttpServer route)
  ▼
ApiRouter.dispatch
  │ matched: dry-run
  ▼
DryRunService.run(projectId)
  │ createJob(projectId, dryRun=true)
  ▼
Orchestrator.runJob(job)
  │ for each state: runStage(job, state)
  ▼
Agent.run(ctx)        (one of 20)
  │ may call: AiGateway.dispatch(req)
  │            │
  │            ▼
  │       Provider.chat(req)
  │            │
  │            ▼
  │       ChatResponse
  │
  │ writes via: Store.saveX(rec)
  │            │
  │            ▼
  │       JCR or InMemoryStore
  │
  │ returns: AgentResult
  ▼
Orchestrator.runJob    (next state)
  │
  ▼
... loop until COMPLETED ...
  │
  ▼
DryRunService returns MigrationJobRecord
  │
  ▼
ApiRouter serialises → JSON
  │
  ▼
DASHBOARD polls /events, updates UI
```

## Failure paths

| Failure | Where it surfaces | Recovery |
|---|---|---|
| Connector unreachable | `ConnectionAgent` | Operator retries with corrected URL |
| AI call rate-limited | `AiGateway` (exponential backoff) | Automatic up to `maxRetries`; manual otherwise |
| AI returns invalid JSON | Agent | Agent validates against `jsonSchema`; fails the agent; orchestrator transitions to `FAILED` |
| Required capability missing | `AiGateway` capability gate | Refused; operator updates `AiRoutingPolicy` |
| `WAITING_FOR_CLARIFICATION` | Clarification engine | Operator answers on `#/clarifications`; the affected agent retries |
| Critical issue (e.g. broken asset) | `IssueRecord` severity=`CRITICAL` | `MIGRATE` button is disabled until resolved |
| Runaway cost | `AiGateway` budget check | `BUDGET_EXCEEDED` event; orchestrator transitions to `FAILED` |
| Bug in agent code | uncaught exception in `run()` | Orchestrator catches, transitions to `FAILED` with stack trace in `lastError` |
