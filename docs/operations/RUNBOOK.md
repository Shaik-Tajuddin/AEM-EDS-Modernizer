# Daily Operations Runbook

The daily operations runbook for the AEM → EDS Modernizer.
Assumes the modernizer is deployed to AEM Cloud.

## Health check

```bash
curl https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/health
# {"status":"ok","time":1700000000000}
```

If the response is not `{"status":"ok",...}`:

1. Check the AEM Author instance health in Cloud Manager.
2. Check the modernizer's logs in Cloud Manager → Logs.
3. If the issue is with the modernizer, see
   [RECOVERY.md](RECOVERY.md).

## List projects

```bash
curl -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects
# [{"id": "...", "name": "..."}]
```

## Trigger a dry run

```bash
curl -X POST -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects/{id}/dry-run
# {"id": "...", "state": "RUNNING", "dryRun": true, ...}
```

The response includes the new job id. Poll the job status:

```bash
curl -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects/{id}/jobs/{jobId}
# {"id": "...", "state": "VALIDATING", "progressPct": 65, ...}
```

## Review the dry run

When the dry run completes:

```bash
# Issues
curl -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects/{id}/issues

# Validations
curl -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects/{id}/validations

# Repairs (Phase 2)
curl -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects/{id}/repairs

# Rollout stages (Phase 2)
curl -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects/{id}/rollout-stages

# Benchmarks (Phase 2)
curl -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects/{id}/benchmarks
```

## Trigger a real migration

When the dry run is approved:

1. Open the dashboard in a browser.
2. Navigate to `#/dryrun` and review the dry run.
3. Click `MIGRATE`.

The dashboard shows a confirmation dialog with the
estimate and the critical issues. Click `Confirm` to start
the real migration.

## Cancel a job

```bash
curl -X POST -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects/{id}/jobs/{jobId}/cancel
# {"state": "CANCELLED", ...}
```

## Resume a job

```bash
curl -X POST -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects/{id}/jobs/{jobId}/resume
# {"state": "RESUMED", ...}
```

## See also

- [RECOVERY.md](RECOVERY.md) — failure recovery.
- [INCIDENT_RESPONSE.md](INCIDENT_RESPONSE.md) — incident
  response.
- [OBSERVABILITY.md](OBSERVABILITY.md) — observability.
