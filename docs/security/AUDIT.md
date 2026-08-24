# Audit Log

Every `JobEventRecord` includes the `actor` (the user who
triggered the event) when running under Sling. The audit
log is a stream of `JobEventRecord`s.

## What is audited

Every state transition, every agent invocation, every AI
call, every connector call, every file write. The audit log
is the source of truth for "what happened in this
migration".

## Event format

```java
public class JobEventRecord {
    String id;
    String jobId;
    String projectId;
    MigrationState fromState;
    MigrationState toState;
    String agent;
    String message;
    Map<String, Object> data;
    String actor;          // the user (or "system" for automated events)
    Instant createdAt;
}
```

## Where the `actor` comes from

When running under Sling, the `actor` is the Sling
`ResourceResolver`'s `getUserID()`. When running in the
standalone runtime, the `actor` is `"system"` (no
authentication).

## Where the audit log is stored

- **AEM Cloud:** JCR at `/content/aem-eds-modernizer/projects/{id}/jobs/{jobId}/events/{eventId}`.
- **Standalone:** `InMemoryStore.events` (a
  `ConcurrentHashMap`).

## How to query the audit log

The dashboard's `#/events` view shows the events for the
current job. For programmatic access:

```bash
curl https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects/{projectId}/events?since={lastEventId}
```

The response is a JSON array of `JobEventRecord`s.

## Retention

The MVP has no retention policy. The audit log is kept for
the life of the job (and beyond, for completed jobs). A
follow-up adds a configurable retention policy (e.g. 90
days).

## See also

- [../adr/0013-events-as-source-of-truth.md](../adr/0013-events-as-source-of-truth.md) —
  the events-as-source-of-truth decision record.
- [../operations/INCIDENT_RESPONSE.md](../operations/INCIDENT_RESPONSE.md) —
  the incident response runbook.
