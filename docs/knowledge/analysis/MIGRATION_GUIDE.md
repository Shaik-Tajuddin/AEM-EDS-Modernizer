# Migration Guide — Architectural Simplification

## Overview

This guide explains the three key architectural changes made to the AEM EDS block system and how to update existing blocks to comply.

---

## Why We Simplified

### Problem
The original architecture included SCSS compilation, CSS custom properties (variables), and a 3-layer design token system. This created:
- **Unnecessary complexity** for simple, self-contained blocks
- **Build pipeline dependency** (SCSS compilation required)
- **Maintenance overhead** (token synchronization, variable management)
- **Divergence from AEM EDS conventions** (the boilerplate uses basic CSS)

### Solution
We simplified to three clear rules:
1. **Basic CSS only** — no SCSS; **shared** palette, spacing, and typography live as **CSS variables** in **`styles/`** and blocks consume them with **`var(--*)`** (**`30-block-css-design-tokens.mdc`**)
2. **Underscore prefix for JSON** — `_block-name.json`
3. **File names match folder names** — predictable, consistent

---

## Change 1: JSON File Naming (Underscore Prefix)

### What Changed
- Block JSON configuration files now require an underscore prefix
- This aligns with the AEM EDS boilerplate convention (e.g., `_hero.json`, `_cards.json`)

### Before
```
blocks/text-callout/
├── text-callout.js
├── text-callout.css
└── text-callout.json        ← no prefix
```

### After
```
blocks/text-callout/
├── text-callout.js
├── text-callout.css
└── _text-callout.json       ← underscore prefix
```

### How to Migrate
```bash
# Rename JSON files
mv blocks/my-block/my-block.json blocks/my-block/_my-block.json
```

---

## Change 2: File Names Must Match Folder

### What Changed
- All block files (JS, CSS, JSON) must use the same name as the folder
- No generic names like `styles.css`, `config.json`, `index.js`

### Before (bad)
```
blocks/text-callout/
├── textCallout.js           ← wrong: camelCase
├── styles.css               ← wrong: generic name
└── _config.json             ← wrong: doesn't match folder
```

### After (correct)
```
blocks/text-callout/
├── text-callout.js          ← matches folder
├── text-callout.css         ← matches folder
└── _text-callout.json       ← matches folder (with underscore)
```

### How to Migrate
```bash
# Rename files to match folder name
mv blocks/my-block/styles.css blocks/my-block/my-block.css
mv blocks/my-block/config.json blocks/my-block/_my-block.json
```

---

## Change 3: Basic CSS Only (No SCSS); tokens in `styles/`

### What changed (historical)
- Removed SCSS (`.scss` files, `$variables`, `@include mixins`, nesting)
- Dropped **per-block** SCSS variables and a **3-layer** token build

### Current approach
- **Block** `.css` files stay plain CSS (no SCSS)
- **Colors / spacing / typography** use **`var(--*)`** pointing at **`:root`** (and imports) in **`styles/colors.css`** and **`styles/styles.css`** — see **`30-block-css-design-tokens.mdc`**

### Before (SCSS with tokens)
```scss
// simple-cta.scss
:root {
  --simple-cta-color-primary: #0066cc;
  --simple-cta-spacing-md: 1rem;
}

$breakpoint-tablet: 768px;

.simple-cta {
  padding: var(--simple-cta-spacing-md);

  @media (min-width: $breakpoint-tablet) {
    padding: var(--simple-cta-spacing-lg);
  }
}

.simple-cta__title {
  color: var(--simple-cta-color-primary);
}
```

### After (basic CSS)
```css
/* simple-cta.css */
.simple-cta {
  padding: 1rem;
}

@media (min-width: 768px) {
  .simple-cta {
    padding: 1.5rem;
  }
}

.simple-cta-title {
  color: #0066cc;
}
```

### How to Convert SCSS to CSS

1. **Replace `$variables` with direct values**:
   - `$breakpoint-tablet: 768px` → use `768px` directly in media queries
   - `$text-primary: #1a1a1a` → use `#1a1a1a` directly

2. **Replace `var(--custom-properties)` with direct values**:
   - `color: var(--simple-cta-color-primary)` → `color: #0066cc`
   - `padding: var(--simple-cta-spacing-md)` → `padding: 1rem`

3. **Remove `:root` token declarations entirely**

4. **Flatten SCSS nesting**:
   ```scss
   // Before (SCSS)
   .simple-cta {
     &__title { color: #1a1a1a; }
     &:hover { background: #f5f5f5; }
   }
   
   // After (CSS)
   .simple-cta-title { color: #1a1a1a; }
   .simple-cta:hover { background: #f5f5f5; }
   ```

5. **Replace `@include` mixins with direct CSS**:
   ```scss
   // Before
   @include respond-to('tablet') { ... }
   
   // After
   @media (min-width: 768px) { ... }
   ```

6. **Convert BEM to hyphenated naming**:
   - `.block__element` → `.block-element`
   - `.block--modifier` → `.block.modifier` (for variants)

7. **Rename file**: `.scss` → `.css`

---

## Quick Reference: Conversion Table

| Old Pattern | New Pattern |
|-------------|-------------|
| `block-name.json` | `_block-name.json` |
| `block-name.scss` | `block-name.css` |
| `var(--custom-prop)` | Direct value (e.g., `#0066cc`) |
| `$scss-variable` | Direct value (e.g., `768px`) |
| `@include mixin()` | Direct CSS |
| SCSS nesting `&__el` | `.block-name-el` |
| `:root { tokens }` | Remove entirely |
| `textCallout.js` | `text-callout.js` (match folder) |
| `styles.css` | `block-name.css` (match folder) |
| `_config.json` | `_block-name.json` (match folder) |

---

## Files Affected in This Migration

### Block Files
- `text_callout_block/text-callout.json` → `_text-callout.json`
- `simple_cta_block/simple-cta.json` → `_simple-cta.json`
- `simple_cta_block/simple-cta.scss` → `simple-cta.css` (rewritten)
- `hero_block/hero.json` → `_hero.json`
- `hero_block/hero.scss` → `hero.css` (rewritten)
- New: `text_callout_block/text-callout.css` (created)

### Documentation Files
- `04-DESIGN_TOKENS_SYSTEM.md` → Archived (deprecation notice added)
- `05-SCSS_STYLING_APPROACH.md` → Rewritten as `05-CSS_STYLING_APPROACH.md`
- `00-HOLISTIC_VISION.md` → Updated (removed token/SCSS references)
- `02-JSON_CONFIGURATION.md` → Updated (underscore prefix noted)
- `03-BLOCK_JAVASCRIPT_PATTERN.md` → Updated (file naming note)
- `06-BLOCK_DESIGN_PHILOSOPHY.md` → Rewritten (removed token philosophy)
- `10-PROJECT_STRUCTURE.md` → Rewritten (new file naming conventions)
- `12-DEVELOPMENT_PATTERNS.md` → Updated (CSS standards, checklists)
- `13-RESPONSIVE_DESIGN_STRATEGY.md` → Rewritten (basic CSS media queries)
- `14-FUTURE_ROADMAP.md` → Updated (removed Figma token references)
- `16-BLOCK_DEVELOPMENT_TEMPLATE.md` → Updated (basic CSS template, underscore prefix)
- All block READMEs updated

---

## Questions?

Refer to:
- [05-CSS_STYLING_APPROACH.md](AEM_EDS_Documentation/05-CSS_STYLING_APPROACH.md) — CSS guidelines
- [10-PROJECT_STRUCTURE.md](AEM_EDS_Documentation/10-PROJECT_STRUCTURE.md) — File naming
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](AEM_EDS_Documentation/16-BLOCK_DEVELOPMENT_TEMPLATE.md) — Complete template
