# AEM EDS Sections Guide

> **Purpose**: Comprehensive guide to understanding and creating sections in AEM EDS.
> Last updated: April 9, 2026 — Phase 6c: Corrected section architecture

---

## 1. What is a Section?

In AEM EDS, a **section** is a container that groups multiple blocks and applies shared behaviour to them. Sections are fundamentally different from blocks:

| Aspect | Block | Section |
|--------|-------|---------|
| Purpose | Self-contained component | Groups/wraps multiple blocks with shared behaviour |
| Examples | Hero, CTA, Card | Carousel, Accordion, Modal, Tabs |
| JSON model location | `blocks/block-name/_block-name.json` | `models/_section-name.json` |
| JS file location | `blocks/block-name/block-name.js` | `blocks/section-name/section-name.js` |
| CSS file location | `blocks/block-name/block-name.css` | `blocks/section-name/section-name.css` |
| Has `_block-name.json` in blocks folder? | **Yes** | **No** — JSON is only in `models/` |
| resourceType | `core/franklin/components/block/v1/block` | `core/franklin/components/section/v1/section` |
| Config source | HTML rows (`[...block.children]`) | `data-` attributes on section div |
| JS loading | Loaded by `loadBlock()` during `loadSection()` | Loaded at **end of `loadEager()`** via custom code |
| Decorator parameter | `block` element | `sectionEl` (the section element) |

---

## 2. Section Metadata & the `section-identifier` Field

### The `section-identifier` Hidden Field

Every section JSON model **must** include a hidden field named `section-identifier`. This field:
- Has a fixed value matching the section name (e.g., `"section-carousel"`)
- Is processed by `decorateSections()` and becomes `data-sectionidentifier` on the section div
- Is used by `loadEager()` to locate and load the section's JS/CSS from `blocks/`

### How Section Metadata Works

All authored data for a section goes into a **section-metadata** div. In Franklin HTML, that node is **usually the last sibling** among the section’s immediate content (blocks and default content) **before** `decorateSections()` runs. The xwalk framework (`decorateSections()` in `aem.js`) processes it as follows:

1. `style` field → CSS classes on the section div
2. All other fields (including hidden identifiers) → `data-*` attributes on the section div (exposed as `element.dataset` in JS)
3. The `section-metadata` div is **removed** from the DOM after processing

If more than one `.section-metadata` node appears under a section, `decorateSections()` in this repo applies the **last** one.

#### Row markup inside `section-metadata`

Processing uses `readBlockConfig()`, the same helper as block tables: each **row** is a direct child `div` with **two** inner `div`s (first cell = key label text, second = value). Keys from the first cell are normalized with `toClassName()` (lowercase; non-alphanumeric characters become `-`), then mapped to `dataset` property names with `toCamelCase()` when values are copied onto the section element.

#### `data-*` names and JavaScript

`data-*` attribute names are lowercase in the DOM. Hyphenated names map to camelCase on `dataset`. Prefer reading values via `element.dataset` using the camelCase names produced by `decorateSections()`.

### Field Name → Data Attribute Conversion

| JSON Field Name | HTML Data Attribute |
|-----------------|--------------------|
| `sectionIdentifier` | `data-sectionidentifier` |
| `autoPlay` | `data-autoplay` |
| `slideDuration` | `data-slideduration` |
| `sectionTitle` | `data-sectiontitle` |

> **Note**: `decorateSections()` uses `toCamelCase()` for the dataset key, so `section-identifier` becomes `dataset.sectionIdentifier` which maps to `data-sectionidentifier` in HTML.

### Example: Before & After

**Section metadata in AEM (before processing):**
```html
<div>
  <div class="block hero">...</div>
  <div class="block text-callout">...</div>
  <div class="section-metadata">
    <div>
      <div>section-identifier</div>
      <div>section-carousel</div>
    </div>
    <div>
      <div>auto-play</div>
      <div>true</div>
    </div>
    <div>
      <div>slide-duration</div>
      <div>5000</div>
    </div>
  </div>
</div>
```

**After `decorateSections()` processing:**
```html
<div class="section hero-container text-callout-container"
     data-section-status="initialized"
     data-sectionidentifier="section-carousel"
     data-autoplay="true"
     data-slideduration="5000">
  <div class="hero-wrapper">
    <div class="hero block" data-block-name="hero" data-block-status="initialized">...</div>
  </div>
  <div class="text-callout-wrapper">
    <div class="text-callout block" data-block-name="text-callout" data-block-status="initialized">...</div>
  </div>
</div>
```

The `section-metadata` div is consumed and removed; its values become `data-` attributes on the section element.

---

## 3. Section JSON Model

Section JSON models live in the `models/` directory (NOT `blocks/`) and use a **different resourceType**.

### Template

```json
{
  "definitions": [{
    "title": "Section (Carousel)",
    "id": "section-carousel",
    "plugins": {
      "xwalk": {
        "page": {
          "resourceType": "core/franklin/components/section/v1/section",
          "template": {
            "name": "Section (Carousel)",
            "model": "section-carousel"
          }
        }
      }
    }
  }],
  "models": [{
    "id": "section-carousel",
    "fields": [
      {
        "component": "text",
        "name": "sectionIdentifier",
        "value": "section-carousel",
        "label": "Section Identifier",
        "hidden": true
      },
      {
        "component": "tab",
        "label": "Section Properties",
        "name": "tabSectionProperties"
      },
      {
        "component": "text",
        "name": "sectionTitle",
        "label": "Section Title"
      },
      {
        "component": "boolean",
        "name": "autoPlay",
        "label": "Auto Play"
      },
      {
        "component": "text",
        "name": "slideDuration",
        "label": "Slide Duration (ms)",
        "description": "Time between slides in milliseconds"
      },
      {
        "component": "tab",
        "label": "Appearance",
        "name": "tabAppearance"
      },
      {
        "component": "select",
        "name": "classes",
        "label": "Style Variant",
        "valueType": "string",
        "options": [
          { "name": "Default", "value": "" },
          { "name": "Full Width", "value": "section-full-width" }
        ]
      }
    ]
  }],
  "filters": []
}
```

### Key Differences from Block JSON

1. **resourceType**: `core/franklin/components/section/v1/section` (NOT `block/v1/block`)
2. **Location**: `models/_section-name.json` (NOT `blocks/block-name/_block-name.json`)
3. **`section-identifier` hidden field**: Required — value matches section name, becomes `data-sectionidentifier`
4. **No `_block-name.json` in blocks folder**: Section JS lives in `blocks/` but its JSON does NOT
5. **Fields become data attributes**: All non-tab, non-classes fields become `data-` attributes on the section div

---

## 4. Section JavaScript

Section JS lives in `blocks/section-name/section-name.js` (same directory pattern as blocks).

### Loading Mechanism

Section JS is loaded at the **end of `loadEager()`** in `scripts.js`, via custom code that:
1. Finds all sections with `data-sectionidentifier`
2. Loads CSS from `blocks/${sectionIdentifier}/${sectionIdentifier}.css`
3. Imports JS from `blocks/${sectionIdentifier}/${sectionIdentifier}.js`
4. Calls the default export with the **section element** as parameter

This mirrors the `loadBlock()` pattern in `aem.js`:

```javascript
// Added at end of loadEager() in scripts.js
async function loadSectionModules(main) {
  const sections = main.querySelectorAll('.section[data-sectionidentifier]');
  for (const section of sections) {
    const name = section.dataset.sectionIdentifier;
    try {
      const cssLoaded = loadCSS(`${window.hlx.codeBasePath}/blocks/${name}/${name}.css`);
      const decorationComplete = new Promise((resolve) => {
        (async () => {
          try {
            const mod = await import(
              `${window.hlx.codeBasePath}/blocks/${name}/${name}.js`
            );
            if (mod.default) {
              await mod.default(section);
            }
          } catch (error) {
            console.error(`failed to load section module for ${name}`, error);
          }
          resolve();
        })();
      });
      await Promise.all([cssLoaded, decorationComplete]);
    } catch (error) {
      console.error(`failed to load section ${name}`, error);
    }
  }
}
```

### Section JS Pattern

```javascript
/**
 * Section: Carousel
 * Wraps child blocks into a rotating carousel.
 *
 * Data attributes (from section-metadata):
 * - data-sectionidentifier: "section-carousel"
 * - data-autoplay: "true" | "false"
 * - data-slideduration: milliseconds
 * - data-sectiontitle: optional heading
 */

function buildCarouselTrack(sectionEl, blocks) {
  const track = document.createElement('div');
  track.classList.add('section-carousel-track');
  blocks.forEach((block) => {
    const slide = document.createElement('div');
    slide.classList.add('section-carousel-slide');
    slide.appendChild(block);
    track.appendChild(slide);
  });
  return track;
}

function buildCarouselNav(blocks) {
  const nav = document.createElement('div');
  nav.classList.add('section-carousel-nav');
  blocks.forEach((_, i) => {
    const dot = document.createElement('button');
    dot.classList.add('section-carousel-dot');
    if (i === 0) dot.classList.add('section-carousel-dot-active');
    dot.setAttribute('aria-label', `Slide ${i + 1}`);
    nav.appendChild(dot);
  });
  return nav;
}

function addCarouselEvents(track, nav, blocks, autoPlay, duration) {
  let current = 0;
  const dots = [...nav.children];

  function goTo(index) {
    current = index;
    track.style.transform = `translateX(-${current * 100}%)`;
    dots.forEach((d, i) => d.classList.toggle('section-carousel-dot-active', i === current));
  }

  dots.forEach((dot, i) => dot.addEventListener('click', () => goTo(i)));

  if (autoPlay) {
    setInterval(() => goTo((current + 1) % blocks.length), duration);
  }
}

/**
 * Section decorator — entry point.
 * Called with the section element as parameter.
 *
 * @param {HTMLElement} sectionEl The section element
 */
export default function decorate(sectionEl) {
  const autoPlay = sectionEl.dataset.autoplay === 'true';
  const duration = parseInt(sectionEl.dataset.slideduration, 10) || 5000;
  const title = sectionEl.dataset.sectiontitle || '';

  const blocks = [...sectionEl.querySelectorAll(':scope > div > .block')];
  if (blocks.length < 2) return;

  const track = buildCarouselTrack(sectionEl, blocks);
  const nav = buildCarouselNav(blocks);

  sectionEl.textContent = '';
  if (title) {
    const heading = document.createElement('h2');
    heading.classList.add('section-carousel-title');
    heading.textContent = title;
    sectionEl.appendChild(heading);
  }
  sectionEl.appendChild(track);
  sectionEl.appendChild(nav);
  sectionEl.classList.add('section-carousel-ready');

  addCarouselEvents(track, nav, blocks, autoPlay, duration);
}
```

### Key Differences from Block JS

| Aspect | Block JS | Section JS |
|--------|----------|------------|
| Input | `block` element | `sectionEl` (section element) |
| Config | Position-based rows (`[...block.children]`) | `data-` attributes via `dataset` |
| Children | Internal rows | Child blocks (wrapped in wrappers) |
| Loading | Via `loadBlock()` during section loading | Via custom code at end of `loadEager()` |
| JSON model | In `blocks/block-name/_block-name.json` | In `models/_section-name.json` |
| Structure | `extractConfig → buildBlock → appendEvents` | Simple decorator that calls focused functions |
| Classes | `classList.add()` | `classList.add()` |

---

## 5. Section CSS

Section CSS lives in `blocks/section-name/section-name.css` (alongside the JS).

```css
/* section-carousel.css */
.section-carousel-ready {
  position: relative;
  overflow: hidden;
}

.section-carousel-track {
  display: flex;
  transition: transform 0.5s ease;
}

.section-carousel-slide {
  min-width: 100%;
  flex-shrink: 0;
}

.section-carousel-nav {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem 0;
}

.section-carousel-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #cccccc;
  border: none;
  cursor: pointer;
  padding: 0;
}

.section-carousel-dot-active {
  background: #0066cc;
}

.section-carousel-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
}

@media (min-width: 768px) {
  .section-carousel-nav {
    gap: 0.75rem;
  }
  .section-carousel-title {
    font-size: 2rem;
  }
}
```

---

## 6. Section Loading in `scripts.js`

The critical code that loads section JS/CSS goes at the **end of `loadEager()`**:

```javascript
async function loadEager(doc) {
  document.documentElement.lang = 'en';
  decorateTemplateAndTheme();
  const main = doc.querySelector('main');
  if (main) {
    decorateMain(main);
    document.body.classList.add('appear');
    await loadSection(main.querySelector('.section'), waitForFirstImage);
  }

  try {
    if (window.innerWidth >= 900 || sessionStorage.getItem('fonts-loaded')) {
      loadFonts();
    }
  } catch (e) {
    // do nothing
  }

  // Load section modules — LAST STEP of loadEager
  if (main) {
    await loadSectionModules(main);
  }
}
```

**Why at the end of `loadEager()`?**
- Section JS needs the section DOM to be decorated and blocks loaded first
- Section JS orchestrates child blocks, so blocks must exist in DOM
- Loading section CSS/JS at end of eager ensures it doesn't delay LCP

---

## 7. File Structure

```
project-root/
├── blocks/                          # Blocks AND section JS/CSS live here
│   ├── hero/
│   │   ├── hero.js
│   │   ├── hero.css
│   │   └── _hero.json               ← Block has JSON here
│   ├── text-callout/
│   │   ├── text-callout.js
│   │   ├── text-callout.css
│   │   └── _text-callout.json       ← Block has JSON here
│   ├── section-carousel/
│   │   ├── section-carousel.js       ← Section JS here
│   │   └── section-carousel.css      ← Section CSS here
│   │                                  ← NO _section-carousel.json here!
│   └── section-accordion/
│       ├── section-accordion.js
│       └── section-accordion.css
│
├── models/                          # Section JSON models live here
│   ├── _section.json                 ← Base section model (from boilerplate)
│   ├── _section-carousel.json        ← Custom section model
│   └── _section-accordion.json       ← Custom section model
│
├── scripts/
│   ├── aem.js
│   ├── scripts.js                    ← Contains loadSectionModules() + updated loadEager()
│   └── utilities/
│       └── block-helpers.js
│
└── styles/
    └── styles.css
```

### Key structural differences:

| | Block | Section |
|--|-------|---------|
| JS/CSS location | `blocks/block-name/` | `blocks/section-name/` |
| JSON model | `blocks/block-name/_block-name.json` | `models/_section-name.json` |
| Has `_*.json` in blocks folder? | ✅ Yes | ❌ No |

---

## 8. When to Use Sections vs Blocks

### Use a Block when:
- The component is **self-contained** (hero, card, CTA)
- It has its own content fields that don't relate to other blocks
- It can be added/removed independently
- It represents a **single piece of functionality**

### Use a Section when:
- The functionality **spans or groups multiple blocks**
- You need **carousel**, **accordion**, **modal**, or **tabs** behaviour
- You want to apply **shared behaviour** to a group of content
- The pattern requires **orchestrating child blocks**

### Decision Flowchart

```
Does it group/wrap multiple blocks?
├── Yes → SECTION
│   ├── Carousel (rotating blocks)
│   ├── Accordion (collapsible sections)
│   ├── Modal (overlay content)
│   └── Tabs (switchable content)
└── No → BLOCK
    ├── Hero (single component)
    ├── CTA (single component)
    └── Card (single component)
```

---

## 9. Section Creation Checklist

1. ☐ **Ask**: Is this truly a section or a block?
2. ☐ **Create JSON** in `models/_section-name.json` with:
   - Section resourceType (`core/franklin/components/section/v1/section`)
   - Hidden `sectionIdentifier` field with section name value
   - Section-specific fields (become `data-` attributes)
3. ☐ **Create JS** in `blocks/section-name/section-name.js`
   - Simple decorator that calls focused functions
   - Receives `sectionEl` as parameter
   - Reads config from `sectionEl.dataset.*`
4. ☐ **Create CSS** in `blocks/section-name/section-name.css`
5. ☐ **Do NOT create** `_section-name.json` in `blocks/section-name/`
6. ☐ **Ensure** `loadSectionModules()` is in `scripts.js` and called at end of `loadEager()`
7. ☐ Use `classList.add()` (not `className =`)
8. ☐ Run `npm run lint`
9. ☐ Run `npm run build:json`
10. ☐ Test with multiple child blocks

---

## References

- Official docs: https://www.aem.live/developer/markup-sections-blocks
- Performance guide: https://www.aem.live/developer/keeping-it-100
- Section source: `aem.js` → `decorateSections()` (line ~501)
- Block loading pattern: `aem.js` → `loadBlock()` (line ~574)
- Rule files: `15-sections-vs-blocks.mdc`, `16-section-creation-pattern.mdc`
- Adobe field types: https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/universal-editor/field-types#component-types
