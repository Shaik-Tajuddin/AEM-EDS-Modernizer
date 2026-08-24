# Incident Response

The incident response procedure for the AEM → EDS
Modernizer.

## Severity levels

| Severity | Definition | Response time |
|---|---|---|
| `SEV1` | The modernizer is completely down or has caused a production outage | 15 minutes |
| `SEV2` | A major feature is broken (e.g. dry run cannot complete) | 1 hour |
| `SEV3` | A minor feature is broken (e.g. one dashboard view does not render) | 4 hours |
| `SEV4` | A cosmetic issue or a non-urgent bug | Next business day |

## SEV1: Production outage caused by the modernizer

### Detection

- Cloud Manager alerts on a sudden spike in AEM Author
  error rate.
- Customer reports the dashboard is unreachable.
- Customer reports that a migration caused a production
  issue (e.g. a broken URL redirect).

### Immediate response

1. **Page the on-call engineer.** Use the standard
   on-call rotation.
2. **Open an incident channel** (e.g. `#incident-2026-08-24-modernizer`).
3. **Stop the bleeding.** If the issue is a bad
   migration, revert the merge commit in GitHub. The
   EDS pipeline deploys the revert automatically.
4. **Communicate.** Update the customer-facing status
   page.

### Investigation

1. **Check the modernizer logs** in Cloud Manager.
2. **Check the AEM Author logs** in Cloud Manager.
3. **Check the GitHub branch** that was merged.
4. **Identify the root cause.**

### Resolution

1. **Fix the root cause.** This may be a code fix, a
   config change, or a process change.
2. **Deploy the fix** via the standard Cloud Manager
   pipeline.
3. **Re-run the migration** on top of the revert.
4. **Verify** the new migration is correct.
5. **Close the incident.**

### Post-mortem

1. **Write a post-mortem** within 5 business days.
2. **Identify contributing factors** (not just the
   proximate cause).
3. **Identify action items** to prevent recurrence.
4. **Track the action items** in the engineering
   backlog.

## SEV1: Modernizer is down

### Detection

- Cloud Manager health check fails.
- Customer reports the dashboard is unreachable.

### Immediate response

1. **Page the on-call engineer.**
2. **Open an incident channel.**
3. **Check AEM Author health** in Cloud Manager.
4. **Restart the AEM Author instance** if needed.
5. **Communicate.**

### Investigation

1. **Check the modernizer logs** in Cloud Manager.
2. **Check the AEM Author logs** in Cloud Manager.
3. **Identify the root cause.**

### Resolution

1. **Fix the root cause** (e.g. a bad config, a JVM
   crash).
2. **Deploy the fix** via the standard pipeline.
3. **Verify** the modernizer is healthy.
4. **Close the incident.**

### Post-mortem

Same as the previous SEV1.

## SEV2: Major feature broken

### Detection

- Customer reports a major feature is broken.
- Operator reports a job is stuck in a state.

### Response

1. **Acknowledge the issue** within 1 hour.
2. **Investigate.**
3. **Fix or work around.**
4. **Communicate.**
5. **Close the incident.**

### Post-mortem

A post-mortem is recommended but not required.

## SEV3 / SEV4: Minor / cosmetic issue

### Response

1. **Acknowledge the issue** within 4 hours (SEV3) or next
   business day (SEV4).
2. **File a bug** in the engineering backlog.
3. **Fix in the next sprint.**

## On-call rotation

The on-call rotation is managed in PagerDuty. The rotation
is:

- 1 primary on-call engineer
- 1 secondary on-call engineer
- 1 manager on-call

The rotation is weekly, from Monday 09:00 to the following
Monday 09:00 (local time).

## Communication

During an incident, all communication goes through the
incident channel. The incident commander is responsible for
updating the channel and the customer-facing status page.

## See also

- [RUNBOOK.md](RUNBOOK.md) — the daily runbook.
- [RECOVERY.md](RECOVERY.md) — failure recovery.
- [OBSERVABILITY.md](OBSERVABILITY.md) — observability.
