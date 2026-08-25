# AEM EDS Blocks — Complete Session Summary

> **Session Date**: April 9, 2026  
> **Purpose**: Comprehensive analysis, documentation, and architectural refinement of an AEM Edge Delivery Services (EDS) block system.

---

## 1. What Was Learned in This Session

### AEM EDS Architecture Fundamentals
- AEM EDS uses a **content-as-markup** approach: the Universal Editor stores content that gets rendered as simple `div>div` HTML structures.
- Blocks are self-contained units with three files: `block-name.js`, `block-name.css`, and `_block-name.json`.
- The framework uses a **3-phase loading** model: eager → lazy → delayed.
- JavaScript blocks follow a strict pattern: `extractConfig()` → `buildBlock()` → `appendEvents()` → `decorate()` (the default export).

### JSON Configuration System
- Block JSON files (`_block-name.json`) define the Universal Editor authoring UI.
- They contain `definitions` (field types), `models` (field groups), and `filters` (allowed children).
- JSON merge system allows shared field definitions across blocks via `models/` directory.
- The underscore prefix (`_`) is required by the AEM EDS boilerplate convention.

### Production Patterns (from text-callout reference)
- **Position-based extraction**: Block rows are accessed by index from `[...block.children]`.
- **Helper functions**: `getTextFromRow`, `getHtmlFromRow`, `getBooleanFromRow` from `block-helpers.js`.
- **No Analytics**: This project does not include analytics. All `dataLayer.js` code and tracking attributes have been removed.
- **DOM construction**: Clear original markup with `block.textContent = ''`, then build fresh semantic DOM.
- **CTA pattern**: Reuse AEM-generated `<a>` elements when available, fallback to `<button>`.

### Reference Repository (aem-boilerplate-xwalk)
- Official Adobe boilerplate for crosswalk (xwalk) AEM EDS projects.
- Contains 6 reference blocks: hero, cards, columns, header, footer, fragment.
- Uses `aem.js` (not `lib-franklin.js`) as the core framework.
- Includes `scripts.js` for loading orchestration and `editor-support.js` for Universal Editor integration.

---

## 2. Key Architectural Changes Made

### Change 1: Basic CSS Only (No SCSS)
- **Before**: SCSS with variables, design tokens, mixins, nesting
- **After**: Plain CSS with direct values (`color: #1a1a1a`, not `color: var(--color-text)`)
- **Why**: AEM EDS has no build pipeline; SCSS requires compilation which doesn't exist in EDS

### Change 2: JSON File Naming — Underscore Prefix
- **Before**: `text-callout.json`
- **After**: `_text-callout.json`
- **Why**: Matches the official AEM EDS boilerplate convention (prevents direct URL access)

### Change 3: File Names Must Match Folder Names
- **Rule**: `blocks/text-callout/text-callout.js`, `text-callout.css`, `_text-callout.json`
- **Why**: AEM EDS framework auto-loads JS/CSS by folder name

### Change 4: Hyphenated Class Names (Not BEM)
- **Before**: `block__element--modifier` (BEM)
- **After**: `text-callout-inner`, `text-callout-title` (hyphenated)
- **Why**: Matches production patterns observed in reference implementations

### Change 5: Analytics Removed
- **Before**: `dataLayer.js` with `applyTracking(block)` and `data-trackinview` / `data-trackclick` attributes
- **After**: No analytics — removed entirely
- **Why**: Analytics is not part of this project scope

---

## 3. Files Created / Updated

### Root-Level Files
| File | Purpose |
|------|---------|
| `SESSION_SUMMARY.md` | This file — session context for new sessions |
| `LEARNINGS.md` | Chronological learnings across all phases |
| `CHANGELOG.md` | Detailed change log with dates |
| `MIGRATION_GUIDE.md` | How to convert old patterns to new architecture |
| `COMPREHENSIVE_ANALYSIS_REPORT.md` | Full repository analysis report |
| `README.md` | (if exists) Project overview |

### Documentation (`AEM_EDS_Documentation/`)
| File | Topic |
|------|-------|
| `00-HOLISTIC_VISION.md` | Overall system vision |
| `01-FUNDAMENTALS.md` | AEM EDS core concepts |
| `02-JSON_CONFIGURATION.md` | JSON authoring config |
| `03-BLOCK_JAVASCRIPT_PATTERN.md` | JS pattern (extractConfig/buildBlock/decorate) |
| `04-DESIGN_TOKENS_SYSTEM.md` | **ARCHIVED** — no longer in use |
| `05-CSS_STYLING_APPROACH.md` | Basic CSS approach (replaced SCSS doc) |
| `06-BLOCK_DESIGN_PHILOSOPHY.md` | Design principles |
| `07-FOUNDATIONAL_BLOCKS.md` | 19 planned blocks overview |
| `08-BLOCK_SPECIFICATIONS.md` | Detailed block specs |
| `09-ANALYTICS_PATTERN.md` | **ARCHIVED** — analytics removed from project |
| `10-PROJECT_STRUCTURE.md` | Repository organization |
| `11-IMPROVEMENTS_TO_REFERENCE.md` | Improvements over boilerplate |
| `12-DEVELOPMENT_PATTERNS.md` | Coding standards & checklist |
| `13-RESPONSIVE_DESIGN_STRATEGY.md` | Mobile-first CSS approach |
| `14-FUTURE_ROADMAP.md` | Phased development plan |
| `16-BLOCK_DEVELOPMENT_TEMPLATE.md` | Step-by-step new block template |

### Reference Blocks
| Directory | Description |
|-----------|-------------|
| `text_callout_block/` | **Production reference** — canonical block implementation |
| `simple_cta_block/` | Test block with full pattern compliance |
| `hero_block/` | Hero block with screenshots & demo |
| `product_hero_test/` | Latest test block (product hero) |

### Utilities (`utilities/`)
| File | Purpose |
|------|---------|
| `block-helpers.js` | Row extraction helpers (getText, getHtml, getBoolean, etc.) |
| `HELPERS_GUIDE.md` | API reference for block-helpers.js |
| `USAGE_EXAMPLES.md` | Practical usage examples |
| `INTEGRATION_PLAN.md` | Plan for integrating helpers into docs |

> **Note:** `dataLayer.js` and `DATALAYER_GUIDE.md` have been archived — analytics is not part of this project.

### Original Reference
| Directory | Description |
|-----------|-------------|
| `Uploads/aem-boilerplate-xwalk-main/` | Official Adobe AEM EDS boilerplate (unmodified) |

---

## 4. Important Patterns to Remember

### Block JS Pattern (copy-paste template)
```javascript
import { getTextFromRow, getHtmlFromRow } from '../../scripts/utilities/block-helpers.js';

function extractConfig(block) {
  const rows = [...block.children];
  return {
    title: getTextFromRow(rows[0]),
    text: getHtmlFromRow(rows[1]),
    // ... position-based extraction
  };
}

function buildBlockName(block, config) {
  block.textContent = '';  // Clear AEM markup
  // Build semantic DOM with hyphenated classes
}

function appendEvents(config) {
  // Click handlers, etc.
}

// JS flow: extractConfig() → buildBlockName() → appendEvents()
export default function decorate(block) {
  const config = extractConfig(block);
  buildBlockName(block, config);
  appendEvents(config);
}
```

### Block CSS Pattern
```css
/* block-name.css — Basic CSS only */
.block-name {
  padding: 2rem 1rem;
  background: #ffffff;
}
.block-name-inner {
  max-width: 1200px;
  margin: 0 auto;
}
@media (min-width: 768px) {
  .block-name { padding: 3rem 2rem; }
}
```

### Block JSON Pattern
```json
{
  "definitions": [{
    "title": "Block Name",
    "id": "block-name",
    "plugins": { "xwalk": { "page": { "resourceType": "core/franklin/components/block/v1/block", "template": { "name": "Block Name", "model": "block-name" } } } }
  }],
  "models": [{
    "id": "block-name",
    "fields": [
      { "component": "text", "name": "title", "label": "Title" }
    ]
  }]
}
```

### File Naming Rules
| Type | Pattern | Example |
|------|---------|---------|
| Folder | `kebab-case` | `text-callout/` |
| JS | matches folder | `text-callout.js` |
| CSS | matches folder, `.css` only | `text-callout.css` |
| JSON | underscore prefix | `_text-callout.json` |
| HTML example | `block-name-example.html` | `text-callout-example.html` |

---

## 5. How to Use This Knowledge Base in a New Session

### Quick Start
1. **Extract the zip** to `/home/ubuntu/aem_eds_analysis/`
2. **Read this file first** (`SESSION_SUMMARY.md`) for full context
3. **For building a new block**: Read `AEM_EDS_Documentation/16-BLOCK_DEVELOPMENT_TEMPLATE.md`
4. **For understanding patterns**: Read `text_callout_block/` files as the canonical reference
5. **For coding standards**: Read `AEM_EDS_Documentation/12-DEVELOPMENT_PATTERNS.md`

### Key Files to Reference When Building Blocks
1. `text_callout_block/text-callout.js` — **THE** canonical JS pattern
2. `text_callout_block/_text-callout.json` — **THE** canonical JSON pattern
3. `text_callout_block/text-callout.css` — **THE** canonical CSS pattern
4. `text_callout_block/text-callout-example.html` — **THE** canonical HTML example
5. `utilities/block-helpers.js` — Import helpers from here
6. `BLOCK_CREATION_STANDARDS.md` — Required deliverables for every block

### Prompt for New Session
Use this when starting a new session:
```
I'm working on AEM Edge Delivery Services (EDS) blocks. I have a comprehensive knowledge base 
from a previous session. Please read SESSION_SUMMARY.md first, then the text_callout_block/ 
directory as the canonical reference pattern. Key rules: basic CSS only (no SCSS/variables), 
underscore prefix for JSON files, hyphenated class names, NO analytics (no dataLayer.js).
Every block must include a block-name-example.html file. See BLOCK_CREATION_STANDARDS.md.
```

### Section Patterns (CORRECTED — Phase 6c)
- **Sections group blocks**: Sections wrap multiple blocks with shared behaviour (carousel, accordion, modal, tabs)
- **Section JSON in `models/`**: Per-section JSON models in `models/_section-name.json` (NOT in `blocks/`)
- **Section JS/CSS in `blocks/`**: `blocks/section-name/section-name.js` and `.css` — but NO `_section-name.json` in blocks folder
- **Hidden `sectionIdentifier` field**: Every section model has a hidden field whose value becomes `data-sectionidentifier`
- **Section metadata**: `style` → CSS classes; other fields → `data-*` attributes on section div
- **Loaded at end of `loadEager()`**: Custom `loadSectionModules()` finds `data-sectionidentifier`, loads JS/CSS from `blocks/`
- **Section JS is simple**: Decorator receives `sectionEl`, calls focused functions (NOT extractConfig → buildBlock pattern)
- **classList over className**: Use `classList.add()` to prevent overwriting existing classes
- **mainEl required**: Always set `config.mainEl` in `buildBlock()` (for blocks only)
- **Always ask**: Never assume — ask clarifying questions before building
- **Post-build**: Always run `npm run lint` and `npm run build:json`

### Architecture Decision Records
- **No SCSS**: AEM EDS has no build pipeline → use basic CSS
- **No CSS variables/tokens**: Simplicity over abstraction
- **No BEM**: Hyphenated names match production patterns
- **No analytics**: Analytics is not part of this project — no dataLayer.js, no tracking attributes
- **Position-based extraction**: Rows are accessed by index, not by name
- **HTML example required**: Every block must include a `block-name-example.html` showing AEM-generated markup
- **classList, not className**: `classList.add()` prevents overwriting existing classes
- **Sections for multi-block patterns**: Use sections (not blocks) for carousel, accordion, modal, tabs

---

## 6. Directory Structure Overview

```
aem_eds_analysis/
├── SESSION_SUMMARY.md          ← START HERE
├── LEARNINGS.md                ← Chronological learnings
├── CHANGELOG.md                ← Change history
├── MIGRATION_GUIDE.md          ← Old → new pattern conversion
├── COMPREHENSIVE_ANALYSIS_REPORT.md
│
├── AEM_EDS_Documentation/      ← 15+ documentation files
│   ├── README.md               ← Documentation index
│   ├── 00-HOLISTIC_VISION.md
│   ├── 01-FUNDAMENTALS.md
│   ├── ...through...
│   └── 16-BLOCK_DEVELOPMENT_TEMPLATE.md
│
├── text_callout_block/         ← CANONICAL REFERENCE BLOCK
│   ├── text-callout.js
│   ├── text-callout.css
│   ├── _text-callout.json
│   ├── text-callout-example.html
│   └── README.md
│
├── simple_cta_block/           ← Test block (full compliance)
├── hero_block/                 ← Hero with screenshots
├── product_hero_test/          ← Product hero test block
│
├── utilities/                  ← Shared JS utilities
│   ├── block-helpers.js
│   ├── HELPERS_GUIDE.md
│   ├── USAGE_EXAMPLES.md
│   └── INTEGRATION_PLAN.md
│
├── BLOCK_CREATION_STANDARDS.md ← Required deliverables for every block
│
├── Uploads/                    ← Original reference materials
│   ├── aem-boilerplate-xwalk-main/  ← Official Adobe boilerplate
│   └── image.png
│
├── BLOCK_ER_PROJECT_BACKUP/    ← Earlier backup (pre-simplification)
└── BLOCK_ER_PROJECT_BACKUP_FOR_SHARING/
```

---

*This summary was auto-generated on April 9, 2026. It captures all knowledge, patterns, and architectural decisions from the AEM EDS block development session.*