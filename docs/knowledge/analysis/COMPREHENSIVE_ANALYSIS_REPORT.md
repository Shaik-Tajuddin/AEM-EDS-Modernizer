# AEM EDS Blocks Repository - Comprehensive Analysis Report

**Generated**: April 9, 2026  
**Repository**: AEM EDS Blocks Repository Backup  
**Total Files**: 247 files across multiple directories  

---

## 1. Repository Overview & Organization

The repository is a comprehensive AEM Edge Delivery Services (EDS) blocks project that includes:

### Top-Level Structure
```
aem_eds_analysis/
├── AEM_EDS_Documentation/          # 15 MD + 15 PDF documentation files
├── BLOCK_ER_PROJECT_BACKUP/        # Full project backup with 4 subdirectories
├── BLOCK_ER_PROJECT_BACKUP_FOR_SHARING/  # Sharing-ready copy of backup
├── Uploads/
│   └── aem-boilerplate-xwalk-main/ # Reference xwalk boilerplate repository
├── hero_block/                     # Standalone hero block implementation
├── lib-franklin.js                 # Franklin library file
├── Various research/analysis .md and .pdf files
└── Screenshots (.png files)
```

### Key Directories

| Directory | Purpose | File Count |
|-----------|---------|------------|
| `AEM_EDS_Documentation/` | Core documentation system (15 numbered guides) | 32 files (MD + PDF) |
| `Uploads/aem-boilerplate-xwalk-main/` | Adobe's xwalk boilerplate reference repo | ~50 files |
| `hero_block/` | Production-ready Hero block implementation | 15 files |
| `BLOCK_ER_PROJECT_BACKUP/` | Complete project backup | ~50 files |

---

## 2. Documentation System (15 Guides)

The documentation is a **~45,000-word, 200+ page** comprehensive system organized as 15 numbered files:

| # | File | Topic | Key Content |
|---|------|-------|-------------|
| 00 | HOLISTIC_VISION | Project Overview | 19 blocks, 4-phase roadmap, success criteria |
| 01 | FUNDAMENTALS | AEM EDS Concepts | Sections, blocks, flat structure, nesting rules |
| 02 | JSON_CONFIGURATION | Models/Definitions/Filters | Tab components, merge-json-cli, conditional visibility |
| 03 | BLOCK_JAVASCRIPT_PATTERN | Standard JS Pattern | extractConfig → buildBlock → appendEventListeners |
| 04 | DESIGN_TOKENS_SYSTEM | 3-Layer Token Architecture | Common → Semantic → Blocks layers |
| 05 | SCSS_STYLING_APPROACH | SCSS Organization | Mixins, utilities, mobile-first strategy |
| 06 | BLOCK_DESIGN_PHILOSOPHY | Design Principles | Modularity, authoring-first, accessibility |
| 07 | FOUNDATIONAL_BLOCKS | 19 Block Specifications | Categories, descriptions, implementation order |
| 08 | BLOCK_SPECIFICATIONS | Detailed Block Specs | Text, Title, Image blocks with full code |
| 09 | ANALYTICS_PATTERN | Tracking System | trackInView, trackClick, trackClick_meta patterns |
| 10 | PROJECT_STRUCTURE | Repository Organization | Complete directory structure, naming conventions |
| 11 | IMPROVEMENTS_TO_REFERENCE | xwalk Improvements | Critical/high/medium priority enhancements |
| 12 | DEVELOPMENT_PATTERNS | Best Practices | Code style, testing, security, debugging |
| 13 | RESPONSIVE_DESIGN_STRATEGY | Mobile-First Approach | Breakpoints, patterns, typography scaling |
| 14 | FUTURE_ROADMAP | 4-Phase Plan | Foundation → Enhancement → Automation → Scale |

---

## 3. Reference Repository (aem-boilerplate-xwalk-main)

### Overview
The Adobe xwalk boilerplate is the **starting point/reference** for the custom block system. It's version **1.3.0** of `@adobe/aem-boilerplate`.

### Blocks in Reference Repo (6 blocks)

| Block | Has JS | Has CSS | Has JSON Model | Description |
|-------|--------|---------|----------------|-------------|
| **Hero** | Empty file | ✅ Full CSS | ✅ (image, imageAlt, text) | Background image with overlay text |
| **Cards** | ✅ Full | ✅ Full | ✅ (image, text per card) | Grid of cards with image+body |
| **Columns** | ✅ Full | ✅ Full | ✅ (columns count, rows count) | Multi-column layout |
| **Fragment** | ✅ Full | ✅ (empty) | ✅ (reference) | Load external HTML fragments |
| **Header** | ✅ Full (complex) | ✅ Full (complex) | N/A (uses fragment) | Site navigation with hamburger |
| **Footer** | ✅ Full | ✅ Full | N/A (uses fragment) | Site footer via fragment |

### Core Scripts

| Script | Size | Purpose |
|--------|------|---------|
| `aem.js` | ~400 lines | Core EDS framework: block loading, decoration, sections, RUM, utilities |
| `scripts.js` | ~80 lines | Page initialization: loadEager → loadLazy → loadDelayed |
| `editor-support.js` | ~120 lines | Universal Editor live editing support |
| `editor-support-rte.js` | ~60 lines | Rich text editor instrumentation grouping |
| `delayed.js` | Empty | Placeholder for deferred functionality |
| `dompurify.min.js` | Library | HTML sanitization for editor support |

### Key Framework Functions (aem.js)
- `sampleRUM()` - Real User Monitoring with beacon
- `loadBlock()` - Dynamic JS/CSS loading per block
- `decorateBlock()` - Block initialization and class setup
- `decorateSections()` - Section processing and metadata
- `decorateButtons()` - Auto button decoration (primary/secondary/default)
- `createOptimizedPicture()` - Responsive image with WebP + fallback
- `wrapTextNodes()` - Text wrapping in paragraphs
- `readBlockConfig()` - Key-value config extraction from block rows
- `loadCSS()` / `loadScript()` - Dynamic asset loading

### Key Utility Functions (scripts.js)
- `moveAttributes()` - Transfer attributes between elements
- `moveInstrumentation()` - Transfer `data-aue-*` and `data-richtext-*` attributes
- `decorateMain()` - Orchestrates full page decoration
- Three-phase loading: `loadEager()` → `loadLazy()` → `loadDelayed()`

### Configuration Files

| File | Purpose |
|------|---------|
| `component-definition.json` | Defines all available components (Text, Title, Image, Button, Section, Cards, Columns, Fragment, Hero) |
| `component-models.json` | Field definitions for each component (9 models: page-metadata, image, title, button, section, card, columns, fragment, hero) |
| `component-filters.json` | Content structure rules (main→section, section→blocks, cards→card, columns→column→content) |
| `fstab.yaml` | AEM Cloud mount point configuration |
| `paths.json` | URL path mappings (`/content/aem-boilerplate/` → `/`) |
| `helix-query.yaml` | Query index configuration (pages index) |
| `helix-sitemap.yaml` | Sitemap generation config |
| `head.html` | CSP headers, viewport, script/style loading |
| `package.json` | Dependencies & build scripts (merge-json-cli, eslint, stylelint, husky) |

### JSON Merge System
The repo uses `merge-json-cli` for composing JSON files:
- Source models in `models/` directory (with `_` prefix for shared patterns)
- Build command: `npm run build:json` merges to root `component-*.json` files
- Supports spread operator (`...filename.json`) for reusable patterns

### Styles
- **styles.css**: CSS custom properties (colors, fonts, body/heading sizes, nav height), base typography, button styles, section layout
- **fonts.css**: Roboto font family (regular, medium, bold, condensed-bold) in WOFF2
- **lazy-styles.css**: Empty (for post-LCP styles)
- **Breakpoint**: Single breakpoint at `900px` (mobile → desktop)
- **Font families**: `roboto` (body), `roboto-condensed` (headings) with fallback fonts

---

## 4. Hero Block Implementation (Production-Ready)

### Overview
A fully implemented, production-ready Hero block (~60KB total) based on a BOTOX® Cosmetic design reference.

### Files

| File | Size | Purpose |
|------|------|---------|
| `hero.js` | 13 KB | Complete block decoration with extract/build/listen pattern |
| `hero.scss` | 12 KB | Mobile-first responsive styles with design tokens |
| `hero.json` | 8.5 KB | Block model with General/Appearance/Analytics tabs |
| `README.md` | 18 KB | Complete documentation |
| `IMPLEMENTATION_PATTERNS.md` | 8 KB | Architecture quick reference |
| `SUMMARY.md` | ~10 KB | Overview and checklist |
| `demo.html` | ~15 KB | Standalone demo with viewport switcher |
| Screenshots | 4 files | Desktop, mobile, CTA hover, interaction states |

### Hero Block Features
- **Content**: Eyebrow (RTE), headline, description, disclaimer, CTA button, optional location input
- **Images**: Desktop + mobile variants with fallback logic
- **Variants**: Default, Dark, Light, Gradient themes
- **Analytics**: In-view tracking (IntersectionObserver), CTA click tracking, location input tracking via mitt
- **Responsive**: Mobile-first with CSS order-based layout switching
- **Accessibility**: WCAG 2.1 AA, keyboard navigation, reduced motion support, semantic HTML

### Hero JSON Model Structure
```
General Tab:
  - id, eyebrow (RTE), headline, description, disclaimer
  - image, imageAlt, imageMobile, imageMobileAlt
  - ctaText, ctaUrl, showLocation (boolean), locationPlaceholder

Appearance Tab:
  - styles (select: default/dark/light/gradient)

Analytics Tab:
  - trackingId, trackingLabel, trackInView (boolean), trackClick (boolean)
```

### Hero JavaScript Pattern
```javascript
export default async function decorate(block) {
  const config = extractConfig(block);   // Extract by DOM row position
  buildBlock(block, config);              // Create clean HTML structure
  appendEventListeners(block, config);    // Analytics + interactions
  block.classList.add('hero--ready');
}
```

### Hero SCSS Token System
Uses CSS custom properties prefixed with `--hero-*`:
- Spacing tokens: `--hero-spacing-xs` through `--hero-spacing-3xl`
- Typography tokens: Font families (serif + sans), sizes (mobile/tablet/desktop), weights
- Color tokens: Text, background, border, button colors
- Semantic tokens: `--hero-bg-color`, `--hero-text-color`, `--hero-cta-color`
- Breakpoints: 320px (mobile), 768px (tablet), 1024px (desktop), 1440px (large)

---

## 5. Planned 19 Foundational Blocks

### Categories and Blocks

**Content & Text (4):**
1. Text - RTE content with color/size/alignment
2. Title - Heading with level selection via RTE
3. Table - Structured data display
4. Quote - Testimonials/quotes (Phase 2)

**Media (3):**
5. Image - Desktop/mobile variants, captions, overlays
6. Video - DAM + YouTube/Vimeo embedding
7. Hero - Background image/video hero section (Phase 2, but implemented)

**Interactive (8):**
8. CTA - Call-to-action with 4-5 variants
9. CTA Group - Multiple CTAs together
10. Button - Single button (may merge with CTA)
11. Form - Multi-field with validation
12. Carousel - Rotating content with navigation
13. Accordion - Collapsible sections
14. Tabs - Tabbed content interface
15. Modal - Dialog with overlay

**Layout & Navigation (3):**
16. Breadcrumb - Hierarchical navigation
17. Pagination - Multi-page navigation
18. Divider - Visual separator/spacer

**Advanced (2):**
19. Fragment - Load external HTML
20. Custom Column Section - Flexible multi-column

### Implementation Timeline
- **Week 1**: Project setup, tokens, JS pattern
- **Week 2**: Text, Title, Image
- **Week 3**: CTA, Form, Table
- **Week 4**: Carousel, Accordion, Tabs, Video
- **Week 5**: Breadcrumb, Pagination, Divider, Fragment, CTA Group, Custom Column
- **Week 6**: Testing, documentation, refinement

---

## 6. Architecture & Design Patterns

### Three Core Patterns

#### 1. JavaScript Block Pattern
```
extractConfig(block) → buildBlock(config) → appendEventListeners(config) → DOM Manipulation
```
- `extractConfig`: Pure extraction from AEM-generated HTML, no side effects
- `buildBlock`: Prepare data structures and elements (no DOM manipulation)
- `appendEventListeners`: Attach handlers, emit events via mitt
- Config attached to element for future access: `block.blockConfig = config`

#### 2. JSON Configuration Pattern
```
Models (authoring fields) + Definitions (metadata) + Filters (structure rules)
```
- **Tab organization**: General → Appearance → Analytics (every block)
- **Reusable patterns**: `_shared-general-fields.json`, `_appearance-defaults.json`, `_analytics.json`
- **JSON merge**: `merge-json-cli` with spread operator (`...filename.json`)
- **Conditional visibility**: `"visible": "{trackInView}"` JSON logic

#### 3. Design Token Pattern (3-Layer)
```
Common (raw values) → Semantic (context names) → Blocks (component-specific)
```
- **Common**: `$color-blue-600`, `$font-size-base`, `$spacing-md`
- **Semantic**: `$text-primary`, `$bg-inverse`, `$border-default`
- **Blocks**: `.block-text { --text-color: var(--text-default); }`
- Optional **Style Dictionary** integration for multi-platform generation

### Event-Driven Architecture
- Uses **mitt** lightweight event emitter for cross-block communication
- Blocks emit events, not call each other directly
- Pattern: `emitter.emit('cta:clicked', data)` → `emitter.on('cta:clicked', handler)`

### Three-Phase Loading
1. **Eager**: Critical above-fold content (hero, first section)
2. **Lazy**: Below-fold content (loaded on scroll)
3. **Delayed**: Interactive features (forms, modals) loaded 3s after page load

### Authoring Approach
- **AEM + Universal Editor** implementation (not document-based)
- All blocks configurable through visual UI, zero code for authors
- Consistent tab organization across all blocks
- RTE for formatted content, select for predefined options
- Conditional field visibility based on other field values

---

## 7. Analytics System

### Three Tracking Types

| Type | Purpose | Use Case | Implementation |
|------|---------|----------|----------------|
| `trackInView` | Viewport impression | Content engagement | IntersectionObserver (50% threshold) |
| `trackClick` | Single click | CTA buttons | Click event handler |
| `trackClick_meta` | Multiple clicks | Links in RTE content | Per-link config, `-` to skip |

### Configuration-Driven
Authors configure tracking through JSON metadata in the Analytics tab:
```json
{
  "trackInView": true,
  "trackInView_meta": "{\"event\": \"text_viewed\", \"section\": \"hero\"}",
  "trackClick": true,
  "trackClick_meta": "{\"event\": \"cta_clicked\"}"
}
```

### Privacy Considerations
- PII detection in metadata (email, phone, SSN patterns)
- GDPR/CCPA compliance guidelines
- Opt-out support
- Data anonymization recommendations

---

## 8. Responsive Design Strategy

### Breakpoints
| Breakpoint | Width | Target |
|-----------|-------|--------|
| Mobile | 320px+ | Base styles (default) |
| Tablet | 768px+ | Two-column layouts |
| Desktop | 1200px+ | Full multi-column layouts |
| Large Desktop | 1920px+ | Max-width containers |

*Note: Reference repo uses single 900px breakpoint; custom system uses 768/1200*

### Mobile-First Approach
- Base CSS targets mobile (no media query)
- `@media (min-width: ...)` adds desktop enhancements
- Progressive enhancement for capable browsers
- Touch targets ≥ 44x44px on mobile

### Key SCSS Mixins
- `@include respond-to('tablet')` / `@include respond-to('desktop')`
- `@include grid-responsive(1, 2, 3)` - Auto columns per breakpoint
- `@include flex-responsive(column, row)` - Stack → side-by-side
- `@include text-style($style-map)` - Typography from token maps

---

## 9. Development Standards

### Code Quality
- **ES6+** JavaScript (const/let, arrow functions, template literals)
- **JSDoc** comments for all public functions
- **ESLint** with Airbnb base configuration
- **Stylelint** for CSS/SCSS
- **Husky** pre-commit hooks
- **Vitest** for unit/integration testing (80%+ coverage target)

### Naming Conventions
- Files: `kebab-case.js`, `kebab-case.scss`
- CSS classes: `.block-name`, `.modifier-name`
- JS variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Data attributes: `data-attribute-name`

### Security
- DOMPurify for HTML sanitization in editor support
- `textContent` over `innerHTML` for user input
- Content Security Policy in `head.html`
- No PII in analytics metadata

---

## 10. Future Roadmap (4 Phases)

| Phase | Timeline | Focus | Key Deliverables |
|-------|----------|-------|-----------------|
| **Phase 1: Foundation** | Weeks 1-5 | 19 blocks + design system | Production-ready blocks, tokens, docs |
| **Phase 2: Enhancement** | Weeks 6-10 | Additional blocks + tooling | Hero, Quote, Storybook, Figma integration |
| **Phase 3: Automation** | Weeks 11-16 | Figma-to-AEM generator | Electron desktop app, auto-generation |
| **Phase 4: Scale** | Future | Enterprise features | Multi-site, team collaboration, advanced analytics |

### Phase 3 Automation Vision
- **Figma-to-AEM Block Generator** (Electron desktop app)
- Auto-detect Figma components → generate HTML/SCSS/JSON/JS
- Figma design tokens → code token synchronization
- Git integration with automatic PR creation
- Expected 80% reduction in manual coding

---

## 11. Dependencies & Integrations

### NPM Dependencies
| Package | Purpose | Version |
|---------|---------|---------|
| `merge-json-cli` | JSON composition with spread operator | 1.0.4 |
| `eslint` + plugins | JavaScript linting | 8.57.1 |
| `stylelint` | CSS linting | 17.0.0 |
| `husky` | Git hooks | 9.1.1 |
| `npm-run-all` | Parallel script execution | 4.1.5 |

### Planned Dependencies
| Package | Purpose |
|---------|---------|
| `mitt` | Lightweight event emitter for cross-block communication |
| `vitest` | Unit/integration testing framework |
| `webpack` | Module bundling |
| `style-dictionary` | Design token generation |
| `scss` compiler | SCSS → CSS compilation |

### External Integrations
- **AEM Cloud**: `author-p130360-e1272151.adobeaemcloud.com` (fstab.yaml mount)
- **AEM Universal Editor**: Content authoring via `data-aue-*` attributes
- **Adobe RUM**: Real User Monitoring via `ot.aem.live`
- **DAM**: Digital Asset Management for images/media

---

## 12. Key Differences: Reference vs. Custom System

| Aspect | xwalk Reference | Custom Block System |
|--------|----------------|---------------------|
| **Blocks** | 6 basic blocks | 19+ foundational blocks |
| **Token System** | Minimal CSS variables | 3-layer architecture (common/semantic/blocks) |
| **Styling** | Flat CSS | SCSS with mixins, utilities, mobile-first |
| **JS Pattern** | Varied per block | Standardized extractConfig/buildBlock/appendEventListeners |
| **Analytics** | None built-in | trackInView/trackClick/trackClick_meta |
| **Authoring** | Basic fields | Tabs (General/Appearance/Analytics) + reusable patterns |
| **Event System** | None | mitt event emitter |
| **Testing** | None | Vitest with 80%+ coverage target |
| **Documentation** | Basic README | 15 comprehensive guides (~45K words) |
| **Breakpoints** | Single (900px) | Three (768px/1200px/1920px) |

---

## 13. Summary Statistics

- **Documentation**: 15 guides, ~45,000 words, 200+ pages
- **Blocks Planned**: 19 foundational + bonus Custom Column
- **Blocks Implemented**: Hero (production-ready), 6 reference blocks
- **Code Examples**: 100+ across documentation
- **Implementation Patterns**: 30+
- **Development Timeline**: 5 weeks (Phase 1), 16 weeks total (all phases)
- **Repository Files**: 247 total
- **Technology Stack**: Vanilla JS (ES6+), SCSS, JSON, HTML5
- **Target Coverage**: 80%+ unit test coverage
- **Accessibility**: WCAG 2.1 Level AA compliance

---

*This report covers all aspects of the AEM EDS Blocks Repository for comprehensive knowledge reference.*
