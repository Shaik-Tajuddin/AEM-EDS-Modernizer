# Changelog

## [2026-04-09] — Phase 6c: Final Section Architecture Correction

> **Critical correction**: Section architecture corrected per direct user guidance.

### Correct Architecture (Phase 6c)
1. Section JSON in `models/_section-name.json` (NOT in `blocks/`)
2. Section JS/CSS in `blocks/section-name/section-name.js/.css`
3. No `_section-name.json` in blocks folder — sections don't have block JSON there
4. Hidden `sectionIdentifier` field → becomes `data-sectionidentifier`
5. Custom `loadSectionModules()` at end of `loadEager()` loads section JS/CSS
6. Section JS: simple decorator → calls focused functions, receives `sectionEl`
7. Carousel/accordion/tabs/modal ARE sections (group multiple blocks)

### Files Updated
- All documentation files corrected (SECTIONS_GUIDE, rules 15/16, BLOCK_CREATION_STANDARDS, DEEP_DIVE, SESSION_SUMMARY, .cursorrules, LEARNINGS, PHASE6_CHANGES_SUMMARY)

### Reference Block Updates
- `product-hero.js` — Added `config.mainEl`
- `hero.js` — Complete rewrite (no BEM, no analytics, mainEl, correct JSON)
- `_hero.json` — Rewritten (no analytics tab, correct format)
- `hero-example.html` — Created (was missing)

---



## [2026-04-09] — Phase 6: Section Knowledge, classList, mainEl & Standards Update

> **Major update**: Added comprehensive section documentation, updated JavaScript standards (classList, mainEl), added clarifying questions requirement, added lint/build steps, and created section reference examples.

### New Knowledge Added

1. **Sections vs Blocks**
   - Sections group multiple blocks (carousel, accordion, modal, tabs)
   - Blocks are self-contained components (hero, CTA, card)
   - Section metadata auto-converts to `data-` attributes (`testedAndTried` → `data-tested-and-tried`)
   - Section files live in different locations: JSON in `models/`, JS/CSS in `scripts/sections/`
   - Section `resourceType`: `core/franklin/components/section/v1/section`

2. **JavaScript Standards Updated**
   - `classList.add()` replaces `className =` in all code (prevents overwriting existing classes)
   - `config.mainEl` must always be set in `buildBlock()` (most important interactive element)

3. **Workflow Standards Updated**
   - Always ask clarifying questions before building (never assume)
   - Suggest sections when appropriate (with reasoning)
   - Always run `npm run lint` and `npm run build:json` after block creation
   - Reference Adobe field types documentation

### New Files Created

| File | Location | Purpose |
|------|----------|---------|
| `15-sections-vs-blocks.mdc` | `.cursor/rules/` | When to use sections vs blocks |
| `16-section-creation-pattern.mdc` | `.cursor/rules/` | Section creation standards |
| `SECTIONS_GUIDE.md` | Analysis root | Complete sections guide |
| `_section-carousel.json` | `reference-sections/` | Example section JSON model |
| `section-carousel.js` | `reference-sections/` | Example section JavaScript |
| `section-carousel.css` | `reference-sections/` | Example section CSS |
| `section-carousel/README.md` | `reference-sections/` | Section example docs |
| `PHASE6_CHANGES_SUMMARY.md` | Analysis root | Summary of all Phase 6 changes |

### Files Updated

| File | Changes |
|------|---------|
| `.cursorrules` (both locations) | Added rules 8-11: classList, mainEl, clarify, lint/build; added section references |
| `02-block-javascript-pattern.mdc` | classList.add() examples, mainEl requirement |
| `07-naming-conventions.mdc` | E-commerce naming (optional) |
| `08-block-creation-checklist.mdc` | Clarifying questions, lint/build, section check, field types ref |
| `14-learnings-and-pitfalls.mdc` | Phase 6 in evolution, classList/mainEl corrections |
| `BLOCK_CREATION_STANDARDS.md` | Sections section, classList, mainEl, updated checklist |
| `DEEP_DIVE_ANALYSIS.md` | Section knowledge, Phase 6 JS standards |
| `SESSION_SUMMARY.md` | Section patterns, updated architecture decisions |
| `LEARNINGS.md` | Phase 6 learnings added |
| `text-callout.js` (both) | `className =` → `classList.add()` |
| `simple-cta.js` (both) | `className =` → `classList.add()` |
| `product-hero.js` (both) | `className =` → `classList.add()` |
| `hero.js` (both) | `className =` → `classList.add()` |

---

## [2026-04-09] — Analytics Removal & HTML Examples

> **Major change**: Removed all analytics code and configuration from the project. Added HTML example files as a mandatory deliverable for every block.

### Changes

1. **Analytics removed entirely**
   - `dataLayer.js` archived (renamed to `.archived`)
   - `DATALAYER_GUIDE.md` archived
   - `applyTracking()` calls removed from all block JS files
   - Analytics tab removed from all JSON models
   - `data-trackinview`, `data-trackclick`, and meta attributes removed from generated HTML
   - `trackInview`, `trackClick`, and meta fields removed from all JSON templates and models

2. **HTML example files added to every block**
   - `text-callout-example.html` — promotional callout example
   - `simple-cta-example.html` — newsletter CTA example
   - `product-hero-example.html` — Galaxy S26 banner example

3. **BLOCK_CREATION_STANDARDS.md created**
   - Documents the Block Quad: JS, CSS, JSON, HTML example
   - Provides templates and checklists for new blocks
   - Explicitly notes analytics is NOT part of this project

4. **JS flow updated across all blocks**
   - Before: `extractConfig() → buildBlock() → appendEvents() → applyTracking()`
   - After: `extractConfig() → buildBlock() → appendEvents()`

5. **Documentation updated**
   - `DEEP_DIVE_ANALYSIS.md` — analytics sections replaced, Block Quad documented
   - `SESSION_SUMMARY.md` — analytics references removed, new standards noted
   - `LEARNINGS.md` — Phase 5 added for analytics removal
   - All block `README.md` files updated

### Affected Files
- `text_callout_block/text-callout.js` — removed dataLayer import and applyTracking
- `text_callout_block/_text-callout.json` — removed Analytics tab
- `simple_cta_block/simple-cta.js` — removed dataLayer import and applyTracking
- `simple_cta_block/_simple-cta.json` — removed Analytics tab
- `product_hero_test/product-hero.js` — removed dataLayer import and applyTracking
- `product_hero_test/_product-hero.json` — removed Analytics tab
- `utilities/dataLayer.js` → `utilities/dataLayer.js.archived`
- `utilities/DATALAYER_GUIDE.md` → `utilities/DATALAYER_GUIDE.md.archived`

---

## [2026-04-09] — Architectural Simplification: Basic CSS Only

> **Major architectural change**: Simplified the entire styling and file naming approach. Removed SCSS, CSS variables, and design tokens. Adopted basic CSS with direct values.

### Three Key Changes

1. **JSON File Naming Convention** — Block JSON files now require underscore prefix
   - `text-callout.json` → `_text-callout.json`
   - `simple-cta.json` → `_simple-cta.json`
   - `hero.json` → `_hero.json`

2. **File Naming Must Match Folder** — All block files use the folder name
   - Folder `text-callout/` → `text-callout.js`, `text-callout.css`, `_text-callout.json`

3. **Basic CSS Only** — Removed all SCSS, CSS variables, and design tokens
   - ❌ No SCSS (`.scss` files, `$variables`, `@include`, nesting)
   - ❌ No CSS custom properties (`var(--anything)`)
   - ❌ No design token system (3-layer architecture)
   - ❌ No Figma tokens
   - ✅ Basic CSS with direct values (`#0066cc`, `1.5rem`)

### Files Changed

#### Block Files
- **Renamed**: `text_callout_block/text-callout.json` → `_text-callout.json`
- **Renamed**: `simple_cta_block/simple-cta.json` → `_simple-cta.json`
- **Renamed**: `hero_block/hero.json` → `_hero.json`
- **Renamed + Rewritten**: `simple_cta_block/simple-cta.scss` → `simple-cta.css` (basic CSS)
- **Renamed + Rewritten**: `hero_block/hero.scss` → `hero.css` (basic CSS)
- **Created**: `text_callout_block/text-callout.css` (basic CSS)
- **Updated**: All block README.md files

#### Documentation Files
- **Archived**: `04-DESIGN_TOKENS_SYSTEM.md` (deprecation notice added)
- **Rewritten**: `05-SCSS_STYLING_APPROACH.md` → `05-CSS_STYLING_APPROACH.md`
- **Rewritten**: `00-HOLISTIC_VISION.md` (removed token/SCSS references)
- **Updated**: `02-JSON_CONFIGURATION.md` (underscore prefix noted)
- **Updated**: `03-BLOCK_JAVASCRIPT_PATTERN.md` (file naming note)
- **Rewritten**: `06-BLOCK_DESIGN_PHILOSOPHY.md` (removed token philosophy)
- **Rewritten**: `10-PROJECT_STRUCTURE.md` (new file naming conventions)
- **Updated**: `12-DEVELOPMENT_PATTERNS.md` (CSS standards, checklists)
- **Rewritten**: `13-RESPONSIVE_DESIGN_STRATEGY.md` (basic CSS media queries)
- **Updated**: `14-FUTURE_ROADMAP.md` (removed Figma token references)
- **Updated**: `16-BLOCK_DEVELOPMENT_TEMPLATE.md` (basic CSS, underscore prefix)
- **Updated**: `AEM_EDS_Documentation/README.md`

#### New Files
- **Created**: `MIGRATION_GUIDE.md` (conversion guide: SCSS → CSS, file naming)

#### Root Files
- **Updated**: `LEARNINGS.md` (Phase 4: Architectural Simplification)
- **Updated**: `CHANGELOG.md` (this entry)

#### Utilities
- **Updated**: `utilities/INTEGRATION_PLAN.md` (SCSS → CSS references)

---

## [2026-04-09] — Integrated Production Reference Implementation

> **Major milestone**: Real production files (text-callout block) analyzed and integrated as the definitive pattern source for all future block development.

### Production Files Analyzed
- `text-callout.js` — Complete block JavaScript with helpers and analytics
- `text-callout.json` — Complete block JSON with boolean, condition, tabs
- `block-helpers.js` — Updated utility helpers (added getBooleanFromRow)
- `dataLayer.js` — Analytics utilities (applyTracking, pushToDataLayer, etc.)

### New Files Created
- `text_callout_block/text-callout.js` — Production reference copy
- `text_callout_block/text-callout.json` — Production reference copy
- `text_callout_block/README.md` — Reference documentation with pattern comparison
- `utilities/dataLayer.js` — Analytics utilities (new)
- `AEM_EDS_Documentation/16-BLOCK_DEVELOPMENT_TEMPLATE.md` — Step-by-step block creation guide

### Documentation Fully Rewritten
- **02-JSON_CONFIGURATION.md** — Rewritten with production patterns: `boolean` component, `condition` (JSON Logic), `valueType`, template default strategy, row layout mapping, complete text-callout.json example
- **03-BLOCK_JAVASCRIPT_PATTERN.md** — Rewritten with production patterns: helper imports, `[...block.children]`, row position mapping, data-attribute analytics, hyphenated class naming, synchronous decorate
- **09-ANALYTICS_PATTERN.md** — Rewritten with dataLayer.js API: `applyTracking()`, data attributes pattern, `pushToDataLayer()`, `getMetaFromElement()`, complete integration flow
- **12-DEVELOPMENT_PATTERNS.md** — Added complete block development workflow section and production conventions checklist

### Updated Files
- `utilities/block-helpers.js` — Replaced with uploaded production version
- `simple_cta_block/simple-cta.js` — Rewritten to match text-callout patterns (helpers, `[...block.children]`, data attrs, applyTracking)
- `simple_cta_block/simple-cta.json` — Rewritten with `boolean`, `condition`, `valueType`, production template structure
- `LEARNINGS.md` — Complete rewrite with 3-phase learning history and correction tables

### Critical Pattern Corrections
| Previous Pattern | Corrected Pattern |
|---|---|
| `checkbox` component | `boolean` component |
| `visible` property | `condition` with JSON Logic |
| `querySelectorAll(':scope > div')` | `[...block.children]` |
| mitt emitter analytics | Data attributes + `applyTracking()` |
| BEM class naming (`__`, `--`) | Hyphenated naming (`block-name-element`) |
| `async function decorate` | Synchronous `function decorate` |
| All fields in template | Only fields with meaningful defaults |
| Manual querySelector extraction | Helper functions from block-helpers.js |

---

## [2026-04-09] — Critical Corrections Based on User Feedback

### Block Implementation Files
- **simple-cta.json**: Fixed definition structure, corrected field components, added proper classes notation
- **simple-cta.js**: Fixed extraction to use position-based div>div nesting, innerHTML for richtext
- **simple-cta.scss**: Added style variants and alignment classes
- **demo.html**: Updated to reflect correct AEM HTML structure

### Documentation
- **02-JSON_CONFIGURATION.md**: Added appendices for correct definition structure, field component guide, classes notation
- **03-BLOCK_JAVASCRIPT_PATTERN.md**: Corrected extraction examples, added appendices for minimal block and DOM extraction types
- **05-SCSS_STYLING_APPROACH.md**: Added appendix for block state classes and BEM naming

---

## [2026-04-09] — Helper Utilities Integration

### New Files
- `utilities/block-helpers.js` — Production helper utilities
- `utilities/HELPERS_GUIDE.md` — Comprehensive API reference
- `utilities/USAGE_EXAMPLES.md` — 9 practical usage examples
- `utilities/INTEGRATION_PLAN.md` — Documentation integration roadmap
- `simple_cta_block/simple-cta-with-helpers.js` — Refactored block using helpers

---

## [2026-04-07] — Initial Analysis Complete

### Created
- Complete AEM EDS Documentation (15 markdown files + PDFs)
- COMPREHENSIVE_ANALYSIS_REPORT.md
- Initial simple-cta block implementation
- Project structure and all core documentation