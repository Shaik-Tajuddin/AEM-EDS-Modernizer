# VisualValidationAgent

> Phase 1 basic visual validation. Takes screenshots at three
> viewports (desktop, tablet, mobile) and records a single
> `ValidationResultRecord` per page per viewport.

- **Stage:** `VALIDATING`
- **Phase:** 1
- **Agent name:** `visual-validation`
- **Task type:** `VISUAL_VALIDATION`

## Inputs

- The deployed preview site.
- The `BrowserClient` (Playwright-shaped; the mock returns a
  deterministic 16×16 PNG).

## Outputs

- `ValidationResultRecord`s with `kind=VISUAL`, one per page
  per viewport.

## Behaviour

The Phase 1 agent validates **every page** at all three
viewports. For a 1000-page site this is 3000 screenshots;
the cost is high. The Phase 2
`AdvancedVisualValidationAgent` uses representative
sampling to bring this down to ~5% of pages.

## AI usage

None in the basic version. The score is computed by the
`ImageDiffEngine` (comparing the preview screenshot to the
AEM original).

## Related

- [AdvancedVisualValidationAgent](AdvancedVisualValidationAgent.md) —
  the Phase 2 version with representative sampling.
