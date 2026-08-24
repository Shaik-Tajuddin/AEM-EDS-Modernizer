# Section Model

The `.md` files the modernizer produces follow the EDS
section model schema: a YAML front matter block, followed by
a sequence of section blocks.

## Schema

```markdown
---
title: { page title }
description: { page description, ≤ 160 chars }
keywords: { comma-separated keywords }
author: { page author, if any }
published-date: { ISO 8601 date }
template: { template name }
---

| Section | Block | Content |
|---|---|---|
| Hero | `wknd/components/hero` | { heading, subheading, image, ctaText, ctaUrl } |
| Cards | `wknd/components/cards` | { items: [...] } |
| Accordion | `wknd/components/accordion` | { items: [...] } |
```

The section table is rendered by the EDS loader into a
sequence of `<div class="section">` elements, each with
one or more `<div class="block">` children.

## How the table maps to the DOM

```
<div class="section">
  <div>
    <div class="hero">...</div>
  </div>
  <div>
    <div class="cards">...</div>
  </div>
</div>
```

The EDS loader's `buildBlock` function handles the DOM
construction. The block's `decorate` function (in
`blocks/{name}/{name}.js`) is then called.

## Why a table, not JSON?

Per EDS conventions, the section model is a table (not
JSON) so that:

- The content is editable in Google Docs or Microsoft Word
  (the upstream authoring source).
- The migration is reversible: a human can take the
  generated `.md` and edit it without learning JSON.
- The diff is human-readable: `git diff` on a `.md` file
  shows the section table changes in plain text.

## Block columns

| Column | Required | Description |
|---|---|---|
| Section | yes | The section heading (rendered as `<h2>` in the section header) |
| Block | yes | The block name (matches a folder in `blocks/`) |
| Content | yes | The block's content (block-specific schema) |

The content column is block-specific. The `cards` block
expects `items: [{ image, title, body }]`. The `hero`
block expects `{ heading, subheading, image, ctaText, ctaUrl }`.

## Migration of nested blocks

EDS supports nested blocks (a block within a block). The
modernizer flattens the nesting by default; a follow-up
adds an option to preserve nested structures.

## Related

- [REPO_CONVENTIONS.md](REPO_CONVENTIONS.md) — the full
  repo layout.
- [../agents/ContentMigrationAgent.md](../agents/ContentMigrationAgent.md) —
  the agent that produces the section models.
