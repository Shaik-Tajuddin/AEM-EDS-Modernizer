# Failure Recovery

How to recover from common failure modes.

## Job is in `FAILED` state

The most common cause is an unhandled exception in an agent
or a connector failure. The `lastError` field on the
`MigrationJobRecord` contains the stack trace.

### Recovery

1. Open the dashboard and navigate to the failed job.
2. Review the `lastError` and the recent `JobEventRecord`s.
3. If the error is transient (e.g. AEM was unreachable),
   click "Resume" to retry from the last checkpoint.
4. If the error is permanent (e.g. invalid AEM path), fix
   the issue and click "Resume".
5. If the issue cannot be fixed, click "Cancel" and start
   a new migration.

## AI rate-limited

The `AiGateway` retries with exponential backoff. If the
rate limit is sustained, the gateway exhausts its retries
and the job moves to `FAILED`.

### Recovery

1. Wait for the rate limit to clear (usually 1 minute).
2. Click "Resume" to retry from the last checkpoint.

## AEM unreachable

The `AemClient` raises a `ConnectorException` with a clear
reason. The job moves to `FAILED`.

### Recovery

1. Verify AEM is reachable:
   ```bash
   curl -I https://author-pXXXX-eYYYY.adobeaemcloud.com
   ```
2. If AEM is down, wait for it to come back.
3. Click "Resume".

## GitHub API rate-limited

The `GitHubClient` retries with exponential backoff. If
the rate limit is sustained, the client exhausts its
retries and the publishing agent moves to `FAILED`.

### Recovery

1. Wait for the rate limit to clear (usually 1 hour for
   unauthenticated requests, 5 minutes for authenticated).
2. Click "Resume" to retry from the last checkpoint.

## EDS preview down

The `PreviewAgent` records a `CRITICAL` issue and the
validation agents cannot run. The job moves to `FAILED` (in
the validation stage).

### Recovery

1. Verify EDS preview is reachable:
   ```bash
   curl -I https://{branch}--{repo}--{org}.hlx.page/
   ```
2. If EDS preview is down, wait for it to come back.
3. Click "Resume".

## Orchestrator JVM killed

The orchestrator's state is persisted in JCR under
`/var/aem-eds-modernizer/` by `JcrStore`. A kill
(-9, OOM, instance restart) leaves the job in the last
persisted state.

### Recovery

1. The AEM Author instance restarts.
2. The orchestrator's Quartz scheduler picks up the
   interrupted job.
3. The orchestrator reads the last `CheckpointRecord` and
   re-enters the state machine.

## AEM instance redeployed mid-migration

Same as "Orchestrator JVM killed". The state is in JCR
under `/var/aem-eds-modernizer/`; the redeployment does
not lose it.

## Operator accidentally triggered a real migration

The real migration can be cancelled, but the
`GeneratedFileRecord`s in the GitHub branch remain. To
clean up:

1. Click "Cancel" on the job in the dashboard.
2. Delete the modernizer branch in GitHub:
   `git push origin --delete modernizer/{projectId}/{jobId}`.
3. Delete the PR in GitHub (if one was opened).

## See also

- [INCIDENT_RESPONSE.md](INCIDENT_RESPONSE.md) — the
  incident response procedure.
- [../migration/CHECKPOINTS.md](../migration/CHECKPOINTS.md) —
  the checkpoint model.
- [RUNBOOK.md](RUNBOOK.md) — the daily runbook.
