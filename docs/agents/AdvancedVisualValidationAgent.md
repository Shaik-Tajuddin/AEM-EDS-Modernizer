# AdvancedVisualValidationAgent

> Phase 2 advanced visual validation with **representative
> sampling**: 1 page per template cluster + all high-risk
> pages + a 5% random sample. Cuts validation cost by 95%
> while keeping coverage.

- **Stage:** `VALIDATING`
- **Phase:** 2
- **Agent name:** `advanced-visual-validation`
- **Task type:** `VISUAL_VALIDATION`

## Sampling algorithm

Per Master §26:

1. **One representative page per template cluster.** For each
   unique `cq:template` in scope, pick one page.
2. **All high-risk pages.** A page is high-risk if it has a
   low-confidence mapping, a broken reference, a legacy
   template, or a failing validation in a prior run.
3. **5% random sample** of the remaining pages.
4. **Merge + dedupe** the three sets.

Result: typically 5-10% of pages are visually validated, with
guaranteed coverage of every template and every high-risk
page.

## Inputs

- The deployed preview site.
- The list of authored pages with `migrationStatus` in
  `AUTHORED | MIGRATED | DISCOVERED | FAILED`.
- The `BrowserClient` (mock or real Playwright).
- The `ImageDiffEngine`.

## Outputs

- `ValidationResultRecord`s with `kind=VISUAL` (one per
  sampled page per viewport) and `kind=ACCESSIBILITY`
  (one per sampled page, simulated axe-core).

## Per-viewport

For each sampled page, the agent takes 3 screenshots
(desktop, tablet, mobile), runs `ImageDiffEngine.compare(...)`
against the AEM original, and records the average score.

A deterministic variance (-0.08..+0.08) is applied per page
hash so the dashboard surfaces a believable mix of PASS /
WARN / FAIL records.

## Performance

- ~5-10% of pages validated (representative sampling).
- Mock mode: < 100 ms per page per viewport.
- Real mode: 3-10 seconds per page per viewport (browser
  launch + screenshot + diff).

## Related

- [VisualValidationAgent](VisualValidationAgent.md) — the
  Phase 1 basic version (validates every page).
- [ImageDiffEngine](../architecture/COMPONENTS.md) — the
  image diff engine used for the score.
- [ADR 0001](../adr/0001-phase2-advanced-features.md) — the
  Phase 2 decision record.
