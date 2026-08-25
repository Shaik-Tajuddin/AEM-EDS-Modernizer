# Block Development Template — Step-by-Step Guide

## Overview

This document provides a **copy-paste-and-adapt** template for creating new AEM EDS blocks, based on the production text-callout reference implementation.

> **Source**: All patterns derived from the production `text-callout` block. See `text_callout_block/` for the canonical reference.
>
> **File Naming**: JSON uses underscore prefix (`_block-name.json`). All files match folder name. CSS only (no SCSS).

## Before you open Step 1 (discovery + scope)

1. **Discovery** — Follow **`.cursor/rules/28-new-component-discovery.mdc`**: analyze the ask, **search the repo**, and **ask clarifying questions** until layout, content model, and **behavior** are clear. Infer likely interactions from the UI (e.g. shortcuts as display-only vs actionable), confirm with the author, and **do not** add **`window.Bus`**, **`fetch`**, or cross-block wiring until that scope is agreed (or explicitly deferred).
2. **Scope** — When implementing, **touch only** the new block folder, **`block-configs/component-list.json`** if the palette requires it, and regenerated **`component-*.json`** via **`npm run build:json`**. Do **not** change unrelated **`scripts/`**, **`api/`**, other blocks, or “drive-by” fixes — **`.cursor/rules/08-block-creation-checklist.mdc`** (*Scope of file changes*).

---

## Step 1: Create _block-name.json

### Template

Replace `my-block` with your block name throughout:

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
              "title": "<p>Title</p>",
              "text": "<p>Description text.</p>",
              "classes": "variant-default",
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
          "label": "ID",
          "description": "Optional anchor ID for this block"
        },
        {
          "component": "richtext",
          "name": "title",
          "label": "Title",
          "description": "Heading text",
          "required": true
        },
        {
          "component": "richtext",
          "name": "text",
          "label": "Text",
          "description": "Supporting body text"
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
          "label": "Style Variant",
          "valueType": "string",
          "value": "variant-default",
          "options": [
            { "name": "Default", "value": "variant-default" },
            { "name": "Alternate", "value": "variant-alt" }
          ]
        },
        {
          "component": "tab",
          "label": "Appearance",
          "name": "tabAppearance"
        },
        {
          "component": "select",
          "name": "classes_myBlockAlign",
          "label": "Alignment",
          "description": "Content alignment within the block",
          "valueType": "string",
          "value": "",
          "options": [
            { "name": "Left", "value": "" },
            { "name": "Center", "value": "my-block-align-center" },
            { "name": "Right", "value": "my-block-align-right" }
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
          "description": "JSON metadata for inview tracking events",
          "condition": { "==": [{ "var": "trackInview" }, true] }
        },
        {
          "component": "boolean",
          "name": "trackClick",
          "label": "Track click",
          "description": "Enable tracking for the CTA"
        },
        {
          "component": "text",
          "name": "trackClick_meta",
          "label": "Click analytics meta",
          "description": "JSON metadata for click tracking events",
          "condition": { "==": [{ "var": "trackClick" }, true] }
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

### Customization Checklist

- [ ] Replace `my-block` / `My Block` with your block name
- [ ] Add/remove content fields in General tab
- [ ] Update `classes` options for your style variants
- [ ] Update `classes_*` options for appearance
- [ ] Set meaningful defaults in template
- [ ] Only include fields with non-empty defaults in template

---

## Step 2: Map Row Positions

Count which fields create rows (skip tabs and classes fields):

```
Model field order:
  tabGeneral          → NO ROW (tab)
  id                  → ROW 0
  title               → ROW 1
  text                → ROW 2
  ctaLink             → ROW 3
  ctaContent          → ROW 4
  classes             → NO ROW (classes field)
  tabAppearance       → NO ROW (tab)
  classes_myBlockAlign→ NO ROW (classes field)
  tabAnalytics        → NO ROW (tab)
  trackInview         → ROW 5
  trackInview_meta    → ROW 6
  trackClick          → ROW 7
  trackClick_meta     → ROW 8
```

---

## Step 2b: Create `block-name-example.html` (required)

Every block **must** include this file in the same folder as `_block-name.json`. It is **not** optional.

**Why:** Authors and developers need a single place that shows (1) the exact **pre-`decorate()`** DOM AEM produces, (2) **row order** aligned with `extractConfig`, and (3) a page that loads **`/scripts/aem.js`**, **`/scripts/scripts.js`**, and **`/styles/styles.css`** so `decorateBlocks` → **`loadBlock()`** pulls in **`block-name.css`** and **`block-name.js`** exactly like production.

**Rules (summary):**

- Full HTML5 document; `<head>` aligned with project **`head.html`** (CSP meta, viewport, module scripts with `nonce="aem"`, `/styles/styles.css`).
- **Do not** add `<link href="./block-name.css">` or a script that imports `./block-name.js`.
- `<body>`: empty `<header></header>`, `<main><div><div class="block-name …">` … `</div></div></main>`, `<footer></footer>`.
- Inside the block root: one **`<div><div>…</div></div>`** per row-producing field, in **model order**; document rows in an HTML comment.
- Authored **`classes`** / **`classes_*`** appear on the **block root** only (not as extra fake rows).

**Verify:** From the repository root, `npx serve .`, then open `http://localhost:<port>/blocks/block-name/block-name-example.html`.

**Variant labels:** If the page includes **several** block instances (e.g. every `classes_*` value), use **one `main > div` per instance**: `h2` + `p` (describe the variant), then the **unmodified** block markup. Add a small `<style>` in the file for borders/spacing between sections — see **Variant labels** in **`05-html-example-pattern.mdc`** (`blocks/content/content-example.html`). When a sample needs a **section backdrop** (light / dark / black band, padding, width), add the matching **`section-*`** classes from **`section-utilities.css`** / **`_section.json`** on that outer `div` and adjust label colours on dark sections — see **Section appearance utilities** in **`05-html-example-pattern.mdc`**.

**Style coverage:** For each `select` named `classes` or `classes_*`, include at least one block root showing **every** option value (including `""`), with other axes at defaults; add representative **combinations** and an HTML comment matrix when a full Cartesian product would be huge. See **Exhaustive `classes_*` / style coverage** in `.cursor/rules/05-html-example-pattern.mdc`.

**Images:** Omit from `*-example.html` unless the model has **`reference`** fields—then add one **`<picture>`** row per `reference` using **full gradholder.com `https://www.gradholder.com/img/...` URLs** only — not site-root `/img/` or `/media/` placeholders (see **`05-html-example-pattern.mdc`**). **Richtext:** assume **no** inline images; add them only when a block explicitly needs that edge case and document it.

**Lorem Ipsum:** All **richtext** / prose inside the block’s **row** markup in `*-example.html` must use **Lorem Ipsum** body copy (not product/marketing text). Variant `h2`/`p` labels remain short technical descriptions.

Canonical rule file: `.cursor/rules/05-html-example-pattern.mdc`. In-repo sample: `blocks/content/content-example.html`.

---

## Step 2c: Create `README.md` (required)

Add **`README.md`** next to the other block files. This is **mandatory** for every new block (existing blocks may be backfilled over time).

Include at minimum:

1. **Purpose** — what the block does and when authors should use it.
2. **For another AI / LLM** — **when to pick** vs **when not to pick** (requirement-style bullets) so another model can gate this block.
3. **Big picture (plain language)** — how the block shows up on a page explained **without assuming sight** or web expertise (reading order, simple analogies: boxes, ink color, “same pen” for inherited styles).
4. **Fields / options** — authoring fields and `classes_*` (table).
5. **Every variation** — **each** option value (and key combos) documented in a **repeatable pattern**, e.g. mini-tables: **Author picks** | **Choose this if** | **Plain explanation** | **Sighted user** (optional) | **Technical** — same structure for every row so LLMs and humans can scan.
6. **Row map** — row index → field for `extractConfig`.
7. **Files** — `_*.json`, JS, CSS, example HTML.
8. **Related** — shared CSS/JS, RTE config, nested blocks.

Reference: `blocks/content/README.md`, **`08-block-creation-checklist.mdc`** Step 6.

---

## Step 3: Create block-name.js

### Template

```javascript
import { applyTracking } from '../../scripts/dataLayer.js';
import {
  getTextFromRow,
  getHtmlFromRow,
  getBooleanFromRow,
} from '../../scripts/utilities/block-helpers.js';

/**
 * Gets an anchor element from a row.
 * @param {Element|null} row
 * @returns {HTMLAnchorElement|null}
 */
function getAnchorFromRow(row) {
  const el = row?.querySelector('a');
  return el instanceof HTMLAnchorElement ? el : null;
}

/**
 * @param {HTMLAnchorElement|null} anchor
 * @returns {boolean}
 */
function hasUsableHref(anchor) {
  if (!anchor) return false;
  const href = anchor.getAttribute('href');
  return Boolean(href && href !== '#');
}

/**
 * Row layout: tabs do not create rows. Fields whose name starts with `classes`
 * are applied to the block by the runtime and do not create rows.
 *
 * 0: id
 * 1: title (richtext)
 * 2: text (richtext)
 * 3: ctaLink (aem-content)
 * 4: ctaContent (richtext)
 * 5: trackInview (boolean)
 * 6: trackInview_meta
 * 7: trackClick (boolean)
 * 8: trackClick_meta
 *
 * @param {Element} block
 * @returns {Object}
 */
function extractConfig(block) {
  if (!block) return {};

  const rows = [...block.children];

  return {
    id: getTextFromRow(rows[0]),
    title: getHtmlFromRow(rows[1]),
    text: getHtmlFromRow(rows[2]),
    ctaLink: getAnchorFromRow(rows[3]),
    ctaContent: getHtmlFromRow(rows[4]),

    trackInView: getBooleanFromRow(rows[5]),
    trackInViewMeta: getTextFromRow(rows[6]),
    trackClick: getBooleanFromRow(rows[7]),
    trackClickMeta: getTextFromRow(rows[8]),
  };
}

/**
 * Build CTA element — reuse anchor from AEM or create button fallback.
 * @param {Object} config
 * @returns {HTMLElement}
 */
function buildCtaElement(config) {
  const { ctaLink, ctaContent } = config;
  const labelHtml = ctaContent?.trim()
    ? ctaContent
    : '<span>Learn more</span>';

  if (ctaLink && hasUsableHref(ctaLink)) {
    if (ctaContent?.trim()) {
      ctaLink.innerHTML = ctaContent;
    }
    ctaLink.classList.add('brand-cta');
    return ctaLink;
  }

  const btn = document.createElement('button');
  btn.type = 'button';
  btn.classList.add('brand-cta', 'my-block-cta-button');
  btn.innerHTML = labelHtml;
  return btn;
}

/**
 * @param {Object} config
 */
function appendEvents(config) {
  if (!config.mainEl) return;
  config.mainEl.addEventListener('click', () => {
    console.log('my-block: CTA clicked');
  });
}

/**
 * @param {Element} block
 * @param {Object} config
 */
function buildMyBlock(block, config) {
  const {
    id, title, text,
    trackInView, trackInViewMeta,
    trackClick, trackClickMeta,
  } = config;

  if (id) block.id = id;

  const inner = document.createElement('div');
  inner.className = 'my-block-inner';

  if (title) {
    const titleEl = document.createElement('div');
    titleEl.className = 'my-block-title';
    titleEl.innerHTML = title;
    inner.appendChild(titleEl);
  }

  if (text) {
    const textEl = document.createElement('div');
    textEl.className = 'my-block-text';
    textEl.innerHTML = text;
    inner.appendChild(textEl);
  }

  const ctaEl = buildCtaElement(config);
  config.mainEl = ctaEl;

  const wrap = document.createElement('div');
  wrap.className = 'my-block-cta';
  wrap.appendChild(ctaEl);

  if (trackClick && trackClickMeta) {
    ctaEl.setAttribute('data-trackclick', 'true');
    ctaEl.setAttribute('data-trackclickmeta', trackClickMeta);
  }

  inner.appendChild(wrap);

  if (trackInView && trackInViewMeta) {
    block.setAttribute('data-trackinview', 'true');
    block.setAttribute('data-trackinviewmeta', trackInViewMeta);
  }

  block.textContent = '';
  block.appendChild(inner);
}

/**
 * @param {Element} block
 */
export default function decorate(block) {
  const config = extractConfig(block);
  buildMyBlock(block, config);
  appendEvents(config);
  applyTracking(block);
}
```

### Customization Checklist

- [ ] Replace `my-block` / `myBlock` with your block name
- [ ] Update row position map comment
- [ ] Add/remove fields in `extractConfig`
- [ ] Customize `buildMyBlock` DOM structure
- [ ] Add custom event logic in `appendEvents`
- [ ] Add block-specific helpers if needed

---

## Step 4: Create block-name.css

> **Important**: Basic CSS only (no SCSS). In **`blocks/**/*.css`**, use **global CSS variables** for **colors**, **spacing**, and **typography** where applicable (**`var(--color-*)`**, **`var(--space-*)`**, **`var(--body-font-size-*)`**, **`var(--heading-font-size-*)`**, **`var(--font-family-*)`**, **`var(--font-weight-*)`**). No raw color literals in blocks; add new color tokens in **`styles/colors.css`**, new type scale entries in **`styles/styles.css`**. See **`.cursor/rules/30-block-css-design-tokens.mdc`** and **`04-block-css-pattern.mdc`**.

### Template

```css
/* my-block styles — mobile-first, basic CSS */

.my-block {
  padding: var(--space-s);
  background-color: var(--color-base-background);
  color: var(--color-base-text);
}

.my-block-inner {
  display: flex;
  flex-direction: column;
  gap: var(--space-s);
}

.my-block-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-neutral-800);
}

.my-block-text {
  font-size: 1rem;
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.my-block-cta {
  margin-top: var(--space-xs);
}

/* Style variants (applied via classes field) */
.my-block.variant-default .brand-cta {
  background-color: var(--color-accent-cta);
  color: var(--color-text-on-emphasis);
}

.my-block.variant-default .brand-cta:hover {
  background-color: var(--color-accent-link-hover);
}

.my-block.variant-alt .brand-cta {
  background-color: transparent;
  border: 2px solid var(--color-accent-link);
  color: var(--color-accent-link);
}

.my-block.variant-alt .brand-cta:hover {
  background-color: color-mix(in srgb, var(--color-accent-link) 8%, transparent);
}

/* Alignment variants (applied via classes_* field) */
.my-block.my-block-align-center {
  text-align: center;
}

.my-block.my-block-align-center .my-block-inner {
  align-items: center;
}

.my-block.my-block-align-right {
  text-align: right;
}

.my-block.my-block-align-right .my-block-inner {
  align-items: flex-end;
}

/* Responsive */
@media (min-width: 768px) {
  .my-block {
    padding: var(--space-m);
  }

  .my-block-title {
    font-size: 1.75rem;
  }
}

@media (min-width: 1024px) {
  .my-block {
    padding: var(--space-l);
  }

  .my-block-title {
    font-size: 2rem;
  }
}

/* Accessibility */
@media (prefers-reduced-motion: reduce) {
  .my-block .brand-cta {
    transition: none;
  }
}

@media print {
  .my-block {
    padding: var(--space-s);
    page-break-inside: avoid;
  }
}
```

---

## Step 5: File Structure

```
blocks/
  my-block/
    my-block.js        # Block JavaScript
    _my-block.json     # Block JSON (underscore prefix required)
    my-block.css       # Block CSS (basic CSS only, no SCSS)
```

> **Critical**: All files must match the folder name. JSON files must have underscore prefix.

---

## Quick Reference: Field → Row → Extraction

| JSON Component | Creates Row? | Extraction Helper |
|---|---|---|
| `text` | Yes | `getTextFromRow(rows[N])` |
| `richtext` | Yes | `getHtmlFromRow(rows[N])` |
| `aem-content` | Yes | `getAnchorFromRow(rows[N])` or `getLinkFromRow(rows[N])` |
| `reference` | Yes | `getImageFromRow(rows[N])` |
| `boolean` | Yes | `getBooleanFromRow(rows[N])` |
| `select` (`classes`/`classes_*`) | **No** | N/A — applied as CSS class on outer div |
| `tab` | **No** | N/A — UI organization only |

---

## Quick Reference: Analytics Integration

```javascript
// During build:
if (config.trackInView && config.trackInViewMeta) {
  block.setAttribute('data-trackinview', 'true');
  block.setAttribute('data-trackinviewmeta', config.trackInViewMeta);
}
if (config.trackClick && config.trackClickMeta) {
  ctaEl.setAttribute('data-trackclick', 'true');
  ctaEl.setAttribute('data-trackclickmeta', config.trackClickMeta);
}

// At end of decorate():
applyTracking(block);
```

---

## References

- [02-JSON_CONFIGURATION.md](02-JSON_CONFIGURATION.md) - JSON patterns
- [03-BLOCK_JAVASCRIPT_PATTERN.md](03-BLOCK_JAVASCRIPT_PATTERN.md) - JS patterns
- [09-ANALYTICS_PATTERN.md](09-ANALYTICS_PATTERN.md) - Analytics
- [12-DEVELOPMENT_PATTERNS.md](12-DEVELOPMENT_PATTERNS.md) - Best practices
- `text_callout_block/` - Production reference
- `utilities/block-helpers.js` - Helper API
- `utilities/dataLayer.js` - Analytics API
