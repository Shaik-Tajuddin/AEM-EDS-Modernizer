# Block Creation Standards

> **Mandatory requirements for every AEM EDS block in this project.**  
> Last updated: April 9, 2026 — Phase 6: Sections, classList, mainEl, clarifying questions

---

## 1. Required Deliverables (The Block Quad)

Every block **must** include these 4 files:

| #   | File                      | Naming Convention               | Purpose                               |
| --- | ------------------------- | ------------------------------- | ------------------------------------- |
| 1   | `block-name.js`           | Matches folder name             | Client-side decoration logic          |
| 2   | `block-name.css`          | Matches folder name             | Basic CSS styling (no SCSS/variables) |
| 3   | `_block-name.json`        | Underscore prefix + folder name | Universal Editor model definition     |
| 4   | `block-name-example.html` | `block-name-example.html`       | AEM-generated HTML markup example     |

A **`README.md` is required** in each block directory: **options**, **plain-language rendering** (no assumption of sight), **LLM selection hints**, and a **repeated per-variation pattern** covering **every** option (see **`08-block-creation-checklist.mdc`** Step 6; see `blocks/content/README.md`). Existing blocks may be backfilled over time.

### Why an HTML example file?

The HTML example file (`block-name-example.html`) shows the **assumed AEM-generated markup** that the Universal Editor would produce **before** the block's `decorate()` function runs. This is critical because:

- It documents the contract between AEM and the block's JavaScript
- It shows how the JSON model fields map to HTML rows
- It explains which fields create rows and which don't (tabs, `classes*`)
- It provides a testable reference for the block's `extractConfig()` function
- It shows both the "before" (AEM markup) and "after" (decorated DOM) states

---

## 2. File Naming Conventions

| Type         | Convention                                     | Example                                  |
| ------------ | ---------------------------------------------- | ---------------------------------------- |
| Folder       | kebab-case (underscores for directory name OK) | `text_callout_block/` or `text-callout/` |
| JS file      | matches block name                             | `text-callout.js`                        |
| CSS file     | matches block name, `.css` only                | `text-callout.css`                       |
| JSON file    | underscore prefix                              | `_text-callout.json`                     |
| HTML example | block name + `-example.html`                   | `text-callout-example.html`              |
| README       | `README.md`                                    | `README.md`                              |

---

## 3. JavaScript Pattern

Every block JS file must follow the `extractConfig() → buildBlock() → appendEvents()` pattern:

```javascript
import {
  getTextFromRow,
  getHtmlFromRow,
} from "../../scripts/utilities/block-helpers.js";

function extractConfig(block) {
  const rows = [...block.children]; // NOT querySelectorAll
  return {
    title: getHtmlFromRow(rows[0]),
    text: getHtmlFromRow(rows[1]),
    // Position-based — order matches JSON model field order
  };
}

function buildBlockName(block, config) {
  block.textContent = ""; // Clear AEM markup
  const inner = document.createElement("div");
  inner.className = "block-name-inner"; // Hyphenated, NOT BEM
  // ... build DOM
  block.appendChild(inner);
}

function appendEvents(config) {
  config.mainEl?.addEventListener("click", () => {
    /* ... */
  });
}

export default function decorate(block) {
  // Synchronous, NOT async
  const config = extractConfig(block);
  buildBlockName(block, config);
  appendEvents(config);
}
```

### Key rules:

- **Synchronous** `decorate()` function (NOT async)
- **`[...block.children]`** for row access (NOT `querySelectorAll`)
- **`block.textContent = ''`** to clear original markup (NOT `innerHTML = ''`)
- **`classList.add()`** to set classes (NOT `className =`) — prevents accidentally overwriting existing classes
- **`config.mainEl`** — always set this in `buildBlock()` to the most important interactive element, or the entire block container if there are multiple interactive elements
- **Hyphenated class names** (`block-name-element`, NOT BEM `block__element--modifier`)
- **Import helpers** from `block-helpers.js` — do NOT rewrite extraction logic
- **No analytics** — do NOT import `dataLayer.js` or use `applyTracking()`

```javascript
// ✅ CORRECT — classList
inner.classList.add("block-name-inner");
title.classList.add("block-name-title");

// ❌ WRONG — className (replaces all classes)
inner.className = "block-name-inner";
title.className = "block-name-title";
```

---

## 4. JSON Configuration Pattern

```json
{
  "definitions": [{ ... }],
  "models": [{
    "id": "block-name",
    "fields": [
      { "component": "tab", "label": "General", "name": "tabGeneral" },
      // ... content fields ...
      { "component": "tab", "label": "Appearance", "name": "tabAppearance" },
      // ... style/alignment fields using classes/classes_*
    ]
  }],
  "filters": [{
    "id": "block-name",
    "components": []
  }]
}
```

### Key rules:

- **Underscore prefix** on filename: `_block-name.json`
- **Tabs**: `General` and `Appearance` — **NO Analytics tab**
- **Boolean toggles**: Use `"component": "boolean"` (NOT checkbox)
- **Conditional fields**: Use `"condition"` with JSON Logic (NOT `"visible"`)
- **Select fields**: Must include `"valueType": "string"`
- **Style variants**: Use `classes` and `classes_*` fields (auto-applied by AEM, no rows)

---

## 5. CSS Pattern

- **Basic CSS only** — no SCSS; use **global CSS variables** from **`styles/`** for colors, spacing, and typography (**`30-block-css-design-tokens.mdc`**)
- **Hyphenated class names** — `.block-name-inner`, `.block-name-title`
- **Mobile-first** with `@media (min-width: ...)` breakpoints
- **Accessibility**: Include `prefers-reduced-motion` and print styles
- **Variants** via block classes applied by AEM runtime (e.g. `.block-name.variant-name`)

---

## 6. HTML Example File Template

```html
<!--
  block-name-example.html
  =======================
  Shows the AEM-generated HTML markup BEFORE decorate() runs.

  Rules:
  - "tab" components do NOT create rows
  - "classes" / "classes_*" fields do NOT create rows (applied as classes on outer div)
  - All other fields create one <div><div>content</div></div> row each
  - Row order matches field order in the JSON model (excluding tabs and classes)
-->

<div class="block-name">
  <!-- Row 0: fieldName (component type) — description -->
  <div>
    <div>field value here</div>
  </div>

  <!-- Row 1: anotherField (richtext) -->
  <div>
    <div><p>Rich text content</p></div>
  </div>

  <!-- ... more rows ... -->
</div>

<!--
  AFTER decorate() runs, the block is rebuilt into:
  <div class="block-name">
    <div class="block-name-inner">
      ... decorated DOM ...
    </div>
  </div>
-->
```

### What to include:

1. **Header comment** explaining the file's purpose
2. **Multiple variants:** before each block sample, **`h2` + `p`** describing the variant; **one `main > div` section per sample**; light `<style>` in `<head>` for separators — **`05-html-example-pattern.mdc`** (Variant labels)
3. **Outer div** with block class name (and any `classes` values)
4. **Each row** with inline comment showing: row number, field name, component type
5. **Realistic data** — use real-world example copy where helpful; **do not** add images unless the model has **`reference`** fields—then use **full `https://www.gradholder.com/img/...` URLs** only (not site-relative `/img/` or `/media/`) and the **`<picture>`** row shape (**`05-html-example-pattern.mdc`**). **Richtext:** assume no inline images unless the block documents an exception.
6. **Empty rows** for optional/unset fields (showing they still exist as empty divs)
7. **Footer comment** showing the expected decorated output

---

## 7. What is NOT Part of This Project

| Feature       | Status                                                  | Notes                                                                                      |
| ------------- | ------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| Analytics     | ❌ Not included                                         | No `dataLayer.js`, no `applyTracking()`, no tracking attributes                            |
| SCSS          | ❌ Not included                                         | AEM EDS has no build pipeline                                                              |
| CSS Variables | ✅ Global in `styles/`                                  | Blocks use `var(--color-*)`, `var(--space-*)`, type tokens — not arbitrary per-block `--*` |
| Design tokens | ✅ `styles/colors.css` + `:root` in `styles/styles.css` | No separate Figma pipeline; literals live under `styles/`                                  |
| BEM Naming    | ❌ Not included                                         | Use hyphenated names                                                                       |
| mitt Events   | ❌ Not included                                         | Removed                                                                                    |

---

## 8. Sections vs Blocks (CORRECTED — Phase 6c)

Before creating a new block, consider whether a **section** is more appropriate:

| Aspect                          | Block                                | Section                                                |
| ------------------------------- | ------------------------------------ | ------------------------------------------------------ |
| Purpose                         | Self-contained component             | Groups/wraps multiple blocks                           |
| Examples                        | Hero, CTA, Card                      | Carousel, Accordion, Modal, Tabs                       |
| JSON model                      | `blocks/block-name/_block-name.json` | `models/_section-name.json`                            |
| JS/CSS location                 | `blocks/block-name/`                 | `blocks/section-name/`                                 |
| Has `_*.json` in blocks folder? | ✅ Yes                               | ❌ No                                                  |
| JS loading                      | Via `loadBlock()`                    | Via `loadSectionModules()` at end of `loadEager()`     |
| Decorator param                 | `block` element                      | `sectionEl` (section element)                          |
| Config source                   | HTML rows (`[...block.children]`)    | `data-` attributes from section-metadata               |
| Required hidden field           | None                                 | `sectionIdentifier` → becomes `data-sectionidentifier` |

See `SECTIONS_GUIDE.md` for complete section documentation.
