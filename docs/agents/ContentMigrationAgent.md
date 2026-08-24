# ContentMigrationAgent

> Converts every eligible AEM page into an EDS section model
> (`.md` file) that references the generated blocks.

- **Stage:** `MIGRATING`
- **Phase:** 1
- **Agent name:** `content-migration`
- **Task type:** `CONTENT_MIGRATION`

## Inputs

- Every eligible `AemPageRecord` with
  `migrationStatus=DISCOVERED` (or higher).
- The block catalogue.
- The component mappings.

## Outputs

- One `GeneratedFileRecord` per page:
  - `operation=CREATE`
  - `stage=CONTENT_MIGRATION`
  - `path={eds-path}.md`
  - `content` = the EDS section model

The content is a YAML front matter + a sequence of section
blocks. For example:

```markdown
---
title: About Us
description: About WKND Adventures
---

| Section | Block |
|---|---|
| Hero | `wknd/components/hero` |
| Cards | `wknd/components/cards` |
```

## AI usage

One AI call per page. The AI receives the AEM page's
components (in order), the block catalogue, and the section
model schema. It returns the YAML + block sequence.

The mock provider returns a deterministic 3-section skeleton
per page.

## Failure modes

- **AEM page has no components:** the agent writes an empty
  section model and records a `MEDIUM` issue.
- **AI returns invalid YAML:** the agent retries; if still
  failing, writes a placeholder page that the operator can
  fill in later.

## Performance

- 1 AI call per page.
- Mock mode: ~10 ms per page.
- Real mode: 1-3 seconds per page.

## Phase 2 companions

- `UrlRedirectService` runs in the same stage (after the
  content migration) to build the AEM → EDS redirect map.
- `DependencyGraphService` runs in the same stage to build
  the dependency graph.

## Related

- [Asset policy](../migration/ASSET_POLICY.md) — assets are
  referenced by URL, not embedded.
