# AEM EDS Block Helpers — Reference Guide

Production-ready utility functions for AEM Edge Delivery Services block development.

> **Source**: `utilities/block-helpers.js`  
> **Version**: 1.0.0  
> **Status**: Production-ready (provided by user)

---

## Table of Contents

1. [Row Value Extractors](#1-row-value-extractors)
2. [Single Row Extractors](#2-single-row-extractors)
3. [Responsive Helpers](#3-responsive-helpers)
4. [Block Grouping Helpers](#4-block-grouping-helpers)
5. [Toggle / Expand Helpers](#5-toggle--expand-helpers)
6. [Environment Helpers](#6-environment-helpers)

---

## Background: AEM EDS DOM Structure

AEM EDS generates a nested `div > div` structure for each block field. **There are no label divs** — extraction is purely position-based.

```html
<div class="block-name variant-class">
  <div><div>row-0-value</div></div>   <!-- index 0 -->
  <div><div>row-1-value</div></div>   <!-- index 1 -->
  <div><div><img src="..."></div></div> <!-- index 2 (image) -->
  <div><div><a href="...">Link</a></div></div> <!-- index 3 (link) -->
</div>
```

The row index matches the field order in the block's JSON model.

---

## 1. Row Value Extractors

**Purpose**: Extract values from block rows by position index. These are the primary extraction helpers used in `extractConfig()` functions.

### `getValue(block, index)`

Returns the raw inner `<div>` element at the given row index.

| Param | Type | Description |
|-------|------|-------------|
| `block` | `HTMLElement` | The block element |
| `index` | `number` | Zero-based row index |
| **Returns** | `HTMLElement \| null` | Inner div element |

**When to use**: When you need the raw DOM element for custom processing.

```js
const el = getValue(block, 2);
if (el) {
  const customData = el.dataset.customAttr;
}
```

### `getText(block, index)`

Returns plain text content from a row. Strips all HTML.

| Param | Type | Description |
|-------|------|-------------|
| `block` | `HTMLElement` | The block element |
| `index` | `number` | Zero-based row index |
| **Returns** | `string` | Plain text, or `''` |

**When to use**: For plain text fields like `id`, labels, or simple values.

```js
const id = getText(block, 0);
const label = getText(block, 1);
```

### `getHTML(block, index)`

Returns inner HTML from a row. Preserves richtext formatting.

| Param | Type | Description |
|-------|------|-------------|
| `block` | `HTMLElement` | The block element |
| `index` | `number` | Zero-based row index |
| **Returns** | `string` | HTML string, or `''` |

**When to use**: For `richtext` fields (title, description, body text) where bold, italic, links, etc. must be preserved.

```js
const title = getHTML(block, 1);   // "<b>Bold</b> title"
const desc = getHTML(block, 2);    // "<p>Rich <em>text</em></p>"
```

### `getImage(block, index)`

Extracts image data from a row containing an `<img>` element.

| Param | Type | Description |
|-------|------|-------------|
| `block` | `HTMLElement` | The block element |
| `index` | `number` | Zero-based row index |
| **Returns** | `{ src, alt, element } \| null` | Image data or null |

**When to use**: For `reference` (image) fields.

```js
const hero = getImage(block, 5);
if (hero) {
  img.src = hero.src;
  img.alt = hero.alt;
}
```

### `getLink(block, index)`

Extracts a URL/link from a row. Checks for `<a>` element first, then falls back to text.

| Param | Type | Description |
|-------|------|-------------|
| `block` | `HTMLElement` | The block element |
| `index` | `number` | Zero-based row index |
| **Returns** | `{ href, text, element }` | Link data (always returns object) |

**When to use**: For `aem-content` (URL/path) fields.

```js
const cta = getLink(block, 4);
if (cta.href) {
  button.href = cta.href;
}
```

---

## 2. Single Row Extractors

**Purpose**: Extract values from an already-selected row element. Useful when iterating rows with `forEach`, or when processing grouped fields.

### `getTextFromRow(row)` / `getHtmlFromRow(row)` / `getLinkFromRow(row)` / `getImageFromRow(row)`

Same logic as the index-based extractors, but accept a row element directly.

| Function | Returns | Use For |
|----------|---------|--------|
| `getTextFromRow(row)` | `string` | Plain text fields |
| `getHtmlFromRow(row)` | `string` | Richtext fields |
| `getLinkFromRow(row)` | `{ href, text, element }` | URL fields |
| `getImageFromRow(row)` | `{ src, alt, element } \| null` | Image fields |

### `getBooleanFromRow(row)`

Interprets row text as boolean. `"true"`, `"1"`, `"yes"`, `"on"` → `true`. Everything else → `false`.

| Param | Type | Description |
|-------|------|-------------|
| `row` | `HTMLElement` | A direct child div of the block |
| **Returns** | `boolean` | Interpreted boolean value |

**When to use**: For checkbox/toggle fields, feature flags.

```js
const rows = block.querySelectorAll(':scope > div');
const showLocation = getBooleanFromRow(rows[9]);
```

---

## 3. Responsive Helpers

**Purpose**: Manage responsive behavior and breakpoint detection using `matchMedia` (no resize polling).

### `DESKTOP_BREAKPOINT`

Constant: `900` (pixels). The standard AEM EDS desktop breakpoint.

### `createResponsiveHelper(breakpoint?)`

Creates a responsive helper object with breakpoint detection.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `breakpoint` | `number` | `900` | Breakpoint width in px |
| **Returns** | `object` | — | Helper with `isDesktop()`, `isMobile()`, `onBreakpointChange()`, `destroy()` |

**When to use**: When a block needs different behavior on mobile vs. desktop (e.g., different image sources, layout changes, event handling).

```js
const responsive = createResponsiveHelper();
if (responsive.isDesktop()) {
  // Load desktop image
}
responsive.onBreakpointChange((isDesktop) => {
  block.classList.toggle('block--desktop', isDesktop);
});
```

### `createDesktopHelper(callback, breakpoint?)`

Convenience wrapper — fires callback immediately and on breakpoint changes.

```js
const cleanup = createDesktopHelper((isDesktop) => {
  block.classList.toggle('hero--desktop-layout', isDesktop);
});
// Later: cleanup.destroy();
```

---

## 4. Block Grouping Helpers

**Purpose**: Group block rows into logical sections for multi-item blocks (accordion, carousel, tabs, card grids).

### `isBlockWrapper(element, blockName?)`

Checks if an element is an AEM block wrapper (`<div class="blockname-wrapper">`).

**When to use**: When traversing the DOM to find sibling blocks or section structure.

### `createAdvancedBlockGrouper(block, options)`

Splits rows into groups based on a separator function.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `isSeparator` | `(row, index) => boolean` | required | Returns true if row starts a new group |
| `includeSeparator` | `boolean` | `true` | Include separator row in its group |
| **Returns** | `HTMLElement[][]` | — | Array of row groups |

**When to use**: Accordion (split on `<h2>` headers), tabs (split on tab title rows).

```js
const groups = createAdvancedBlockGrouper(block, {
  isSeparator: (row) => !!row.querySelector('h2'),
});
// groups[0] = [h2Row, contentRow1, contentRow2]
// groups[1] = [h2Row, contentRow3]
```

### `createBlockGrouper(block, groupSize)`

Splits rows into fixed-size groups.

**When to use**: Card grids, carousels where each item uses N consecutive rows.

```js
// Each card = 3 rows: image, title, description
const cards = createBlockGrouper(block, 3);
```

---

## 5. Toggle / Expand Helpers

**Purpose**: Create expandable/collapsible UI patterns with full accessibility.

### `createToggle(options)`

Creates a complete toggle component (trigger button + content panel) with ARIA attributes.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `trigger` | `string \| HTMLElement` | required | Trigger content |
| `content` | `string \| HTMLElement` | required | Panel content |
| `expanded` | `boolean` | `false` | Initial state |
| `className` | `string` | `'toggle'` | Base CSS class |
| `onChange` | `(expanded) => void` | — | State change callback |

Returns: `{ wrapper, triggerEl, panelEl, toggle(), expand(), collapse(), isExpanded() }`

### `addToggleListeners(container, options)`

Wires up toggle behavior on existing DOM elements. Supports accordion mode (only one open at a time).

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `triggerSelector` | `string` | required | CSS selector for triggers |
| `panelSelector` | `string` | — | CSS selector for panels |
| `activeClass` | `string` | `'is-active'` | Active state class |
| `accordion` | `boolean` | `false` | Single-open mode |
| `onChange` | `(trigger, panel, expanded) => void` | — | Callback |

---

## 6. Environment Helpers

**Purpose**: Detect the runtime environment (author mode vs. publish mode).

### `isAuthorMode()`

Returns `true` if the page is in AEM Author/Universal Editor mode.

**Detection methods**:
1. `window.hlx.aemRoot` is set
2. URL contains AEM Cloud author indicators
3. HTML element has `adobe-ue-edit` class
4. Page is in a Universal Editor frame

**When to use**: Skip animations, show placeholder content, disable navigation, or adjust block behavior for authors.

```js
export default async function decorate(block) {
  if (isAuthorMode()) {
    block.classList.add('block--author-preview');
    // Show static preview instead of animation
    return;
  }
  // Normal decoration...
}
```
