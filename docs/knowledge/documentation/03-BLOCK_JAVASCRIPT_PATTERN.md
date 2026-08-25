# Block JavaScript Pattern - Standard Implementation

## Overview

Every AEM EDS block follows a consistent JavaScript pattern:

1. **Import helpers** — `block-helpers.js` for extraction, nested-block handling, and row-preserving rebuild; `dataLayer.js` for analytics where used
2. **`await checkAndHandleNestedBlocks(block)`** — First line of `decorate` when using `async` (standard); decorates UE nested blocks before you read rows
3. **extractConfig** — Extract data from authored HTML using position-based helpers (exclude **`isNestedBlockRowElement`** rows when mapping “all rows” to data)
4. **buildBlock** — **`removeNonBlockChildRows`** / **`replaceBlockRowsPreservingNestedBlocks`** / **`prependBlockBuiltNodes`** instead of wiping the whole block
5. **appendEvents** — Attach interaction handlers
6. **applyTracking** — Wire up analytics via data attributes (blocks that use `dataLayer.js`)

> **Production Reference**: See `text_callout_block/text-callout.js` for the canonical example.
>
> **File Naming**: Block JS files must match folder name. CSS uses basic `.css` (no SCSS). JSON uses underscore prefix `_block-name.json`.

See: [01-FUNDAMENTALS.md](01-FUNDAMENTALS.md) for block decoration concepts

### Example HTML (`<block-name>-example.html`)

Ship a **standalone** `blocks/<block-folder>/<block-folder>-example.html` with the same global scripts/styles as `head.html`, Franklin `main` → section → block shape, and one row wrapper per **row-producing** model field. This file is the living contract for your **`extractConfig`** indices and must stay in sync when the model changes. It must also **exhaust every `classes_*` / `classes` select value** (including empty options) across block roots, per **Exhaustive `classes_*` / style coverage** in `.cursor/rules/05-html-example-pattern.mdc`. **Images** appear in the example **only** if the model has **`reference`** fields—then use **full `https://www.gradholder.com/img/...` URLs** (not site-relative `/img/` or `/media/` paths) and the **`<picture>`** row shape in **`05-html-example-pattern.mdc`**. **Richtext:** assume no inline images unless the block documents an exception. Example (no `reference`, multi-variant layout): `blocks/content/content-example.html` — **Variant labels** + **Lorem Ipsum** body rows in **`05-html-example-pattern.mdc`**. Block **`README.md`** documents options, **plain-language rendering**, **LLM selection**, and **every variation** in a fixed table pattern (`blocks/content/README.md`).

---

## Production Pattern (text-callout)

```javascript
import { applyTracking } from '../../scripts/dataLayer.js';
import {
  getTextFromRow,
  getHtmlFromRow,
  getBooleanFromRow,
  checkAndHandleNestedBlocks,
  replaceBlockRowsPreservingNestedBlocks,
} from '../../scripts/utilities/block-helpers.js';

/**
 * Row layout (tabs and classes_* fields do NOT create rows):
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

function buildBlock(block, config) {
  // ... build semantic DOM ...
  // Set analytics data attributes during build
  if (config.trackClick && config.trackClickMeta) {
    ctaEl.setAttribute('data-trackclick', 'true');
    ctaEl.setAttribute('data-trackclickmeta', config.trackClickMeta);
  }
  if (config.trackInView && config.trackInViewMeta) {
    block.setAttribute('data-trackinview', 'true');
    block.setAttribute('data-trackinviewmeta', config.trackInViewMeta);
  }
  replaceBlockRowsPreservingNestedBlocks(block, inner);
}

function appendEvents(config) {
  if (!config.mainEl) return;
  config.mainEl.addEventListener('click', () => { ... });
}

export default async function decorate(block) {
  await checkAndHandleNestedBlocks(block);
  const config = extractConfig(block);
  buildBlock(block, config);
  appendEvents(config);
  applyTracking(block);  // Scans data attributes and wires observers/listeners
}
```

---

## Detailed Breakdown

### 1. Imports

Every block imports two categories:

```javascript
// Analytics utility — always import applyTracking
import { applyTracking } from '../../scripts/dataLayer.js';

// Extraction helpers — import only what you need
import {
  getTextFromRow,
  getHtmlFromRow,
  getBooleanFromRow,
  getLinkFromRow,
  getImageFromRow,
} from '../../scripts/utilities/block-helpers.js';
```

**Import paths** follow the project structure:
- `../../scripts/dataLayer.js` — analytics
- `../../scripts/utilities/block-helpers.js` — extraction helpers

### 2. extractConfig Function

**Purpose**: Extract all authored data from AEM-generated HTML using helper functions.

#### Row Access Pattern

```javascript
const rows = [...block.children];  // Spread into array
```

> **⚠️ Use `[...block.children]`** — not `querySelectorAll(':scope > div')`. The spread pattern is the production standard.

#### Helper Functions for Each Field Type

| JSON Component | Helper | Returns |
|---|---|---|
| `text` | `getTextFromRow(row)` | `string` (trimmed) |
| `richtext` | `getHtmlFromRow(row)` | `string` (inner HTML) |
| `boolean` | `getBooleanFromRow(row)` | `boolean` |
| `aem-content` | `getLinkFromRow(row)` or custom | `string` (href) |
| `reference` | `getImageFromRow(row)` | `Element` (picture/img) |

#### Row Position Mapping

Document the row layout in a JSDoc comment above `extractConfig`. This is the **most critical documentation** in the block — it maps JSON model fields to extraction positions:

```javascript
/**
 * Row layout: tabs do not create rows. Fields whose name starts with `classes`
 * are applied to the block by the runtime and do not create rows.
 *
 * 0: id
 * 1: title (richtext)
 * 2: text (richtext)
 * 3: ctaLink
 * 4: ctaContent
 * 5: trackInview
 * 6: trackInview_meta
 * 7: trackClick
 * 8: trackClick_meta
 */
```

#### Custom Extraction Helpers

For special field types (e.g., extracting an actual `<a>` element rather than just href), define block-local helpers:

```javascript
function getAnchorFromRow(row) {
  const el = row?.querySelector('a');
  return el instanceof HTMLAnchorElement ? el : null;
}

function hasUsableHref(anchor) {
  if (!anchor) return false;
  const href = anchor.getAttribute('href');
  return Boolean(href && href !== '#');
}
```

#### Guard Clause

Always start with a guard:

```javascript
function extractConfig(block) {
  if (!block) return {};
  // ...
}
```

### 3. Build Function

**Purpose**: Clear original AEM markup and build semantic DOM.

#### Pattern

```javascript
function buildTextCallout(block, config) {
  const { id, title, text } = config;

  // Set block-level attributes
  if (id) block.id = id;

  // Create inner container
  const inner = document.createElement('div');
  inner.className = 'text-callout-inner';

  // Build child elements conditionally
  if (title) {
    const titleEl = document.createElement('div');
    titleEl.className = 'text-callout-title';
    titleEl.innerHTML = title;
    inner.appendChild(titleEl);
  }

  if (text) {
    const textEl = document.createElement('div');
    textEl.className = 'text-callout-text';
    textEl.innerHTML = text;
    inner.appendChild(textEl);
  }

  // Build CTA element
  const ctaEl = buildCtaElement(config);
  config.mainEl = ctaEl;  // Store for event binding

  const wrap = document.createElement('div');
  wrap.className = 'text-callout-cta';
  wrap.appendChild(ctaEl);

  // Set analytics data attributes during build
  if (config.trackClick && config.trackClickMeta) {
    ctaEl.setAttribute('data-trackclick', 'true');
    ctaEl.setAttribute('data-trackclickmeta', config.trackClickMeta);
  }

  inner.appendChild(wrap);

  if (config.trackInView && config.trackInViewMeta) {
    block.setAttribute('data-trackinview', 'true');
    block.setAttribute('data-trackinviewmeta', config.trackInViewMeta);
  }

  replaceBlockRowsPreservingNestedBlocks(block, inner);
}
```

#### Key Build Patterns

| Pattern | Detail |
|---|---|
| **Clear with `removeNonBlockChildRows` + prepend** | Drops authored rows only; keeps nested `.block` / `eds-block-*` / `.block-child-wrapper` children |
| **Inner container** | `block-name-inner` div wraps all content |
| **Conditional elements** | Only create elements for non-empty config values |
| **Config mutation** | Store `mainEl` on config for event binding |
| **Analytics data attrs** | Set during build, processed by `applyTracking()` |

#### Class Naming Convention

Use **hyphenated names** based on block name:

```
text-callout-inner
text-callout-title
text-callout-text
text-callout-cta
text-callout-cta-button
```

> **Note**: Production uses hyphenated naming (`text-callout-title`), not BEM (`text-callout__title`). Follow this convention.

#### CTA Element Builder

Separate CTA construction into its own function — this handles both link and button cases:

```javascript
function buildCtaElement(config) {
  const { ctaLink, ctaContent } = config;
  const labelHtml = ctaContent?.trim()
    ? ctaContent
    : '<span>Learn more</span>';

  // If we have a valid link, reuse the anchor element from AEM
  if (ctaLink && hasUsableHref(ctaLink)) {
    if (ctaContent?.trim()) {
      ctaLink.innerHTML = ctaContent;
    }
    ctaLink.classList.add('brand-cta');
    return ctaLink;
  }

  // Fallback: create a button
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.classList.add('brand-cta', 'text-callout-cta-button');
  btn.innerHTML = labelHtml;
  return btn;
}
```

**Key insight**: When `aem-content` provides a real `<a>` element, reuse it directly. Only create a `<button>` as fallback.

### 4. appendEvents Function

**Purpose**: Attach interaction handlers to built elements.

```javascript
function appendEvents(config) {
  if (!config.mainEl) return;
  config.mainEl.addEventListener('click', () => {
    console.log('text-callout: CTA clicked');
  });
}
```

This function is intentionally simple. Custom event logic goes here. Analytics tracking is handled separately by `applyTracking()`.

### 5. decorate Function (Entry Point)

```javascript
export default async function decorate(block) {
  await checkAndHandleNestedBlocks(block);
  const config = extractConfig(block);
  buildTextCallout(block, config);
  appendEvents(config);
  applyTracking(block);
}
```

**Key points**:
- **`async` + `await checkAndHandleNestedBlocks(block)`** — standard so nested blocks load before rebuild; the block loader awaits `decorate`
- **`export default`** — required by EDS framework
- **`applyTracking(block)`** — always call last, after DOM is built (when using analytics)

---

## Analytics Integration Pattern

Analytics uses **data attributes**, not mitt events. The flow is:

1. **JSON model** defines `boolean` toggle + `text` meta fields
2. **extractConfig** reads boolean/meta from row positions
3. **buildBlock** sets data attributes on elements: `data-trackinview`, `data-trackinviewmeta`, `data-trackclick`, `data-trackclickmeta`
4. **`applyTracking(block)`** scans for these attributes and wires up IntersectionObserver / click listeners

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

// At end of decorate:
applyTracking(block);
```

See [09-ANALYTICS_PATTERN.md](09-ANALYTICS_PATTERN.md) for the full `dataLayer.js` API.

---

## DOM Field Extraction Reference

### AEM HTML Structure

Each field (except tabs and classes) generates a `<div>` child of the block:

```html
<div class="text-callout cta-primary-filled text-callout-align-center">
  <div>id-value</div>                          <!-- row 0: text -->
  <div><div><p>Title HTML</p></div></div>       <!-- row 1: richtext -->
  <div><div><p>Body HTML</p></div></div>        <!-- row 2: richtext -->
  <div><div><a href="/page">link</a></div></div><!-- row 3: aem-content -->
  <div><div><p>CTA Label</p></div></div>        <!-- row 4: richtext -->
  <div>true</div>                               <!-- row 5: boolean -->
  <div>{"event":"view"}</div>                    <!-- row 6: text (meta) -->
  <div>false</div>                              <!-- row 7: boolean -->
  <div></div>                                   <!-- row 8: text (meta) -->
</div>
```

### Extraction Method by Field Type

| Field Type | Component | Helper | What It Returns |
|---|---|---|---|
| Plain text | `text` | `getTextFromRow(rows[N])` | Trimmed text content |
| Rich text | `richtext` | `getHtmlFromRow(rows[N])` | Inner HTML (preserves `<p>`, `<strong>`, etc.) |
| Boolean | `boolean` | `getBooleanFromRow(rows[N])` | `true`/`false` |
| Link/URL | `aem-content` | `getLinkFromRow(rows[N])` or custom `getAnchorFromRow()` | href string or `<a>` element |
| Image | `reference` | `getImageFromRow(rows[N])` | `<picture>` or `<img>` element |
| Style variant | `select` (classes) | **N/A — not a row** | Applied as CSS class on outer div |

---

## Best Practices

### ✓ Do
- Import and use helpers from `block-helpers.js`
- Use `[...block.children]` for row access
- Document row position map in JSDoc above `extractConfig`
- Use `removeNonBlockChildRows` / `replaceBlockRowsPreservingNestedBlocks` instead of clearing the entire block
- Set analytics data attributes during build
- Call `applyTracking(block)` at end of `decorate`
- Use hyphenated class names (`block-name-element`)
- Prefer `async` `decorate` with `await checkAndHandleNestedBlocks(block)` at the start
- Guard `extractConfig` with `if (!block) return {}`

### ✗ Don't
- Use `querySelectorAll(':scope > div')` — use `[...block.children]`
- Use `innerHTML = ''` or `replaceChildren()` on the block root to wipe nested blocks — use `block-helpers.js` row-preserving helpers instead
- Manually wire up IntersectionObserver for analytics — use `applyTracking()`
- Use mitt/emitter for analytics events — use data attributes
- Omit `await checkAndHandleNestedBlocks` only for trivial pass-through blocks with no nested-block contract
- Use BEM (`__`, `--`) for element classes — use hyphens
- Forget to document the row position map
- Ship a block without **`<block-name>-example.html`** — required for row contract and local `loadBlock()` checks (see **05-html-example-pattern**)
- Add images to **`*-example.html`** when the model has **no** **`reference`** fields (richtext-only blocks assume **no** inline images unless documented)
- Use **marketing or bespoke copy** inside **richtext example rows** — use **Lorem Ipsum** (**`05-html-example-pattern.mdc`**)
- Ship a new block **without** **`README.md`** (options + on-screen appearance — **`08-block-creation-checklist.mdc`**)

---

## Testing Pattern

```javascript
describe('Text Callout Block', () => {
  test('extractConfig returns correct data', () => {
    const block = document.createElement('div');
    block.innerHTML = `
      <div>my-id</div>
      <div><div><p>Hello</p></div></div>
      <div><div><p>World</p></div></div>
      <div><div><a href="/page">link</a></div></div>
      <div><div><p>Click me</p></div></div>
      <div>true</div>
      <div>{"event":"view"}</div>
      <div>false</div>
      <div></div>
    `;

    const config = extractConfig(block);
    expect(config.id).toBe('my-id');
    expect(config.title).toContain('Hello');
    expect(config.trackInView).toBe(true);
  });
});
```

---

## References

- [01-FUNDAMENTALS.md](01-FUNDAMENTALS.md) - Block decoration concepts
- [02-JSON_CONFIGURATION.md](02-JSON_CONFIGURATION.md) - JSON configuration (includes required **`*-example.html`**)
- `.cursor/rules/05-html-example-pattern.mdc` - Standalone block demo pages
- [09-ANALYTICS_PATTERN.md](09-ANALYTICS_PATTERN.md) - Analytics integration
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](16-BLOCK_DEVELOPMENT_TEMPLATE.md) - Step-by-step guide
- `text_callout_block/text-callout.js` - Production reference
- `utilities/block-helpers.js` - Helper functions API
- `utilities/dataLayer.js` - Analytics utilities
