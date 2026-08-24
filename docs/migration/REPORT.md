# Migration Report

The migration report format. The `VerificationAgent`
produces a `MigrationReport` JSON at the end of every
migration (real or dry run).

## Structure

```json
{
  "jobId": "...",
  "projectId": "...",
  "dryRun": false,
  "startedAt": "2026-08-24T08:00:00.000Z",
  "finishedAt": "2026-08-24T08:30:00.000Z",
  "durationSec": 1800,
  "inventory": {
    "pages": 59,
    "eligiblePages": 59,
    "components": 15,
    "templates": 3
  },
  "estimate": {
    "pagesEligible": 59,
    "edsBlocksNew": 3,
    "aiRequestsExpected": 144,
    "costExpected": 1.35,
    "timeExpectedSec": 600,
    "validationsExpected": 30,
    "repairsExpected": 12
  },
  "actual": {
    "aiRequests": 100,
    "costActual": 0.95,
    "timeActualSec": 540,
    "validationsPassed": 18,
    "validationsFailed": 0,
    "repairsApplied": 23
  },
  "diff": {
    "files": [
      {
        "path": "us/en/about.md",
        "operation": "CREATE",
        "stage": "CONTENT_MIGRATION"
      }
    ]
  },
  "issues": [...],
  "validations": [...],
  "repairs": [...],
  "rollout": {
    "stages": [
      {"name": "PREVIEW", "percentage": 0, "status": "COMPLETED"},
      ...
    ]
  },
  "aiUsage": {
    "providers": {"mock": {"requests": 100, "costMicros": 0, "tokensIn": 5000, "tokensOut": 1226}},
    "routing": "MULTI_PROVIDER"
  },
  "benchmarks": {
    "perAgent": {
      "discovery": {"p50Ms": 10, "p95Ms": 50, "samples": 5},
      ...
    }
  }
}
```

## Estimate vs actual

The report includes both the pre-implementation estimate
and the actual values. The operator can see the variance
per metric.

## Downloading

The dashboard's `#/report` view has a "Download JSON"
button that serialises the report and downloads it as
`{jobId}.json`.

## Redaction

Before the report is written, the `Redactor` strips any
secret patterns (see
[../security/REDACTOR.md](../security/REDACTOR.md)). The
downloaded JSON is safe to share with the customer.

## See also

- [../adr/0013-events-as-source-of-truth.md](../adr/0013-events-as-source-of-truth.md) —
  the events that make up the report.
- [../agents/VerificationAgent.md](../agents/VerificationAgent.md) —
  the agent.
