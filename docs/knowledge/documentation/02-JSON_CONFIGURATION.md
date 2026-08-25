# JSON Configuration - Models, Definitions, and Filters

## Overview

AEM EDS blocks are configured through a **single JSON file** per block that contains three top-level arrays:

1. **`definitions`** - Block metadata, resource type, and template defaults
2. **`models`** - Authoring interface fields (what authors edit)
3. **`filters`** - Placement restrictions (where the block can go)

> **Production Reference**: See `text_callout_block/_text-callout.json` for the canonical example.
>
> **File Naming**: Block JSON files MUST have an underscore prefix: `_text-callout.json`, `_hero.json`, `_simple-cta.json`

See: [01-FUNDAMENTALS.md](01-FUNDAMENTALS.md) for core concepts

## New blocks and sections (discovery)

When someone requests a **new block**, **new section**, or describes UI from a **prompt or screenshot**, the workflow is **discovery first** (not an automatic new JSON file):

1. **Analyze** the requirement and **ask clarifying questions** until intent is clear — including **inferred behavior** from the UI (e.g. a shortcut list: reference-only vs must emit **`window.Bus`** events; who reacts; empty states). Use product intuition to surface questions; **wait for answers** (or an explicit “v1 is display-only” scope) before wiring interaction.
2. **Check what already exists** in this repository: **`blocks/*/`** (custom blocks), **`models/_section.json`** and **`styles/section-utilities.css`** (section appearance, inner grid, page grid row), and any other **`models/_*.json`** section models.
3. If the need can be met by **composing** those pieces, recommend that. **Do not** force composition when a new block or section is genuinely needed.
4. When naming components for authors, **prefer custom `blocks/`** implementations over **reference-only** samples under `.cursor/knowledge/reference-blocks/` or **generic xwalk/boilerplate** block types—unless no custom block fits and a stock pattern is acceptable.
5. **Limit edits** to the new block’s footprint (typically **`blocks/<name>/`**, **`block-configs/component-list.json`** when needed, and **`npm run build:json`** outputs) — **no** unrelated scripts, drive-by lint fixes, or opportunistic refactors. See **`.cursor/rules/08-block-creation-checklist.mdc`** (*Scope of file changes*).

Authoritative Cursor rule: **`.cursor/rules/28-new-component-discovery.mdc`**. Related: **`15-sections-vs-blocks.mdc`**, **`08-block-creation-checklist.mdc`**, **`16-section-creation-pattern.mdc`**.

## Complete JSON Structure

Every block JSON file follows this top-level structure:

```json
{
  "definitions": [ ... ],
  "models": [ ... ],
  "filters": [ ... ]
}
```

There are **no** top-level `groups` objects. The structure is flat arrays.

---

## Definitions

### Purpose

Define block metadata: title, ID, resource type, and template defaults.

### Structure (Production Pattern)

```json
{
  "definitions": [
    {
      "title": "Text callout",
      "id": "text-callout",
      "plugins": {
        "xwalk": {
          "page": {
            "resourceType": "core/franklin/components/block/v1/block",
            "template": {
              "name": "Text callout",
              "model": "text-callout",
              "filter": "text-callout",
              "title": "<p>Title</p>",
              "text": "<p>Add your message here.</p>",
              "ctaContent": "<p>Learn more</p>",
              "classes": "cta-primary-filled",
              "trackInview": false,
              "trackClick": false
            }
          }
        }
      }
    }
  ]
}
```

### Critical Rules

| Rule | Detail |
|---|---|
| **resourceType** | Always `"core/franklin/components/block/v1/block"` — never custom |
| **template.name** | Display name of the block |
| **template.model** | Must match the model `id` |
| **template.filter** | Must match the filter `id` |
| **template defaults** | Include only fields with **meaningful defaults** — omit fields that start empty (e.g., `id`, `ctaLink`, meta fields) |

### Template Default Strategy

The template provides initial values when an author first adds the block. Only include fields where a non-empty default improves the experience:

```json
"template": {
  "name": "Text callout",
  "model": "text-callout",
  "filter": "text-callout",
  "title": "<p>Title</p>",           // Meaningful placeholder
  "text": "<p>Add your message here.</p>",  // Meaningful placeholder
  "ctaContent": "<p>Learn more</p>",  // Sensible default label
  "classes": "cta-primary-filled",     // Default style variant
  "trackInview": false,                // Boolean defaults
  "trackClick": false                  // Boolean defaults
  // id: omitted — starts empty
  // ctaLink: omitted — starts empty
  // trackInview_meta: omitted — starts empty
  // trackClick_meta: omitted — starts empty
}
```

---

## Models

### Mandatory opening fields (project rule)

Every **block** model (`blocks/*/_*.json`) must include these **opening fields** (a leading `tab` is allowed before them):

1. **`id`** — `text`, block / bus identifier — **first row-producing field** in generated HTML.
2. **`classes`** — `text`, **`name` must be exactly `classes`**, **`hidden`: true** (and **`readOnly`: true** recommended), **`value`** = literal **`eds-block-<kebab-block-name>`** matching the block folder (e.g. `eds-block-hero`). **Does not create a table row** — merged onto the block root as CSS classes. Franklin also adds the generic **`block`** class on the root in `decorateBlock`. Include both **`id`** and **`classes`** in **`definitions` → `template`**.

**Appearance and light behavior** (layout, selection mode, tones, per-item width, etc.) use **`classes_*`** fields (no rows); read variants in JS from **`block.classList`**.

**Typography:** AEM does not emit **`u-font-*`** on table cells. Add font utilities in **`decorate()`** on nodes your block JS creates (see **`18-typography-font-utilities.mdc`**).

See `.cursor/rules/03-block-json-pattern.mdc` (Mandatory opening fields). **`decorate()`** row indices and **`*-example.html`** must list **row-producing fields only** (skip `classes` / `classes_*` in the count). A block authored **nested** inside another may use **one fewer `div` wrapper per row** than at section level — use **`getHtmlFromBlockRow` / `getTextFromBlockRow`** in **that** block’s JS when needed. Listing nested blocks in a parent’s **`filters`** does **not** require special parent JavaScript (see **`22-repeatable-parent-child-blocks.mdc`**).

### Page metadata models (WCM templates / Universal Editor)

**Page-level** properties (title, SEO, template-specific fields) are modeled separately from blocks. Universal Editor uses **`document.body`**’s **`data-aue-model`** to pick which **`models[]` entry** from the merged **`component-models.json`** drives the page metadata panel.

| Source file | Model `id` on `models[0]` | When to use |
|-------------|---------------------------|-------------|
| **`models/_page.json`** | **`page-metadata`** | Default metadata for generic site pages. |
| **`models/_page-<TEMPLATE_NAME>.json`** | **`<TEMPLATE_NAME>-metadata`** | Same **`<TEMPLATE_NAME>`** as the AEM WCM template **folder name** (kebab-case), when that template needs **different** page fields. |

Register each partial in **`models/_component-models.json`** (e.g. `"…": "./_page.json#/models"`, then `"…": "./_page-pdp-page.json#/models"`). The build merges them into root **`component-models.json`**.

**Stability:** Do **not** rename existing field **`name`** values in **`models/_page.json`** / **`models/_page-*.json`** or rename existing **`helix-query.yaml`** index property keys / **`select`** selectors unless the user explicitly requests a migration — authored content and indexes may already depend on them. Add **new** fields or properties only when asked. See **`.cursor/rules/29-helix-query-page-metadata.mdc`**.

**HTML:** AEM may set **`body[data-aem-template]`** to a path such as `/conf/<project>/settings/wcm/templates/pdp-page`. Universal Editor does not always set **`body[data-aue-model]`** to the matching model. **`scripts/editor-support.js`** runs on load: if **`data-aem-template`** is present, it takes the **last path segment**; if that segment contains **`page`** (case-insensitive), it sets **`data-aue-model`** to **`<segment>-metadata`**. Otherwise it leaves **`data-aue-model`** unchanged. Full rules: **`.cursor/rules/23-page-template-metadata.mdc`**.

### Example HTML file (required for every block)

Each block folder under `blocks/<name>/` must include **`<name>-example.html`**: a **standalone** HTML page (same global `<head>` assets as `head.html`) that embeds the **assumed AEM-generated** block markup **before** `decorate()`. It documents row order, block root classes (`classes` + `classes_*`), and lets you verify CSS/JS via **`loadBlock()`** without hand-wiring the block’s own `.css`/`.js`. The page must **exhaust every `classes_*` (and `classes`) select value**—including empty-string options—across one or more block instances, per **Exhaustive `classes_*` / style coverage** in `.cursor/rules/05-html-example-pattern.mdc`. **Do not** add image markup unless the model includes **`reference`** fields; **richtext** examples assume **no** inline images unless a block documents an exception. **Multiple block samples** on one page: each sample gets **`h2` + `p` + block root** inside its **own** `main > div`, with light inline `<style>` for separators (**Variant labels** in **`05-html-example-pattern.mdc`**). **Richtext and other prose inside rows** use **Lorem Ipsum** only (**Body copy** rule in **`05-html-example-pattern.mdc`**). Each block folder **must** include **`README.md`** with options, **plain-language rendering** (usable without assuming sight or web expertise), **LLM-oriented “when to pick”** text, and a **fixed per-variation pattern** covering **every** option value (**`08-block-creation-checklist.mdc`** Step 6; example `blocks/content/README.md`). Do **not** link the block stylesheet or module directly. Full rules: `.cursor/rules/05-html-example-pattern.mdc`, checklist steps 5–6 in `08-block-creation-checklist.mdc`.

### Purpose

Define the authoring interface — what fields authors can edit and how they're presented.

### Structure

```json
{
  "models": [
    {
      "id": "text-callout",
      "fields": [
        { "component": "tab", "label": "General", "name": "tabGeneral" },
        { "component": "text", "name": "id", "label": "ID" },
        { "component": "richtext", "name": "title", "label": "Title", "required": true },
        ...
      ]
    }
  ]
}
```

### Field Components

| Component | Input Type | Use Case | Notes |
|-----------|-----------|----------|-------|
| `text` | Single-line text | IDs, short strings, tracking metadata | Plain text only |
| `richtext` | Rich text editor | **All content text** — titles, body, CTA labels | Generates `<p>`, `<strong>`, etc. |
| `aem-content` | Content reference | **All URL/link fields** — page links | Generates `<a>` element in HTML |
| `reference` | Asset picker | **All image fields** | Emits `<picture>` + `<img>` in the block row (see below) |
| `select` | Dropdown | Style variants, alignment | Use `valueType: "string"` |
| `boolean` | Toggle switch | Enable/disable features | **Not `checkbox`** — use `boolean` |
| `tab` | Tab container | Group fields into tabs | Does NOT create rows in HTML |
| `number` | Numeric input | Measurements, counts | |
| `multiselect` | Multiple selection | Tags, categories | |

> **⚠️ Critical**: Use `boolean` (not `checkbox`) for toggle fields. This is the correct production component.

### `reference` (image) — HTML shape in block output

A **`reference`** field creates **one row** (`<div><div>…</div></div>`). The inner cell typically contains a **`<picture>`** with a WebP **`source`** and a fallback **`<img>`**:

```html
<div>
  <div>
    <picture>
      <source
        type="image/webp"
        srcset="https://example.com/image.webp"
        media="(min-width: 600px)"
      />
      <img
        loading="lazy"
        alt="background image for teaser"
        src="https://example.com/image.jpg"
        width="1440"
        height="305"
      />
    </picture>
  </div>
</div>
```

In **`blocks/*/*-example.html`**, add **`<picture>` / image rows only** for fields that are **`reference`** in the model. Do **not** add images only because **`richtext`** exists—assume text-only richtext unless a block explicitly documents an exception. When you **do** include `reference` rows, use **full gradholder.com URLs** (not DAM URLs and **not** site-relative image paths such as `/img/...` or `/media/...` on the project host — the string `/img/` may appear **inside** a gradholder URL after `gradholder.com`; that is correct). Pattern:

`https://www.gradholder.com/img/{width}x{height}/{hexWithoutHashFrom}/{hexWithoutHashTo}?type={horizontal|vertical|radial}`

Example: `https://www.gradholder.com/img/1440x305/2563eb/fbbf24?type=horizontal` — use **two distinct** hex stops (multi-color gradient), vary pairs across samples, same URL in `srcset` and `src` for simple local demos. Full rules: `.cursor/rules/05-html-example-pattern.mdc`. Example without images: `blocks/content/content-example.html`; blocks with **`reference`** should include matching picture rows.

### Multi-fields and `container` (properties panel)

Adobe documents that when a field uses **`"multi": true`**, **container nesting is not permitted for multi-fields in the properties panel** of the Universal Editor. See [Model definitions, fields, and component types — Fields (`multi`)](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/universal-editor/field-types#fields).

**Project rule:** Do **not** model repeating items as `{ "component": "container", "multi": true, "fields": [ … ] }` for block models used in UE. That pattern is invalid for the properties panel.

**Instead:** Use **flat, numbered fields** and **one tab per item** (see `.cursor/rules/10-json-advanced-patterns.mdc`), e.g. `option1Id`, `option1Label`, `option1MonthlyPrice`, `option1TotalPrice`, then `option2Id`, … up to the maximum slots you support.

### Tab Organization

Tabs organize the authoring UI into logical groups. Fields after a `tab` component belong to that tab until the next `tab` appears.

**Standard Tab Pattern** (3 tabs):

```json
"fields": [
  { "component": "tab", "label": "General", "name": "tabGeneral" },
  // ... content fields (id, title, text, links, etc.)

  { "component": "tab", "label": "Appearance", "name": "tabAppearance" },
  // ... appearance fields (alignment, spacing, etc.)

  { "component": "tab", "label": "Analytics", "name": "tabAnalytics" },
  // ... analytics fields (trackInview, trackClick, etc.)
]
```

**Key facts about tabs**:
- Tab `name` should use camelCase with `tab` prefix (e.g., `tabGeneral`)
- **Tabs do NOT create rows** in the output HTML — they are purely UI organization
- **`classes` and `classes_*` fields do NOT create rows** — their values are applied as CSS classes on the block element

### Conditional Visibility (`condition`)

Show/hide fields based on other field values using **JSON Logic** in the `condition` property:

```json
{
  "component": "text",
  "name": "trackInview_meta",
  "label": "Inview analytics meta",
  "condition": {
    "==": [
      { "var": "trackInview" },
      true
    ]
  }
}
```

> **⚠️ Critical**: The property is `condition` (not `visible`). It uses JSON Logic objects (not string expressions).

**Common Patterns**:

```json
// Show when boolean is true
"condition": { "==": [{ "var": "trackInview" }, true] }

// Show when select has specific value
"condition": { "==": [{ "var": "classes" }, "cta-link"] }

// Show when field is not empty
"condition": { "!!": [{ "var": "ctaLink" }] }
```

### Classes Notation for Style Variants

AEM EDS uses a special pattern where field names starting with `classes` are auto-applied as CSS classes on the block's outer `<div>`. **These fields do NOT create rows in the HTML**.

#### Primary `classes` Field

```json
{
  "component": "select",
  "name": "classes",
  "label": "CTA Type",
  "valueType": "string",
  "value": "cta-primary-filled",
  "options": [
    { "name": "Primary filled", "value": "cta-primary-filled" },
    { "name": "Primary outlined", "value": "cta-primary-outlined" },
    { "name": "Secondary filled", "value": "cta-secondary-filled" },
    { "name": "Secondary outlined", "value": "cta-secondary-outlined" },
    { "name": "Link", "value": "cta-link" }
  ]
}
```

#### Secondary `classes_*` Fields

```json
{
  "component": "select",
  "name": "classes_textCalloutAlign",
  "label": "Alignment",
  "valueType": "string",
  "value": "",
  "options": [
    { "name": "Left", "value": "" },
    { "name": "Center", "value": "text-callout-align-center" },
    { "name": "Right", "value": "text-callout-align-right" }
  ]
}
```

#### Resulting HTML

When author selects `cta-primary-filled` and `text-callout-align-center`:

```html
<div class="text-callout cta-primary-filled text-callout-align-center">
  <div><div>...</div></div>
  ...
</div>
```

Both `classes` and `classes_*` values become CSS classes on the outer block element. JavaScript does not need to handle them.

### Analytics Fields Pattern

Every block should include analytics fields in the Analytics tab:

```json
{
  "component": "tab",
  "label": "Analytics",
  "name": "tabAnalytics"
},
{
  "component": "boolean",
  "name": "trackInview",
  "label": "Track inview",
  "description": "Enable tracking when this block comes into view."
},
{
  "component": "text",
  "name": "trackInview_meta",
  "label": "Inview analytics meta",
  "description": "Metadata to send with inview tracking events",
  "condition": { "==": [{ "var": "trackInview" }, true] }
},
{
  "component": "boolean",
  "name": "trackClick",
  "label": "Track click",
  "description": "Enable tracking for the CTA link"
},
{
  "component": "text",
  "name": "trackClick_meta",
  "label": "Click analytics meta",
  "description": "Metadata to send with click tracking events",
  "condition": { "==": [{ "var": "trackClick" }, true] }
}
```

**Key**: Use `text` component for meta fields (not `textarea`). The meta value is typically a JSON string that gets stored as a data attribute.

---

## Filters

### Purpose

Control where a block can be placed in the page structure.

### Structure

```json
{
  "filters": [
    {
      "id": "text-callout",
      "components": []
    }
  ]
}
```

### When to Use

| Scenario | Filter Config | Example |
|---|---|---|
| Block can go anywhere | `"components": []` | Text callout, generic CTA |
| Block restricted to specific sections | `"components": ["section-name"]` | Header-only blocks |
| Container block with child blocks | `"components": ["child-block"]` | Tabs container, accordion |

If a block has no placement restrictions, use empty `components: []`. The filter is still included for completeness.

### Allowing blocks inside another block (filters only)

To let authors place block **B** inside block **A**, add **B**’s filter `id` to **A**’s `"components"`. This is an **authoring rule** only. **A**’s JavaScript does **not** need extra “parent” logic **unless** the product requires **A** to read, reorder, or wrap **B**’s DOM (see `.cursor/rules/22-repeatable-parent-child-blocks.mdc`). Franklin decorates **each** block with the usual `decorate()` contract.

### Repeatable parent + child blocks

When a **screenshot** or **requirement** implies **repeating** UI modeled as a **parent block** and a **child** block (see `.cursor/rules/22-repeatable-parent-child-blocks.mdc`):

- The **parent** block’s **`filters[].components`** **must include** the **child block’s filter `id`** (same string as the child’s `filters[].id`, e.g. `"my-list-item"`).
- Put **shared** fields (title, footnote after the list, outer layout) on the **parent** model.
- Put **per-repeat** fields and **per-item** style variants on the **child** model.

Nested child blocks are still Franklin blocks but may get a **different AEM-generated row shape** than at section level; the **child** block’s **`extractConfig`** and **`*-example.html`** should reflect that when the child is authored nested. The **parent** uses normal block JS **unless** it must **compose** those children (e.g. `option-selector`).

---

## Row Layout: What Creates Rows vs. What Doesn't

This is critical for position-based extraction in JavaScript:

| Field Type | Creates Row? | Notes |
|---|---|---|
| `text`, `richtext`, `aem-content`, `reference`, `boolean`, `number` | **Yes** | Each creates a `<div><div>value</div></div>` row |
| `select` with name `classes` or `classes_*` | **No** | Values applied as CSS classes on outer div |
| `tab` | **No** | Purely UI organization |

### Row Position Map Example (text-callout)

Given this model field order (excluding tabs and classes fields):

```
0: id (text)
1: title (richtext)
2: text (richtext)
3: ctaLink (aem-content)
4: ctaContent (richtext)
5: trackInview (boolean)
6: trackInview_meta (text)
7: trackClick (boolean)
8: trackClick_meta (text)
```

This maps directly to `[...block.children]` positions in JavaScript.

---

## Complete Production Example: text-callout.json

```json
{
  "definitions": [
    {
      "title": "Text callout",
      "id": "text-callout",
      "plugins": {
        "xwalk": {
          "page": {
            "resourceType": "core/franklin/components/block/v1/block",
            "template": {
              "name": "Text callout",
              "model": "text-callout",
              "filter": "text-callout",
              "title": "<p>Title</p>",
              "text": "<p>Add your message here.</p>",
              "ctaContent": "<p>Learn more</p>",
              "classes": "cta-primary-filled",
              "trackInview": false,
              "trackClick": false
            }
          }
        }
      }
    }
  ],
  "models": [
    {
      "id": "text-callout",
      "fields": [
        {
          "component": "tab",
          "label": "General",
          "name": "tabGeneral"
        },
        {
          "component": "text",
          "name": "id",
          "label": "ID",
          "description": "Optional anchor ID for this block"
        },
        {
          "component": "richtext",
          "name": "title",
          "label": "Title",
          "description": "Heading displayed above the body text",
          "required": true
        },
        {
          "component": "richtext",
          "name": "text",
          "label": "Text",
          "description": "Supporting copy below the title"
        },
        {
          "component": "aem-content",
          "name": "ctaLink",
          "label": "CTA link"
        },
        {
          "component": "richtext",
          "name": "ctaContent",
          "label": "CTA text"
        },
        {
          "component": "select",
          "name": "classes",
          "label": "CTA Type",
          "valueType": "string",
          "value": "cta-primary-filled",
          "options": [
            { "name": "Primary filled", "value": "cta-primary-filled" },
            { "name": "Primary outlined", "value": "cta-primary-outlined" },
            { "name": "Secondary filled", "value": "cta-secondary-filled" },
            { "name": "Secondary outlined", "value": "cta-secondary-outlined" },
            { "name": "Link", "value": "cta-link" }
          ]
        },
        {
          "component": "tab",
          "label": "Appearance",
          "name": "tabAppearance"
        },
        {
          "component": "select",
          "name": "classes_textCalloutAlign",
          "label": "Alignment",
          "description": "Align title, text, and CTA within the block",
          "valueType": "string",
          "value": "",
          "options": [
            { "name": "Left", "value": "" },
            { "name": "Center", "value": "text-callout-align-center" },
            { "name": "Right", "value": "text-callout-align-right" }
          ]
        },
        {
          "component": "tab",
          "label": "Analytics",
          "name": "tabAnalytics"
        },
        {
          "component": "boolean",
          "name": "trackInview",
          "label": "Track inview",
          "description": "Enable tracking when this block comes into view."
        },
        {
          "component": "text",
          "name": "trackInview_meta",
          "label": "Inview analytics meta",
          "description": "Metadata to send with inview tracking events",
          "condition": { "==": [{ "var": "trackInview" }, true] }
        },
        {
          "component": "boolean",
          "name": "trackClick",
          "label": "Track click",
          "description": "Enable tracking for the CTA link"
        },
        {
          "component": "text",
          "name": "trackClick_meta",
          "label": "Click analytics meta",
          "description": "Metadata to send with click tracking events",
          "condition": { "==": [{ "var": "trackClick" }, true] }
        }
      ]
    }
  ],
  "filters": [
    {
      "id": "text-callout",
      "components": []
    }
  ]
}
```

---

## JSON Merge with merge-json-cli

### Purpose

Reuse common field definitions across multiple blocks without duplication.

### Installation

```bash
npm install --save-dev merge-json-cli
```

### Syntax

Use the spread operator (`...`) to merge shared patterns into fields arrays:

```json
{
  "id": "my-block",
  "fields": [
    { "component": "tab", "label": "General", "name": "tabGeneral" },
    "..._shared-general-fields.json",
    { "component": "tab", "label": "Analytics", "name": "tabAnalytics" },
    "..._analytics.json"
  ]
}
```

### Shared Analytics Pattern (`_analytics.json`)

```json
[
  {
    "component": "boolean",
    "name": "trackInview",
    "label": "Track inview"
  },
  {
    "component": "text",
    "name": "trackInview_meta",
    "label": "Inview analytics meta",
    "condition": { "==": [{ "var": "trackInview" }, true] }
  },
  {
    "component": "boolean",
    "name": "trackClick",
    "label": "Track click"
  },
  {
    "component": "text",
    "name": "trackClick_meta",
    "label": "Click analytics meta",
    "condition": { "==": [{ "var": "trackClick" }, true] }
  }
]
```

---

## Best Practices

### Field `description` (Universal Editor)

The optional **`description`** on each field is shown to **content authors** in the properties panel. It should explain **what to enter** and **why it matters** in everyday language.

- **Do not** document implementation there (HTML structure, table rows, merge behavior, CSS class names, field collapse, or developer events).
- **Omit** `description` when the **label** already makes the field obvious, or when only developers see the field (for example hidden **`classes`** markers).
- Full project rules: **`.cursor/rules/25-json-field-descriptions.mdc`**.

### Field Organization

✓ **Do**:
- Organize into General → Appearance → Analytics tabs
- Use `richtext` for all content text
- Use `aem-content` for all URL/link fields
- Use `boolean` for toggles
- Use `condition` (JSON Logic) for conditional visibility
- Include `valueType: "string"` on select fields
- Add **`description`** only when it helps authors in **plain language**; skip it when the **label** is self-explanatory. Do **not** use descriptions for implementation details (DOM, rows, CSS class names) — see **`.cursor/rules/25-json-field-descriptions.mdc`**
- Set `required: true` on essential fields

✗ **Don't**:
- Use `checkbox` — use `boolean` instead
- Use `visible` — use `condition` with JSON Logic
- Use `text` for content that needs formatting
- Include every field in the template — only meaningful defaults
- Forget `valueType` on select fields

### Naming Conventions

```
// Tab names: camelCase with "tab" prefix
"name": "tabGeneral"
"name": "tabAppearance"
"name": "tabAnalytics"

// Content field names: camelCase
"name": "ctaLink"
"name": "ctaContent"

// Analytics fields: camelCase with underscore for meta
"name": "trackInview"
"name": "trackInview_meta"
"name": "trackClick"
"name": "trackClick_meta"

// Class fields: "classes" or "classes_descriptiveName"
"name": "classes"
"name": "classes_textCalloutAlign"
```

---

## References

- [01-FUNDAMENTALS.md](01-FUNDAMENTALS.md) - Core concepts
- [03-BLOCK_JAVASCRIPT_PATTERN.md](03-BLOCK_JAVASCRIPT_PATTERN.md) - How to use config in JS
- [09-ANALYTICS_PATTERN.md](09-ANALYTICS_PATTERN.md) - Analytics field structure
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](16-BLOCK_DEVELOPMENT_TEMPLATE.md) - Step-by-step block creation
- `text_callout_block/text-callout.json` - Production reference
