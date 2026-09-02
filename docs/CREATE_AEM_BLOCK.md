# Create an AEM EDS Block — Agent Guide

> **Audience:** Claude, Antigravity, Cursor, and any LLM creating or editing blocks in this repo.  
> **Canonical in-repo example:** `blocks/content/` (simple content + appearance).  
> **Classes-only example:** `blocks/spacer/`.  
> **All project rules (single file):** [AEM_EDS_RULES.md](./AEM_EDS_RULES.md) — consolidated from `.cursor/rules/*.mdc`.

When the user asks to **create a block**, **add a component**, or **build a new EDS block**, follow this file end-to-end. For full standards, open **[AEM_EDS_RULES.md](./AEM_EDS_RULES.md)**. Do not invent alternate file layouts or analytics patterns.

---

## 0. Before writing code

1. **Ask clarifying questions** (layout, content fields, interactions, display-only vs Bus/`fetch`).
2. Search **`blocks/*/`** for an existing fit — prefer compose/reuse over inventing.
3. Decide **block vs section** ([AEM_EDS_RULES.md § 15-sections-vs-blocks](./AEM_EDS_RULES.md#15-sections-vs-blocks)).
4. Repeatable items → **parent + child** ([AEM_EDS_RULES.md § 22-repeatable-parent-child-blocks](./AEM_EDS_RULES.md#22-repeatable-parent-child-blocks)).
5. **Scope:** only `blocks/<name>/`, `block-configs/component-list.json`, and `npm run build:json` outputs. Do **not** touch `api/`, unrelated `scripts/`, or other blocks.

---

## 1. Required deliverables (every new block)

```
blocks/<block-name>/
  _<block-name>.json          # UE model (underscore prefix!)
  <block-name>.js             # decorate + createBlock
  <block-name>.css            # basic CSS only (no SCSS)
  <block-name>-example.html   # standalone demo (required)
  README.md                   # LLM “when to pick” + row map (required)
```

Also:

- Append filter **`id`** to **`block-configs/component-list.json`** → `components` (do **not** hand-edit `models/_section.json` filters).
- Run: `npm run lint` and `npm run build:json`.

**Hard project rules**

| Rule | Detail |
|------|--------|
| No analytics | Never import `dataLayer.js`, never `applyTracking`, no Analytics tab |
| Names | Folder = kebab-case; files match folder; JSON has `_` prefix |
| Classes | Hyphenated `block-name-element`; use `classList.add()` not `className =` |
| CSS tokens | `var(--color-*)`, `var(--space-*)`, type tokens — see [32-block-root-tokens](./AEM_EDS_RULES.md#32-block-root-tokens) |
| Rows | `[...block.children]` by index; skip `tab`, `classes`, `classes_*` |

---

## 2. How AEM turns fields into HTML

For each **row-producing** model field, AEM emits:

```html
<div><div>…field content…</div></div>
```

**Do not create rows:**

- `"component": "tab"` — editor UI only
- `"name": "classes"` or `"name": "classes_*"` — merged onto the **block root** as CSS classes

**Always open the model with:**

1. Optional `tab` (General)
2. **`id`** (`text`) — **row 0**
3. **`classes`** (`text`, `hidden: true`, `readOnly: true`, `value`: `eds-block-<kebab-name>`) — **no row**

Appearance / layout variants → **`classes_*` selects or booleans**, read in JS from `block.classList`, not from rows.

---

## 3. `_block-name.json` — copy-adapt template

Replace `my-block` / `My Block` throughout. Mirror structure from `blocks/content/_content.json`.

```json
{
  "definitions": [
    {
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
              "id": "",
              "classes": "eds-block-my-block",
              "title": "<p>Lorem ipsum dolor sit amet.</p>",
              "text": "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>",
              "classes_align": "",
              "classes_tone": "my-block-tone-default"
            }
          }
        }
      }
    }
  ],
  "models": [
    {
      "id": "my-block",
      "fields": [
        {
          "component": "tab",
          "label": "General",
          "name": "tabGeneral"
        },
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
        },
        {
          "component": "richtext",
          "name": "title",
          "label": "Title"
        },
        {
          "component": "richtext",
          "name": "text",
          "label": "Text"
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
          "component": "tab",
          "label": "Appearance",
          "name": "tabAppearance"
        },
        {
          "component": "select",
          "name": "classes_align",
          "label": "Alignment",
          "valueType": "string",
          "value": "",
          "options": [
            { "name": "Left", "value": "" },
            { "name": "Center", "value": "my-block-align-center" },
            { "name": "Right", "value": "my-block-align-right" }
          ]
        },
        {
          "component": "select",
          "name": "classes_tone",
          "label": "Tone",
          "valueType": "string",
          "value": "my-block-tone-default",
          "options": [
            { "name": "Default", "value": "my-block-tone-default" },
            { "name": "Emphasis", "value": "my-block-tone-emphasis" }
          ]
        }
      ]
    }
  ],
  "filters": [
    {
      "id": "my-block",
      "components": []
    }
  ]
}
```

### Field → row → helper

| JSON `component` | Creates row? | Read with |
|------------------|--------------|-----------|
| `text` | Yes | `getTextFromBlockRow(rows[N])` |
| `richtext` | Yes | `getHtmlFromRow(rows[N])` |
| `aem-content` | Yes | `getLinkFromRow` / anchor helper |
| `reference` | Yes | `getImageFromRow` / picture query |
| `boolean` (name **not** `classes_*`) | Yes | `getBooleanFromRow(rows[N])` |
| `boolean` named `classes_*` | **No** | class on root when `true` |
| `select` named `classes` / `classes_*` | **No** | `block.classList` |
| `tab` | **No** | — |

**Author `description`:** plain language only (no row/CSS jargon) — [25-json-field-descriptions](./AEM_EDS_RULES.md#25-json-field-descriptions).

**Parent that nests children:** put child filter ids in `filters[].components`.

### Row map for the template above

```
tabGeneral     → no row
id             → row 0
classes        → no row (root class eds-block-my-block)
title          → row 1
text           → row 2
ctaLink        → row 3
ctaContent     → row 4
tabAppearance  → no row
classes_align  → no row (root class)
classes_tone   → no row (root class)
```

---

## 4. `block-name.js` — mandatory pattern

Flow (use **`async`**):

1. `await checkAndHandleNestedBlocks(block)` (and nested `decorateBlock`/`loadBlock` when hosting children — see [02-block-javascript-pattern](./AEM_EDS_RULES.md#02-block-javascript-pattern))
2. `extractConfig(block)` — **before** removing rows
3. `buildBlock(block, config)` — set **`config.mainEl`**; preserve nested blocks (`replaceBlockRowsPreservingNestedBlocks` — **never** `block.textContent = ''`)
4. Optional async loaders that only mutate under `config.mainEl`
5. `appendEvents(config)`
6. Named export **`createBlock(options)`** returning Franklin-shaped HTML string ([33-programmatic-createBlock](./AEM_EDS_RULES.md#33-programmatic-createblock))

### Minimal working example (aligned with `blocks/content/content.js`)

```javascript
import {
  checkAndHandleNestedBlocks,
  replaceBlockRowsPreservingNestedBlocks,
  getTextFromBlockRow,
  getHtmlFromRow,
  coerceAuthorClasses,
  escapeHtml,
  escapeHtmlAttribute,
  franklinBlockRow,
} from '../../scripts/utilities/block-helpers.js';

/**
 * Row layout — tab / classes / classes_* create no rows.
 * 0: id
 * 1: text (richtext)
 */
function extractConfig(block) {
  if (!block) return {};
  const rows = [...block.children];
  return {
    id: getTextFromBlockRow(rows[0]),
    text: getHtmlFromRow(rows[1]),
  };
}

function buildBlock(block, config) {
  const inner = document.createElement('div');
  inner.classList.add('my-block-inner');
  if (config.text) {
    inner.innerHTML = config.text;
  }
  replaceBlockRowsPreservingNestedBlocks(block, inner);
  if (config.id) block.id = config.id;
  // eslint-disable-next-line no-param-reassign
  config.mainEl = inner;
}

function appendEvents(config) {
  if (!config?.mainEl) return;
  // Wire listeners here when needed
}

export default async function decorate(block) {
  await checkAndHandleNestedBlocks(block);
  const config = extractConfig(block);
  buildBlock(block, config);
  appendEvents(config);
}

/**
 * Programmatic Franklin / UE table markup (same row semantics as extractConfig).
 * @param {{ id?: string, text?: string, classes?: string|string[] }} [options]
 * @returns {string}
 */
export function createBlock(options = {}) {
  const id = escapeHtml(options.id ?? '');
  const text = typeof options.text === 'string' ? options.text : '';
  const extra = coerceAuthorClasses(options.classes);
  const rootClasses = ['my-block', 'eds-block-my-block', extra].filter(Boolean).join(' ');
  return `<div class="${escapeHtmlAttribute(rootClasses)}">${franklinBlockRow(id)}${franklinBlockRow(
    text
  )}</div>`;
}
```

**Prefer** `getHtmlFromRow` / `getTextFromBlockRow` (nested-safe). Use plain `getHtmlFromRow` / `getTextFromRow` only when the block is never nested.

**Classes-only blocks** (like `spacer`): decorate may only call `checkAndHandleNestedBlocks`; still export `createBlock`.

---

## 5. `block-name.css` — sketch

```css
/* my-block — mobile-first; tokens only */

.my-block {
  padding: var(--space-s);
  color: var(--color-base-text);
  background-color: var(--color-base-background);
}

.my-block-inner {
  display: flex;
  flex-direction: column;
  gap: var(--space-s);
}

.my-block.my-block-align-center {
  text-align: center;
}

.my-block.my-block-tone-emphasis {
  color: var(--color-accent-link);
}

@media (min-width: 768px) {
  .my-block {
    padding: var(--space-m);
  }
}

@media (min-width: 1024px) {
  .my-block {
    padding: var(--space-l);
  }
}

@media (prefers-reduced-motion: reduce) {
  .my-block * {
    transition: none;
  }
}

@media print {
  .my-block {
    page-break-inside: avoid;
  }
}
```

---

## 6. `block-name-example.html` — required shape

Standalone page; **do not** link `./my-block.css` or import `./my-block.js` — `loadBlock()` loads them.

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta
      http-equiv="Content-Security-Policy"
      content="script-src 'nonce-aem' 'strict-dynamic' 'unsafe-inline' http: https:; base-uri 'self'; object-src 'none';"
      move-to-http-header="true"
    />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>my-block — local demo</title>
    <script nonce="aem" src="/scripts/aem.js" type="module"></script>
    <script nonce="aem" src="/scripts/scripts.js" type="module"></script>
    <link rel="stylesheet" href="/styles/styles.css" />
  </head>
  <body>
    <!--
      Row map: 0 id, 1 title, 2 text, 3 ctaLink, 4 ctaContent
      classes / classes_* live on the block root only.
      Serve: npx serve . → /blocks/my-block/my-block-example.html
    -->
    <header></header>
    <main>
      <div class="block-example-variant-section">
        <h2>Default</h2>
        <p><code>classes_tone</code> = <code>my-block-tone-default</code>.</p>
        <div class="my-block eds-block-my-block my-block-tone-default">
          <div><div>demo-default</div></div>
          <div><div><p>Lorem ipsum dolor sit amet.</p></div></div>
          <div>
            <div>
              <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
            </div>
          </div>
          <div><div><a href="#">Learn more</a></div></div>
          <div><div><p>Learn more</p></div></div>
        </div>
      </div>
    </main>
    <footer></footer>
  </body>
</html>
```

**Must also:**

- Cover **every** `classes_*` option value (including `""`) on at least one sample — one `main > div` per sample ([05-html-example-pattern](./AEM_EDS_RULES.md#05-html-example-pattern)).
- Richtext / prose cells → **Lorem Ipsum** only.
- Images only if model has **`reference`** → full `https://www.gradholder.com/img/...` URLs (vary hex pairs).
- Videos → `/videos/<file>` under repo `videos/` only.

Gold-standard exhaustive demo: `blocks/content/content-example.html`.

---

## 7. `README.md` — required sections

Mirror `blocks/content/README.md`:

1. Purpose  
2. **For another AI / LLM** — when to pick / when not  
3. Big picture (plain language)  
4. Fields / options table  
5. Every variation (fixed columns: Author picks | Choose this if | Plain | Technical)  
6. Row map  
7. Files list  
8. Related blocks / utilities  

---

## 8. Finish checklist

```bash
# add "my-block" to block-configs/component-list.json → components
npm run lint
npm run build:json
# smoke: npx serve . → /blocks/my-block/my-block-example.html
```

- [ ] Five files in `blocks/my-block/`
- [ ] Filter id in `component-list.json`
- [ ] `createBlock` exported; decorate flow correct; `config.mainEl` set
- [ ] No analytics; no `api/` usage
- [ ] Example covers all style options; README has LLM gate

---

## 9. Where to dig deeper

**Full rules (all topics):** [AEM_EDS_RULES.md](./AEM_EDS_RULES.md)

| Topic | Section in AEM_EDS_RULES.md |
|-------|-----------------------------|
| Checklist | [08-block-creation-checklist](./AEM_EDS_RULES.md#08-block-creation-checklist) |
| JS pattern | [02-block-javascript-pattern](./AEM_EDS_RULES.md#02-block-javascript-pattern) |
| JSON pattern | [03-block-json-pattern](./AEM_EDS_RULES.md#03-block-json-pattern) |
| CSS pattern | [04-block-css-pattern](./AEM_EDS_RULES.md#04-block-css-pattern) |
| Example HTML | [05-html-example-pattern](./AEM_EDS_RULES.md#05-html-example-pattern) |
| `createBlock` | [33-programmatic-createBlock](./AEM_EDS_RULES.md#33-programmatic-createblock) |
| Discovery | [28-new-component-discovery](./AEM_EDS_RULES.md#28-new-component-discovery) |
| Parent/child | [22-repeatable-parent-child-blocks](./AEM_EDS_RULES.md#22-repeatable-parent-child-blocks) |
| No analytics | [06-no-analytics](./AEM_EDS_RULES.md#06-no-analytics) |
| Design tokens | [32-block-root-tokens](./AEM_EDS_RULES.md#32-block-root-tokens) |
| Helpers | `scripts/utilities/block-helpers.js` + `.cursor/knowledge/utilities/HELPERS_GUIDE.md` |
| Pattern refs (not shipped blocks) | `.cursor/knowledge/reference-blocks/` |
| Older long template | `.cursor/knowledge/documentation/16-BLOCK_DEVELOPMENT_TEMPLATE.md` (prefer this guide + [AEM_EDS_RULES.md](./AEM_EDS_RULES.md) when they conflict — **no analytics**, **async decorate**, **createBlock** required) |
