# AuthoringAgent

> Creates AEM Universal Editor (UE) compatible page
> structures in the target. This is how the migrated content
> is reflected back into AEM as authored pages, not just as
> EDS files.

- **Stage:** `AUTHORING`
- **Phase:** 1
- **Agent name:** `authoring`
- **Task type:** `AUTHORING`

## Inputs

- Every `AemPageRecord` with `migrationStatus=MIGRATED`.
- The target AEM Author (via `AemClient`).
- The selected `AuthoringStrategy` (Phase 2 registry).

## Outputs

- AEM pages in the target, with UE-compatible field
  structure.
- Updated `AemPageRecord`s with `migrationStatus=AUTHORED`.

## Authoring strategies (Phase 2)

The `AuthoringStrategyRegistry` provides 4 built-in
strategies:

| Strategy | When to use |
|---|---|
| `UNIVERSAL_EDITOR` | Default. Creates UE-compatible page structures. |
| `DOC_BASED` | Creates doc-based authoring pages (for sites that prefer doc-based editing). |
| `EXISTING_REPO` | Reuses an existing target repo (no new pages created in AEM). |
| `CUSTOM_ADAPTER` | Plug in a custom authoring backend. |

The selected strategy is recorded on the project's
`ProjectRecord.authoringStrategy`.

## AI usage

One AI call per page to map the AEM field structure to the
UE field structure. The mock provider returns a
deterministic 1:1 mapping.

## Failure modes

- **AEM Author rejects the page creation** (e.g. invalid
  template path): the agent records a `HIGH` issue and
  continues with the next page.
- **AI returns an incompatible field map:** the agent falls
  back to a minimal mapping (title + body only) and records
  a `MEDIUM` issue.

## Performance

- 1 AI call per page + 1 AEM write per page.
- Mock mode: ~5 ms per page.

## Related

- [AuthoringStrategyRegistry](AuthoringStrategyRegistry.md) —
  the Phase 2 strategy registry.
