# AEM EDS Blocks — Comprehensive Deep-Dive Analysis

**Analysis Date**: April 9, 2026  
**Source**: `AEM_EDS_Blocks_Complete_Knowledge.zip`  
**Total Files**: ~150 unique files (excluding backup duplicates)

---

## 1. Executive Summary

This project is a **comprehensive knowledge base and implementation framework** for building **AEM Edge Delivery Services (EDS) blocks** using the **crosswalk (xwalk) authoring approach** with the **Universal Editor**. It is NOT a deployable application — it is a **reference architecture, documentation system, and block development toolkit** that contains:

- **15 numbered documentation guides** (~45,000 words, 200+ pages) covering every aspect of AEM EDS block development
- **4 implemented reference blocks** (text-callout, simple-cta, hero, product-hero) with full JS/CSS/JSON + HTML examples
- **1 shared utility library** (block-helpers.js)
- **Adobe's official xwalk boilerplate** (unmodified reference copy)
- **Research and analysis files** from live AEM sites and repository analysis
- **Architectural decision records** documenting pattern evolution and corrections

The project underwent a significant **architectural simplification** during its session, moving from SCSS/design-tokens/BEM to **basic CSS/direct-values/hyphenated-names**. Analytics is **NOT** part of this project — all analytics code and configuration has been removed.

---

## 2. Project Structure & Organization

```
aem_eds_analysis/
│
├── 📄 SESSION_SUMMARY.md                    # Entry point — full session context
├── 📄 COMPREHENSIVE_ANALYSIS_REPORT.md      # Full repository analysis
├── 📄 LEARNINGS.md                          # Chronological learnings (4 phases)
├── 📄 CHANGELOG.md                          # Detailed change history
├── 📄 MIGRATION_GUIDE.md                    # Old→new pattern conversion guide
├── 📄 HERO_BLOCK_DELIVERY.txt               # Hero block delivery notes
├── 📄 DUPLICATE_CREATED.txt                 # File tracking note
├── 📄 lib-franklin.js                       # Legacy Franklin library (deprecated)
├── 🖼️ screenshot_*.png (4 files)            # Hero block screenshots
│
├── 📁 AEM_EDS_Documentation/               # ★ CORE: 15 numbered guides
│   ├── README.md                            # Documentation index
│   ├── FILE_GUIDE.txt                       # File descriptions
│   ├── 00-HOLISTIC_VISION.md/.pdf
│   ├── 01-FUNDAMENTALS.md/.pdf
│   ├── 02-JSON_CONFIGURATION.md/.pdf
│   ├── 03-BLOCK_JAVASCRIPT_PATTERN.md/.pdf
│   ├── 04-DESIGN_TOKENS_SYSTEM.md/.pdf      # ⚠️ ARCHIVED (deprecated)
│   ├── 05-CSS_STYLING_APPROACH.md/.pdf       # Replaced SCSS approach
│   ├── 05-SCSS_STYLING_APPROACH.md/.pdf      # ⚠️ Legacy (superseded)
│   ├── 06-BLOCK_DESIGN_PHILOSOPHY.md/.pdf
│   ├── 07-FOUNDATIONAL_BLOCKS.md/.pdf
│   ├── 08-BLOCK_SPECIFICATIONS.md/.pdf
│   ├── 09-ANALYTICS_PATTERN.md/.pdf
│   ├── 10-PROJECT_STRUCTURE.md/.pdf
│   ├── 11-IMPROVEMENTS_TO_REFERENCE.md/.pdf
│   ├── 12-DEVELOPMENT_PATTERNS.md/.pdf
│   ├── 13-RESPONSIVE_DESIGN_STRATEGY.md/.pdf
│   ├── 14-FUTURE_ROADMAP.md/.pdf
│   └── 16-BLOCK_DEVELOPMENT_TEMPLATE.md/.pdf # Step-by-step new block guide
│
├── 📁 text_callout_block/                   # ★ CANONICAL reference block
│   ├── text-callout.js                      # Production JS implementation
│   ├── text-callout.css                     # Basic CSS (no variables)
│   ├── _text-callout.json                   # Universal Editor model
│   ├── text-callout-example.html            # AEM-generated HTML example
│   └── README.md                            # Pattern documentation
│
├── 📁 simple_cta_block/                     # Test block (matches canonical)
│   ├── simple-cta.js
│   ├── simple-cta.css
│   ├── _simple-cta.json
│   ├── simple-cta-example.html              # AEM-generated HTML example
│   ├── simple-cta-with-helpers.js           # Alt version with helpers
│   ├── LEARNINGS.md/.pdf
│   └── README.md
│
├── 📁 hero_block/                           # Full hero with screenshots
│   ├── hero.js                              # ~13KB, complex implementation
│   ├── hero.css                             # Basic CSS version
│   ├── _hero.json                           # Multi-tab model (older format)
│   ├── demo.html                            # Standalone demo with viewport switcher
│   ├── IMPLEMENTATION_PATTERNS.md/.pdf
│   ├── SUMMARY.md/.pdf
│   ├── README.md
│   ├── FILE_INDEX.txt
│   └── screenshot_*.png (4 files)           # Desktop/mobile/hover/interaction
│
├── 📁 product_hero_test/                    # Latest test block
│   ├── product-hero.js
│   ├── product-hero.css
│   ├── _product-hero.json
│   ├── product-hero-example.html            # AEM-generated HTML example
│   ├── demo.html
│   └── README.md
│
├── 📁 utilities/                            # ★ Shared JS utilities
│   ├── block-helpers.js                     # Row extractors, grouping, toggles
│   ├── HELPERS_GUIDE.md/.pdf                # API reference
│   ├── USAGE_EXAMPLES.md/.pdf               # Practical examples
│   └── INTEGRATION_PLAN.md/.pdf             # Integration roadmap
│
├── 📁 Uploads/aem-boilerplate-xwalk-main/   # ★ Official Adobe reference
│   ├── blocks/{hero,cards,columns,fragment,header,footer}/
│   ├── scripts/{aem.js,scripts.js,editor-support.js,...}
│   ├── styles/{styles.css,fonts.css,lazy-styles.css}
│   ├── models/_*.json                       # Source JSON models
│   ├── component-{definition,models,filters}.json
│   ├── package.json                         # v1.3.0 @adobe/aem-boilerplate
│   ├── fstab.yaml, paths.json, head.html
│   └── .eslintrc.js, .stylelintrc.json, .husky/
│
├── 📁 BLOCK_ER_PROJECT_BACKUP/              # Earlier backup (pre-simplification)
│   ├── 01_DOCUMENTATION/                    # Copy of docs
│   ├── 02_HERO_BLOCK_IMPLEMENTATION/        # Hero with SCSS version
│   ├── 03_RESEARCH_FILES/                   # Analysis markdown files
│   ├── 04_REFERENCE_REPO/                   # Zipped boilerplate
│   └── BACKUP_MANIFEST.txt
│
├── 📄 aem_eds_comprehensive_documentation_review.md/.pdf
├── 📄 aem_element_grouping_analysis.md/.pdf
├── 📄 aem_live_page_analysis.md/.pdf
├── 📄 aem_live_source.html                  # Captured live AEM page source
├── 📄 aem_xwalk_repo_analysis.md/.pdf
└── 📄 AEM_EDS_Tabs_and_JSONMerge_Complete_Guide.md/.pdf
```

---

## 3. What AEM Edge Delivery Services (EDS) Is

### The Platform
AEM EDS is Adobe's **content-as-markup** web delivery system. Content authors use the **Universal Editor** (a visual WYSIWYG tool) to create pages, and AEM renders them as **simple, flat HTML** (`div>div` structures). JavaScript "blocks" then **decorate** this markup client-side into rich, interactive components.

### The xwalk Approach
This project uses the **crosswalk (xwalk)** variant of AEM EDS, which means:
- Content is authored in **AEM Cloud** (not Google Docs/SharePoint)
- The **Universal Editor** provides the authoring UI
- JSON configuration files define the **authoring experience** (fields, tabs, validation)
- `data-aue-*` attributes enable **live editing** in the Universal Editor

### Three-Phase Loading Model
```
loadEager()  → Above-fold content (hero, first section) — blocks LCP
loadLazy()   → Below-fold content (loaded as sections scroll into view)
loadDelayed() → Interactive features (forms, modals) — 3s after page load
```

---

## 4. Core Architecture & Design Patterns

### 4.1 The Block Quad (Every Block = 4 Required Deliverables)

| File | Purpose | Naming Convention |
|------|---------|-------------------|
| `block-name.js` | Client-side decoration logic | Matches folder name |
| `block-name.css` | Styling (basic CSS only) | Matches folder name |
| `_block-name.json` | Universal Editor model definition | Underscore prefix + folder name |
| `block-name-example.html` | AEM-generated HTML markup example | Shows what AEM produces before JS runs |

See [BLOCK_CREATION_STANDARDS.md](aem_eds_analysis/BLOCK_CREATION_STANDARDS.md) for full details.

### 4.2 JavaScript Block Pattern (The Core Pattern)

Every block follows a strict **extract → build → events** pattern:

```javascript
import { getTextFromRow, getHtmlFromRow } from '../../scripts/utilities/block-helpers.js';

// 1. EXTRACT: Pull data from AEM-generated HTML by row position
function extractConfig(block) {
  const rows = [...block.children];  // NOT querySelectorAll
  return {
    title: getHtmlFromRow(rows[0]),
    text: getHtmlFromRow(rows[1]),
    // Position-based — order matches JSON model field order
  };
}

// 2. BUILD: Construct clean semantic HTML
function buildBlockName(block, config) {
  block.textContent = '';  // Clear AEM markup
  const inner = document.createElement('div');
  inner.className = 'block-name-inner';  // Hyphenated, NOT BEM
  // ... build DOM
  block.appendChild(inner);
}

// 3. EVENTS: Attach click handlers, store references on config
function appendEvents(config) {
  config.mainEl?.addEventListener('click', () => { /* ... */ });
}

// 4. DECORATE: The default export — orchestrates everything
export default function decorate(block) {  // Synchronous, NOT async
  const config = extractConfig(block);
  buildBlockName(block, config);
  appendEvents(config);
}
```

> **Note:** Analytics is NOT part of this project. No `dataLayer.js` imports or `applyTracking()` calls.

### 4.3 JSON Configuration Pattern

Each block's `_block-name.json` contains three sections:

```json
{
  "definitions": [{
    "title": "Block Name",
    "id": "block-name",
    "plugins": { "xwalk": { "page": {
      "resourceType": "core/franklin/components/block/v1/block",
      "template": {
        "name": "Block Name",
        "model": "block-name",
        "filter": "block-name",
        "title": "<p>Default Title</p>"
      }
    }}}
  }],
  "models": [{
    "id": "block-name",
    "fields": [
      { "component": "tab", "label": "General", "name": "tabGeneral" },
      { "component": "richtext", "name": "title", "label": "Title" },
      { "component": "tab", "label": "Appearance", "name": "tabAppearance" },
      { "component": "select", "name": "classes", "label": "Style variant",
        "valueType": "string", "value": "", "options": [...] }
    ]
  }],
  "filters": [{
    "id": "block-name",
    "components": []  // No child components allowed
  }]
}
```

**Key JSON patterns:**
- **Tabs**: `tab` components organize fields into General → Appearance
- **Boolean toggles**: Use `"component": "boolean"` (NOT checkbox)
- **Conditional fields**: Use `"condition"` with JSON Logic (NOT `"visible"`)
- **Style variants**: `"classes"` and `"classes_*"` fields are auto-applied by AEM runtime (no rows)
- **Select fields**: Must include `"valueType": "string"`
- **No Analytics tab** — analytics is not part of this project

### 4.4 CSS Approach (Post-Simplification)

```css
/* Mobile-first, basic CSS only — NO variables, NO tokens, NO SCSS */
.block-name {
  padding: 2rem 1rem;        /* Direct values */
  background: #ffffff;        /* No var(--anything) */
  color: #1a1a1a;
}

.block-name-inner {           /* Hyphenated class names */
  max-width: 1200px;
  margin: 0 auto;
}

@media (min-width: 768px) {   /* Standard media queries */
  .block-name { padding: 3rem 2rem; }
}

/* Variants via block classes (applied by AEM) */
.block-name.variant-dark { background: #1a1a1a; color: #ffffff; }

/* Accessibility */
@media (prefers-reduced-motion: reduce) { /* ... */ }
@media print { /* ... */ }
```

### 4.5 Analytics

> **Analytics is NOT part of this project.** The `dataLayer.js` utility and all `applyTracking()` / `data-trackinview` / `data-trackclick` patterns have been removed. If analytics is needed in the future, it should be implemented as a separate concern outside of individual blocks.

---

## 5. Shared Utility Libraries

### 5.1 `block-helpers.js` (~400 lines)

Six categories of utilities:

| Category | Functions | Purpose |
|----------|-----------|---------|
| **Row Extractors (indexed)** | `getValue()`, `getText()`, `getHTML()`, `getImage()`, `getLink()` | Extract from `rows[index]` |
| **Row Extractors (single)** | `getTextFromRow()`, `getHtmlFromRow()`, `getLinkFromRow()`, `getImageFromRow()`, `getBooleanFromRow()` | Extract from individual row elements |
| **Responsive Helpers** | `createResponsiveHelper()`, `createDesktopHelper()` | Media query with debounced resize |
| **Block Grouping** | `createBlockGrouper()`, `createAdvancedBlockGrouper()`, `isBlockWrapper()` | Group consecutive blocks (accordion/carousel) |
| **Toggle/Expand** | `createToggle()`, `addToggleListeners()` | ARIA-compliant expand/collapse |
| **Environment** | `isAuthorMode()` | Detect Universal Editor mode |

> **Note:** `dataLayer.js` has been removed. Analytics is not part of this project.

---

## 6. Reference Blocks Analysis

### 6.1 Text Callout (★ Canonical Reference)

The **definitive pattern** all new blocks should follow. Features:
- ID, richtext title, richtext body, CTA (link + content)
- CTA reuses AEM `<a>` element or falls back to `<button>`
- Style variants via `classes` field (primary/secondary/accent)
- Alignment via `classes_textCalloutAlign` field
- Includes `text-callout-example.html` showing AEM-generated markup

### 6.2 Simple CTA

Near-identical to text-callout — serves as a **validation test** that the pattern is replicable. Same structure, same patterns, different class prefix (`simple-cta-*`).

### 6.3 Hero Block

The most complex block (~13KB JS). Uses an **older pattern** (pre-simplification):
- Label-based extraction via switch/case (vs. position-based)
- `querySelectorAll(':scope > div')` (vs. `[...block.children]`)
- Uses SCSS originally (converted to CSS)
- Has desktop/mobile image variants, location input, disclaimer
- Includes standalone `demo.html` with viewport switcher
- 4 screenshots showing responsive behavior

### 6.4 Product Hero Test

Latest test block following updated patterns. Simpler than hero.

---

## 7. Adobe Boilerplate Reference (`aem-boilerplate-xwalk-main`)

### Version: `@adobe/aem-boilerplate` v1.3.0

This is the **unmodified official Adobe starter** included as reference:

#### Core Framework (`aem.js` — ~400 lines)
- `sampleRUM()` — Real User Monitoring beacons to `ot.aem.live`
- `loadBlock()` — Dynamic JS/CSS loading per block folder
- `decorateBlock()` — Block class setup and initialization
- `decorateSections()` — Section processing with metadata
- `decorateButtons()` — Auto primary/secondary/default button decoration
- `createOptimizedPicture()` — Responsive WebP + fallback images
- `wrapTextNodes()` — Text wrapping in paragraphs
- `readBlockConfig()` — Key-value config from block rows

#### Page Orchestration (`scripts.js` — ~130 lines)
- `decorateMain()` — Full page decoration pipeline
- `loadEager()` → `loadLazy()` → `loadDelayed()` — Three-phase loading
- `moveAttributes()` / `moveInstrumentation()` — Universal Editor support
- `loadFonts()` — Session-storage-cached font loading

#### Reference Blocks (6)
| Block | Complexity | Notes |
|-------|-----------|-------|
| Hero | CSS-only (empty JS) | Background image with text overlay |
| Cards | Full JS+CSS | Grid layout with image+body cards |
| Columns | Full JS+CSS | Multi-column layout |
| Fragment | Full JS | Load external HTML fragments |
| Header | Complex JS+CSS | Navigation with hamburger menu |
| Footer | Full JS+CSS | Footer via fragment loading |

#### JSON Merge Build System
```bash
npm run build:json  # Merges models/*.json → component-*.json
```
Uses `merge-json-cli` with spread operator (`...filename.json`) for composable JSON models.

#### Configuration Files
| File | Purpose |
|------|---------|
| `fstab.yaml` | AEM Cloud mount: `author-p130360-e1272151.adobeaemcloud.com` |
| `paths.json` | URL mapping: `/content/aem-boilerplate/` → `/` |
| `helix-query.yaml` | Page index configuration |
| `helix-sitemap.yaml` | Sitemap generation |
| `head.html` | CSP, viewport, script/style loading |
| `package.json` | Dependencies: merge-json-cli, eslint, stylelint, husky |

---

## 8. Architectural Evolution (Key Decisions)

The project went through **4 phases** of learning and correction:

| Phase | What Changed | Why |
|-------|-------------|-----|
| **Phase 1: Initial Analysis** | Built SCSS/tokens/BEM/mitt architecture | Based on theoretical best practices |
| **Phase 2: User Corrections** | Fixed HTML structure understanding, extraction patterns | Production code showed different patterns |
| **Phase 3: Production Integration** | Adopted helper imports, position-based extraction | Real `text-callout` code as reference |
| **Phase 4: Simplification** | Removed SCSS/variables/tokens/BEM → basic CSS, underscore JSON, hyphenated names | AEM EDS has no build pipeline; simplicity wins |
| **Phase 5: Analytics Removal** | Removed all analytics code (dataLayer.js, applyTracking, data-tracking attrs) | Analytics is not needed for this project |

### Final Architecture Rules
| Rule | ✅ Correct | ❌ Wrong |
|------|-----------|---------|
| CSS | Basic `.css` with direct values | SCSS, CSS variables, design tokens |
| JSON naming | `_block-name.json` | `block-name.json` |
| Class names | `block-name-element` (hyphenated) | `block__element--modifier` (BEM) |
| Row access | `[...block.children]` | `querySelectorAll(':scope > div')` |
| Analytics | Not used in this project | dataLayer.js / applyTracking / mitt |
| Boolean fields | `"component": "boolean"` | `"component": "checkbox"` |
| Conditional fields | `"condition": { JSON Logic }` | `"visible": "..."` |
| decorate function | Synchronous | async |
| Clear markup | `block.textContent = ''` | `block.innerHTML = ''` |

---

## 9. Planned 19 Foundational Blocks

### Categorized Block List

| # | Category | Block | Status |
|---|----------|-------|--------|
| 1 | Content | Text | Documented |
| 2 | Content | Title | Documented |
| 3 | Content | Table | Planned |
| 4 | Content | Quote | Phase 2 |
| 5 | Media | Image | Documented |
| 6 | Media | Video | Planned |
| 7 | Media | **Hero** | **Implemented** |
| 8 | Interactive | **CTA** | **Implemented** (simple-cta) |
| 9 | Interactive | CTA Group | Planned |
| 10 | Interactive | Button | May merge with CTA |
| 11 | Interactive | Form | Planned |
| 12 | Interactive | Carousel | Planned |
| 13 | Interactive | Accordion | Planned |
| 14 | Interactive | Tabs | Planned |
| 15 | Interactive | Modal | Planned |
| 16 | Layout | Breadcrumb | Planned |
| 17 | Layout | Pagination | Planned |
| 18 | Layout | Divider | Planned |
| 19 | Advanced | Fragment | In boilerplate |
| 20 | Advanced | Custom Column Section | Planned |

---

## 10. Development Standards & Tooling

### Code Quality
- **ES6+** JavaScript (const/let, arrow functions, template literals, spread)
- **JSDoc** comments on all public functions
- **ESLint** with Airbnb base + xwalk plugin
- **Stylelint** for CSS
- **Husky** pre-commit hooks
- Target: **80%+ unit test coverage** (Vitest, not yet implemented)

### Naming Conventions
| Type | Convention | Example |
|------|-----------|---------|
| Folders | kebab-case | `text-callout/` |
| Files | match folder | `text-callout.js` |
| CSS classes | hyphenated | `.text-callout-inner` |
| JS variables | camelCase | `trackInView` |
| JS constants | UPPER_SNAKE | `DESKTOP_BREAKPOINT` |
| Data attributes | lowercase | `data-option-index` |

### Responsive Breakpoints
| Breakpoint | Width | Purpose |
|-----------|-------|---------|
| Mobile | 320px+ | Base styles (default) |
| Tablet | 768px+ | Two-column layouts |
| Desktop | 1024px+ | Full layouts |
| Large | 1440px+ | Max-width containers |

*(Note: Adobe boilerplate uses a single 900px breakpoint)*

---

## 11. Dependencies

### Current (from boilerplate package.json)
| Package | Version | Purpose |
|---------|---------|---------|
| `merge-json-cli` | 1.0.4 | JSON composition with spread operator |
| `eslint` | 8.57.1 | JavaScript linting |
| `eslint-plugin-xwalk` | GitHub | AEM xwalk-specific rules |
| `stylelint` | 17.0.0 | CSS linting |
| `husky` | 9.1.1 | Git hooks |
| `npm-run-all` | 4.1.5 | Parallel script execution |

### Planned (not yet integrated)
- `vitest` — Unit/integration testing
- `style-dictionary` — Design token generation (may be deprecated post-simplification)

### External Services
- **AEM Cloud**: `author-p130360-e1272151.adobeaemcloud.com`
- **Adobe RUM**: `ot.aem.live` for Real User Monitoring
- **DOMPurify**: HTML sanitization for editor support (bundled)

---

## 12. Research & Analysis Files

The project includes extensive research artifacts:

| File | Content |
|------|---------|
| `aem_eds_comprehensive_documentation_review.md` | Full review of AEM EDS documentation |
| `aem_element_grouping_analysis.md` | How AEM groups elements in markup |
| `aem_live_page_analysis.md` | Analysis of a live AEM EDS page |
| `aem_live_source.html` | Captured HTML source from live site |
| `aem_xwalk_repo_analysis.md` | Analysis of the xwalk boilerplate |
| `AEM_EDS_Tabs_and_JSONMerge_Complete_Guide.md` | Tab configuration and JSON merge guide |

---

## 13. Future Roadmap (4 Phases)

| Phase | Timeline | Focus |
|-------|----------|-------|
| **Phase 1: Foundation** | Weeks 1-5 | 19 blocks + design system |
| **Phase 2: Enhancement** | Weeks 6-10 | Additional blocks + Storybook + Figma |
| **Phase 3: Automation** | Weeks 11-16 | Figma-to-AEM block generator (Electron app) |
| **Phase 4: Scale** | Future | Multi-site, team collaboration |

The **Phase 3 vision** is particularly ambitious: an Electron desktop app that auto-detects Figma components and generates complete AEM EDS blocks (HTML/CSS/JSON/JS) with Git integration.

---

## 14. Key Insights & Observations

### Strengths
1. **Exceptionally thorough documentation** — 45K words covering every pattern
2. **Strong canonical reference** — text-callout block is clean, well-documented
3. **Clear architectural decisions** — every change is documented with rationale
4. **Reusable utilities** — block-helpers.js handles common extraction patterns
5. **HTML examples for every block** — shows assumed AEM-generated markup before JS runs
6. **Accessibility-aware** — WCAG 2.1 AA, reduced-motion, print styles, ARIA

### Areas for Development
1. **No testing framework** yet — Vitest is planned but not implemented
2. **Hero block uses older patterns** — needs migration to match text-callout
3. **Only 2-3 blocks fully implemented** out of 19 planned
4. **No build pipeline** — blocks use basic CSS (conscious choice for simplicity)
5. **Backup duplication** — BLOCK_ER_PROJECT_BACKUP contains copies of main files

### Sections (Phase 6 Addition)

AEM EDS distinguishes between **blocks** (self-contained components) and **sections** (containers that group multiple blocks):

| Aspect | Block | Section |
|--------|-------|---------|
| Purpose | Self-contained component | Groups/wraps multiple blocks |
| Examples | Hero, CTA, Card | Carousel, Accordion, Modal, Tabs |
| JSON location | `blocks/block-name/_block-name.json` | `models/_section-name.json` |
| JS/CSS location | `blocks/block-name/` | `scripts/sections/` |
| resourceType | `.../block/v1/block` | `.../section/v1/section` |
| Config source | HTML rows | `data-` attributes from section-metadata |

**Section metadata processing**: The xwalk framework auto-converts camelCase field names to hyphenated `data-` attributes. For example, `testedAndTried` becomes `data-tested-and-tried` on the section element.

See `SECTIONS_GUIDE.md` for complete documentation.

### JavaScript Standards (Phase 6 Updates)

- **classList over className**: Use `classList.add('class-name')` instead of `className = 'class-name'` to prevent accidentally overwriting existing classes
- **config.mainEl required**: Always set `config.mainEl` in `buildBlock()` to the most important interactive element
- **Ask clarifying questions**: Never assume — always ask before building
- **Post-build steps**: Always run `npm run lint` and `npm run build:json`

### How to Use This Knowledge Base
1. **Start**: Read `SESSION_SUMMARY.md` for full context
2. **Build a block**: Follow `BLOCK_CREATION_STANDARDS.md` and `16-BLOCK_DEVELOPMENT_TEMPLATE.md`
3. **Build a section**: Follow `SECTIONS_GUIDE.md`
4. **Reference**: Use `text_callout_block/` as the canonical pattern
5. **Utilities**: Import from `block-helpers.js`
6. **Standards**: Check `BLOCK_CREATION_STANDARDS.md` for required deliverables
7. **No analytics**: This project does not include analytics — do not add `dataLayer.js` or tracking code
8. **Field types**: Reference https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/universal-editor/field-types#component-types

---

*This analysis covers the complete contents of the AEM_EDS_Blocks_Complete_Knowledge.zip archive, updated with Phase 6 knowledge (sections, classList, mainEl).*
