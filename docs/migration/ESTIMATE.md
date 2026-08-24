# Pre-Implementation Estimate

The pre-implementation estimate model. Per Master §0A.2A
and §0A.2B, every migration must produce an estimate before
the operator can click `MIGRATE`.

## What the estimate covers

| Field | Description | Unit |
|---|---|---|
| `pagesEligible` | Pages in scope | count |
| `edsBlocksNew` | Distinct EDS blocks to generate | count |
| `aiRequestsExpected` | AI calls the migration will make | count |
| `costExpected` | Expected AI cost (USD) | currency |
| `timeExpectedSec` | Expected duration (seconds) | duration |
| `validationsExpected` | Validations to run | count |
| `repairsExpected` | Repairs expected | count |

For each numeric field, the estimate includes a **derivation
trail** so the user can see *why* each number was produced.

## The derivation trail

Every estimate field is a sum of contributions from each
agent. For example:

```
aiRequestsExpected: 144
  = component-intelligence: 15   (1 per component)
  + mapping: 15                  (1 per component)
  + block-generation: 10         (1 per distinct block)
  + code: 5                      (1 per scaffold file)
  + content-migration: 59        (1 per page)
  + authoring: 59                (1 per page)
  + advanced-figma-intelligence: 2 (1 per Figma file)
  + advanced-visual-validation: 6 (1 per sampled page)
  + advanced-repair: 12          (estimated, 0.2x sampled)
  + advanced-rollout: 1          (1 per rollout policy)
  = sum
```

The dashboard's `#/estimate` view shows the derivation
trail as a table. The user can click on any line to see
the per-agent details.

## The estimate is "CURRENT" if

- The `MigrationPlannerAgent` ran in the current job, OR
- The `MigrationPlannerAgent` ran in a more recent job for
  the same project.

If the estimate is stale (e.g. the dry run is 2 weeks old
and the AEM content has changed), the dashboard shows a
warning and the `MIGRATE` button is disabled until a fresh
estimate is produced.

## Lo / Expected / Hi

For each numeric field, the estimate shows three values:

- **Lo** — the lower bound (e.g. minimum cost, minimum
  time).
- **Expected** — the median.
- **Hi** — the upper bound (e.g. 95th percentile).

The `Lo / Expected / Hi` comes from the historical
`BenchmarkSampleRecord`s (Phase 2 `BenchmarkService`).

## How the estimate is computed

The `EstimatorService` (a pure function over the
`SiteInventory` and the `BenchmarkSampleRecord`s) computes
the estimate:

```java
public Estimate estimate(SiteInventory inventory, List<BenchmarkSample> samples) {
    int aiCalls = inventory.components() * 1   // component-intelligence
                 + inventory.components() * 1   // mapping
                 + inventory.distinctBlocks() * 1   // block-generation
                 + 5   // code (scaffold)
                 + inventory.pages() * 1   // content-migration
                 + inventory.pages() * 1   // authoring
                 + inventory.figmaFiles() * 2   // advanced-figma-intelligence
                 + Math.max(1, inventory.pages() / 20) * 1   // advanced-visual-validation
                 + ... ;
    double cost = samples.stream()
        .filter(s -> s.agent() == "content-migration")
        .mapToDouble(BenchmarkSample::costMicros)
        .average()
        .orElse(DEFAULT_COST_MICROS)
        * inventory.pages() / 1_000_000.0;
    ...
}
```

The pure-function design makes the estimate easy to test
and easy to reason about.

## See also

- [../adr/0005-dry-run-is-mandatory.md](../adr/0005-dry-run-is-mandatory.md) —
  the dry run that produces the estimate.
- [../agents/MigrationPlannerAgent.md](../agents/MigrationPlannerAgent.md) —
  the agent.
