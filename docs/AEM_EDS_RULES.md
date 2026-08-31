# AEM EDS - Complete Project Rules

> Consolidated from .cursor/rules/*.mdc for Claude, Antigravity, Cursor, and other agents.
> **Block creation quickstart:** [CREATE_AEM_BLOCK.md](./CREATE_AEM_BLOCK.md)

---

## Table of contents

- [01-aem-eds-overview](#01-aem-eds-overview)
- [02-block-javascript-pattern](#02-block-javascript-pattern)
- [03-block-json-pattern](#03-block-json-pattern)
- [04-block-css-pattern](#04-block-css-pattern)
- [05-html-example-pattern](#05-html-example-pattern)
- [06-no-analytics](#06-no-analytics)
- [07-naming-conventions](#07-naming-conventions)
- [08-block-creation-checklist](#08-block-creation-checklist)
- [09-utilities-and-helpers](#09-utilities-and-helpers)
- [10-json-advanced-patterns](#10-json-advanced-patterns)
- [11-responsive-design](#11-responsive-design)
- [12-project-architecture](#12-project-architecture)
- [13-foundational-blocks](#13-foundational-blocks)
- [14-learnings-and-pitfalls](#14-learnings-and-pitfalls)
- [15-sections-vs-blocks](#15-sections-vs-blocks)
- [16-section-creation-pattern](#16-section-creation-pattern)
- [17-code-formatting](#17-code-formatting)
- [18-typography-font-utilities](#18-typography-font-utilities)
- [19-bus-actionable-elements](#19-bus-actionable-elements)
- [20-cms-content-flexibility](#20-cms-content-flexibility)
- [21-pull-request](#21-pull-request)
- [22-repeatable-parent-child-blocks](#22-repeatable-parent-child-blocks)
- [23-page-template-metadata](#23-page-template-metadata)
- [24-block-filters-component-list](#24-block-filters-component-list)
- [25-json-field-descriptions](#25-json-field-descriptions)
- [26-example-html-sync](#26-example-html-sync)
- [27-classes-boolean-fields](#27-classes-boolean-fields)
- [28-new-component-discovery](#28-new-component-discovery)
- [29-helix-query-page-metadata](#29-helix-query-page-metadata)
- [30-block-css-design-tokens](#30-block-css-design-tokens)
- [30-local-json-fetch-fallback](#30-local-json-fetch-fallback)
- [31-api-folder-off-limits](#31-api-folder-off-limits)
- [32-block-root-tokens](#32-block-root-tokens)
- [33-programmatic-createBlock](#33-programmatic-createblock)
- [34-create-block-agent-guide](#34-create-block-agent-guide)

---

## 01-aem-eds-overview

> Source: .cursor/rules/01-aem-eds-overview.mdc

# AEM Edge Delivery Services (EDS) — Architecture Overview

## What AEM EDS Is
AEM EDS is Adobe's content-as-markup web delivery system. Content authors use the Universal Editor (visual WYSIWYG) to create pages. AEM renders them as simple, flat HTML (`div>div` structures). JavaScript "blocks" then decorate this markup client-side.

## The xwalk Approach
This project uses the crosswalk (xwalk) variant:
- Content authored in AEM Cloud
- Universal Editor provides the authoring UI
- JSON config files define the authoring experience
- `data-aue-*` attributes enable live editing

## Three-Phase Loading
```
loadEager()   → Above-fold content (hero, first section)
loadLazy()    → Below-fold content (as sections scroll into view)
loadDelayed() → Interactive features (3s after page load)
```

## How AEM Generates Block HTML
For each field in a block's JSON model, AEM generates one `<div><div>content</div></div>` row.

**EXCEPT:**
- `"component": "tab"` — organises editor UI only, creates NO row
- `"classes"` / `"classes_*"` fields — applied as CSS classes on the outer block div, create NO row

So if a model has: `tab`, `id`, `title`, `text`, `ctaLink`, `classes` — only `id`, `title`, `text`, `ctaLink` produce rows (indices 0–3).

## Flat Content Structure
```
Page
├── Section 1
│   ├── Block 1
│   ├── Block 2
│   └── Block 3
├── Section 2
│   └── Block 1
└── Section 3
    └── Block 1
```
- Sections CANNOT be nested inside other sections
- Blocks CAN contain blocks (but rendering differs — needs special handling)

## Content Publishing Flow
1. **Authoring** — Content created in Universal Editor → stored in AEM repository
2. **Publishing** — Content published to Git repository → serialized to markdown/JSON
3. **Delivery** — Content served via CDN → JavaScript decorates client-side

## Block Decoration Process
1. Block HTML received from server
2. `extractConfig()` — extract data from DOM
3. `buildBlock()` — rebuild semantic DOM
4. `appendEvents()` — add interactivity
5. Block ready for user interaction

## Instrumentation (Authoring Only)
AEM adds `data-aue-*` attributes for Universal Editor editing. When replacing the main block element, use `moveInstrumentation(oldEl, newEl)` to preserve authoring capability.

## Key References
- Full fundamentals: `.cursor/knowledge/documentation/01-FUNDAMENTALS.md`
- Holistic vision: `.cursor/knowledge/documentation/00-HOLISTIC_VISION.md`
- Boilerplate source: `.cursor/knowledge/boilerplate/scripts.js`

---

## 02-block-javascript-pattern

> Source: .cursor/rules/02-block-javascript-pattern.mdc

# Block JavaScript Pattern

## Mandatory Flow
Every block's `decorate()` function MUST follow this exact sequence (use **`async`** so nested handling can `await`):

1. **`await checkAndHandleNestedBlocks(block)`** when the block can host nested UE blocks (`filters[].components` non-empty), or when shared practice is to stay consistent — otherwise omit only if the block is a trivial pass-through with no nested contract. This helper handles Universal Editor **marker rows** inside direct children (see **`09-utilities-and-helpers.mdc`**). It does **not** replace the nested-loading step below when composed nested roots still need **`loadBlock`**.
2. **`await ensureNestedChildBlocksReady(block)`** (or equivalent) **only when required** — see **Nested child blocks — decorate pipeline** below. Omit when the block never hosts nested blocks that must run their own `decorate()`.
3. **`const config = extractConfig(block);`** — **always** run this **before** removing or replacing authored rows, so row indices match the model. `extractConfig` returns a **plain object** with every field the block reads from rows (and any defaults).
4. **`buildBlock(block, config)`** — build the DOM shell, preserve nested blocks with **`replaceBlockRowsPreservingNestedBlocks`** / **`removeNonBlockChildRows`** / **`prependBlockBuiltNodes`** as required, and **always assign `config.mainEl`** to the primary root you own (the interactive surface, or the single wrapper that contains the block’s authored/runtime UI). **`appendEvents`** relies on `config.mainEl` (or other keys you document on `config`).
5. **`appendEvents(config)`** — wire listeners; no-op is allowed if there is nothing to bind, but **`config.mainEl` must still be set** in step 4.

If the block **`await`s network or other async work** after the shell exists, keep the same steps: run **nested helpers** (`checkAndHandleNestedBlocks`, then **`ensureNestedChildBlocksReady`** when required) **before** **`extractConfig`**; then **`extractConfig`** (rows still present), **`buildBlock`** (initial shell + **`config.mainEl`**), optional **`await` loader**, then **`appendEvents`**.

```javascript
import {
  checkAndHandleNestedBlocks,
  replaceBlockRowsPreservingNestedBlocks,
} from '../../scripts/utilities/block-helpers.js';

export default async function decorate(block) {
  await checkAndHandleNestedBlocks(block);
  // await ensureNestedChildBlocksReady(block); // only when parent hosts nested blocks — see below
  const config = extractConfig(block);
  buildBlock(block, config);
  await loadAsyncContent(config); // optional — only when the block must await before final DOM
  appendEvents(config);
}
```

## Nested child blocks — decorate pipeline

**Problem:** In **`scripts/aem.js`**, **`decorateBlocks(main)`** only runs on **`main.querySelectorAll('.section > .section-inner > div > div')`** — i.e. **one wrapper depth under `.section-inner`**. A **nested** block root (child block **inside** a parent block’s table) is **deeper** in the DOM, so it is **not** in that set. The initial section **`loadBlock`** pass therefore **does not** run the child’s JS/CSS unless something else wires it.

**`checkAndHandleNestedBlocks(block)`** (from **`scripts/utilities/block-helpers.js`**) walks **direct children** of `block` and, when a child’s **`innerHTML`** contains the substring **`eds-block-`** (UE marker-cell flow), merges classes, calls **`decorateBlock(child)`**, and **`await loadBlock(child)`**. Nested roots that are **already composed** (e.g. local **`*-example.html`** or markup where the **`eds-block-*`** marker exists only on the **root `class`**, not inside serialized **inner** HTML) may **not** match that innerHTML check — those children still need **`decorateBlock` + `loadBlock`**.

**Required pattern for parents that host nested blocks the author/runtime must execute:**

1. **`await checkAndHandleNestedBlocks(block)`** first (UE marker rows when present).
2. **`await ensureNestedChildBlocksReady(block)`** — your block-local helper (name as you prefer) that finds each nested child root (e.g. by **`eds-block-<child-name>`** or the child’s first logical class), and for each root:
   - if **`!root.classList.contains('block')`**, call **`decorateBlock(root)`** (from **`scripts/aem.js`**);
   - if **`root.dataset.blockStatus`** is not **`loading`** / **`loaded`**, **`await loadBlock(root)`**.
3. Then **`extractConfig` → `buildBlock` → appendEvents`** so row reads and DOM moves happen **after** nested children are initialized.

**Reference implementation:** **`blocks/magic/magic.js`** — **`ensureNestedBlocksReady`** (imports **`decorateBlock`**, **`loadBlock`** from **`scripts/aem.js`**), invoked after **`checkAndHandleNestedBlocks`** when appropriate. Magic also uses **`nestedRowsLookComposed`** to skip the marker-row pass when nested blocks are already full composed nodes.

Parent **`*-example.html`** files that nest real child blocks **must** ship parent JS that follows this pattern (or nested rows will render as raw table markup in local demos). See **`22-repeatable-parent-child-blocks.mdc`** and **`05-html-example-pattern.mdc`**.

## 1. extractConfig(block)
Extract data from AEM-generated HTML using position-based row access.

```javascript
import {
  getTextFromRow,
  getHtmlFromRow,
  getHtmlFromRow,
  getTextFromBlockRow,
  getLinkFromRow,
  getImageFromRow,
  getBooleanFromRow,
} from '../../scripts/utilities/block-helpers.js';

/**
 * Row layout — count **DOM rows** only: `tab`, `classes`, and `classes_*` do **not** create rows.
 * The `classes` field still exists in the model (merged onto the block root as CSS classes).
 *
 * 0: id
 * 1: title (richtext)
 * 2: text (richtext)
 * 3: ctaLink (aem-content)
 *
 * **Blocks that may be nested** under another block: **always** use `getHtmlFromRow` / `getTextFromBlockRow` for text and richtext rows (see **`22-repeatable-parent-child-blocks.mdc`** — strict rule). `getHtmlFromRow` / `getTextFromRow` are for blocks that are **never** nested, or legacy only.
 * **Appearance** variants: read from `block.classList` (`classes_*` applied by the runtime).
 */
function extractConfig(block) {
  if (!block) return {};
  const rows = [...block.children];
  return {
    id: getTextFromBlockRow(rows[0]),
    title: getHtmlFromRow(rows[1]),
    text: getHtmlFromRow(rows[2]),
    ctaLink: getLinkFromRow(rows[3]),
  };
}
```

### Critical Rules for extractConfig:
- Use `[...block.children]` — NEVER `querySelectorAll(':scope > div')`
- When iterating rows for **data** (not only positional `rows[i]`), **exclude nested block roots** with **`isNestedBlockRowElement(row)`** from `block-helpers.js` so sibling nested blocks do not shift your map
- Row index = field order in JSON model (excluding `tab`, `classes`, and `classes_*` — none of those create rows)
- **Field collapse** (e.g. **`Alt`**): a property like **`iconAlt`** next to **`icon`** does **not** create another row — read **`alt`** from **`<img>`** in that reference row. See **`03-block-json-pattern.mdc`** (*Field collapse*).
- Document the row layout in a JSDoc comment above extractConfig
- Always null-check with `if (!block) return {};`
- Use `block-helpers.js` functions, not manual DOM queries; prefer **`getHtmlFromRow` / `getTextFromBlockRow`** whenever the block appears in **`filters[].components`** of a parent (nested-capable)
- **Typography:** Universal Editor output does not include **`u-font-*`** on inner cells. Add **`classList.add('u-font-body', …)`** (etc.) only on **DOM nodes you create** in `buildBlock()` — see **`18-typography-font-utilities.mdc`**

### Filters listing nested block types

If **`filters[].components`** in this block’s `_*.json` names other blocks so authors can nest them, **do not** add parent-specific row logic **for that reason alone** — implement this block’s own **`extractConfig` / `buildBlock` / `appendEvents`** for its authored rows. **Exception:** when nested children **must** run their own **`decorate()`** pipeline (not only placement in JSON), the parent **must** ensure **`decorateBlock` + `loadBlock`** on each nested root **before** rebuilding the parent shell — see **Nested child blocks — decorate pipeline** above and **`22-repeatable-parent-child-blocks.mdc`**.

## 2. buildBlock(block, config)
Remove **only** authored rows that are **not** nested blocks, then **prepend** semantic HTML (built shell stays **first**; nested child blocks remain as trailing siblings unless a design needs otherwise).

```javascript
import {
  replaceBlockRowsPreservingNestedBlocks,
  removeNonBlockChildRows,
  prependBlockBuiltNodes,
} from '../../scripts/utilities/block-helpers.js';

function buildBlock(block, config) {
  const inner = document.createElement('div');
  inner.classList.add('block-name-inner');

  if (config.title) {
    const title = document.createElement('div');
    title.classList.add('block-name-title');
    title.innerHTML = config.title;
    inner.appendChild(title);
  }

  // Removes non-`.block` / non-`eds-block-*` / non-`.block-child-wrapper` rows; prepends `inner`
  replaceBlockRowsPreservingNestedBlocks(block, inner);

  if (config.id) block.id = config.id;

  config.mainEl = inner;
}
```

**Helpers (see `block-helpers.js`):** `isNestedBlockRowElement`, `removeNonBlockChildRows`, `prependBlockBuiltNodes`, `replaceBlockRowsPreservingNestedBlocks`.

### Critical Rules for buildBlock:
- Do **not** use `block.textContent = ''` or `block.replaceChildren(...)` on the whole block — that drops nested blocks. Use **`removeNonBlockChildRows`** (and **`prependBlockBuiltNodes`** when you have multiple top-level nodes, e.g. track + footer)
- Use `classList.add()` instead of `className =` for setting classes
- Use hyphenated class names: `block-name-element`
- Reuse `<a>` elements from AEM when available (don't recreate)
- **Always set `config.mainEl`** — the most important interactive element, or the entire block container if there are multiple interactive elements
- Store interactive elements on config for appendEvents: `config.mainEl = ctaEl;`

```javascript
// ✅ CORRECT — classList
inner.classList.add('block-name-inner');
cta.classList.add('product-hero-cta-primary', 'brand-cta');

// ❌ WRONG — className (replaces all classes)
inner.className = 'block-name-inner';
cta.className = 'product-hero-cta-primary';
```

## 3. appendEvents(config)
Attach interaction handlers. NO analytics. **`config.mainEl` must exist** (set in `buildBlock`) even when this function adds no listeners — use a no-op body or minimal a11y setup (e.g. `tabIndex`) rather than skipping `appendEvents` entirely.

```javascript
function appendEvents(config) {
  if (!config.mainEl) return;
  config.mainEl.addEventListener('click', () => { /* ... */ });
}
```

### Actionable elements + `window.Bus`
For controls that should drive other blocks or page behavior: set **`id`** attributes **only from author fields**, **as authored** (no auto-generated or uniquified ids unless the block spec says otherwise), then **`window.Bus?.emit(EDS_EVENTS.<name>, detail)`** on activation. Import **`EDS_EVENTS`** from **`scripts/events.js`**. Full rules: **`19-bus-actionable-elements.mdc`**.

## `decorate` and `async`
Use **`export default async function decorate(block)`** and **`await checkAndHandleNestedBlocks(block)`** at the top when nested UE markers may exist; **`await`** any **explicit nested `loadBlock`** step next when the parent hosts composed nested blocks (**Nested child blocks — decorate pipeline**). The block loader **`await`s** the default export. Synchronous `decorate` is only acceptable if the block is a trivial pass-through with no nested-block contract.

## HTML example (`*-example.html`)
Every block folder must include **`<block-name>-example.html`**: assumed AEM row markup, standalone page with `head.html` assets, no direct link to the block’s own CSS/JS (`loadBlock` loads those). Keeps **`extractConfig` row indices** honest. See **`05-html-example-pattern.mdc`** and **`08-block-creation-checklist.mdc`**.

## New block — section palette
JSON/merge step, not JS: when adding a **new** block folder, also update **`block-configs/component-list.json`** so the block can be placed in a section (**`24-block-filters-component-list.mdc`**).

## Reference Implementations
- Canonical: `.cursor/knowledge/reference-blocks/text-callout/text-callout.js`
- Complex: `.cursor/knowledge/reference-blocks/product-hero/product-hero.js`
- Nested child loading: **`blocks/magic/magic.js`** (`ensureNestedBlocksReady`, `nestedRowsLookComposed`)
- Full docs: `.cursor/knowledge/documentation/03-BLOCK_JAVASCRIPT_PATTERN.md`

---

## 03-block-json-pattern

> Source: .cursor/rules/03-block-json-pattern.mdc

# Universal Editor JSON Configuration

## File Structure
Every block JSON file has three top-level arrays:
```json
{
  "definitions": [ ... ],
  "models": [ ... ],
  "filters": [ ... ]
}
```

## Definitions Section
```json
{
  "definitions": [{
    "title": "My Block",
    "id": "my-block",
    "plugins": {
      "xwalk": {
        "page": {
          "resourceType": "core/franklin/components/block/v1/block",
          "template": {
            "name": "My Block",
            "model": "my-block",
            "filter": "my-block",
            "title": "<p>Default Title</p>",
            "classes": "variant-default"
          }
        }
      }
    }
  }]
}
```

### Rules:
- `resourceType` is ALWAYS `"core/franklin/components/block/v1/block"`
- `template.model` must match the model `id`
- `template.filter` must match the filter `id`
- Only include fields with **meaningful defaults** in template — omit empty fields
- **`template` must include `id` (may be `""`) and `classes` (block marker)** — see **Mandatory opening fields** below

## Mandatory opening fields (every block)

These apply to **all** blocks under `blocks/<kebab-name>/` (not page/section models in `models/` unless this project extends them the same way).

1. **`id`** — The **first** field that creates a **block row** in Franklin output. Use `component: "text"`, `name: "id"`, author-facing label (e.g. **Block ID**). May follow a leading `tab` (tabs do not create rows).

2. **`classes` (block marker — model only, no row)** — Required **`component: "text"`** field with **`name` must be exactly `"classes"`**, **`hidden`: true**, optional **`readOnly`: true**, `valueType: "string"`, and **`value`** set to the exact literal:

   **`eds-block-<kebab-block-name>`**

   where **`<kebab-block-name>`** is the block folder name (`product-hero`, `text-callout`, `my-block`, …). Example: **`eds-block-product-hero`**. Franklin still adds the generic **`block`** class on the root in `decorateBlock` — this marker is **additional** identification, not a replacement for **`block`**.

   AEM merges this (and **`classes_*`** values) as **CSS classes on the block root**. It **does not** emit a table cell / `<div>` row in generated HTML. **Do not** use a separate field named `blockType` for this marker.

3. **`definitions` → `plugins` → `xwalk` → `page` → `template`** must set both keys, e.g. `"id": ""`, `"classes": "eds-block-my-block"`.

4. **Downstream:** **`blocks/<folder>/<folder>-example.html` is required** (standalone page per **`05-html-example-pattern.mdc`**). It must mirror **row-producing fields only** — **do not** add a fake row for `classes` (the marker appears on the block root’s `class` attribute only). Include **every `classes_*` / `classes` select option** (including `""`) on at least one block root in that file (**exhaustive coverage** — see **`05-html-example-pattern.mdc`**). **Several block instances:** use **Variant labels** (`h2` + `p` + block per **`main > div`**). **`extractConfig` / row maps:** **row 0 = `id`**, then each subsequent **content** field in model order (skip `tab`, `classes`, and `classes_*`).

```json
{
  "component": "text",
  "name": "id",
  "label": "Block ID",
  "valueType": "string",
  "value": ""
},
{
  "component": "text",
  "name": "classes",
  "label": "Block classes",
  "valueType": "string",
  "value": "eds-block-my-block",
  "hidden": true,
  "readOnly": true
}
```

5. **Appearance and light behavior variants** (colors, layout mode, selection mode, widths, density, etc.) **must not** use extra `text` / `select` rows that only drive styling. Model them with **`classes`** (primary variant) and **`classes_*`** fields (`classes_layout`, `classes_tone`, …). For **string** options use **`select`** with **full class name** values on the block root. **Read these in JS from `block.classList`**, not from table rows.

### `classes_*` **boolean** toggles (on → CSS class)

- Use **`component`: `boolean`**, **`valueType`: `boolean`**, and a **`name`** that starts with **`classes_`**.
- The **CSS class** applied when **`true`** is exactly the part **after** `classes_` (use **kebab-case** after the prefix; do **not** use camelCase there). Example: `classes_test-abcd` → class **`test-abcd`**; `classes_fade-show-cta` → **`fade-show-cta`**.
- When **`false`**, that class is not merged. These fields **do not** create Franklin table rows (same as other `classes_*` metadata).

### Other **`boolean`** fields (not `classes_*`)

- If **`name`** does **not** start with **`classes_`**, the boolean is normal content/behavior: it **creates a block row** with text **`true`** or **`false`** (read via `getBooleanFromRow` / row text where applicable).

## Models Section — Field Types

| Component | Creates Row? | Use For |
|-----------|-------------|--------|
| `text` | ✅ Yes | Plain text, IDs |
| `richtext` | ✅ Yes | Formatted content |
| `aem-content` | ✅ Yes | Links/URLs |
| `reference` | ✅ Yes | DAM images |
| `boolean` | ✅ Yes * | Toggle switches; see * below |
| `select` | ✅ Yes | Dropdowns |
| `tab` | ❌ No | Editor UI tabs |
| `classes` / `classes_*` | ❌ No | CSS class variants (applied to outer div) |

\* **`boolean`**: creates a row **unless** **`name`** starts with **`classes_`** — those merge a class on the block root and **do not** create a row. See **`27-classes-boolean-fields.mdc`**.

## Field `description` (authors)

Use **`description`** only to help **authors** in plain language. Do **not** put HTML, row counts, merge behavior, or other implementation detail there. Omit **`description`** when the **label** already makes the field obvious. Full rules: **`25-json-field-descriptions.mdc`**.

## Multi-field + `container` (not allowed in UE properties panel)

Per Adobe [field types — `multi`](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/universal-editor/field-types#fields): **container nesting is not permitted for multi-fields in the properties panel**.

Do **not** use:

```json
{ "component": "container", "name": "items", "multi": true, "fields": [ … ] }
```

For repeating authoring slots, use **flat numbered fields** + **tabs per slot**. Implement **`decorate`** with **one block row per field** (tabs / `classes*` do not create rows).

## Tab Organization
```json
"fields": [
  { "component": "tab", "label": "General", "name": "tabGeneral" },
  { "component": "text", "name": "id", "label": "Block ID", "valueType": "string", "value": "" },
  {
    "component": "text",
    "name": "classes",
    "label": "Block classes",
    "valueType": "string",
    "value": "eds-block-my-block",
    "hidden": true,
    "readOnly": true
  },
  { "component": "richtext", "name": "heading", "label": "Heading" },
  { "component": "tab", "label": "Appearance", "name": "tabAppearance" },
  { "component": "select", "name": "classes_variant", "label": "Variant", "valueType": "string", "options": [] }
]
```
Tabs use `"name": "tabGeneral"` (camelCase with `tab` prefix). After any leading tab(s), **`id` is the first row-producing field**; **`classes` follows in the model** but **does not produce a row**. Further appearance / light-behavior toggles use **`classes_*`** (also **no rows**).

## Field collapse (reserved name suffixes)
AEM merges fields using **suffixes** defined in [Field collapse](https://www.aem.live/developer/component-model-definitions#field-collapse). A property whose **`name` ends with** one of these **case-sensitive** tokens is treated as an **attribute** (or special role), **not** as standalone block cell text:

| Suffix | Role |
|--------|------|
| **`Title`** | HTML **`title`** attribute on the base property (e.g. `link` + `linkTitle`) |
| **`Type`** | **`type`** (e.g. `heading` + `headingType` → heading level) |
| **`MimeType`** | MIME type where applicable |
| **`Alt`** | **`alt`** on the image (e.g. `image` + `imageAlt`, or **`icon` + `iconAlt`** on the **CTA** block) |
| **`Text`** | Visible text for a **link/button** (e.g. `link` + `linkText`) |

**Collapsed `reference` + `Alt`:** The alt field **does not create a second block row**. Universal Editor outputs **one** row containing `<picture>` / `<img>` with **`alt`** on the image. In **`extractConfig`**, read alt via **`img.getAttribute('alt')`** (or equivalent) from that row — **do not** advance a row index for `*Alt` when documenting row maps.

**Project rule:** Do **not** use the **`Title`** suffix on a field `name` for normal headings, labels, or card lines. Use names like **`heading`**, **`card1Label`**, **`optionLabel`**, etc. Use **`Title`** only when the authoring intent is the **`title`** attribute. Same idea for **`Text`** (link label) and **`Type`** (semantic type), **`Alt`** (image alt).

## Conditional Field Visibility
Use `condition` with JSON Logic:
```json
{
  "component": "text",
  "name": "ctaText",
  "label": "CTA Text",
  "condition": { "!=": [{ "var": "ctaLink" }, ""] }
}
```

## Select Fields (Style Variants)
```json
{
  "component": "select",
  "name": "classes",
  "label": "Style",
  "valueType": "string",
  "options": [
    { "name": "Primary", "value": "cta-primary-filled" },
    { "name": "Secondary", "value": "cta-secondary-filled" }
  ]
}
```
Always include `"valueType": "string"` on select fields.

## Boolean Fields
Use `"component": "boolean"` (NOT `"checkbox"`):
```json
{
  "component": "boolean",
  "name": "showBadge",
  "label": "Show badge"
}
```

## Element grouping
See [Element grouping](https://www.aem.live/developer/component-model-definitions#element-grouping) in the AEM component model definitions.

**Purpose:** Unlike **field collapse** (one semantic element, multiple properties), **element grouping** puts **several semantic elements into a single table cell**—useful when authors should only create a fixed set of pieces (e.g. subtitle + heading + paragraph + two CTAs) that render together.

**Naming:** `{groupName}_{propertyName}` — an underscore separates the **group name** from each member. **Field collapse** suffixes (`Title`, `Type`, `MimeType`, `Alt`, `Text`) still apply **inside** the group (e.g. `teaserText_cta1` + `teaserText_cta1Text` + `teaserText_cta1Type`).

**Example (from docs):** Properties like `teaserText_subtitle`, `teaserText_title`, `teaserText_titleType`, `teaserText_description`, `teaserText_cta1`, `teaserText_cta1Text`, … produce **one** block row whose cell contains multiple elements, e.g.:

```html
<p>Adobe Experience Cloud</p>
<h2>Meet the Experts</h2>
<p>Join us in this ask me everything session...</p>
<p><a href="...">More Details</a></p>
<p><strong><a href="...">RSVP</a></strong></p>
```

If a field **`name` equals the group prefix** (e.g. `teaserText`) already exists, it is folded into that group so grouping can be added without migrating old content.

**Block options (`classes_*`):** Multiple mutually exclusive or extra options can be authored as `classes`, `classes_background`, `classes_fullwidth`, etc. Booleans add the segment after `classes_` as a block class when true (e.g. `classes_fullwidth: true` → `fullwidth` on the block root).

**This repo:** Most authored blocks use **one row per field** (camelCase names, no grouping). **Do not** introduce `group_prop` names unless you intentionally want one merged cell. Always **avoid reserved collapse suffixes** on `name` unless you mean that attribute (see **Field collapse** above).

## Filters Section
```json
{
  "filters": [{
    "id": "my-block",
    "components": ["text", "image", "button"]
  }]
}
```

### Section palette — **`block-configs/component-list.json`** (required for new blocks)

When you create a **new** top-level block that authors should be able to **insert inside a section**, you **must** append that block’s filter **`id`** (same string as `filters[].id` in `_<block-name>.json`, kebab-case folder name) to the **`components`** array in **`block-configs/component-list.json`**.

The **`models/_section.json`** section filter **merges** that file — do **not** duplicate block ids inside **`_section.json`** `filters`. Full workflow: **`24-block-filters-component-list.mdc`**.

### Parent block that contains a repeated child block
When the design uses a **parent + child** pair for repeatables (see **`22-repeatable-parent-child-blocks.mdc`**), the **parent** filter **must** include the **child block’s filter `id`** in `"components"` (e.g. `"components": ["my-list-item"]`). Otherwise authors may not be allowed to place the child inside the parent. The **child** typically uses `"components": []` unless you intentionally restrict placement.

### Allowing other blocks inside this block (filters only)

To let authors place block **B** inside block **A**, add **B**’s filter `id` to **A**’s `"components"`. That is an **authoring / placement** rule in JSON. **A**’s JavaScript stays a **normal** block for **A**’s own rows unless the design **requires** A to compose or coordinate B’s DOM (see **`22-repeatable-parent-child-blocks.mdc`** — default: no extra parent logic).

## Before you create or extend block JSON

Finish **discovery** first (**`28-new-component-discovery.mdc`**): confirm layout, fields, and **behavior** (display-only vs actions / Bus / navigation) with the user. Keep edits inside the **new block footprint** (**`08-block-creation-checklist.mdc`** — *Scope of file changes*); do **not** expand scope to unrelated files while authoring JSON.

## Reference
- New block/section requests — discovery, behavior questions, file scope, and suggesting **custom `blocks/`** first: **`28-new-component-discovery.mdc`**
- Images in `*-example.html` only when the model has **`reference`** fields — **full `https://www.gradholder.com/img/...` URLs** + `<picture>` row — **`05-html-example-pattern.mdc`** (richtext: no images by default; not site-relative `/img/` or `/media/`)
- **`README.md`** required — **LLM when-to-pick**, **plain-language rendering**, **per-variation** table pattern (every option) — **`08-block-creation-checklist.mdc`** Step 6 / `blocks/content/README.md`; **Lorem Ipsum** in `*-example.html` — **`05-html-example-pattern.mdc`**
- Canonical: `.cursor/knowledge/reference-blocks/text-callout/_text-callout.json`
- Advanced: `.cursor/knowledge/reference-blocks/product-hero/_product-hero.json`
- Full docs: `.cursor/knowledge/documentation/02-JSON_CONFIGURATION.md`
- Element grouping: `.cursor/knowledge/analysis/aem_element_grouping_analysis.md`
- Tabs/merge: `.cursor/knowledge/analysis/AEM_EDS_Tabs_and_JSONMerge_Complete_Guide.md`
- Repeatable parent/child: `.cursor/rules/22-repeatable-parent-child-blocks.mdc`
- Section palette / `component-list.json`: `.cursor/rules/24-block-filters-component-list.mdc`
- Author-facing field `description`: `.cursor/rules/25-json-field-descriptions.mdc`

---

## 04-block-css-pattern

> Source: .cursor/rules/04-block-css-pattern.mdc

# CSS Styling Conventions

## Core Rules
- ✅ Basic `.css` files only
- ✅ **Colors** in **`blocks/**`** — **`var(--color-*)`** from **`styles/colors.css`** (loaded via **`styles/styles.css`**). **No** raw **`#…` / `rgb()` / `hsl()`** for ink, borders, fills, shadows, or outlines in block CSS. Workflow: match token → else **closest** existing → else **add** `--color-…` in **`styles/colors.css`** once, then use **`var(...)`**. See **`30-block-css-design-tokens.mdc`**.
- ✅ **Spacing** — prefer **`var(--space-*)`** from **`styles/styles.css`** when the step exists on the scale; other lengths may stay literal when there is no token
- ✅ **Typography in block CSS** — prefer **`var(--body-font-size-*)`**, **`var(--heading-font-size-*)`**, **`var(--font-family-body)`** / **`var(--heading-font-family)`**, **`var(--font-weight-*)`** from **`styles/styles.css`** when not relying solely on **`u-font-*`** from JS (**`18-typography-font-utilities.mdc`**)
- ✅ Standard `@media` queries for responsive
- ✅ Hyphenated class names: `.block-name-element`
- ✅ **Typography:** add **`u-font-title`**, **`u-font-body`**, **`u-font-body-bold`**, **`u-font-ui`**, **`u-font-ui-semibold`** in **block JS** on nodes you create — AEM markup will not include these; see **`18-typography-font-utilities.mdc`**. Avoid duplicating `font-family` stacks in block CSS for those nodes.
- ✅ **CMS copy** — layouts must tolerate more/less content than any screenshot; see **`20-cms-content-flexibility.mdc`**.
- ❌ NO SCSS (`.scss`, `$variables`, `@include`, nesting)
- ❌ NO arbitrary CSS variables **inside `blocks/**`** — do **not** invent `var(--my-block-*)` for colors, type, or spacing. Use **`var(--space-*)`**, **`var(--color-*)`**, **`var(--body-font-size-*)`**, **`var(--heading-font-size-*)`**, **`var(--font-family-*)`** from **`styles/styles.css`** / **`styles/colors.css`** only; see **`32-block-root-tokens.mdc`**.
- ❌ NO block-local “design token” layers (SCSS variables, token-only files for one block)
- ❌ NO BEM (`__`, `--`)
- ❌ NO `!important`

## Parent vs child block (repeatable patterns)

When a design uses a **parent + child** pair for repeating UI (see **`22-repeatable-parent-child-blocks.mdc`**):

- **Parent CSS** (`parent-name.css`) — Layout and look of the **whole** module: outer spacing, list/grid container, typography for shared headings/footnotes, variants that wrap **all** items.
- **Child CSS** (`child-name.css`) — **Single repeated item**: card surface, row rhythm, per-item states, compact/wide variants for **one** unit.

Avoid putting **per-item** visual rules only on the parent if they belong on each repeated unit (and vice versa for shell chrome).

## Block CSS Structure Template
```css
/* Block Name Styles — Mobile-first */

/* CONTAINER */
.block-name {
  display: flex;
  padding: var(--space-xl) var(--space-m);
  background-color: var(--color-base-background);
  color: var(--color-base-text);
}

@media (min-width: 768px) {
  .block-name { padding: var(--space-xl) var(--space-l); }
}

@media (min-width: 1024px) {
  .block-name { padding: var(--space-xl); }
}

/* INNER ELEMENTS */
.block-name-inner { max-width: 640px; width: 100%; }
/* title node also gets .u-font-title in JS */
.block-name-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-neutral-800);
  margin: 0 0 var(--space-xs) 0;
}
/* body node also gets .u-font-body in JS */
.block-name-text {
  font-size: 1rem;
  line-height: 1.75;
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-m) 0;
}

/* CTA ELEMENTS */
.block-name .brand-cta {
  display: inline-block;
  padding: var(--space-s) var(--space-xl);
  background-color: var(--color-accent-cta);
  color: var(--color-text-on-emphasis);
  border-radius: 4px;
  text-decoration: none;
  cursor: pointer;
}
.block-name .brand-cta:hover {
  background-color: var(--color-accent-link-hover);
}

/* STYLE VARIANTS (from JSON classes field) */
.block-name.variant-name .brand-cta {
  background-color: var(--color-ui-grey-500);
}

/* ALIGNMENT */
.block-name.block-name-align-center { justify-content: center; text-align: center; }

/* ACCESSIBILITY */
@media (prefers-reduced-motion: reduce) {
  .block-name .brand-cta { transition: none; }
}
@media print {
  .block-name { padding: var(--space-m); page-break-inside: avoid; }
}
```

## Responsive Breakpoints
| Name | Width | Usage |
|------|-------|-------|
| Mobile | Default | Base styles |
| Tablet | `768px` | `@media (min-width: 768px)` |
| Desktop | `1024px` | `@media (min-width: 1024px)` |
| Large | `1440px` | `@media (min-width: 1440px)` (when needed) |

## Class Naming
| Element | Class |
|---------|-------|
| Container | `.text-callout` |
| Inner wrapper | `.text-callout-inner` |
| Title | `.text-callout-title` |
| CTA | `.brand-cta` (shared across blocks) |

## Reference
- Shared tokens in blocks: **`32-block-root-tokens.mdc`**, **`styles/styles.css`**, **`styles/colors.css`**
- Typography utilities: `18-typography-font-utilities.mdc`, `styles/font-utilities.css`
- Canonical: `.cursor/knowledge/reference-blocks/text-callout/text-callout.css`
- Complex: `.cursor/knowledge/reference-blocks/product-hero/product-hero.css`
- Full docs: `.cursor/knowledge/documentation/05-CSS_STYLING_APPROACH.md`
- Responsive: `.cursor/knowledge/documentation/13-RESPONSIVE_DESIGN_STRATEGY.md`

---

## 05-html-example-pattern

> Source: .cursor/rules/05-html-example-pattern.mdc

# HTML Example Requirements

## Purpose
**Deliverable rule:** Every new or updated block under `blocks/<block-folder>/` MUST ship **`<block-folder>-example.html`** next to **`_<block-folder>.json`**, **`<block-folder>.js`**, and **`<block-folder>.css`**. Omitting it breaks the authoring → DOM contract for reviewers and for local smoke tests. See **`08-block-creation-checklist.mdc`** (Step 5) and **`16-BLOCK_DEVELOPMENT_TEMPLATE.md`**.

The file MUST:
1. Show the exact HTML AEM will generate (the contract between Universal Editor output and `decorate()`).
2. Be a **complete, standalone HTML document** that mirrors **`head.html`**: global `styles.css` plus `aem.js` / `scripts.js`, so the page behaves like production.
3. **Not** link or import the block’s own `block-name.css` / `block-name.js` — **`aem.js` `loadBlock()`** loads them after `decorateBlock()` runs, same as any other page.

## Standalone document shell

### `<head>` — align with `head.html`
Use the same global assets as the site (root-absolute URLs). Optional: add `<meta charset="utf-8" />` first for clarity.

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta
    http-equiv="Content-Security-Policy"
    content="script-src 'nonce-aem' 'strict-dynamic' 'unsafe-inline' http: https:; base-uri 'self'; object-src 'none';"
    move-to-http-header="true"
  >
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>my-block — local demo</title>
  <script nonce="aem" src="/scripts/aem.js" type="module"></script>
  <script nonce="aem" src="/scripts/scripts.js" type="module"></script>
  <link rel="stylesheet" href="/styles/styles.css"/>
</head>
```

- Match **`head.html`**: no font CDNs — `scripts.js` loads **`styles/fonts.css`** (local `@font-face`). See **`18-typography-font-utilities.mdc`**.
- Do **not** add `<link href="./my-block.css">` or a module script that imports `./my-block.js`.
- `scripts.js` runs `loadPage()` on import; it calls `decorateMain()` → `decorateSections` / `decorateBlocks`, then `loadSections()` which calls `loadBlock()` for each block (loads CSS + JS from `/blocks/...`).

### `<body>` — Franklin-shaped DOM
Match what `decorateSections` and `decorateBlocks` in `aem.js` expect:

- `<header></header>` and `<footer></footer>` — required; `scripts.js` `loadLazy()` calls `loadHeader` / `loadFooter` which append to these elements.
- `<main>` contains one or more **section wrappers** (`decorateSections` turns each direct `main > div` into `.section`).
- Inside each section wrapper, the **block root** `div` whose **first class** is the block name (e.g. `class="title-text"`) must appear **exactly** as AEM would emit: row `div`s only inside that root — **do not** wrap rows inside `h2`/`p` or merge legend nodes into the block.

Minimal shape (single sample):

```html
<body>
  <header></header>
  <main>
    <div>
      <div class="my-block optional-variant-class">
        <!-- AEM rows: row 0 = id, then content fields in model order (`classes` / `classes_*` = no rows; classes on root only) -->
      </div>
    </div>
  </main>
  <footer></footer>
</body>
```

After `decorateSections`, each `main > div` becomes **`main > .section`** with **exactly one** **`.section-inner`**; all section content (variant labels, default-content groups, and every block root) is wrapped into **child wrappers** of that single `.section-inner` (see `decorateSections` in **`scripts/aem.js`**). **`decorateBlocks`** still targets **`div.section > div.section-inner > div > div`** (wrapper → block root). A **nested** child block lives **inside** a parent block’s table; its **AEM-generated HTML path differs** from a section-level sibling — mirror the real nested structure in the parent’s **and** child’s examples. See **`22-repeatable-parent-child-blocks.mdc`**.

**Nested blocks in parent demos:** Because nested roots are **outside** the `decorateBlocks` selector, the **parent** block’s **`decorate()`** must run the same **`decorateBlock` + `loadBlock`** pattern as **`blocks/magic/magic.js`** (`ensureNestedBlocksReady`) **before** the parent rebuilds its shell — otherwise **`*-example.html`** will show **undecorated** nested markup. **`checkAndHandleNestedBlocks`** alone may not fire on composed local HTML. See **`02-block-javascript-pattern.mdc`** (*Nested child blocks — decorate pipeline*).

### Variant labels (multiple samples in one page)
When the file documents **more than one** block instance (e.g. exhaustive `classes_*` options), **before each** block add a **short title** and **one line of explanation** (what authoring field / class the sample shows). **Visually separate** samples with **light, page-local CSS** — a `<style>` block in the example’s `<head>` is allowed (minimal typography, border, spacing only; **do not** link an extra project stylesheet for this).

**DOM contract:** `decorateSections` groups direct children of each `main > div`. Putting `h2` + `p` + `div.block-name` as **siblings in one section** can merge the heading into the wrong wrapper and break `decorateBlocks`. **Recommended pattern:** use **one `main > div` per sample**, in this order:

1. Optional wrapper class for styling, e.g. `class="block-example-variant-section"`.
2. **`h2`** — variant title.
3. **`p`** — what this sample demonstrates (field names, class values).
4. **`div` block root** — unchanged AEM row structure only.

Repeat: `main > div` (sample A) → `main > div` (sample B) → … Separators are **CSS** between sections (e.g. `border-top` on `* + *`), not extra nodes that confuse section grouping.

Reference: `blocks/content/content-example.html`.

### Section appearance utilities (optional on sample wrappers)
When a sample needs a **page backdrop** (light band, dark grey, black, width, inner padding, etc.), add the **same `section-*` class strings** that authoring applies from **`models/_section.json`** (`style_bg`, `style_size`, `style_contentSize`, `style_contentAlignment`, `style_contentPaddingBlock`, `style_contentPaddingInline`). They are implemented in **`styles/section-utilities.css`** (and baseline **`styles/styles.css`** for `.section-inner` when no padding utilities are set) and apply to **`main > .section`** after **`decorateSections`** adds the `section` class to each direct **`main > div`**.

| Authoring / class | Role |
|-------------------|------|
| `section-default-bg` | Default background (often no extra class needed) |
| `section-light-bg` | Light grey band |
| `section-dark-bg` | Dark grey band + light text |
| `section-black-bg` | Black band + light text |
| `section-small-size` / `section-large-size` | Section max-width |
| `section-*-content-size` / `section-*-content-alignment` | `.section-inner` width, alignment |
| `section-*-content-padding-block` / `section-*-content-padding-inline` | `.section-inner` vertical / horizontal padding (separate authoring) |

Put them on the **outer** sample wrapper next to `block-example-variant-section`, e.g. `class="block-example-variant-section section-black-bg section-large-content-padding-block section-large-content-padding-inline"`. **Page-local** `h2` / `p` styles in the example’s `<style>` often set a dark grey `color` on labels; for **`section-dark-bg`** / **`section-black-bg`** the section sets light **inherited** text — override label `p`/`h2` with **`color: inherit`** (or equivalent) so copy stays readable. Examples: `blocks/cta/cta-example.html`, `blocks/spacer/spacer-example.html`, `blocks/content/content-example.html` (`section-light-bg`).

### Opening the file
- Serve the **repository root** over HTTP (`npx serve .` or equivalent) so `/scripts/`, `/styles/`, and `/blocks/` resolve.
- Open `http://localhost:<port>/blocks/<block-folder>/<block-name>-example.html`.
- `window.hlx.codeBasePath` is derived from the URL of `/scripts/scripts.js`; keeping that path correct requires serving from the project root, not only the block folder.

## AEM markup block (inside `<main>`)
**Top-level** blocks: flat rows of **`div` (row) → `div` (cell)** inside `<div class="block-name">`.

**Nested** child blocks (inside a parent): typically **one `div` per row** with the cell content inside that single wrapper (see **`22-repeatable-parent-child-blocks.mdc`**). Examples must match the real shape.

**Parent examples that compose nested blocks** (e.g. **Magic** with nested Content / CTA / Asset / Spacer): the **child block markup inside the parent** MUST use this **nested** row shape end-to-end. Do **not** paste section-level `div > div` row markup for those children — reviewers and `decorate()` expect the flatter nested DOM.

```html
<div class="block-name optional-variant-class">
  <div><div>id-value</div></div>
  <div><div><p>Title content</p></div></div>
  ...
</div>
```

## Rules
- Each JSON model field (except `tab` and `classes` / `classes_*`) creates one row; **top-level** rows are usually `<div><div>…</div></div>`, **nested** rows are often `<div>…</div>`
- Row order matches field order in JSON model
- **Body copy in AEM rows (required):** all **richtext** cell HTML and other **prose** placeholders inside the block’s row markup must use **Lorem Ipsum** (Latin filler), not marketing or domain-specific copy. You may still use **structural** markup (`<strong>`, `<i>`, lists, links) to exercise the RTE. **Variant labels** outside the block (`h2` / `p` in **Variant labels** sections) stay short technical English describing the sample. **`text`** ID rows keep realistic **ids** (e.g. `demo-size-regular`), not Lorem.
- Rich text wraps content in `<p>` tags; **do not** embed `<img>` in richtext cells **by default** (authors assumed not to add images); only when a block explicitly documents that edge case
- Links use `<a>` with `href`
- Boolean fields render as plain text `true`/`false`
- NO analytics attributes
- Class variants appear on the outer block div: `<div class="block-name variant-class">` (block name + authored **`classes` / `classes_*`** only — **not** `u-font-*`; those are added in JS)

### Exhaustive `classes_*` / style coverage (required)
When you add or change **`classes_*`** values or other **major** demo-affecting behavior, update this block’s **`*-example.html`** in the same change (see **`26-example-html-sync.mdc`**).

For **every** `select` whose `name` is `classes` or `classes_*` (and any similar **mutually exclusive** style control on the block root), the example file MUST include **at least one full block instance** (correct row structure) so that:

1. **Each non-empty `value`** from that field’s `options` array appears on **some** block root as that class (or merged segment).
2. **Each empty-string `value` (`""`)** option, if present, appears on **at least one** block root **without** that field’s class segment (other `classes_*` may still apply).

When demoing one axis, hold other axes to **neutral defaults** from the model (e.g. default size and color while sweeping colors). **Full Cartesian products** are **not** required when the product is large; add **one or two representative combined blocks** and document the option matrix in an HTML comment. Small blocks (few options) may include every combination if practical.

Repeat for **nested** child blocks in their own `*-example.html` when their `classes_*` differ from the parent.

### Images in `*-example.html` — only when the model has `reference` fields
**Default:** Do **not** put `<img>`, `<picture>`, or image URLs in `*-example.html` unless the block’s JSON model includes at least one **`"component": "reference"`** field (or another field that explicitly emits image rows per project docs).

- **`reference` rows:** For **each** `reference` field, include the corresponding **`<picture>`** row(s) in the example. Use **full `https://www.gradholder.com/img/...` URLs only** — no DAM, and **no site-root placeholder paths** such as `src="/img/..."`, `src="/media/..."`, or other repo-relative image URLs (unless the user explicitly asks otherwise). **Note:** Gradholder’s URL path contains `/img/` **after the hostname**; that is correct and is **not** the same as a local `/img` folder on your site. See **URL pattern** and markup below.
- **Gradholder colors:** The URL encodes a **two-stop gradient** (`hexFrom` / `hexTo`). Use **two clearly different** hex colors so placeholders read as **multi-color**, not one neutral wash. **Do not** reuse the same pair on every row (e.g. avoid defaulting every example to `…/808080/ffffff` or always using `ffffff` as the second stop). **Vary** hues across samples and blocks (e.g. blue→amber, teal→coral, purple→gold) so demos are easy to distinguish.
- **`richtext` fields:** Assume authors **do not** embed images. Example HTML should use **text-only** richtext (`<p>`, `<strong>`, `<a>`, lists, etc.). **Exception:** document in the block README + a short HTML comment in the example if a specific block **must** demo inline images; still use gradholder URLs if you add them.

**Gradholder URL pattern** (when `reference` rows are present):

```txt
https://www.gradholder.com/img/{width}x{height}/{hexFrom}/{hexTo}?type={gradient}
```

**Common mistake:** Authors or reviewers see `/img/` in the URL bar and assume it is a **local** `/img` asset. It must stay the **full gradholder.com** URL above — never shorten it to a root-relative `/img/...` path on the project host.

| Part | Meaning |
|------|--------|
| `{width}x{height}` | Pixel size, e.g. `800x800`, `1440x305` |
| `{hexFrom}` `{hexTo}` | Colours **without** `#` — pick **distinct** stops (e.g. `2563eb`, `fbbf24`), not the same tint twice |
| `type` | `horizontal` \| `vertical` \| `radial` |

Examples: `…/img/800x800/2563eb/fbbf24?type=horizontal`, `…/400x400/0d9488/f472b6?type=vertical`, `…/200x200/581c87/e9d5ff?type=radial`

Use matching `width` / `height` on `<img>`, `loading="lazy"`, and meaningful `alt`.

### Videos in `*-example.html` — repo `videos/` folder (required when demoing video)
When an example page includes **video** row markup (e.g. **`aem-content`** `href` to a media file, or any `<video>` / source demo that should play locally), use files from the repository **`videos/`** directory at the project root — **not** third-party hosts (MDN sample URLs, random CDNs, etc.) unless the user explicitly asks otherwise.

- **URL shape:** root-absolute paths **`/videos/<filename>`** (e.g. `/videos/video-1.webm`) so they resolve when the **repository root** is served (`npx serve .`), same as `/scripts/` and `/styles/`.
- **Add assets:** place or reuse committed clips under **`videos/`**; keep filenames stable so examples and docs do not rot. Prefer short, rights-cleared samples appropriate for the repo.
- **Block-specific:** `blocks/asset/asset-example.html` (and any other block that demos video) must point row-5-style links at **`/videos/...`** only.

### `reference` field — AEM row shape (include in example only if model has `reference`)
A **`reference`** field produces one row (`<div><div>…</div></div>`) containing **`<picture>`** + **`<img>`** (typical UE output):

```html
<div>
  <div>
    <picture>
      <source
        type="image/webp"
        srcset="https://www.gradholder.com/img/1440x305/2563eb/fbbf24?type=horizontal"
        media="(min-width: 600px)"
      />
      <img
        loading="lazy"
        alt="Sample gradient placeholder"
        src="https://www.gradholder.com/img/1440x305/2563eb/fbbf24?type=horizontal"
        width="1440"
        height="305"
      />
    </picture>
  </div>
</div>
```

Use the **same** gradholder URL in `srcset` and `src` for simple local examples unless mirroring production variants.

## Reference
- **`26-example-html-sync.mdc`** — keep **`*-example.html`** aligned when variants or big features land (includes **Videos in `*-example.html`** above when adding video demos)
- `styles/section-utilities.css` + `models/_section.json` — **`section-*`** classes for sample wrappers (see **Section appearance utilities** above)
- `head.html` — canonical head fragment
- `.cursor/knowledge/reference-blocks/text-callout/text-callout-example.html`
- `.cursor/knowledge/reference-blocks/` — other `*-example.html` fragments (new demos: standalone page + `head.html` pattern)

---

## 06-no-analytics

> Source: .cursor/rules/06-no-analytics.mdc

# No Analytics — Strict Enforcement

This project does NOT use analytics. Every block must be analytics-free.

## Prohibited Patterns
```javascript
// ❌ NEVER do any of these:
import { applyTracking } from '../../scripts/dataLayer.js';
applyTracking(block);
block.setAttribute('data-trackinview', 'true');
block.setAttribute('data-trackclick', 'true');
el.setAttribute('data-trackinviewmeta', meta);
el.setAttribute('data-trackclickmeta', meta);
```

## Prohibited JSON Fields
```json
// ❌ NEVER include in JSON models:
{ "component": "tab", "label": "Analytics" }
{ "component": "boolean", "name": "trackInview" }
{ "component": "boolean", "name": "trackClick" }
{ "name": "trackInview_meta" }
{ "name": "trackClick_meta" }
```

## Correct JS Flow (No Analytics Step)
```javascript
export default function decorate(block) {
  const config = extractConfig(block);
  buildBlock(block, config);
  appendEvents(config);
  // NO applyTracking(block) — analytics removed
}
```

## If Asked About Analytics
The analytics system is documented in `.cursor/knowledge/documentation/09-ANALYTICS_PATTERN.md` for reference only. It uses `dataLayer.js` with data-attribute-driven tracking. But this project has explicitly removed it.

---

## 07-naming-conventions

> Source: .cursor/rules/07-naming-conventions.mdc

# File & Class Naming Standards

## Block File Structure
```
blocks/block-name/
├── block-name.js           # JavaScript (matches folder)
├── block-name.css          # CSS (matches folder)
├── _block-name.json        # JSON (underscore prefix!)
├── block-name-example.html # Full HTML demo: head.html + main/header/footer (see 05-html-example-pattern.mdc)
└── README.md               # Required: options + visual appearance on render (see 08-block-creation-checklist.mdc)
```

## Critical Rules

### 1. JSON files MUST have underscore prefix
```
✅ _text-callout.json
✅ _hero.json
❌ text-callout.json
❌ config.json
```

### 2. All files MUST match folder name
```
✅ text-callout/ → text-callout.js, text-callout.css, _text-callout.json
❌ text-callout/ → textCallout.js, styles.css, _config.json
```

### 3. Use kebab-case everywhere
```
✅ text-callout, simple-cta, hero
❌ textCallout, text_callout, TextCallout
```

### 4. CSS files only (no SCSS)
```
✅ text-callout.css
❌ text-callout.scss
```

## CSS Class Naming
- Format: `block-name-element` (hyphenated)
- NO BEM: no `__` or `--` modifiers
- NO generic names: `.title`, `.content`, `.wrapper`

```css
✅ .text-callout-title
✅ .hero-content
✅ .hero-content
❌ .text-callout__title
❌ .text-callout--active
❌ .title
```

## Shared Elements
- `.brand-cta` — shared CTA class across all blocks
- Use block-scoped selectors: `.block-name .brand-cta`

## E-Commerce Naming (Optional)
When building blocks for e-commerce contexts, consider using domain-specific names:
- `product-card` instead of `content-card`
- `price-selector` instead of `plan-picker`
- `add-to-cart` instead of `action-button`

> **Note**: This is a suggestion, not a requirement. Use e-commerce naming when it improves clarity for the authoring team. Always discuss with the user first.

---

## 08-block-creation-checklist

> Source: .cursor/rules/08-block-creation-checklist.mdc

# Block Creation Checklist

## `api/` folder

Blocks, sections, and their JSON **must not** depend on **`api/`** for runtime behavior or authoring contracts. See **`31-api-folder-off-limits.mdc`**.

## Before Starting — Ask Clarifying Questions First!
> **NEVER assume** — always ask the user clarifying questions before building.

> **Discovery first (all new work):** See **`28-new-component-discovery.mdc`**. Prefer meeting the need with **`blocks/*/`** custom blocks and **`models/_section.json`** (and related section utilities) when possible; **do not** treat reference-only or generic xwalk blocks as the default suggestion for authors. Reference material is for **patterns**, not the shipped component set.

1. **Ask clarifying questions** (use **layout + content + behavior** — infer likely behavior from the UI, then confirm):
   - What is the block's purpose?
   - What content fields does it need?
   - **Interaction:** Is the UI **display-only**, **copy/reference**, or **must it drive actions** (same page, **`window.Bus`**, navigation, `fetch`)? Who listens? Any keyboard / focus / live-region requirements?
   - Should this be a **block** or a **section**? (See `15-sections-vs-blocks.mdc`)
   - Does the design show **repeatable** items? If yes, plan a **parent + child** pair (`22-repeatable-parent-child-blocks.mdc`): parent **`filters` must include** the child id; **overall** appearance on parent, **item** appearance on child.
   - Are there style variants needed?
   - Is this part of a larger pattern (carousel, accordion, modal)?
   - **Defer implementation** until you have answers (or explicit “ship display-only v1” scope from the user).
2. **Suggest a section** if the functionality spans multiple blocks (with reasoning)
3. **Inventory this repo:** scan **`blocks/*/`** and section capabilities (**`models/_section.json`**, **`styles/section-utilities.css`**) for an existing fit or composition. Only then, for **implementation patterns** (JSON/JS shape), use **`.cursor/knowledge/reference-blocks/`** (e.g. text-callout). Do **not** lead with reference or boilerplate block **names** as author-facing recommendations when a **custom** `blocks/` block applies.
4. Review the canonical **`text-callout`** reference implementation as a **code pattern** baseline when authoring **new** JSON/JS — not as a substitute for shipping custom blocks in this project.
5. Identify which fields the block needs — reference [Adobe field types documentation](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/universal-editor/field-types#component-types)

## Scope of file changes (new block only)

When implementing a **new** block (or a tightly scoped change to one existing block), **edit only files that belong to that work**:

- **In scope:** `blocks/<block-name>/` (`_<block-name>.json`, JS, CSS, `*-example.html`, `README.md`); **`block-configs/component-list.json`** when the block must appear in the section palette; merged **`component-definition.json`**, **`component-models.json`**, **`component-filters.json`** only as produced by **`npm run build:json`** after your JSON or component-list edits — not hand-edited for unrelated reasons.
- **Out of scope:** unrelated **`scripts/`**, **`api/`**, global **`styles/`** (unless the block genuinely needs a new shared token and the user agreed), **drive-by** lint fixes, duplicate-key cleanups, refactors, or other blocks — see **`28-new-component-discovery.mdc`** (*File scope when implementing a new block*).

## Step 1: Create Folder
```bash
mkdir blocks/my-block
```

## Step 2: Create `_my-block.json`
- [ ] **Section palette (mandatory for every new block that belongs in a section):** append the block’s filter **`id`** to **`block-configs/component-list.json`** → **`components`**. Same string as **`filters[].id`** in this `_my-block.json`. Do **not** edit **`models/_section.json`** filters for this — see **`24-block-filters-component-list.mdc`**
- [ ] **Author `description` text:** plain language only; no HTML/row/merge/CSS jargon (**`25-json-field-descriptions.mdc`**). Omit **`description`** when the label is enough.
- [ ] Three top-level arrays: `definitions`, `models`, `filters`
- [ ] **Mandatory opening model fields:** **`id`** (`text`) = first **HTML row**; **`classes`** (`text`, **`name` must be `classes`**, `hidden: true`, `readOnly: true` recommended, **`value`** = **`eds-block-my-block`**) **required in JSON** but **does not create a row** — merged onto block root only
- [ ] **`template`** includes **`id`** and **`classes`** (same `eds-block-<kebab-name>` string)
- [ ] `resourceType` = `"core/franklin/components/block/v1/block"`
- [ ] Template defaults for meaningful fields only
- [ ] `"component": "boolean"` (NOT checkbox)
- [ ] `"valueType": "string"` on all select fields
- [ ] Tab names use camelCase with `tab` prefix: `"tabGeneral"`
- [ ] NO analytics tab or tracking fields
- [ ] If authors should place another block **inside** this block: add that block’s filter `id` to **`filters[].components`** (JSON only — **no** extra “parent” JavaScript unless the design composes children; see **`22-repeatable-parent-child-blocks.mdc`**)
- [ ] If this is a **parent** of a repeated **child** block: **`filters[].components`** includes the **child’s filter `id`**

## Step 3: Create `my-block.js`
- [ ] Import helpers from `block-helpers.js`
- [ ] Export **`createBlock(options)`** (named) returning AEM-shaped block HTML string — see **`33-programmatic-createBlock.mdc`**
- [ ] Document row layout in JSDoc comment
- [ ] **`decorate()` flow** matches **`02-block-javascript-pattern.mdc`**: `extractConfig(block)` → `buildBlock(block, config)` → optional **`await` loaders** that only mutate DOM under **`config.mainEl`** (or children) → **`appendEvents(config)`**
- [ ] `extractConfig()` with `[...block.children]` — run **before** row removal so indices match the model
- [ ] `buildBlock()` preserves nested blocks: use `replaceBlockRowsPreservingNestedBlocks` / `removeNonBlockChildRows` — **never** `block.textContent = ''` or `replaceChildren` on the whole block
- [ ] Use `classList.add()` (NOT `className =`) for all class assignments
- [ ] Set **`config.mainEl` in `buildBlock`** — primary interactive element, or the single wrapper you own (required even when `appendEvents` is a no-op)
- [ ] `appendEvents(config)` — wire listeners; no-op body is allowed **only** when `config.mainEl` is still set
- [ ] `export default async function decorate(block)` with `await checkAndHandleNestedBlocks(block)` when nested UE markers are possible, and **`await`** an explicit **nested `decorateBlock` + `loadBlock`** helper **before** `extractConfig` when the parent hosts composed nested child roots that are **not** under the global `decorateBlocks` selector — see **`02-block-javascript-pattern.mdc`** (*Nested child blocks — decorate pipeline*) and **`blocks/magic/magic.js`** (`ensureNestedBlocksReady`)
- [ ] NO `dataLayer.js` import, NO `applyTracking()`

## Step 4: Create `my-block.css`
- [ ] Mobile-first with `@media (min-width: 768px)` and `1024px`
- [ ] Colors, spacing, and type in CSS use **`styles/colors.css`** / **`styles/styles.css`** `:root` **`var(--color-*)`**, **`var(--space-*)`**, **`var(--*-font-size-*)`**, **`var(--font-family-*)`** per **`32-block-root-tokens.mdc`**; literals only where that rule allows
- [ ] Hyphenated class names
- [ ] `prefers-reduced-motion` and print styles (print colors still use **`var(--color-*)`**, not hex)
- [ ] Style variants matching JSON `classes` field

## Step 5: Create `my-block-example.html` (**required**)
This is **not optional**: the example is the shared contract for row shape, block root classes, and local smoke-testing with real `loadBlock()` behavior.

- [ ] **Standalone HTML5 document** per `05-html-example-pattern.mdc`
- [ ] `<head>` matches **`head.html`**: CSP (as in project), viewport, **`/scripts/aem.js`** and **`/scripts/scripts.js`** (`type="module"`, `nonce="aem"`), **`/styles/styles.css`**
- [ ] **No** direct `<link>` / import for `./my-block.css` or `./my-block.js` — `aem.js` `loadBlock()` loads them
- [ ] `<body>` includes `<header></header>`, `<main>`, `<footer></footer>`; block markup under `main > div > div.my-block` (Franklin section + block shape)
- [ ] Exact AEM-generated row structure inside the block root; one `<div><div>...</div></div>` per data field; row order matches JSON
- [ ] **`classes_*` / style exhaustiveness** per **`05-html-example-pattern.mdc`**: every select option value (including `""`) appears on at least one block root; neutral defaults for non-swept axes; optional combo blocks + comment matrix when many permutations exist
- [ ] **Images in examples:** omit images entirely unless the model has **`reference`** (image) fields — then add one **`picture`** row per `reference` using **full `https://www.gradholder.com/img/...` URLs** only — **not** site-root paths like `/img/...` or `/media/...` (**`05-html-example-pattern.mdc`**). **Gradholder:** use **two distinct** `hexFrom`/`hexTo` colors per placeholder and **vary** pairs across samples — not all `ffffff` or one repeated pair (**`05-html-example-pattern.mdc`**). **Richtext:** no inline images by default; only if the block documents an exception
- [ ] **Videos in examples:** when the demo includes video file URLs, use **`/videos/<filename>`** under the repo **`videos/`** folder only — **not** third-party sample URLs (**`05-html-example-pattern.mdc`** — Videos in example HTML)
- [ ] **Section utilities in `*-example.html` (when useful):** if a sample needs a backdrop (light/dark/black section, padding, width), add the matching **`section-*`** classes from **`models/_section.json`** / **`section-utilities.css`** on that sample’s outer **`main > div`**; fix page-local label colours on dark sections so **`h2`/`p`** stay readable (**`05-html-example-pattern.mdc`** — Section appearance utilities)
- [ ] **Example HTML sync:** when you add **new `classes_*` options**, **row shapes**, or other **large** contract changes, update **`blocks/<block>/<block>-example.html`** (and any related demos) per **`26-example-html-sync.mdc`**; run **`npm run build:json:models`** if models merged into **`component-models.json`** changed
- [ ] **Multiple samples:** each has **`h2` + `p`** (what the variant shows) **then** the block root; use **one `main > div` per sample** + light `<style>` in `<head>` for separators (**`05-html-example-pattern.mdc`** — Variant labels)
- [ ] HTML comment at top of block (or file) documenting **row index → field** (skip `tab`, `classes`, `classes_*`)
- [ ] Verify via HTTP with **repo root** as docroot (`npx serve .`), then open `/blocks/my-block/my-block-example.html`

## Step 6: Create `README.md` (**required**)
Every new block **must** ship **`README.md`** in the block folder. Authors, developers, and **automated choosers (e.g. other LLMs)** use it to match requirements to this block — not optional.

- [ ] **Purpose** — what the block is for and when to use it
- [ ] **For another AI / LLM** — short **when to pick** vs **when not to pick** this block (searchable requirement language)
- [ ] **Big picture (plain language)** — explain rendering as if to someone with **minimal webpage knowledge** or **non-visual context** (stack of boxes, reading order, “same pen” / ink color analogies). No assumption they see the screen.
- [ ] **Fields / options** — table of authoring fields (`classes_*`, etc.)
- [ ] **Every variation** — for **each** distinct option value (and important combinations): use a **fixed pattern**, e.g. **Author picks** | **Choose this if** (LLM) | **Plain explanation** | **Sighted user** (optional) | **Technical** — so requirements map cleanly to rendering
- [ ] **Row map** — which model fields create HTML rows (`extractConfig` order)
- [ ] **Files** — pointer to `_*.json`, JS, CSS, `*-example.html`
- [ ] **Related** — linked utilities, RTE config, parent/child blocks if any

Reference: `blocks/content/README.md`

## Step 7: Section palette (if the block should appear in a section)
- [ ] Use **Step 2** — add the filter **`id`** to **`block-configs/component-list.json`** only. **Do not** edit **`models/_section.json`** `filters` to list block ids (the section filter already merges **`component-list.json`** — **`24-block-filters-component-list.mdc`**).
- [ ] If creating a **new section type** (new model under `models/`), follow **`16-section-creation-pattern.mdc`**

## Step 8: Run Format, Lint & Build
```bash
npm run format        # Prettier (optional if you relied on format-on-save + pre-commit)
npm run lint          # Validate JS and CSS
npm run build:json    # Rebuild component-definition.json, component-models.json, component-filters.json
```
- [ ] If you changed **`block-configs/component-list.json`**, **`npm run build:json`** (or **`build:json:filters`**) must run so **`component-filters.json`** includes the new block in the section filter (**`24-block-filters-component-list.mdc`**)
- [ ] `npm run format:check` passes (or `npm run format` applied)
- [ ] `npm run lint` passes with no errors
- [ ] `npm run build:json` completes successfully

## Validation
- [ ] **Scope:** The change set matches **Scope of file changes** — no unrelated repo files, drive-by fixes, or other blocks touched unless the user explicitly asked
- [ ] All files match folder name
- [ ] **`README.md` exists** with **LLM “when to pick”**, **plain-language rendering**, and **per-variation** tables/pattern (Step 6)
- [ ] **`<block-name>-example.html` exists** and loads via repo-root HTTP (`05-html-example-pattern.mdc`); richtext/body rows use **Lorem Ipsum** (**`05-html-example-pattern.mdc`**)
- [ ] JSON has underscore prefix
- [ ] No SCSS; no ad hoc **`var(--block-*)`** — token **`var()`** limited to **`32-block-root-tokens.mdc`**
- [ ] No analytics imports or attributes
- [ ] Classes are hyphenated (using `classList.add()`)
- [ ] `config.mainEl` is set in buildBlock
- [ ] `decorate()` matches **`02-block-javascript-pattern.mdc`** (`async` + nested handling when applicable)
- [ ] Block filter **`id`** is in **`block-configs/component-list.json`** when it should appear in sections (do **not** hand-edit **`models/_section.json`** filters for that — **`24-block-filters-component-list.mdc`**)
- [ ] Prettier / `format:check` passes
- [ ] Lint passes
- [ ] build:json completes

## References
- Block Development Template: `.cursor/knowledge/documentation/16-BLOCK_DEVELOPMENT_TEMPLATE.md`
- Adobe field types: https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/universal-editor/field-types#component-types
- Sections guide: `15-sections-vs-blocks.mdc` and `16-section-creation-pattern.mdc`

---

## 09-utilities-and-helpers

> Source: .cursor/rules/09-utilities-and-helpers.mdc

# Block Helpers — Utility Reference

Source: `scripts/utilities/block-helpers.js`

## Import Pattern
```javascript
import {
  getTextFromRow,
  getHtmlFromRow,
  getHtmlFromRow,
  getTextFromBlockRow,
  getLinkFromRow,
  getImageFromRow,
  getBooleanFromRow,
  checkAndHandleNestedBlocks,
  replaceBlockRowsPreservingNestedBlocks,
} from '../../scripts/utilities/block-helpers.js';
```

## Row Value Extractors
AEM generates `<div><div>content</div></div>` per field. These helpers extract data:

| Function | Returns | Use For |
|----------|---------|--------|
| `getValue(block, index)` | `HTMLElement\|null` | Raw inner div element |
| `getText(block, index)` | `string` | Plain text (strips HTML) |
| `getHTML(block, index)` | `string` | Rich HTML content |
| `getImage(block, index)` | `HTMLImageElement\|null` | `<img>` element |
| `getLink(block, index)` | `HTMLAnchorElement\|null` | `<a>` element |

## Single-Row Extractors (for `[...block.children]` pattern)
| Function | Returns | Use For |
|----------|---------|--------|
| `getTextFromRow(row)` | `string` | Plain text from row div |
| `getHtmlFromRow(row)` | `string` | Rich HTML from row div (classic `div > div` cell) |
| `getHtmlFromRow(row)` | `string` | Rich HTML — top-level **or nested** (one fewer wrapper) |
| `getTextFromBlockRow(row)` | `string` | Plain text — same wrapper rules as `getHtmlFromRow` |
| `getLinkFromRow(row)` | `HTMLAnchorElement\|null` | Link from row div |
| `getImageFromRow(row)` | `HTMLImageElement\|null` | Image from row div |
| `getBooleanFromRow(row)` | `boolean` | Boolean toggle — uses same wrapper rules as `getTextFromBlockRow` |

**Nested child blocks** should prefer **`getHtmlFromRow` / `getTextFromBlockRow`** (see **`22-repeatable-parent-child-blocks.mdc`**).

## Nested blocks + rebuild (every `decorate`)
| Function | Use |
|----------|-----|
| `checkAndHandleNestedBlocks(block)` | `await` first in `decorate` when UE may emit marker rows; decorates/loads **direct** children whose **`innerHTML`** contains **`eds-block-`**. **Not sufficient alone** for every composed nested root — parents that host nested blocks often need **`decorateBlock` + `loadBlock`** from **`scripts/aem.js`** (see **`02-block-javascript-pattern.mdc`** *Nested child blocks — decorate pipeline*; reference **`blocks/magic/magic.js`** `ensureNestedBlocksReady`) |
| `isNestedBlockRowElement(el)` | True for `.block`, `eds-block-*`, or `.block-child-wrapper` with nested `.block` |
| `removeNonBlockChildRows(block)` | Remove authored rows only |
| `prependBlockBuiltNodes(block, ...nodes)` | Prepend multiple built roots in order (e.g. track + footer) |
| `replaceBlockRowsPreservingNestedBlocks(block, builtRoot)` | `removeNonBlockChildRows` + prepend one root |

See **`02-block-javascript-pattern.mdc`**.

## Responsive Helpers
```javascript
import { createResponsiveHelper, createDesktopHelper } from '../../scripts/utilities/block-helpers.js';

// Listen for breakpoint changes (debounced)
const responsive = createResponsiveHelper((isDesktop) => {
  // Update layout based on viewport
});

// Simple desktop check
const isDesktop = createDesktopHelper();
if (isDesktop()) { /* desktop layout */ }
```

## Block Grouping (Accordion/Carousel Patterns)
```javascript
import { createBlockGrouper, createAdvancedBlockGrouper } from '../../scripts/utilities/block-helpers.js';

// Group consecutive blocks of same type
const grouper = createBlockGrouper('accordion');
const groups = grouper(section);
```

## Toggle/Expand Helpers (ARIA-compliant)
```javascript
import { createToggle, addToggleListeners } from '../../scripts/utilities/block-helpers.js';

const toggle = createToggle(triggerEl, contentEl);
addToggleListeners(toggle);
```

## Environment Detection
```javascript
import { isAuthorMode } from '../../scripts/utilities/block-helpers.js';

if (isAuthorMode()) {
  // Universal Editor-specific behavior
}
```

## Deprecated helpers (do not use in new blocks)
- **`ensureBlockElementId`**, **`ensureUniqueElementId`** — Set **`id`** from author fields **as-is** when non-empty; otherwise leave unset. See **`19-bus-actionable-elements.mdc`**. The functions remain in **`block-helpers.js`** for legacy code only (`@deprecated` JSDoc).

## Reference
- Source code: `.cursor/knowledge/utilities/block-helpers.js`
- Full guide: `.cursor/knowledge/utilities/HELPERS_GUIDE.md`
- Usage examples: `.cursor/knowledge/utilities/USAGE_EXAMPLES.md`

---

## 10-json-advanced-patterns

> Source: .cursor/rules/10-json-advanced-patterns.mdc

# JSON Advanced Patterns

Reserved **`name`** suffixes (`Title`, `Type`, `MimeType`, `Alt`, `Text`) follow AEM [field collapse](https://www.aem.live/developer/component-model-definitions#field-collapse); see **`03-block-json-pattern.mdc`**.

## Tab Implementation (Two Approaches)

### Approach 1: Flat Tabs (Our Production Pattern)
Tabs as dividers in the fields array:
```json
{
  "fields": [
    { "component": "tab", "label": "General", "name": "tabGeneral" },
    { "component": "text", "name": "id", "label": "ID" },
    { "component": "richtext", "name": "heading", "label": "Heading" },
    { "component": "tab", "label": "Appearance", "name": "tabAppearance" },
    { "component": "select", "name": "classes", "label": "Style" }
  ]
}
```

### Approach 2: Container Tabs (AEM Documentation Pattern)
Tabs with nested containers:
```json
{
  "fields": [{
    "component": "tab",
    "name": "tabs",
    "fields": [
      {
        "component": "container",
        "label": "General",
        "fields": [
          { "component": "text", "name": "id" }
        ]
      }
    ]
  }]
}
```

**⚠️ Repeating items:** Do **not** combine **`container`** with **`"multi": true`** and nested **`fields`** for Universal Editor **properties panel** models — Adobe states **container nesting is not permitted for multi-fields** there ([field types — `multi`](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/universal-editor/field-types#fields)). Use **numbered flat fields** + **per-item tabs** instead (`card1Label`, `option1Id`, …).

## Element grouping (AEM underscore convention)

Official behavior: [Element grouping](https://www.aem.live/developer/component-model-definitions#element-grouping).

- **`{group}_{member}`** — same **group** prefix + underscore → **one table cell**, **multiple** DOM elements inside that cell (subtitle, headings, paragraphs, links, etc., as in the **Teaser** example in the docs).
- **`classes` + `classes_*`** — extra block option fields grouped under `classes`. **`classes_*` + `boolean`**: when **`true`**, merge the class equal to the **`name`** suffix after `classes_` (kebab-case suffix; no camelCase after the prefix). See **`27-classes-boolean-fields.mdc`**. Other **`boolean`** fields create **`true`/`false`** rows.
- **Field collapse** still applies to members (`…Text`, `…Title`, `…Type`, …). See **`03-block-json-pattern.mdc`** (Field collapse + Element grouping).

**This codebase:** Prefer **separate rows** via camelCase field names (`option1Label`, `card1Label`) unless the design explicitly needs one grouped cell.

## merge-json-cli (Build Tool)
Used to compose `component-definition.json`, `component-models.json`, and `component-filters.json` from per-block JSON files.

```bash
# package.json scripts
"build:json:models": "merge-json-cli -i 'models/_component-models.json' 'blocks/**/_*.json' -o component-models.json -k models",
"build:json:definitions": "merge-json-cli -i 'models/_component-definition.json' 'blocks/**/_*.json' -o component-definition.json -k definitions",
"build:json:filters": "merge-json-cli -i 'models/_component-filters.json' 'blocks/**/_*.json' -o component-filters.json -k filters"
```

## Reference
- Element grouping analysis: `.cursor/knowledge/analysis/aem_element_grouping_analysis.md`
- Tabs & merge guide: `.cursor/knowledge/analysis/AEM_EDS_Tabs_and_JSONMerge_Complete_Guide.md`

---

## 11-responsive-design

> Source: .cursor/rules/11-responsive-design.mdc

# Responsive Design Strategy

## Core Principle: Mobile-First
Base styles target mobile. Media queries add styles for larger screens.

Also account for **variable CMS copy** (more or less text than any screenshot); see **`20-cms-content-flexibility.mdc`**.

## Standard Breakpoints
```css
/* Base: Mobile (320px+) — default styles */
.block-name { font-size: 0.875rem; padding: 1rem; flex-direction: column; }

/* Tablet (768px+) */
@media (min-width: 768px) {
  .block-name { font-size: 1rem; padding: 1.5rem; flex-direction: row; }
}

/* Desktop (1024px+) */
@media (min-width: 1024px) {
  .block-name { font-size: 1.125rem; padding: 2rem; }
}

/* Large (1440px+) — only when needed */
@media (min-width: 1440px) {
  .block-name { max-width: 1200px; margin: 0 auto; }
}
```

## Layout Patterns

### Stack → Side-by-Side
```css
.block-name { display: flex; flex-direction: column; gap: 1rem; }
@media (min-width: 768px) {
  .block-name { flex-direction: row; gap: 2rem; }
}
```

### Grid Columns
```css
.block-name-list { display: grid; grid-template-columns: 1fr; gap: 1rem; }
@media (min-width: 768px) {
  .block-name-list { grid-template-columns: repeat(2, 1fr); }
}
@media (min-width: 1024px) {
  .block-name-list { grid-template-columns: repeat(3, 1fr); }
}
```

## JS Responsive Helper
When CSS alone isn't enough, use the JS helper:
```javascript
import { createDesktopHelper } from '../../scripts/utilities/block-helpers.js';
const isDesktop = createDesktopHelper();
// Match CSS breakpoint: DESKTOP_BREAKPOINT = 1024
```

## Reference
- Full strategy: `.cursor/knowledge/documentation/13-RESPONSIVE_DESIGN_STRATEGY.md`
- CSS guide: `.cursor/knowledge/documentation/05-CSS_STYLING_APPROACH.md`

---

## 12-project-architecture

> Source: .cursor/rules/12-project-architecture.mdc

# Project Architecture & Boilerplate

## Repository Structure
```
aem-eds-blocks/
├── .gitattributes             # text eol=lf; binary overrides
├── .stylelintignore           # e.g. .cursor/ (lint-staged passes staged *.css)
├── blocks/                    # Block implementations
│   └── block-name/
│       ├── block-name.js
│       ├── block-name.css
│       ├── block-name-example.html  # Required: standalone demo + AEM row markup (see 05-html-example-pattern.mdc)
│       ├── README.md                # Required: options + on-screen appearance (08-block-creation-checklist.mdc)
│       └── _block-name.json
├── block-configs/
│   └── component-list.json    # Section insert palette: add new block filter ids here (**24-block-filters-component-list.mdc**); merged via **models/_section.json**
├── models/                    # Shared JSON patterns (for merge-json-cli)
│   ├── _component-definition.json
│   ├── _component-models.json # Merges block models + _page*.json (see 23-page-template-metadata.mdc)
│   ├── _component-filters.json
│   ├── _page.json             # Page metadata model id: page-metadata
│   └── _page-<template>.json  # Optional per-WCM-template metadata; id: <template>-metadata
├── scripts/                   # Shared utilities
│   ├── editor-support.js      # UE: syncs body data-aue-model from data-aem-template (see 23-page-template-metadata.mdc)
│   ├── scripts.js             # loadEager: window.Bus (mitt) first, then decorate
│   ├── aem.js                 # AEM framework (RUM, decorateBlock, loadBlock)
│   ├── events.js              # EDS_EVENTS (Bus event name constants)
│   ├── vendor/mitt.min.js     # Vendored mitt ESM
│   ├── constants.js           # Shared `constants` object (e.g. sessionStorage keys) — safe for blocks to import
│   └── utilities/
│       └── block-helpers.js   # Row extractors, nested-block preservation, optional grouping/toggles (see repo rules for `id` policy)
├── fonts/                     # Local webfont binaries (eds-heading, eds-body-*, eds-ui-*)
├── styles/                    # Global styles
│   ├── styles.css             # @imports fonts.css, font-utilities.css; :root tokens
│   ├── font-utilities.css     # u-font-title / body / ui utilities (var-based)
│   ├── fonts.css              # @font-face → ../fonts/eds-*.woff2 / .otf
│   └── lazy-styles.css
├── component-definition.json  # Generated (merge output)
├── component-models.json      # Generated (merge output)
├── component-filters.json     # Generated (merge output)
└── package.json               # Build scripts, dependencies
```

## Core Scripts

### scripts.js (Page Orchestrator)
- `loadEager()` — First: `window.Bus = mitt()`. Then LCP-critical above-fold content.
- `loadLazy()` — Below-fold, header/footer
- `loadDelayed()` — Non-critical after 3s delay
- `decorateMain()` — Applies button, icon, section, block decorations
- `moveInstrumentation()` — Transfers authoring attributes to new elements

### aem.js (AEM Framework)
- `sampleRUM()` — Real User Monitoring
- `decorateBlock()` / `loadBlock()` — Block loading pipeline
- `decorateButtons()` / `decorateIcons()` — Global decorators
- `loadCSS()` / `loadScript()` — Async resource loading

### block-helpers.js (Development Utilities)
Row extractors, responsive helpers, block grouping, toggle/expand, DOM id helpers for Bus wiring, environment detection.

See **`19-bus-actionable-elements.mdc`** for actionable element + emit conventions.

## package.json Key Scripts
```json
"scripts": {
  "format": "prettier --write --ignore-unknown .",
  "format:check": "prettier --check --ignore-unknown .",
  "lint:js": "eslint . --ext .json,.js,.mjs",
  "lint:css": "stylelint \"blocks/**/*.css\" \"styles/*.css\"",
  "lint": "npm run lint:js && npm run lint:css",
  "lint:fix": "npm run lint:js -- --fix && npm run lint:css -- --fix",
  "build:json": "npm-run-all build:json:*"
}
```

- **`npm run format`** — apply Prettier project-wide (respects `.prettierignore`).
- **`npm run format:check`** — CI-friendly check without writing files.
- **Husky pre-commit** runs **`lint-staged`** (ESLint + Prettier on staged JS; Prettier + Stylelint `--fix` on staged CSS; Prettier on JSON/HTML/YAML/MD), then existing `build:json` regeneration when `_*` partials change.
- **Editor:** committed `.vscode/settings.json` enables **format on save** with Prettier for Cursor/VS Code users.
Uses `merge-json-cli` to combine per-block JSON into root component files.

## Reference
- Boilerplate source: `.cursor/knowledge/boilerplate/`
- Project structure: `.cursor/knowledge/documentation/10-PROJECT_STRUCTURE.md`
- Improvements: `.cursor/knowledge/documentation/11-IMPROVEMENTS_TO_REFERENCE.md`

---

## 13-foundational-blocks

> Source: .cursor/rules/13-foundational-blocks.mdc

# Foundational Blocks — 19 Block Specification

## Block Categories

### Content & Text (4 blocks)
1. **Text Block** — Formatted text content, color/size variants
2. **Title Block** — Headings with RTE-based heading levels
3. **Table Block** — Structured data, responsive tables
4. **Quote Block** (Phase 2) — Testimonials with attribution

### Media (3 blocks)
5. **Image Block** — Desktop/mobile variants, captions, overlays
6. **Video Block** — Local DAM, YouTube, Vimeo embedding
7. **Icon Block** — SVG icons with optional labels

### Navigation & CTA (3 blocks)
8. **CTA Block** — Call-to-action with style variants
9. **Button Group** — Multiple buttons in a row
10. **Breadcrumb** (Phase 2) — Navigation breadcrumbs

### Layout (3 blocks)
11. **Columns Block** — Multi-column layouts
12. **Cards Block** — Card grid layouts
13. **Divider Block** — Visual separators

### Interactive (3 blocks)
14. **Accordion Block** — Expandable sections (uses block grouping)
15. **Tabs Block** — Tabbed content panels
16. **Carousel Block** — Rotating content

### Page Structure (3 blocks)
17. **Header Block** — Site header with navigation
18. **Footer Block** — Site footer with links
19. **Fragment Block** — Reusable content fragments

## Implemented Reference Blocks
- `text-callout` — Canonical reference (text + CTA)
- `simple-cta` — Simple call-to-action
- `hero` — Hero banner
- `product-hero` — Product banner variant

## Reference
- Full specs: `.cursor/knowledge/documentation/07-FOUNDATIONAL_BLOCKS.md`
- Detailed specs: `.cursor/knowledge/documentation/08-BLOCK_SPECIFICATIONS.md`
- Design philosophy: `.cursor/knowledge/documentation/06-BLOCK_DESIGN_PHILOSOPHY.md`

---

## 14-learnings-and-pitfalls

> Source: .cursor/rules/14-learnings-and-pitfalls.mdc

# Learnings & Common Pitfalls

## Critical JSON Corrections
| Wrong | Correct | Impact |
|-------|---------|--------|
| `"component": "checkbox"` | `"component": "boolean"` | Must use `boolean` for toggles |
| `"visible": ...` | `"condition": {"==": [{"var": "field"}, true]}` | JSON Logic syntax |
| All fields in template | Only meaningful defaults | Omit empty fields |
| `"value"` on every field | `"value"` only on select fields | Others use template defaults |
| Tab name `"general"` | `"tabGeneral"` (camelCase) | Follow naming convention |
| No `valueType` on select | `"valueType": "string"` | Required for value handling |

## Critical JavaScript Corrections
| Wrong | Correct | Impact |
|-------|---------|--------|
| `new URL(src)` for arbitrary image `src` (e.g. CTA icon from `getAttribute('src')`) | `new URL(src, window.location.href)` (as in **`createOptimizedPicture`**) | Relative AEM paths like `./media_….webp` throw without a base; block `decorate` fails and raw rows remain |
| `querySelectorAll(':scope > div')` | `[...block.children]` | Simpler row access |
| Manual DOM queries | `block-helpers.js` functions | Consistent extraction |
| BEM classes (`__`, `--`) | Hyphenated (`block-name-el`) | Convention compliance |
| `className = 'class-name'` | `classList.add('class-name')` | Prevents overwriting existing classes |
| `async function decorate` | `function decorate` (sync) | No async unless needed |
| `innerHTML = ''` to clear | `textContent = ''` | More efficient |
| Building `<a>` from scratch | Reuse AEM's `<a>` element | `getAnchorFromRow()` pattern |
| No `config.mainEl` | Always set `config.mainEl` in buildBlock | Required for appendEvents |

## CSS Pitfalls
| Wrong | Correct |
|-------|--------|
| `var(--color-primary)` | `#0066cc` (direct value) |
| `$breakpoint-tablet` | `@media (min-width: 768px)` |
| `.scss` files | `.css` files only |
| `@include respond-to()` | Standard `@media` queries |
| BEM modifiers | Variant classes from JSON `classes` field |

## Element Grouping Trap
Fields named with underscores (`option1_label`, `option1_price`) get merged into ONE cell.
**Fix**: Use camelCase (`option1Label`, `option1Price`) to keep fields in separate rows.

## Architectural Evolution
1. **Phase 1**: Initial analysis (SCSS, tokens, 19 blocks)
2. **Phase 2**: User corrections (HTML structure, JSON structure)
3. **Phase 3**: Production reference integration (text-callout)
4. **Phase 4**: Architectural simplification (basic CSS, underscore JSON, file naming)
5. **Phase 5**: Analytics removal, HTML examples added
6. **Phase 6**: Section knowledge, classList, mainEl, clarifying questions, lint/build

## Reference
- Full learnings: `.cursor/knowledge/analysis/LEARNINGS.md`
- Session summary: `.cursor/knowledge/analysis/SESSION_SUMMARY.md`
- Changelog: `.cursor/knowledge/analysis/CHANGELOG.md`
- Migration guide: `.cursor/knowledge/analysis/MIGRATION_GUIDE.md`

---

## 15-sections-vs-blocks

> Source: .cursor/rules/15-sections-vs-blocks.mdc

# Sections vs Blocks

## When to Use a Block
Blocks are **modular, self-contained components** that:
- Represent a single piece of functionality (hero, CTA, card, etc.)
- Have their own JS/CSS/JSON triad in `blocks/block-name/`
- Can be added/removed independently by authors
- Live in `blocks/block-name/` directory

### Repeatable UI → parent + child (two blocks)
If a **screenshot** or **requirement** shows **repeating** UI (same item pattern many times), split into:
- **Parent block** — Non-repeated fields (titles, footnotes after the list, group behavior) and **overall** appearance (`classes` / layout of the whole module). The parent’s **`filters` → `components` must list the child block’s id** so the child can be authored inside the parent.
- **Child block** — Repeated unit; **per-item** appearance stays on the child model.

Nested blocks receive **different AEM-generated HTML** (different DOM path) than top-level blocks — see **`22-repeatable-parent-child-blocks.mdc`**.

## When to Use a Section
Sections are for **non-modular functionality that spans multiple blocks**:
- **Carousel**: Groups multiple blocks into a sliding container
- **Accordion**: Groups multiple blocks into collapsible panels
- **Modal**: Groups content blocks into a modal overlay
- **Tabbed content**: Groups blocks into switchable tabs

### Key Difference
> A **block** is a self-contained component. A **section** wraps and orchestrates multiple blocks.

## Section Metadata
- All authored data for a section goes into a **`div.section-metadata`**. In Franklin HTML it is **typically the last sibling** row among the section’s content before decoration; each row inside metadata is **`div` > key cell `div`, value cell `div`** (same shape as `readBlockConfig()` for blocks).
- The xwalk framework **auto-processes** section metadata via `decorateSections()` in `scripts/aem.js` (keys normalized with `toClassName`, then stored on `dataset` via `toCamelCase`). If multiple `.section-metadata` nodes exist, the **last** wins.
- `style` field → CSS classes on section div
- All other fields → `data-*` attributes on the section div
- Example: `section-identifier` field with value `section-carousel` → `data-sectionidentifier="section-carousel"`
- **Reading in JS:** `data-*` names are lowercase; prefer `element.dataset` camelCase

## Section File Locations
Section JS/CSS live in `blocks/` but section JSON lives in `models/`:

```
project-root/
├── blocks/              # Blocks AND Section JS/CSS
│   ├── my-block/
│   │   ├── my-block.js
│   │   ├── my-block.css
│   │   └── _my-block.json     ← Block has JSON here
│   │
│   └── section-carousel/
│       ├── section-carousel.js  ← Section JS here
│       └── section-carousel.css ← Section CSS here
│                                 ← NO JSON here!
│
├── models/              # Section JSON models
│   ├── _section.json            ← Base section model
│   └── _section-carousel.json   ← Custom section model
```

## Section JSON Model
Section JSON uses a **different resourceType** and requires a **hidden `sectionIdentifier` field**:

```json
{
  "definitions": [{
    "title": "Section (Carousel)",
    "id": "section-carousel",
    "plugins": {
      "xwalk": {
        "page": {
          "resourceType": "core/franklin/components/section/v1/section",
          "template": { "name": "Section (Carousel)", "model": "section-carousel" }
        }
      }
    }
  }],
  "models": [{
    "id": "section-carousel",
    "fields": [
      { "component": "text", "name": "sectionIdentifier", "value": "section-carousel", "label": "Section Identifier", "hidden": true },
      { "component": "boolean", "name": "autoPlay", "label": "Auto Play" }
    ]
  }]
}
```

Key points:
- `resourceType` is `core/franklin/components/section/v1/section` (NOT `.../block/v1/block`)
- Hidden `sectionIdentifier` field → becomes `data-sectionidentifier` → used to load JS/CSS from `blocks/`

## Section JS Loading
Section JS is loaded at the **end of `loadEager()`** in `scripts.js`:
1. Find all sections with `data-sectionidentifier`
2. Load CSS + JS from `blocks/${sectionIdentifier}/${sectionIdentifier}.js`
3. Call default export with the **section element** as parameter

This mirrors `loadBlock()` in `aem.js` but operates on the section element.

## Section JS Pattern
Section JS has a **simple decorator that calls focused functions**:

```javascript
function buildTrack(sectionEl, blocks) { /* ... */ }
function buildNav(blocks) { /* ... */ }
function addEvents(track, nav, blocks) { /* ... */ }

export default function decorate(sectionEl) {
  const blocks = [...sectionEl.querySelectorAll(':scope > div > .block')];
  const track = buildTrack(sectionEl, blocks);
  const nav = buildNav(blocks);
  addEvents(track, nav, blocks);
}
```

## When to Suggest a Section
If the user requests functionality that involves:
- Grouping/wrapping multiple blocks
- Carousel, accordion, tabs, or modal patterns
- Applying behaviour to a group of content rather than a single component

→ **Suggest creating a section instead of a block**, and explain why.

## New blocks vs new sections (discovery)

When the user asks for **new** layout or components (prompt or screenshot), **analyze and ask questions** first; see **`28-new-component-discovery.mdc`**. Check whether **`models/_section.json`** (appearance, optional grid layout) plus existing **`blocks/*/`** already satisfies the need before proposing a new section model or block. When naming options for authors, **prefer this repo’s custom blocks** over reference-only or generic xwalk examples unless nothing custom fits.

## Reference
- New component discovery: **`28-new-component-discovery.mdc`**
- Section guide: `.cursor/knowledge/analysis/SECTIONS_GUIDE.md`
- Official docs: https://www.aem.live/developer/markup-sections-blocks

---

## 16-section-creation-pattern

> Source: .cursor/rules/16-section-creation-pattern.mdc

# Section Creation Pattern

Before adding or extending a **section model**, run the same **discovery** pass as for blocks (**`28-new-component-discovery.mdc`**): confirm the base **`models/_section.json`** (and existing section utilities) cannot already meet the need with custom **`blocks/`** content.

## Section vs Block: File Locations

| Aspect | Block | Section |
|--------|-------|--------|
| JSON model | `blocks/block-name/_block-name.json` | `models/_section-name.json` |
| JavaScript | `blocks/block-name/block-name.js` | `blocks/section-name/section-name.js` |
| CSS | `blocks/block-name/block-name.css` | `blocks/section-name/section-name.css` |
| Has `_*.json` in blocks folder? | ✅ Yes | ❌ No |
| resourceType | `core/franklin/components/block/v1/block` | `core/franklin/components/section/v1/section` |
| JS loading | Via `loadBlock()` | Via `loadSectionModules()` at end of `loadEager()` |
| Decorator param | `block` element | `sectionEl` (section element) |

## Section JSON Model Template

```json
{
  "definitions": [{
    "title": "Section (My Section)",
    "id": "section-my-section",
    "plugins": {
      "xwalk": {
        "page": {
          "resourceType": "core/franklin/components/section/v1/section",
          "template": {
            "name": "Section (My Section)",
            "model": "section-my-section"
          }
        }
      }
    }
  }],
  "models": [{
    "id": "section-my-section",
    "fields": [
      {
        "component": "text",
        "name": "sectionIdentifier",
        "value": "section-my-section",
        "label": "Section Identifier",
        "hidden": true
      },
      {
        "component": "tab",
        "label": "Section Properties",
        "name": "tabSectionProperties"
      },
      {
        "component": "text",
        "name": "sectionTitle",
        "label": "Section Title",
        "description": "Title displayed above the section"
      },
      {
        "component": "boolean",
        "name": "autoPlay",
        "label": "Auto Play"
      }
    ]
  }],
  "filters": []
}
```

### CRITICAL: Hidden `sectionIdentifier` field
- Every section model **MUST** have a hidden `sectionIdentifier` field
- Value must match the section name (e.g., `"section-carousel"`)
- This becomes `data-sectionidentifier` on the section div
- `loadEager()` uses this to find and load the section's JS/CSS from `blocks/`

## Section Metadata → Data Attributes

Franklin emits a **`div.section-metadata`** (usually the **last** content sibling under the section before `decorateSections()`). Rows match the block table: each row is a `div` with two inner `div`s (key, value). `readBlockConfig()` reads those rows; `decorateSections()` turns each entry into a `data-*` on the section (except `style`, which becomes classes). Multiple `.section-metadata` nodes: this repo uses the **last** one.

The xwalk framework automatically converts section-metadata fields to `data-` attributes:

| Field Name (JSON) | Attribute on Section `<div>` |
|--------------------|-------------------------------|
| `sectionIdentifier` | `data-sectionidentifier` |
| `sectionTitle` | `data-sectiontitle` |
| `autoPlay` | `data-autoplay` |
| `slideDuration` | `data-slideduration` |

When implementing section logic that depends on `dataset`, treat HTML’s lowercase `data-*` serialization as authoritative and read values via camelCase `dataset` properties (as produced by `decorateSections()`).

## Section JavaScript Pattern

Section JS has a **simple decorator function that calls other focused functions**:

```javascript
/**
 * Section: Carousel
 * Wraps child blocks in a carousel slider.
 *
 * Data attributes (from section-metadata):
 * - data-sectionidentifier: "section-carousel"
 * - data-autoplay: "true" | "false"
 * - data-slideduration: milliseconds (default: 5000)
 */

function buildTrack(sectionEl, blocks) {
  // Build carousel track from child blocks
}

function buildNav(blocks) {
  // Build navigation dots
}

function addCarouselEvents(track, nav, blocks, config) {
  // Add click/auto-play event handlers
}

export default function decorate(sectionEl) {
  const autoPlay = sectionEl.dataset.autoplay === 'true';
  const duration = parseInt(sectionEl.dataset.slideduration, 10) || 5000;

  const blocks = [...sectionEl.querySelectorAll(':scope > div > .block')];
  if (blocks.length < 2) return;

  const track = buildTrack(sectionEl, blocks);
  const nav = buildNav(blocks);

  sectionEl.textContent = '';
  sectionEl.appendChild(track);
  sectionEl.appendChild(nav);
  sectionEl.classList.add('section-carousel-ready');

  addCarouselEvents(track, nav, blocks, { autoPlay, duration });
}
```

### Key Differences from Block JS:
- Receives the **section element**, not a block element
- Reads config from **`data-` attributes** (not row extraction)
- Operates on **child blocks**, not internal rows
- Uses `sectionEl.dataset.*` to access metadata
- Uses `classList.add()` instead of `className =`
- Simple decorator → calls focused functions (not extractConfig → buildBlock → appendEvents)

## Section CSS Pattern

```css
/* blocks/section-carousel/section-carousel.css */
.section-carousel-ready {
  position: relative;
  overflow: hidden;
}

.section-carousel-nav {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 1rem;
}

@media (min-width: 768px) {
  .section-carousel-nav {
    gap: 0.75rem;
  }
}
```

## `loadSectionModules()` in scripts.js

This function must exist in `scripts.js` and be called at the **end of `loadEager()`**:

```javascript
async function loadSectionModules(main) {
  const sections = main.querySelectorAll('.section[data-sectionidentifier]');
  for (const section of sections) {
    const name = section.dataset.sectionIdentifier;
    try {
      const cssLoaded = loadCSS(`${window.hlx.codeBasePath}/blocks/${name}/${name}.css`);
      const decorationComplete = new Promise((resolve) => {
        (async () => {
          try {
            const mod = await import(`${window.hlx.codeBasePath}/blocks/${name}/${name}.js`);
            if (mod.default) await mod.default(section);
          } catch (error) {
            console.error(`failed to load section module for ${name}`, error);
          }
          resolve();
        })();
      });
      await Promise.all([cssLoaded, decorationComplete]);
    } catch (error) {
      console.error(`failed to load section ${name}`, error);
    }
  }
}
```

## Section Creation Checklist

1. **Ask**: Is this truly a section (spans multiple blocks) or a block (self-contained)?
2. **Create JSON** in `models/_section-name.json` with:
   - Section resourceType
   - Hidden `sectionIdentifier` field
   - Section-specific config fields
3. **Create JS** in `blocks/section-name/section-name.js`
   - Simple decorator → calls focused functions
   - Receives `sectionEl`, reads from `dataset`
4. **Create CSS** in `blocks/section-name/section-name.css`
5. **Do NOT** create `_section-name.json` in `blocks/section-name/`
6. **Ensure** `loadSectionModules()` exists in `scripts.js` and is called at end of `loadEager()`
7. **Run `npm run lint`** to validate code
8. **Run `npm run build:json`** to rebuild component JSON files
9. **Test** with multiple child blocks in the section

## Reference
- Discovery workflow: **`28-new-component-discovery.mdc`**
- Sections guide: `.cursor/knowledge/analysis/SECTIONS_GUIDE.md`
- Official docs: https://www.aem.live/developer/markup-sections-blocks
- Adobe field types: https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/universal-editor/field-types#component-types

---

## 17-code-formatting

> Source: .cursor/rules/17-code-formatting.mdc

# Code Formatting (Cursor / VS Code)

## Line endings (LF only)
The repo uses **Unix line endings (LF)** everywhere for text files. **CRLF causes ESLint `linebreak-style` errors.**

- **`.gitattributes`** — `* text=auto eol=lf` so Git checks out and stores LF consistently (see binary overrides there).
- **`.editorconfig`** — `end_of_line = lf`.
- **`.prettierrc.json`** — `"endOfLine": "lf"` (Prettier rewrites to LF on format).
- **`.vscode/settings.json`** — `"files.eol": "\n"`.
- **ESLint** — `'linebreak-style': ['error', 'unix']` in `.eslintrc.js`.

After cloning on Windows, if old CRLF files linger, run **`npm run format`** once, then optionally **`git add --renormalize .`** to align the index with `.gitattributes`.

## Source of truth
- **Prettier** — `.prettierrc.json` (100 cols, 2 spaces, single quotes JS, LF, semicolons, `trailingComma: es5`).
- **ESLint** — style + quality; **`eslint-config-prettier`** is last in `extends` so it does not fight Prettier.
- **Stylelint** — CSS quality; **`npm run lint:css`** only targets `blocks/**/*.css` and `styles/*.css`. **`.stylelintignore`** includes **`.cursor/`** so reference CSS is skipped when **`lint-staged`** runs **`stylelint --fix`** on staged `*.css`. **`lint-staged`** uses **`--allow-empty-input`** so a commit that only touches ignored CSS still exits 0.

## Commands
- `npm run format` — write: format the whole repo (honors `.prettierignore`).
- `npm run format:check` — read-only check (use in CI).
- `npm run lint` / `npm run lint:fix` — ESLint + Stylelint.

## Ignored by Prettier
See `.prettierignore`: `node_modules`, `**/*.min.js`, merged `component-*.json`, `.cursor/`, lockfiles, etc.

## ESLint and merged UE JSON
**`component-models.json`**, **`component-definition.json`**, and **`component-filters.json`** are **linted** by ESLint (they are **not** in `.eslintignore`). **`xwalk/max-cells`** is turned **off** only for those three merged files via **`.eslintrc.js` `overrides`**, because the rule targets single-block models while the merge output is a catalog.

## Ignored by ESLint
See `.eslintignore`: **`.cursor/`**, vendor/minified paths, etc.

## Ignored by Stylelint
See **`.stylelintignore`**: **`.cursor/`** (same reason — `lint-staged` passes every staged `*.css`; without this, reference CSS under `.cursor/` can fail rules and block commits).

## Git commit
`.husky/pre-commit.mjs` runs **`npx lint-staged`** first, then regenerates merged component JSON when `_*` partials are staged.

## Editor (Cursor / VS Code)
- Workspace **`.vscode/settings.json`**: `editor.formatOnSave: true`, **`files.eol`: `\n`**, default formatter **Prettier**.
- **`.vscode/extensions.json`**: recommends Prettier, ESLint, Stylelint — accept prompts to install.

Do not hand-format in ways that contradict `.prettierrc.json`; run `npm run format` before large merges if needed.

---

## 18-typography-font-utilities

> Source: .cursor/rules/18-typography-font-utilities.mdc

# Typography — local files, variables, utilities

## Source of truth
- **`styles/fonts.css`** — `@font-face` only. **No CDN.** Binary files live under **`/fonts/`** with the **`eds-*`** filenames referenced there.
- **`styles/styles.css`** — `:root` **font variables** (`--font-family-heading`, `--font-family-body`, `--font-family-ui`, weights). Adjust these to change typography globally.
- **`styles/font-utilities.css`** — utility classes use **`var(--font-family-…)`** so they follow `:root`. Loaded via **`styles/styles.css`**.
- **`head.html`** — Do not link font CDNs. The main stylesheet pulls in **`fonts.css`** through **`@import`**.

When block **CSS** (not utilities on JS-built nodes) sets **`font-size`** or **`font-family`**, use the same **`:root`** tokens: **`--heading-font-size-*`**, **`--body-font-size-*`**, **`--heading-font-family`**, **`--font-family-*`** from **`styles/styles.css`** — see **`32-block-root-tokens.mdc`**.

## Roles (logical, not vendor names)
| Role | Typical use |
|------|----------------|
| **Heading** | `h1`–`h6`, `.u-font-title` |
| **Body** | `.u-font-body`, `body`, paragraphs |
| **Body bold** | `strong`/`b`, `.u-font-body-bold` |
| **UI accent** | `.u-font-ui`, `.u-font-ui-semibold` when a block needs a secondary text style |

## Utility classes
| Class | Variables |
|-------|-----------|
| **`u-font-title`** | `--font-family-heading`, `--font-weight-heading` |
| **`u-font-body`** | `--font-family-body`, `--font-weight-body` |
| **`u-font-body-bold`** | `--font-family-body`, `--font-weight-body-bold` |
| **`u-font-ui`** | `--font-family-ui`, `--font-weight-ui` |
| **`u-font-ui-semibold`** | `--font-family-ui`, `--font-weight-ui-semibold` |

Apply via **`classList.add(...)`** in block JS when you **create or own** the node. Do not duplicate `font-family` stacks in block CSS for those nodes.

## Universal Editor / AEM output (strict)

On the **block root**, the runtime typically applies only:

- the **block name** class (e.g. `option-selector`), **`block`**, and
- classes from authored **`classes`** / **`classes_*`** fields.

**Do not** expect **`u-font-*`** (or other typography utilities) on AEM-generated inner markup. **Add font utilities in `decorate()` / `buildBlock()`** via `classList.add(...)` on elements your JS builds.

## Block CSS
Keep size, color, and spacing in **`blocks/*/*.css`**; font **families** for JS-built nodes come from **`u-font-*`** added in JS, not from assumptions about raw UE HTML.

## Local HTML demos (`*-example.html`)
Match **`head.html`**: `aem.js`, `scripts.js`, `/styles/styles.css` only. **Do not** put **`u-font-*`** in example markup to simulate production — utilities belong on nodes created in JS.

## Reference
`styles/fonts.css`, `styles/styles.css`, `styles/font-utilities.css`

---

## 19-bus-actionable-elements

> Source: .cursor/rules/19-bus-actionable-elements.mdc

# Event bus and actionable elements

## Global bus
- **`window.Bus`** is a [mitt](https://github.com/developit/mitt) instance created **at the very start** of **`loadEager`** in **`scripts/scripts.js`** (before `document.documentElement.lang`, `decorateTemplateAndTheme`, or `decorateMain`).
- Event name constants live in **`scripts/events.js`** (default export **`EDS_EVENTS`**). Import that file in blocks; do not scatter magic strings. Example: **`EDS_EVENTS.CTA_CLICKED`** (`eds:cta-clicked`) from the **CTA** block when authors activate the control.

## Actionable UI (blocks)
For any **user-activated** control (click, Enter/Space on a custom widget, primary CTA that drives cross-block behavior):

1. **DOM `id`** — When the block’s model defines an **`id`** for an interactive element, set **`element.id`** to the **author value as-is** (trim whitespace only if you normalize at all). **Do not** auto-generate, slugify, or uniquify ids unless that block’s spec **explicitly** requires it. Empty author id → **leave the attribute unset** (do not fabricate fallbacks).
2. **Block root** — The block container may use **row 0** **`id`** so listeners filter on **`detail.blockId`**. Set **`block.id`** only from that field when the author provided a value (**as-is**); otherwise omit. The **`classes`** marker (`eds-block-<kebab-name>`) is **not a row** — it only appears on the root `classList`; the bus payload’s **`blockType`** is still the **logical block name** (e.g. `option-selector`).
3. **Bus emit** — On successful activation (after toggling `aria-selected` / state), call **`window.Bus?.emit(EDS_EVENTS.<NAME>, detail)`** with a plain object payload. Include **`blockType`** and any fields your block documents (e.g. **`optionId`**, **`blockId`**, **`behaviorId`**, **`href`**). Use **author-supplied `id` values as-is** on the DOM when present, and **omit** payload keys when the author left the id empty (unless the block spec says otherwise).

## Listening
```javascript
import EDS_EVENTS from '../../scripts/events.js';

window.Bus?.on(EDS_EVENTS.OPTION_SELECTED, (detail) => {
  if (detail.blockId && detail.blockId !== 'my-block-id') return;
  if (detail.optionId === 'care-screen-1y') { /* ... */ }
});
```
Use **`off`** or a single handler with internal guards when blocks are re-decorated (rare on EDS; still avoid duplicate anonymous listeners if `decorate` can run twice).

## Vendored mitt
- Runtime file: **`scripts/vendor/mitt.min.js`** (not imported from `node_modules` in the browser).

## No analytics
Bus events replace ad-hoc `console.log` and must not add analytics attributes or `dataLayer` usage (see **`06-no-analytics.mdc`**).

---

## 20-cms-content-flexibility

> Source: .cursor/rules/20-cms-content-flexibility.mdc

# CMS content vs screenshots

## Expectation
Authors control copy length, optional fields, and number of list/cards in Universal Editor. **A screenshot or design comp is a reference, not a fixed contract.**

## Rules for implementation
1. **Variable length** — Assume headings, body, labels, and CTAs may be **shorter or longer** than the mock. Avoid fixed heights that clip text; prefer `min-height`, flexible grids, wrapping, and `overflow` only where intentional (e.g. line clamps with a documented max).
2. **Empty / partial content** — Blocks MUST degrade gracefully when optional rows or RTE fields are empty (no broken layout, no giant gaps unless spacing tokens allow).
3. **More items than the design** — Lists, cards, tabs, and multifields may have **more** entries than shown in a screenshot. Layouts should scroll, wrap, or stack per breakpoints — not assume a fixed count.
4. **Fewer items** — Single-card or single-option states must still look intentional (alignment, spacing).
5. **Do not hard-code demo copy** into CSS or JS as if it were the only case; document row semantics in JSDoc / README, not literal string lengths from a comp.

When a user attaches a screenshot, treat it as **visual intent** and confirm overflow, empty states, and max-content cases explicitly if the layout is tight.

---

## 21-pull-request

> Source: .cursor/rules/21-pull-request.mdc

# Pull request (GitHub)

When the user asks to **create a PR**, types **create PR**, or asks to **draft**, **open**, **write**, or **generate** a **pull request** / **merge request** / **PR description**, you **must**:

1. **Compose** a **title** and **description** using the sections below (include **Test URL** once).
2. **Open the PR with the GitHub CLI** (`gh`) when the environment allows — do **not** stop at paste-only Markdown unless `gh` is unavailable or the user explicitly asked for text only.

## Open the PR with `gh`

1. Run from the **repository root** (where `.git` exists).
2. **Auth:** If `gh auth status` fails, tell the user to run `gh auth login`; still provide title + body for manual use.
3. **Push:** If the branch is not on the remote, run `git push -u origin HEAD` (after confirming with the user if pushing is sensitive).
4. **Body:** Write the full Markdown description to a **temporary file** (e.g. `pr-body.md` in repo root). Use **`gh pr create --title "…" --body-file pr-body.md`**. Remove **`pr-body.md`** after success (do not commit it). If a temp file outside the repo is easier on the user’s shell, use that path instead.
5. **Command:**
   ```bash
   gh pr create --title "YOUR_TITLE_HERE" --body-file pr-body.md
   ```
   Use **`--draft`** if the user wants a draft PR. Use **`--base <branch>`** only if the default base is not `main` / not what they want.

If **`gh`** is not installed, say so, link https://cli.github.com/, output title + body for the GitHub web UI, and stop.

**Branch name:** Use `git branch --show-current` when needed. Use that value for `<branch-slug>` in the Test URL (see below).

## Title

- One line, concise (roughly **50–72 characters** when possible).
- Describe the **outcome** (what ships), not the process.

## Description body (what goes in `--body-file`)

Use these sections (Markdown headings recommended).

### Summary

- **2–4 sentences**: what this PR does and **why**.

### What changed

- Bullet list of **modifications** to existing behavior, refactors, config, fixes.

### What was added

- Bullet list of **new** files, blocks, rules, scripts, or capabilities (if none, write “None.”).

### Features

- Bullet list for **authors**, **reviewers**, or **end users**: what they can **do** or **see** after merge.

### Related issues

- `Fixes #<id>` or `Relates to #<id>`; placeholder if unknown.

### Test URL

Include **one** preview link only (this branch’s AEM preview). No “before/after” or **main** comparison row.

Pattern:

```text
https://<branch-slug>--aem-genui-eds--thebarbariangroup.aem.live/
```

**`<branch-slug>`:** Git branch name with every **`/`** replaced by **`-`** (no `/` in that segment). Preserve other characters and casing (e.g. `Feature/foo` → `Feature-foo`).

Example line in the body:

```markdown
**Test URL:** https://feature-option-bus--aem-genui-eds--thebarbariangroup.aem.live/
```

Do **not** change **`aem-genui-eds`** or **`thebarbariangroup`** unless the user explicitly gives another repo or org.

## Optional in the body

- **Breaking changes**, **migrations**, or **follow-up** if relevant.

---

## 22-repeatable-parent-child-blocks

> Source: .cursor/rules/22-repeatable-parent-child-blocks.mdc

# Repeatable components → parent block + child block

## When this applies

For any **screenshot** or **written requirement** that includes **repeatable** UI (lists of cards, rows, tiles, items that share a pattern), **do not** model the repeat as a single block with many multifields unless the product explicitly asks for that.

Instead, treat it as **two block types**:

1. **Parent block** — Wraps the **whole** pattern: shared chrome, layout, and any **non-repeated** content.
2. **Child block** — The **repeated** unit; authors add **one child block instance per repeat**.

## Filters vs JavaScript (default)

**Authoring only:** Listing another block’s filter `id` in this block’s **`filters[].components`** tells Universal Editor **where authors may place** that block (e.g. `simple-text` inside `simple-title`). **No change to this block’s JavaScript is required for that.**

**Default contract:** Implement the normal **`extractConfig` → `buildBlock` → `appendEvents`** flow for **this block’s own model rows** only — same as any block that does not compose children.

**When the parent’s JS must integrate children:** Only when the design **requires** the parent to **read, reorder, replace, or coordinate** child block DOM (e.g. `option-selector` building a grid shell and selection behavior around `option-selector-item` children). Do **not** add partition logic, child preservation, or “parent block” handling **solely** because `components` lists nested block types.

### Loading nested children (`decorateBlock` + `loadBlock`)

Nested child roots **do not** receive the initial **`decorateBlocks` → `loadBlock`** pass from **`scripts/aem.js`** (that pass only targets **`.section > .section-inner > div > div`**). **`checkAndHandleNestedBlocks`** handles Universal Editor **marker rows** when a direct child’s **`innerHTML`** includes **`eds-block-`**; **composed** nested markup (classes on the nested root, **no** marker substring inside **`child.innerHTML`**) may **skip** that pass.

**When nested children must run their own `decorate()`** (typical for parent + child lists): the parent **`decorate()`** must **`await`** a small helper **after** **`checkAndHandleNestedBlocks`** and **before** **`extractConfig` / `buildBlock`**, which for each nested root calls **`decorateBlock(root)`** and **`await loadBlock(root)`** from **`scripts/aem.js`**, guarding with **`dataset.blockStatus`** so **`loadBlock`** is not invoked twice. **Canonical pattern:** **`blocks/magic/magic.js`** — **`ensureNestedBlocksReady`** (and **`nestedRowsLookComposed`** when skipping the marker pass). Full write-up: **`02-block-javascript-pattern.mdc`** (*Nested child blocks — decorate pipeline*).

Parent **`*-example.html`** that inlines nested child blocks **depends** on this parent JS path; otherwise nested rows stay raw in local demos (**`05-html-example-pattern.mdc`**).

---

## Configuration split

| Concern | Lives on |
|--------|----------|
| Shared title, intro, legal/footnote **below** the list, **group selection mode** (`classes_selection` / … on parent), spacing of the **overall** module | **Parent** model |
| Fields that **repeat per item** (labels, prices, per-row ids, per-row media) and **per-item width / layout** (`classes_itemWidth`, `classes_layout`, …) | **Child** model |
| **`classes` / `classes_*`** for the **whole** component (block marker, selection mode, footer chrome, …) | **Parent** model — values merge onto the **block root `class`**, **no HTML row** |
| **`classes` / `classes_*`** for **one repeated item** (marker, layout, width, tones, …) | **Child** model — same: **classes only**, **no row** |

## Filters (required for parent)

The **parent** block’s **`filters`** entry **must allow the child block** to be placed inside the parent. In `_parent-block.json`:

- Set **`filters`** so the parent’s `"components"` array **includes the child block’s filter `id`** (the same string as the child’s `id` in its `filters` array — typically the **kebab-case block name**).

Example shape:

```json
{
  "filters": [
    {
      "id": "my-list",
      "components": ["my-list-item"]
    }
  ]
}
```

The **child** block usually keeps **`"components": []`** (or stricter rules only if product requires it).

Without listing the child on the parent filter, authors may be unable to insert the repeated block where the design expects.

## AEM-generated HTML (nested vs top-level)

- **`classes` and every `classes_*` field** — merged onto the block root as CSS classes; **they do not add `<div>` rows**. Row indices in **`extractConfig` / `extractMeta`** count only **row-producing** fields (`id`, richtext, links, references, …), in model order after skipping `tab` and any `classes` / `classes_*` entry.
- A block sitting **directly under the section** (after Franklin section decoration) follows the usual **top-level** table: `div.<block-name>` with rows of **`div` (row) → `div` (cell)** for those fields only.
- A **nested** child block is still a **block**, but AEM typically emits **one fewer wrapper per authored field row** inside that child: each row is often a **single** `div` whose content is the cell (richtext, text, link, image, etc.), **not** `div > div` for every row. **`extractConfig` must use `getHtmlFromRow` / `getTextFromBlockRow`** from `block-helpers.js` (or equivalent logic) so both top-level and nested shapes work.
- The child still lives **inside** the parent’s table (e.g. inside a cell). **`decorateNestedBlock`** may wrap each child row in `.block-child-wrapper`; unwrap that first when iterating rows.
- **`block-name-example.html`** for the **child** must document the **nested** row shape when authors nest that child (one wrapper per row vs top-level). If the demo loads the **child alone**, match the shape your **`extractConfig`** expects.
- **Parents that compose children** (e.g. `option-selector`): the parent example should show **where** child roots appear and any **footer/meta** row order; parent JS may use **`partition`** / similar **only** because that design requires it — not as the default for every parent filter.

### Strict rule (nested child blocks — row extraction)

1. **Mandatory when a block can be nested:** For **every** block type that authors may place **inside** another block (filters / UE nesting), **`extractConfig` MUST use `getHtmlFromRow` / `getTextFromBlockRow`** (and **`getBooleanFromRow`**, which reads text via the same nested rules) for **text / richtext / boolean** rows — not only `getHtmlFromRow` / `getTextFromRow`. **Assume one fewer `div` per authored field row** than the section-level Franklin table (`row > cell`), unless you have proof your pipeline always emits the extra cell wrapper.
2. **Parent `*-example.html` files** (e.g. **Magic**) that demo nested children MUST show the **nested** row shape for those children — **not** the top-level `div > div` per row pattern — so the file matches production UE output and the child `extractConfig` contract.
3. **Never** read appearance-only variants from those rows — use **`classes` / `classes_*`** on the block root and **`block.classList`** in JS.
4. If a parent **rebuilds** the DOM from authored rows, its **`decorate()`** must **not** leave raw row text after build. Use **`removeNonBlockChildRows`** / **`replaceBlockRowsPreservingNestedBlocks`** (see **`02-block-javascript-pattern.mdc`**) so **nested child `.block` roots are preserved** while authored field rows are cleared and the built shell is **prepended**.

## Documentation in code (reference blocks)

For **`option-selector`** + **`option-selector-item`**:

- **JS** — File-level summary; **`extractMeta` / `extractConfig`** document the **DOM row index → field** map (**excluding** `classes` / `classes_*`); **`buildBlock`** comments explain **each created element** (header regions, grid, footer, item surface); **`wireSelection`** explains listbox wiring and bus emit.
- **CSS** — Section comments group **shell vs grid vs footer modifiers** (parent) and **card chrome vs width spans vs layout variants** (child).

When adding new parent/child pairs, mirror this pattern so row order and DOM shape stay discoverable without opening JSON.

## Footnotes and trailing copy

If the design shows **copy after** the repeated items (footnote, disclaimer, badge):

- Model those fields on the **parent** (not on the child).
- Parent meta extraction (**`extractMeta`** or equivalent) must treat rows **after** the region that holds child instances (document the order in the parent’s JS header).

## Sections vs this pattern

If repeats are **heterogeneous** (mixed block types) or need **section-level** chrome (carousel, accordion shell), prefer a **section** (see **`15-sections-vs-blocks.mdc`**). Use **parent + child blocks** when the repeat is **one** child block type inside **one** parent shell.

## References

- **Filters:** `03-block-json-pattern.mdc` (Filters section), `02-JSON_CONFIGURATION.md`
- **Appearance / `classes`:** `04-block-css-pattern.mdc`
- **HTML examples:** `05-html-example-pattern.mdc`
- **Fundamentals:** `01-FUNDAMENTALS.md` (block nesting)

---

## 23-page-template-metadata

> Source: .cursor/rules/23-page-template-metadata.mdc

# Page templates and page metadata models (Universal Editor)

## Goal

Each **AEM page template** that needs its own **page properties** (metadata) in Universal Editor should map to a **distinct model id** on `<body>`: **`{TEMPLATE_NAME}-metadata`**, where **`TEMPLATE_NAME`** is the **template folder name** (last segment of the WCM template path).

UE often reads **`data-aue-model`** on **`body`** to choose the model. Delivery HTML may set **`data-aem-template`** correctly while **`data-aue-model`** stays **`page-metadata`**. **`scripts/editor-support.js`** fixes that on load when the rules below apply.

## `body` attributes

| Attribute | Example | Role |
|-----------|---------|------|
| **`data-aem-template`** | `/conf/my-site/settings/wcm/templates/pdp-page` | AEM path to the template; **source of truth** for template name. |
| **`data-aue-model`** | `pdp-page-metadata` | Must match a **`models[].id`** in **`component-models.json`** for page metadata fields. |

## Runtime sync (`editor-support.js`)

On **page load** (Universal Editor / preview using editor-support):

1. If **`body`** has **`data-aem-template`**, take the **last non-empty path segment** after splitting on **`/`** (handles trailing slashes).
2. If that segment **does not** contain the substring **`page`** (case-insensitive), **do nothing** (leave `data-aue-model` as delivered).
3. Otherwise set **`data-aue-model`** to **`{segment}-metadata`**.

Examples:

- `…/templates/pdp-page` → `data-aue-model="pdp-page-metadata"`
- `…/templates/home` → **no change** (segment has no `page`)

## JSON source files (`merge-json-cli`)

- **Default** site pages: **`models/_page.json`** — model **`id`** **`page-metadata`** (generic).
- **Template-specific** metadata: add **`models/_page-<TEMPLATE_NAME>.json`** (kebab-case folder name, e.g. `_page-pdp-page.json`).

Each file’s **`models[0].id`** **must** be **`"<TEMPLATE_NAME>-metadata"`** (same `TEMPLATE_NAME` as the WCM template folder name).

Register the partial in **`models/_component-models.json`** with a **`"...": "./_page-<TEMPLATE_NAME>.json#/models"`** entry (typically **after** **`./_page.json#/models`** so defaults exist, then overrides/additions per template).

Do **not** rename existing **`name`** values in page metadata partials or existing **`helix-query.yaml`** property keys unless the user explicitly asks (**`.cursor/rules/29-helix-query-page-metadata.mdc`**).

## Checklist

- [ ] WCM template folder name includes **`page`** if you want **editor-support** to rewrite **`data-aue-model`**.
- [ ] **`component-models.json`** merged output contains **`id`** **`{name}-metadata`** for that template.
- [ ] **`_page-<name>.json`** field **`name`** values follow project conventions (avoid duplicate `id`s across models if merging).

## Reference

- Implementation: `scripts/editor-support.js` (`syncBodyPageMetadataModelFromTemplate`)
- Example partial: `models/_page-pdp-page.json` (`id`: `pdp-page-metadata`)
- Merge root: `models/_component-models.json`

---

## 24-block-filters-component-list

> Source: .cursor/rules/24-block-filters-component-list.mdc

# Block filters — `component-list.json` (single source for section)

## Rule

When you **add a new block** (new `blocks/<name>/` with `_<name>.json`):

1. **Append the block’s filter `id`** to the `components` array in **`block-configs/component-list.json`**.  
   - The string must match **`filters[].id`** in that block’s `_<name>.json` (same kebab-case as the folder, e.g. `spacer`, `product-carousel`).

2. **Do not** extend the section’s allowed-components list by hand inside **`models/_section.json`**.  
   The section filter already merges that list from **`component-list.json`**:

```154:159:models/_section.json
  "filters": [
    {
      "id": "section",
      "...": "../block-configs/component-list.json"
    }
  ]
```

So updating **`block-configs/component-list.json`** is what makes the block **insertable in a section** after merge / publish.

## Also required (unchanged)

- The block’s own **`filters`** array in **`blocks/<name>/_<name>.json`** (e.g. `{ "id": "<name>", "components": [] }`) still defines **placement rules** for that block type.
- Regenerate merged output when your workflow expects it: **`npm run build:json:filters`** (and related **`build:json`** scripts) so root **`component-filters.json`** stays in sync.

## See also

- **`08-block-creation-checklist.mdc`** — Step 2 + Step 7 + Step 8 (mandatory **`component-list.json`** + **`build:json`**)  
- **`03-block-json-pattern.mdc`** — **Filters Section** → *Section palette*  
- **`02-block-javascript-pattern.mdc`** — *New block — section palette*

---

## 25-json-field-descriptions

> Source: .cursor/rules/25-json-field-descriptions.mdc

# JSON field `description` (Universal Editor / authors)

`description` on model fields is **for content authors** in the properties panel. It is **not** the place for implementation notes (HTML shape, rows, merge, `classes_*`, field collapse, Franklin, CSS, or bus events).

## Do

- Use **plain language**: what to enter and **why it matters** for the page or accessibility.
- Add a description only when it **adds something beyond the label** (edge cases, conditional behavior authors need to know, or accessibility).

## Don’t

- Repeat the **label** in different words when the label is already clear (e.g. “Text” / “Link” / “Border color”).
- Use **developer terms** unless authors are expected to know them.

## Hidden / system fields

Fields with **`hidden`: true** (e.g. **`classes`** block marker) usually **do not need** a `description` in JSON — authors do not see them. Document technical behavior in **`.cursor/rules`**, **`README.md`** (developers), or **row maps** in **`*-example.html`**, not in `description`.

## See also

- **`03-block-json-pattern.mdc`** — model structure and mandatory fields  
- **`08-block-creation-checklist.mdc`** — block checklist

---

## 26-example-html-sync

> Source: .cursor/rules/26-example-html-sync.mdc

# Keep `*-example.html` in sync with blocks and shared styles

When you introduce or change **meaningful** authoring or rendering surface area, update the **living HTML demos** so they still match what authors and `decorate()` expect.

## When to update examples

Do **not** skip sample HTML updates when you:

- Add or remove **`classes_*` / `classes`** options, or change their **values**
- Add/remove **row-producing** fields or change **richtext** / structure contracts
- Change **defaults** in **`definitions` → `template`** that affect the merged block root
- Introduce **shared utilities** (e.g. new `u-*` / `as-*` / `section-*` classes) that blocks are expected to use — demos should show at least one **exhaustive** pass per option (or a documented matrix in an HTML comment when the Cartesian product is huge)
- Change behavior that **cross-cuts** multiple blocks (section utilities, RTE config) and existing demos should reflect the new story

Small refactors with **no** author-visible or DOM-contract change do **not** require example churn.

## What to edit

- The block’s **`blocks/<folder>/<folder>-example.html`** — maintain **exhaustive `classes_*` coverage** per **`05-html-example-pattern.mdc`**
- Any **other** `*-example.html` that **documents** the same pattern (if the change is global)
- **`npm run build:json:models`** when **`models/`** or block **`_*.json`** models feed **`component-models.json`**
- **`npm run lint`** before you finish

## References

- **`05-html-example-pattern.mdc`** — variant labels, `main > div` per sample, gradholder (**distinct** `hexFrom`/`hexTo` per placeholder, varied across samples), **`videos/`** for video demos, section utilities
- **`08-block-creation-checklist.mdc`** — Step 5 (example file)

---

## 27-classes-boolean-fields

> Source: .cursor/rules/27-classes-boolean-fields.mdc

# `classes_*` boolean fields (toggle → block root class)

## Naming

- Field **`name`** must start with **`classes_`**.
- The **CSS class** merged onto the **block root** when the toggle is **on** is the substring **after** `classes_`, **verbatim** (typically **kebab-case**).
- Do **not** use **camelCase** in the suffix. Prefer kebab segments, e.g. `classes_fade-show-cta` → class `fade-show-cta`, `classes_magic-scroll-reveal` → `magic-scroll-reveal`.

## Behavior

- **`component`: `boolean`**, **`valueType`: `boolean`**. When **true**, Universal Editor / Franklin merge the derived class on the block root; when **false**, that class is omitted.
- These fields **do not** produce a Franklin **table row** (same as other `classes_*` grouped fields). Read styling in JS from **`block.classList`**, not from `readBlockConfig` rows.

## Contrast: non-`classes_` booleans

- A **`boolean`** whose **`name` does not start with `classes_`** is a normal content/behavior toggle. It **creates a block row** whose cell value is the string **`true`** or **`false`** (see `getBooleanFromRow` in `scripts/utilities/block-helpers.js` when reading rows).

## References

- **`03-block-json-pattern.mdc`** — `classes` / `classes_*` and field types table
- **`10-json-advanced-patterns.mdc`** — element grouping

---

## 28-new-component-discovery

> Source: .cursor/rules/28-new-component-discovery.mdc

# New blocks and sections — discovery before building

When the user asks to **create a new block**, **new section**, or describes UI from a **prompt or screenshot** (or similar), treat it as a **discovery** problem first, not an automatic greenfield build.

## Always do first

1. **Analyze** what the layout or behavior actually needs (structure, repeatability, cross-block behavior, styling only, etc.).
2. **Ask clarifying questions** until intent is clear (content model, breakpoints, states, accessibility, where it lives in the page tree). Do **not** assume missing details. **Also infer interaction from the UI type** and confirm with the user before coding — examples:
   - A **keyboard shortcut** list: display-only / copy-to-clipboard / **must trigger in-app actions** (then **`window.Bus`** + **`scripts/events.js`** per **`19-bus-actionable-elements.mdc`**)?
   - **CTAs, toggles, filters:** who reacts (this block only, other blocks, navigation)?
   - **Empty, partial, or overflow content:** required degradations (see **`20-cms-content-flexibility.mdc`**).
   - **Authoring:** who maintains copy, how often items repeat, any legal or locale constraints.
3. **Infer → question → wait** — use reasonable product intuition to **draft** questions the user may not have thought of; **do not** ship interaction (Bus emits, `fetch`, storage, etc.) until answers exist or the user explicitly defers (“display-only for now”).
4. **Search this repo** for an existing solution:
   - **Blocks:** `blocks/*/` (custom implementations shipped with the project).
   - **Section shell / layout:** `models/_section.json`, `styles/section-utilities.css`, `scripts/aem.js` (appearance utilities, optional grid layout).
   - **Other section models:** `models/_*.json` beyond `_section.json` if present.
5. If something **can** meet the need by **composing** existing sections + blocks (or section utilities + blocks), **say so** and outline the composition. **Do not** force-fit: if the design truly needs a new block or section, recommend building it.
6. **Do not** wire new blocks or sections to the repo’s **`api/`** JSON as a dependency — see **`31-api-folder-off-limits.mdc`**.

## When suggesting components by name

- **Prefer custom blocks in `blocks/`** (and this project’s **Section** model capabilities) when recommending what authors should use.
- **Do not** default to suggesting **reference-only** examples under `.cursor/knowledge/reference-blocks/` or **generic xwalk / boilerplate** block names **unless**:
  - there is **no** custom block in this repo that fits, **and**
  - a **stock** pattern is a reasonable match for the requirement.

Reference / boilerplate material is for **patterns and JSON shape**, not the primary catalog of what **this site** ships.

## When to still create something new

- The behavior or DOM contract is **not** achievable with existing blocks/sections (after honest evaluation).
- Composition would be **fragile**, **author-hostile**, or **off-brand** compared to a dedicated component.
- The user **explicitly** wants a new block or section after trade-offs are explained.

## File scope when implementing a new block

After discovery answers exist, **change only what the block needs**. Do **not** edit unrelated packages, shared scripts, or “drive-by” fixes (lint, typos, refactors) outside the new block’s surface — see **`08-block-creation-checklist.mdc`** (*Scope of file changes*). Allowed touch points are typically **`blocks/<block-name>/`**, **`block-configs/component-list.json`** when the block must appear in the section palette, and regenerating merged **`component-*.json`** via **`npm run build:json`** when your workflow requires it.

## References

- **Sections vs blocks:** `.cursor/rules/15-sections-vs-blocks.mdc`
- **Block checklist (after discovery):** `.cursor/rules/08-block-creation-checklist.mdc`
- **Section pattern:** `.cursor/rules/16-section-creation-pattern.mdc`
- **Discovery workflow (doc):** `.cursor/knowledge/documentation/02-JSON_CONFIGURATION.md` — *New blocks and sections (discovery)*
- **`api/` off limits for blocks/sections:** `.cursor/rules/31-api-folder-off-limits.mdc`

---

## 29-helix-query-page-metadata

> Source: .cursor/rules/29-helix-query-page-metadata.mdc

# Page metadata (`_page.json`) and `helix-query.yaml` — preserve existing keys

## Do not rename or rewire by default

Unless the user **explicitly** asks to rename fields, migrate content, or change Helix selectors:

1. **`models/_page.json`** and **`models/_page-*.json`** — do **not** change existing field **`name`** values (they are tied to stored page metadata and delivery).
2. **`helix-query.yaml`** — do **not** rename existing **`indices.*.properties`** keys or change **`select`** expressions for those keys (downstream tools, indexes, and docs may rely on them).

## Safe changes when the user requests a feature

- **Add** a new property under **`indices.*.properties`** or **add** a new page model field **only when asked**, and prefer **new** keys rather than renaming old ones.
- If the user wants “alignment” between Helix and page models, **call out** that renaming breaks existing content and **get explicit confirmation** before editing **`name`** or property keys.

## Runtime note

**`getMetadata()`** in **`scripts/aem.js`** matches **`meta`** **`name`** / **`property`** **case-insensitively** in the DOM; that does **not** require changing model field names.

## Reference

- **`.cursor/rules/23-page-template-metadata.mdc`** — template-specific page models
- **`.cursor/knowledge/documentation/02-JSON_CONFIGURATION.md`** — page metadata models

---

## 30-block-css-design-tokens

> Source: .cursor/rules/30-block-css-design-tokens.mdc

# Block CSS — global design tokens

When **creating or editing** **`blocks/**/*.css`**, use **CSS custom properties** from **`styles/styles.css`** (which imports **`styles/colors.css`**) wherever they match the design. **Do not** reintroduce old “no `var()` in blocks” guidance.

## Source of truth

| Concern | Define literals / scale in | Consume in block CSS with |
|--------|----------------------------|---------------------------|
| **Colors** | **`styles/colors.css`** (`:root`, `[data-theme="dark"]`) | **`var(--color-*)`**, legacy **`--link-*`** etc., **`color-mix(..., var(--color-*) …)`** |
| **Spacing** | **`styles/styles.css`** `:root` (`--space-*`, section spacing) | **`var(--space-*)`** (and section tokens when appropriate) |
| **Font families & weights** | **`styles/styles.css`** `:root` | **`var(--font-family-heading|body|ui)`**, **`var(--font-weight-*)`**, **`var(--heading-font-family)`**, **`var(--body-font-family)`** |
| **Font sizes** | **`styles/styles.css`** `:root` (+ breakpoint overrides) | **`var(--body-font-size-m|s|xs)`**, **`var(--heading-font-size-xxl|xl|l|m|s|xs)`** |

**Typography utility classes** (**`u-font-*`**) are still applied in **block JS** on nodes you own — see **`18-typography-font-utilities.mdc`**. In block CSS, prefer **tokens** for **`font-size` / `font-family` / `font-weight`** when you are styling elements that do not carry those utilities or need explicit overrides.

## Colors (strict)

**Ink, borders, fills, shadows, outlines, icon tints** must **not** use raw **`#…` / `rgb()` / `hsl()`** in **`blocks/**/*.css`**.

### Workflow when you need a color

1. **Search `styles/colors.css`** for **`--color-*`** (or legacy text/link tokens) that match intent.
2. If no exact match, pick the **closest** token (borders, surfaces, text hierarchy, neutrals, accents — same mapping as before).
3. If still no fit, **add** **`--color-…`** in **`styles/colors.css`** (`:root` and **`[data-theme="dark"]`** when needed), then **`var(--…)`** in the block.

### Allowed without a color token

- **`transparent`**, **`currentColor`**, **`none`**
- **Non-color** values (lengths, angles, numbers) as literals when no token exists

## Spacing

Prefer **`var(--space-xs|s|m|l|xl)`** and the **section / page** spacing variables when the authored layout should track the global scale. Use literal **`rem` / `px`** only when there is no matching token.

## Typography

Prefer **`var(--body-font-size-*)`**, **`var(--heading-font-size-*)`**, **`var(--font-family-body)`** (or heading/ui), and **`var(--font-weight-*)`** so type tracks **`styles/styles.css`**. If a block needs a **new** semantic size, add it to **`:root`** in **`styles/styles.css`** (and media-query overrides if needed), then use **`var(...)`** in the block — avoid one-off pixel **`font-size`** in **`blocks/`** when a shared scale entry is appropriate.

## Anti-patterns

- Raw color literals in **`blocks/**/*.css`** for real UI colors
- Declaring **block-scoped** **`--my-block-*`** for colors or global type scales that should live under **`styles/`**
- “No CSS variables in blocks” (outdated)

## Reference

- **`styles/colors.css`**, **`styles/styles.css`**, **`styles/font-utilities.css`**
- **`04-block-css-pattern.mdc`**, **`18-typography-font-utilities.mdc`**
- Block checklist: **`08-block-creation-checklist.mdc`** (Step 4 — `my-block.css`)

---

## 30-local-json-fetch-fallback

> Source: .cursor/rules/30-local-json-fetch-fallback.mdc

# Local JSON fetch fallback

When a block or script **`fetch`es** JSON that only exists after Helix indexing (**`/query-index.json`**, **`/phones.json`**, etc.), use **`fetchJsonWithLocalFallback(url, fetchInit)`** from **`scripts/aem.js`** instead of raw **`fetch` + `res.json()`**.

1. On **`localhost`**, **`127.0.0.1`**, or **`[::1]`**, a failed primary request (network or non-OK status) retries **`/dev-mocks/<basename>`**, preserving any path prefix before the file (e.g. **`/eds/query-index.json`** → **`/eds/dev-mocks/query-index.json`**).
2. Add or edit files under **`dev-mocks/`** so the mock shape matches **`helix-query.yaml`**.
3. When introducing a **new** top-level JSON asset, add its **`basename`** to **`mockBasenames`** in **`resolveLocalJsonMockPath`** and commit a starter file under **`dev-mocks/`**.

Local static server: **`npm run serve`** (port **5500**). Use another port via **`npx live-server . --port=<port> --host=127.0.0.1 --no-browser`**.

---

## 31-api-folder-off-limits

> Source: .cursor/rules/31-api-folder-off-limits.mdc

# `api/` folder — off limits for blocks and sections

When **creating or editing blocks, sections, block JSON**, or **Universal Editor–driven page behavior**:

1. **Do not** import, `fetch`, or otherwise **depend on files under `api/`** (e.g. `api/customer-profiles.json`, `api/*.json`) from block JS, section JS, or merged component models.
2. **Do not** cite `api/` paths as the authoring or delivery contract for a block — runtime content for authors comes from AEM / Franklin paths, `sessionStorage`, page metadata, **`/dev-mocks/`** (local JSON fallback per **`30-local-json-fetch-fallback.mdc`**), or other **explicit** product-approved sources.
3. **`api/`** may still exist for **servers, tooling, or docs** outside the block pipeline; that does not make it a dependency surface for **block or section** implementations.

If a feature needs profile or catalog data at runtime, use **documented** delivery (metadata, content fragments, Express routes, mocks) — not ad hoc reads from **`api/`** in block code.

---

## 32-block-root-tokens

> Source: .cursor/rules/32-block-root-tokens.mdc

# Block CSS — shared `:root` tokens only

## Source files (single source of truth)

| Concern | File | Token prefix / names |
|---------|------|------------------------|
| **Spacing** | **`styles/styles.css`** `:root` | `--space-xs` … `--space-xl`, **`--space-128`**, `--space-page-*`, `--space-section-*` |
| **Colors** | **`styles/colors.css`** `:root` | `--color-*` (palette, text, borders, accents, CTA stops, shadows, semantic success/error, Samsung partner hues where listed) |
| **Font families & weights** | **`styles/styles.css`** `:root` | `--font-family-heading|body|ui`, `--font-weight-*`, aliases `--heading-font-family`, `--body-font-family` |
| **Font sizes** | **`styles/styles.css`** `:root` (+ **`@media (width >= 900px)`** overrides) | `--body-font-size-*`, `--heading-font-size-*` |

Blocks load after global CSS in normal pages; **`var(--token)`** in **`blocks/**/*.css`** resolves against these roots.

## Rules

1. **Prefer `var(--…)` from the tables above** for spacing, colors, type size, and type family whenever the intent matches the design system.
2. **Do not** add **`--my-block-*`** (or block-prefixed) custom properties for values that already have a global equivalent. **Do not** add new `:root` entries **unless** the user explicitly asks or the gap from the nearest token is **large** (different brand hue, new semantic like error/success, or a documented partner palette).
3. **Pick the closest existing token** when a mock used an off-scale number (e.g. **15px** gutter → **`--space-s`** **16px**). Reserve **new** `:root` names only when no token is within a reasonable step (e.g. partner **Samsung** blue vs product CTA blue → **`--color-brand-samsung-accent`** in **`colors.css`**).
4. **Literals are OK** when tokens do not apply: **0**, **1px** hairlines, **max-width** / **min-height** layout numbers, **line-height** unitless, **opacity**, **`z-index`**, **`border-radius`** quirks, **`clamp()`** middle `vw` terms, or **`calc()`** that mixes a token with a fixed offset (e.g. **`calc(var(--space-m) - 2px)`** only if unavoidable — prefer pure tokens first).
5. **Typography utilities** — for copy that should follow global heading/body roles, prefer **`u-font-*`** on nodes **built in JS** (**`18-typography-font-utilities.mdc`**). When block **CSS** must set **`font-size`** / **`font-family`** on third-party markup, still use **`--body-font-size-*`**, **`--heading-font-size-*`**, **`--font-family-*`**.

## New global tokens

Add to **`styles/colors.css`** or **`styles/styles.css`** only when:

- The value is **reused** or **should be reused** (semantic error, partner brand), or  
- The user **explicitly** requested a new name, or  
- No existing token is within a **reasonable** step and the design **requires** a new stop (document in a short comment next to the declaration).

## Reference

- **Magic** gap scale: **`blocks/magic/magic.css`** (`--magic-visual-body-gap` from **`--space-*`** / **`--space-128`**).
- **Spacer** scale: **`blocks/spacer/spacer.css`**.
- **`04-block-css-pattern.mdc`** — general block CSS; token **`var()`** policy is defined **here**.

---

## 33-programmatic-createBlock

> Source: .cursor/rules/33-programmatic-createBlock.mdc

# Programmatic `createBlock` (off-DOM HTML)

## Contract

Every **new custom block** in `blocks/<name>/` MUST export a **named** function:

```js
/**
 * @param {Record<string, unknown>} options Authoring-shaped values (ids, copy, hrefs, `classes`, etc.)
 * @returns {string} Outer HTML of one block root — Franklin / Universal Editor table row shape (`<div class="…">` + `<div><div>…</div></div>` rows), same semantics as the block’s `extractConfig` / row map.
 */
export function createBlock(options) {
  return '…';
}
```

- **No analytics** (see `06-no-analytics.mdc`).
- **Return value** is a **string** of HTML only; do not call `decorate` from `createBlock`.
- **Orchestrator** for authors of programmatic UI: `createBlock(blockName, options, wrapperClasses)` in `scripts/utilities/block-helpers.js` — dynamic-imports `blocks/<name>/<name>.js`, calls that module’s `createBlock(options)`, wraps markup, optionally resolves `./media_*` when `options.fragmentBasePath` or `options.mediaBasePath` is set (same rules as `loadFragment`), then runs `decorateMain` + `loadSections` on a wrapper `div` (with classes from the third argument).

## Shared helpers

Prefer `escapeHtml`, `escapeHtmlAttribute`, `coerceAuthorClasses`, and `franklinBlockRow` from `block-helpers.js` when building strings.

## Fragment parity

`setRootInnerHtmlAndResolveFragmentMediaPaths` in `block-helpers.js` mirrors `blocks/fragment/fragment.js` (assign `innerHTML`, rewrite `./media_*` on `img` / `source`). `loadFragment` calls that helper instead of duplicating the rewrite.

`decorateMain` lives in `scripts/decorate-main.js` (imported by `scripts.js` and `block-helpers.js`) so programmatic block creation does not create an import cycle with `scripts.js`.

---

## 34-create-block-agent-guide

> Source: .cursor/rules/34-create-block-agent-guide.mdc

# New / edited blocks — agent entry point

Before scaffolding or changing a block under `blocks/`, read:

1. **`.cursor/CREATE_AEM_BLOCK.md`** — quickstart + copy-paste templates  
2. **`.cursor/AEM_EDS_RULES.md`** — full consolidated rules (all former `.cursor/rules/*.mdc` sections)

Canonical simple example: `blocks/content/`. Classes-only: `blocks/spacer/`.

---

