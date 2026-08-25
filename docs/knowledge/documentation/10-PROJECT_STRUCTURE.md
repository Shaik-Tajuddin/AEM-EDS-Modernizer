# Project Structure - Repository Organization

## Overview

Clean, modular project structure based on the xwalk boilerplate with improvements.

See: [11-IMPROVEMENTS_TO_REFERENCE.md](11-IMPROVEMENTS_TO_REFERENCE.md) for enhancements

## Complete Directory Structure

```
aem-eds-blocks/
│
├── README.md                          # Project overview
├── package.json                        # Dependencies and scripts
├── .eslintrc.json                     # Linting rules
├── .gitignore                         # Git ignore rules
│
├── blocks/                            # Block implementations
│   ├── text-callout/
│   │   ├── text-callout.js           # Block JavaScript
│   │   ├── text-callout.css          # Block CSS (basic CSS)
│   │   └── _text-callout.json        # Block JSON (underscore prefix)
│   ├── hero/
│   │   ├── hero.js
│   │   ├── hero.css
│   │   └── _hero.json
│   ├── simple-cta/
│   │   ├── simple-cta.js
│   │   ├── simple-cta.css
│   │   └── _simple-cta.json
│   ├── cards/
│   │   ├── cards.js
│   │   ├── cards.css
│   │   └── _cards.json
│   ├── columns/
│   │   ├── columns.js
│   │   ├── columns.css
│   │   └── _columns.json
│   ├── accordion/
│   │   ├── accordion.js
│   │   ├── accordion.css
│   │   └── _accordion.json
│   ├── tabs/
│   │   ├── tabs.js
│   │   ├── tabs.css
│   │   └── _tabs.json
│   └── ... (more blocks)
│
├── models/                            # Shared JSON patterns (for merge)
│   ├── _shared-general-fields.json    # Reusable field patterns
│   ├── _appearance-defaults.json       # Reusable appearance fields
│   └── _analytics.json                 # Reusable analytics fields
│
├── scripts/                           # Shared utilities
│   ├── scripts.js                     # Core EDS scripts
│   ├── aem.js                         # AEM utilities
│   ├── dataLayer.js                   # Analytics tracking
│   └── utilities/
│       └── block-helpers.js           # Block helper functions
│
├── styles/                            # Global styles (basic CSS)
│   ├── styles.css                     # Main styles
│   ├── fonts.css                      # Font imports
│   └── lazy-styles.css                # Deferred styles
│
├── docs/                              # Documentation
│   ├── README.md
│   └── *.md
│
└── tests/                             # Tests
    └── ...
```

## File Naming Conventions

### **Critical Rules**

1. **JSON files MUST have underscore prefix**:
   - ✅ `_text-callout.json`, `_hero.json`, `_simple-cta.json`
   - ❌ `text-callout.json`, `hero.json`, `config.json`

2. **All block files MUST match the folder name**:
   - ✅ Folder `text-callout/` → `text-callout.js`, `text-callout.css`, `_text-callout.json`
   - ❌ Folder `text-callout/` → `textCallout.js`, `styles.css`, `_config.json`

3. **CSS files only** (no SCSS):
   - ✅ `text-callout.css`
   - ❌ `text-callout.scss`

### **Block Folder Pattern**

```
blocks/block-name/
├── block-name.js              # Block JavaScript (decorate function)
├── block-name.css             # Block CSS (basic CSS, direct values)
├── block-name-example.html    # Required: standalone AEM markup demo + global head (see 05-html-example-pattern.mdc)
├── README.md                  # Required: options + how it looks on screen (08-block-creation-checklist.mdc)
└── _block-name.json           # Block JSON (underscore prefix)
```

### **File Type Conventions**
- JavaScript: `kebab-case.js`
- CSS: `kebab-case.css`
- JSON: `_kebab-case.json` (underscore prefix for block JSON)
- Tests: `kebab-case.test.js`

```
✓ text-callout.js
✗ TextCallout.js
✗ text_callout.js

✓ _text-callout.json
✗ text-callout.json
✗ _config.json
```

### **Shared JSON Patterns**
Reusable JSON patterns for `merge-json-cli` use underscore prefix in the `models/` directory:
```
models/
├── _shared-general-fields.json    # Reusable across blocks
├── _appearance-defaults.json
└── _analytics.json
```

## File Organization Principles

### **Blocks Directory**
- One folder per block
- Co-locate JS, CSS, JSON, **`*-example.html`**, and tests
- Block name matches folder name
- Use kebab-case for multi-word names

```
blocks/
├── text-callout/
│   ├── text-callout.js            ✓ Matches block name
│   ├── text-callout.css           ✓ Basic CSS
│   ├── text-callout-example.html  ✓ Standalone demo (required for new blocks)
│   ├── README.md                  ✓ Options + visual appearance (required for new blocks)
│   └── _text-callout.json         ✓ Underscore prefix
│
├── simple-cta/
│   ├── simple-cta.js
│   ├── simple-cta.css
│   ├── simple-cta-example.html
│   └── _simple-cta.json
```

### **Scripts Directory**
- Shared code used by multiple blocks
- `dataLayer.js` for analytics
- `block-helpers.js` for DOM extraction utilities

### **Styles Directory**
- Basic CSS files only
- No SCSS; block CSS uses **global CSS variables** from **`styles/`** (colors, spacing, typography) — see **`30-block-css-design-tokens.mdc`**
- Global styles in `styles/styles.css`

## Import Organization

### **Block File Imports**

```javascript
// blocks/text-callout/text-callout.js

// 1. Analytics utility
import { applyTracking } from '../../scripts/dataLayer.js';

// 2. Block helpers
import {
  getTextFromRow,
  getHtmlFromRow,
  getBooleanFromRow,
} from '../../scripts/utilities/block-helpers.js';
```

### **CSS — No Imports Needed**
Each block has its own standalone `.css` file. No SCSS imports, no token imports, no build process required for styles.

## Naming Conventions

### **CSS Classes**
- Hyphenated names: `block-name-element`
- NOT BEM: no `__` or `--` modifiers
- Example: `.text-callout-title`, `.hero-content`, `.simple-cta-text`

### **Folders**
- Block folders: `kebab-case/`
- Category folders: `lowercase/`

## Checklist for Adding New Block

- [ ] Create folder in `blocks/` with kebab-case name
- [ ] Create `block-name.js` with decorate function
- [ ] Create `block-name.css` with basic CSS (no variables)
- [ ] Create `_block-name.json` with underscore prefix
- [ ] Ensure all filenames match folder name
- [ ] Use hyphenated class names in CSS
- [ ] Include `applyTracking(block)` for analytics
- [ ] Add responsive `@media` queries
- [ ] Test locally before pushing

## References

- [00-HOLISTIC_VISION.md](00-HOLISTIC_VISION.md) - Project overview
- [05-CSS_STYLING_APPROACH.md](05-CSS_STYLING_APPROACH.md) - CSS guidelines
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](16-BLOCK_DEVELOPMENT_TEMPLATE.md) - Block template
- [12-DEVELOPMENT_PATTERNS.md](12-DEVELOPMENT_PATTERNS.md) - Development standards
