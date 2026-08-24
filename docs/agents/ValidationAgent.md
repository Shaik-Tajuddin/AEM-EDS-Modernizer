# ValidationAgent

> Runs functional, SEO, and accessibility checks against the
> EDS preview site. Emits a `ValidationResultRecord` per check
> per page.

- **Stage:** `VALIDATING`
- **Phase:** 1
- **Agent name:** `validation`
- **Task type:** `VALIDATION`

## Inputs

- The deployed preview site.
- The list of authored pages.
- The `BrowserClient` (Playwright-shaped).

## Outputs

- `ValidationResultRecord`s with: `pagePath`, `kind`
  (`CONTENT`, `FUNCTIONAL`, `SEO`, `ACCESSIBILITY`),
  `score`, `passed`, `failed`, `warnings`.

## Checks per kind

- **CONTENT** — does the page contain the expected sections?
  Are the headings hierarchical? Are the links resolvable?
- **FUNCTIONAL** — do the buttons / forms / accordions work?
- **SEO** — is the `<title>` set? Is the meta description
  set? Are the Open Graph tags present?
- **ACCESSIBILITY** — axe-core run; reports violations.

## AI usage

The agent calls `AiGateway.dispatch(...)` for natural-language
content checks (e.g. "is the description meaningful?"). The
mock provider returns a deterministic pass.

## Failure modes

- **Page returns 5xx:** the agent records a `CRITICAL` issue
  and skips remaining checks for that page.
- **axe-core reports > 5 violations:** the agent records a
  `HIGH` issue and continues.

## Related

- [VisualValidationAgent](VisualValidationAgent.md) — runs
  in the same stage, focuses on visual diffs.
- [AdvancedVisualValidationAgent](AdvancedVisualValidationAgent.md) —
  the Phase 2 version with representative sampling.
