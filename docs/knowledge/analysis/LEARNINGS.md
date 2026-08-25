# AEM EDS Block Development — Cumulative Learnings

> Last updated: 2026-04-09 — Phase 6c: Final Section Architecture Correction

---

## Phase 6c: Final Section Architecture Correction

### Evolution of Section Understanding
1. **Phase 6** (wrong): Sections have JS/CSS in `scripts/sections/`, per-section JSON in `models/`
2. **Phase 6b** (wrong): Sections have NO JS/CSS at all, ONE shared `_section.json`
3. **Phase 6c** (correct): Sections have JS/CSS in `blocks/`, per-section JSON in `models/`, loaded at end of `loadEager()`

### Correct Section Architecture
- **Section JSON**: `models/_section-name.json` — NOT in `blocks/` folder
- **Section JS/CSS**: `blocks/section-name/section-name.js/.css` — same folder pattern as blocks
- **No `_*.json` in section's blocks folder**: Section blocks do NOT have a `_block-name.json` file
- **Hidden `sectionIdentifier` field**: Every section model has this; becomes `data-sectionidentifier`
- **Loaded at end of `loadEager()`**: Custom `loadSectionModules()` finds sections with `data-sectionidentifier`, loads JS/CSS from `blocks/`
- **Section JS pattern**: Simple decorator receives `sectionEl`, calls focused functions (NOT extractConfig → buildBlock → appendEvents)
- **Carousel/accordion/tabs/modal ARE sections**: They group multiple blocks with shared behaviour

---

## Phase 6: classList, mainEl & Standards Update (Still Valid)

### Changes
1. **classList over className** — All reference blocks updated to use `classList.add()` instead of `className =`
2. **mainEl requirement** — `config.mainEl` must always be set in `buildBlock()`
3. **Ask clarifying questions** — Never assume; always ask users before building
4. **Run lint & build:json** — Post-creation: `npm run lint` and `npm run build:json`
5. **Adobe field types reference** — https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/universal-editor/field-types#component-types
6. **E-commerce naming** — Optional domain-specific names for e-commerce blocks

### Files Updated in Phase 6 + 6c
- `.cursorrules` — Rules 8-11, corrected rule 10 with section guidance
- `SECTIONS_GUIDE.md` — Complete rewrite with correct architecture
- `15-sections-vs-blocks.mdc` — Corrected file structure and loading mechanism
- `16-section-creation-pattern.mdc` — Full section creation pattern with `loadSectionModules()`
- `BLOCK_CREATION_STANDARDS.md` — Corrected sections table
- `SESSION_SUMMARY.md` — Corrected section patterns
- `DEEP_DIVE_ANALYSIS.md` — Corrected section knowledge
- All reference block JS files — `className =` → `classList.add()`, `config.mainEl` added

### classList Correction
```javascript
// ❌ WRONG (Phase 1-5 pattern)
inner.className = 'block-name-inner';

// ✅ CORRECT (Phase 6+ pattern)
inner.classList.add('block-name-inner');
```

### mainEl Pattern
```javascript
function buildBlock(block, config) {
  const inner = document.createElement('div');
  inner.classList.add('block-name-inner');
  // ... build DOM ...
  block.textContent = '';
  block.appendChild(inner);
  
  // REQUIRED: Set mainEl for appendEvents
  config.mainEl = inner; // or specific interactive element
}
```

---

## Phase 5: Analytics Removal & HTML Examples

### Changes
1. **All analytics code removed** — `dataLayer.js` archived, `applyTracking()` calls removed from all blocks
2. **Analytics tab removed** from all JSON models — no `trackInview`, `trackClick`, or meta fields
3. **HTML example files added** to every block — mandatory deliverable per `BLOCK_CREATION_STANDARDS.md`
4. **JS flow simplified** to `extractConfig() → buildBlock() → appendEvents()` (no `applyTracking()` step)
5. **`BLOCK_CREATION_STANDARDS.md` created** — documents the Block Quad (JS, CSS, JSON, HTML example)

### Why
- Analytics is not needed for this project
- HTML example files document the AEM ↔ JS contract
- Simplifies blocks by removing tracking attributes and data-tracking code

---

## Phase 4: Architectural Simplification

### Three Key Changes

1. **JSON File Naming Convention** — Underscore prefix required
2. **File Names Must Match Folder** — Predictable, consistent naming
3. **Basic CSS Only** — No SCSS; **global** CSS variables in **`styles/`** for colors, spacing, and typography (**`30-block-css-design-tokens.mdc`**)

### Why We Simplified

| Previous Approach | Problem | New Approach |
|---|---|---|
| SCSS with `$variables` | Requires build pipeline, adds complexity | Basic `.css` files (no SCSS) |
| Ad hoc `var(--name)` per block | Hard to theme consistently | **`var(--color-*)`**, **`var(--space-*)`**, type tokens from **`styles/`** |
| 3-layer design token build | Over-engineered for AEM EDS | **Centralized** tokens in **`styles/colors.css`** + **`styles/styles.css`** |
| `block-name.json` | Didn't match AEM EDS boilerplate convention | `_block-name.json` |
| Arbitrary file names | Confusing, inconsistent | All files match folder name |

### What Changed

#### Files Renamed
- `text-callout.json` → `_text-callout.json`
- `simple-cta.json` → `_simple-cta.json`
- `hero.json` → `_hero.json`
- `simple-cta.scss` → `simple-cta.css` (rewritten as basic CSS)
- `hero.scss` → `hero.css` (rewritten as basic CSS)
- New: `text-callout.css` (created)

#### Documentation Rewritten
- `04-DESIGN_TOKENS_SYSTEM.md` → Archived with deprecation notice
- `05-SCSS_STYLING_APPROACH.md` → Rewritten as CSS-only guide
- `06-BLOCK_DESIGN_PHILOSOPHY.md` → Removed token philosophy
- `10-PROJECT_STRUCTURE.md` → New file naming conventions
- `13-RESPONSIVE_DESIGN_STRATEGY.md` → Basic CSS media queries
- `14-FUTURE_ROADMAP.md` → Removed Figma token references
- Multiple other docs updated with underscore prefix and CSS-only notes

#### New Files
- `MIGRATION_GUIDE.md` — How to convert SCSS to CSS, rename files
- `text_callout_block/text-callout.css` — CSS reference implementation

### Rule Summary

```
✅ _text-callout.json     (underscore prefix)
✅ text-callout.css        (basic CSS, matches folder)
✅ text-callout.js         (matches folder)
✅ color: var(--color-accent-cta)   (global token)
✅ @media (min-width: 768px)  (standard query)

❌ text-callout.json       (missing underscore)
❌ text-callout.scss       (no SCSS)
❌ color: #0066cc          (raw UI color in `blocks/**/*.css`)
❌ $breakpoint-tablet      (no SCSS variables)
❌ @include respond-to()   (no mixins)
❌ styles.css              (doesn't match folder)
```

---

## Phase 3: Production Reference Integration (text-callout)

### Source
Four production files analyzed:
- `text-callout.js` — Complete block JavaScript
- `text-callout.json` — Complete block JSON configuration
- `block-helpers.js` — Updated utility helpers
- `dataLayer.js` — Analytics/tracking utilities (**now archived — not used**)

### Critical Corrections from Production Code

#### JSON Configuration

| What We Had | What Production Uses | Impact |
|---|---|---|
| `checkbox` component for toggles | **`boolean`** component | Must use `boolean` in all JSON configs |
| `visible` property for conditional fields | **`condition`** with JSON Logic | Syntax is `{"==": [{"var": "field"}, true]}` |
| All fields listed in template | **Only fields with meaningful defaults** | Omit empty fields (id, ctaLink, meta fields) |
| `value` on every model field | **`value` only on select fields** | Other fields rely on template defaults |
| Tab `name` like `"general"` | **`"tabGeneral"` (camelCase with prefix)** | Follow production naming |
| No `valueType` on select | **`valueType: "string"`** on all selects | Required for proper value handling |
| Separate CTA text + URL fields | **`ctaLink` (aem-content) + `ctaContent` (richtext)** | Link and label are separate |

#### JavaScript Patterns

| What We Had | What Production Uses | Impact |
|---|---|---|
| `querySelectorAll(':scope > div')` | **`[...block.children]`** | Simpler, more direct row access |
| Manual `querySelector('div')` extraction | **Helper functions** (`getTextFromRow`, `getHtmlFromRow`, etc.) | Cleaner, consistent extraction |
| BEM class naming (`__`, `--`) | **Hyphenated naming** (`block-name-element`) | Follow production convention |
| `async function decorate` | **Synchronous `function decorate`** | No async unless truly needed |
| mitt emitter for analytics | **Data attributes + `applyTracking()`** | (**Now removed** — analytics not used) |
| `innerHTML = ''` to clear | **`textContent = ''`** to clear | More efficient DOM clearing |
| Building `<a>` elements from scratch | **Reusing `<a>` from AEM** when available | `getAnchorFromRow()` pattern |
| `block.classList.add('--ready')` | **No `--ready` class** in production | Removed from pattern |

#### Analytics Pattern (ARCHIVED — No longer used)

> All analytics has been removed from this project. See Phase 5 above.

### Key Architectural Insights

2. **CTA element reuse**: When `aem-content` provides a real `<a>` element, the production code reuses it directly (just updates innerHTML and adds classes). Only creates a `<button>` as fallback.

3. **Row position documentation**: The JSDoc comment above `extractConfig` is the single most important documentation in a block — it maps JSON model fields to extraction positions.

4. **`config.mainEl` mutation**: During build, the CTA element is stored on `config.mainEl` so `appendEvents` can bind to it. This is a config-mutation pattern, not a return value.

5. **Helpers abstract the div-nesting**: `getHtmlFromRow(row)` checks for inner `<div>` and returns its innerHTML. This abstracts away the AEM-specific `<div><div>content</div></div>` nesting.

---

## Phase 2: User Feedback Corrections (Earlier)

### HTML Structure
- AEM generates `<div><div>value</div></div>` nested structure
- **No label divs** — extraction is purely position-based
- Style classes applied directly to outer block `<div>` by AEM runtime
- Field order in HTML matches model field order in JSON

### JSON Structure
- Top-level keys: `definitions`, `models`, `filters` (no `groups`)
- `resourceType` is always `"core/franklin/components/block/v1/block"`
- `richtext` for all content text, `aem-content` for URLs, `reference` for images
- `classes` and `classes_*` fields for style variants (auto-applied, no rows)

---

## Phase 1: Initial Analysis (Repository)

### Project Structure
- 19 blocks planned, Hero most implemented
- Design token system (3 layers: common → semantic → block)
- Analytics via configuration-driven approach
- SCSS with mobile-first strategy
- 3-phase loading: eager → lazy → delayed

### Helper Utilities
- `block-helpers.js` — 6 categories of helpers
- Row extractors (index-based and single-row)
- Responsive helpers with debounce
- Block grouping for accordion/carousel patterns
- Toggle/expand with ARIA
- Environment detection (author mode)

---

## Quick Reference: What to Use When

| Need | Use | Import |
|---|---|---|
| Extract plain text | `getTextFromRow(rows[N])` | `block-helpers.js` |
| Extract rich HTML | `getHtmlFromRow(rows[N])` | `block-helpers.js` |
| Extract boolean | `getBooleanFromRow(rows[N])` | `block-helpers.js` |
| Extract link | `getLinkFromRow(rows[N])` or custom | `block-helpers.js` |
| Extract image | `getImageFromRow(rows[N])` | `block-helpers.js` |
| Toggle field | `boolean` component | JSON config |
| Conditional visibility | `condition` with JSON Logic | JSON config |
| Style variants | `classes` / `classes_*` fields | JSON config |