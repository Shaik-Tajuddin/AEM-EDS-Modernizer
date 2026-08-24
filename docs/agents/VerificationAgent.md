# VerificationAgent

> Final production crawl. Runs the full validation suite
> against the production EDS site and produces the migration
> report.

- **Stage:** `VERIFYING`
- **Phase:** 1
- **Agent name:** `verification`
- **Task type:** `VERIFICATION`

## Inputs

- The production EDS URL.
- The list of pages that were rolled out.

## Outputs

- A `MigrationReport` JSON with:
  - The final inventory (pages, components, blocks)
  - The final estimate (vs the pre-implementation estimate)
  - The full list of generated files
  - The full list of issues
  - The full list of validations
  - The AI usage summary (cost, tokens, requests per
    provider)
  - The rollout summary (stages completed, any halts)
  - The benchmark summary (agent durations)

The report is downloadable from the dashboard's `#/report`
view.

## AI usage

None. Pure validation + report generation.

## Failure modes

- **Production site unreachable:** the agent records a
  `CRITICAL` issue; the job transitions to `FAILED`. The
  operator can retry after the production site is back.
- **Validation regression vs preview:** the agent records
  a `HIGH` issue per regression; the report highlights
  them in a "regressions" section.
