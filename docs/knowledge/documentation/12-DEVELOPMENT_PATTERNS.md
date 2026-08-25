# Development Patterns - Standards and Best Practices

## Overview

Standards and conventions for building blocks consistently and professionally.

See: [03-BLOCK_JAVASCRIPT_PATTERN.md](03-BLOCK_JAVASCRIPT_PATTERN.md) for JavaScript pattern

## Code Style

### **JavaScript Standards**

**Use Modern ES6+**:
```javascript
// Good
const config = extractConfig(block);
const { title, color } = config;

// Avoid
var config = extractConfig(block);
let title = config.title;
```

**Use Arrow Functions**:
```javascript
// Good
const items = blocks.filter(b => b.active);
const names = items.map(i => i.name);

// Avoid
const items = blocks.filter(function(b) { return b.active; });
const names = items.map(function(i) { return i.name; });
```

**Use Template Literals**:
```javascript
// Good
const message = `Block ${name} loaded successfully`;

// Avoid
const message = 'Block ' + name + ' loaded successfully';
```

**Use Const by Default**:
```javascript
// Good
const config = { ... };

// Only use let if variable changes
let currentSlide = 0;
currentSlide = currentSlide + 1;

// Avoid
var config = { ... };
```

### **CSS Standards**

> **Basic CSS only** — no SCSS. Use **project CSS variables** from **`styles/`** for **colors**, **spacing**, and **typography** in **`blocks/**/*.css`** wherever applicable (**`30-block-css-design-tokens.mdc`**, **`04-block-css-pattern.mdc`**).

**Prefer global tokens**:
```css
/* Good: shared tokens — track theme and design updates */
.text-callout-title {
  color: var(--color-neutral-800);
  padding: var(--space-m);
  font-size: var(--body-font-size-m);
  font-weight: var(--font-weight-body);
  font-family: var(--body-font-family);
  line-height: 1.6;
}

/* Bad: raw UI colors in block CSS */
.text-callout-title {
  color: #1a1a1a;
  padding: 1.5rem;
}
```

**Use standard media queries**:
```css
/* Good: Standard @media queries */
@media (min-width: 768px) {
  .text-callout-title {
    font-size: 1.75rem;
  }
}

/* Bad: SCSS mixins — do NOT use */
/* @include respond-to('tablet') { ... } */
```

**Keep selectors flat** (max 2 levels):
```css
/* Good */
.text-callout.callout-primary .brand-cta {
  background-color: var(--color-accent-cta);
}

/* Bad: too deep */
.block .wrapper .inner .item .label { ... }
```

## Naming Conventions

### **JavaScript**
```javascript
// Variables and functions: camelCase
const config = extractConfig(block);
function buildBlock(config) { }

// Constants: UPPER_SNAKE_CASE
const MAX_SLIDES = 10;
const DEFAULT_COLOR = 'default';

// Private functions: underscore prefix
function _internalHelper() { }
```

### **CSS Classes**
```css
/* Hyphenated block names */
.text-callout
.simple-cta
.hero

/* Element names: block-name-element */
.text-callout-inner
.text-callout-title
.hero-content
.hero-headline

/* Variant classes (applied by AEM via classes field) */
.callout-primary
.cta-primary-filled
.text-callout-align-center

/* Shared element classes */
.brand-cta
```

### **Data Attributes**
```html
<!-- Configuration -->
<div data-block-config="...">

<!-- State -->
<div data-active="true">

<!-- Behavior -->
<div data-auto-play="true">

<!-- Analytics -->
<div data-tracked="true">
```

### **File Naming**
```
blocks/text-callout/           # Block folder
├── text-callout.js            # Block JavaScript (matches folder name)
├── text-callout.css           # Block CSS (basic CSS, matches folder name)
├── _text-callout.json         # Block JSON (underscore prefix, matches folder name)
└── __tests__/
    └── text-callout.test.js   # Tests
```

## Comment Standards

### **JSDoc Comments**

```javascript
/**
 * Extract configuration from block HTML
 * @param {HTMLElement} block - The block element
 * @returns {Object} Configuration object with all block data
 */
function extractConfig(block) {
  // Implementation
}

/**
 * CTA block configuration
 * @typedef {Object} CTAConfig
 * @property {string} text - Button text
 * @property {string} url - Target URL
 * @property {string} variant - Button variant (primary, secondary)
 */
```

### **Inline Comments**

```javascript
// Use sparingly for complex logic
// Good: explains why
if (config.autoPlay) {
  // Restart animation when resumed from pause
  resetAnimation();
}

// Avoid: obvious code
// Set config to block
block.config = config;
```

### **CSS Comments**

```css
/* Section comments */
/* ============================================================
   Text Callout Block Styles
   ============================================================ */

/* Sub-section comments */
/* Typography variants */
.text-callout-title { ... }

/* Use sparingly for complex selectors */
/* Match specific variant combination */
.simple-cta.cta-primary-filled .brand-cta { ... }
```

## Error Handling

### **Validation**

```javascript
// Validate input
function extractConfig(block) {
  if (!block || !(block instanceof HTMLElement)) {
    throw new Error('Block must be an HTMLElement');
  }

  const config = { mainEl: block };
  
  // Safe extraction with defaults
  config.title = block.querySelector('[data-title]')?.textContent || 'Untitled';
  config.color = block.dataset.color || 'default';

  return config;
}
```

### **Try-Catch for Known Risky Operations**

```javascript
function parseMetadata(metadataString) {
  try {
    return JSON.parse(metadataString);
  } catch (error) {
    console.warn('Invalid JSON metadata:', metadataString);
    return {};
  }
}
```

### **Graceful Degradation**

```javascript
// Works with or without feature
function decorate(block) {
  const config = extractConfig(block);
  
  // Optional: if IntersectionObserver available, use it
  if ('IntersectionObserver' in window && config.trackInView) {
    trackInView(block, config.trackInView_meta);
  } else {
    console.log('IntersectionObserver not supported');
  }
}
```

## Testing Patterns

### **Unit Tests**

```javascript
// test/blocks/text.test.js
import { describe, test, expect } from 'vitest';
import { extractConfig, buildBlock } from '../../blocks/text/text.js';

describe('Text Block', () => {
  describe('extractConfig', () => {
    test('should extract text content', () => {
      const block = document.createElement('div');
      block.innerHTML = '<div><div>Hello</div></div>';

      const config = extractConfig(block);

      expect(config.content).toBe('Hello');
    });

    test('should handle missing content', () => {
      const block = document.createElement('div');

      const config = extractConfig(block);

      expect(config.content).toBe('');
    });
  });

  describe('buildBlock', () => {
    test('should add variant classes', () => {
      const config = {
        color: 'primary',
        classes: [],
      };

      const result = buildBlock(config);

      expect(result.classes).toContain('text-primary');
    });
  });
});
```

### **Integration Tests**

```javascript
// tests/integration.test.js
import { describe, test, expect } from 'vitest';

describe('Text Block Integration', () => {
  test('should decorate and attach config', async () => {
    const block = document.createElement('div');
    block.className = 'block block-text';
    block.innerHTML = '<div><div>Test</div></div>';

    const { default: decorate } = await import('../../blocks/text/text.js');
    decorate(block);

    expect(block.blockConfig).toBeDefined();
    expect(block.blockConfig.content).toBe('Test');
  });
});
```

### **Visual Regression Tests**

```javascript
// tests/visual.test.js
import { describe, test, expect } from 'vitest';

describe('Visual Regression', () => {
  test('text block snapshot matches', async () => {
    const block = document.createElement('div');
    block.className = 'block block-text text-primary';
    block.innerHTML = '<div><div>Test</div></div>';

    expect(block).toMatchSnapshot();
  });
});
```

## Performance Guidelines

### **JavaScript Performance**

✓ **Do**:
- Use event delegation for multiple elements
- Debounce resize/scroll handlers
- Cache DOM queries
- Use `requestAnimationFrame` for animations
- Lazy load heavy features

```javascript
// Good: Cache and event delegation
const container = block.querySelector('[data-items]');
container?.addEventListener('click', (e) => {
  if (e.target.matches('[data-item]')) {
    handleItemClick(e.target);
  }
});

// Avoid: Query on every event
element.addEventListener('click', () => {
  block.querySelector('[data-items]').classList.add('active');
});
```

✗ **Don't**:
- Query DOM repeatedly
- Use synchronous operations on large datasets
- Create memory leaks with event listeners
- Block the main thread
- Ignore bundle size

### **CSS Performance**

✓ **Do**:
- Use CSS instead of JavaScript for styling
- Minimize selector specificity
- Use classes instead of elements
- Leverage CSS variables
- Use flexbox/grid over float

```scss
// Good: Simple selector, CSS handling
.block-text.is-active {
  opacity: 1;
}

// Avoid: Complex selector, JS needed
body.dark-mode .page .main .content .block-text {
  opacity: 1;
}
```

✗ **Don't**:
- Use !important (except last resort)
- Create deeply nested selectors
- Animate expensive properties
- Use vendor prefixes (let autoprefixer handle)
- Import entire libraries for single function

## Security Best Practices

### **XSS Prevention**

```javascript
// Good: Use textContent for plain text
element.textContent = userInput;  // Safe

// Avoid: innerHTML with user data
element.innerHTML = userInput;    // Unsafe

// If HTML needed: sanitize first
import DOMPurify from 'dompurify';
element.innerHTML = DOMPurify.sanitize(userInput);
```

### **Data Handling**

```javascript
// Don't expose sensitive data
const config = {
  apiKey: 'secret',  // ✗ Never!
  title: 'Public',   // ✓ OK
};

// Validate and sanitize
function validateMetadata(str) {
  try {
    const parsed = JSON.parse(str);
    // Check for PII patterns
    if (JSON.stringify(parsed).match(/\d{3}-\d{2}-\d{4}/)) {
      return {};
    }
    return parsed;
  } catch {
    return {};
  }
}
```

## Documentation Standards

### **Before creating a new block or section**

See **`.cursor/rules/28-new-component-discovery.mdc`** and [02-JSON_CONFIGURATION.md](02-JSON_CONFIGURATION.md) (*New blocks and sections (discovery)*): analyze the request, ask questions, check **`blocks/*/`** and **`models/_section.json`** for an existing fit, and prefer suggesting **custom** repo blocks over reference-only or generic xwalk examples when recommending authoring options.

### **README for Blocks** (required for every `blocks/<name>/`)

`README.md` is **mandatory** when creating a block (see **`08-block-creation-checklist.mdc`** Step 6). It must support **humans**, **non-expert readers**, and **other LLMs** choosing the block from requirements.

Include:

- **For another AI / LLM** — when to pick / not pick (requirement phrasing).
- **Big picture** — plain-language rendering (no assumption of sight; simple analogies).
- **Per-variation pattern** — **every** distinct option (and important combinations) using the **same** subsection shape, for example:

```markdown
### Option name — Value

| | |
| --- | --- |
| **Author picks** | … |
| **Choose this if** | Keywords another LLM can match (size, color, layout words). |
| **Plain explanation** | Kid/blind-friendly: ink, boxes, reading order, slant vs color. |
| **Sighted user** | One line (optional). |
| **Technical** | Class names, CSS, DOM. |
```

Also: authoring table, row map, technical **Rendered layout**, **Files**, **Related**.

**Example:** `blocks/content/README.md` (full pattern with all Content variations).

### **Inline Documentation**

```javascript
/**
 * Text Block
 * 
 * Displays formatted text with styling options.
 * 
 * @module blocks/text
 * 
 * ## Configuration
 * - color: Text color variant (default, primary, secondary, etc.)
 * - size: Font size (small, default, large)
 * - alignment: Text alignment (left, center, right)
 * 
 * ## Events
 * - analytics:in-view - When block becomes visible
 * 
 * ## Accessibility
 * Uses semantic HTML (p, h1-h6) for proper document structure.
 * Respects prefers-color-scheme media query.
 */
```

## Debugging Tips

### **Console Logging**

```javascript
// Use descriptive log messages
console.log('[text-block] Config:', config);
console.warn('[text-block] Missing required field:', field);
console.error('[text-block] Failed to parse metadata:', error);

// Use console groups for organization
console.group('[text-block]');
console.log('Config:', config);
console.log('Classes:', classes);
console.groupEnd();
```

### **Debugging Selectors**

```javascript
// Verify your selectors work
const els = document.querySelectorAll('[data-item]');
console.log(`Found ${els.length} items`);

// Test in console
document.querySelectorAll('[data-item]').forEach(el => {
  console.log(el.textContent);
});
```

### **Performance Profiling**

```javascript
// Measure performance
console.time('block-decoration');
decorate(block);
console.timeEnd('block-decoration');

// Use Performance API
const start = performance.now();
// ... code to measure
const end = performance.now();
console.log(`Took ${end - start}ms`);
```

## Checklist for Code Review

- [ ] Follows standard JS pattern (extractConfig, buildBlock, appendEventListeners)
- [ ] Uses basic CSS with direct values (no variables, no tokens)
- [ ] Properly handles errors and missing data
- [ ] No console errors or warnings
- [ ] Tests pass locally
- [ ] Documentation updated
- [ ] No performance issues
- [ ] Accessible (WCAG AA)
- [ ] Works on mobile/tablet/desktop
- [ ] No security issues

## Common Pitfalls to Avoid

| Pitfall | ✗ Bad | ✓ Good |
|---------|-------|--------|
| Global state | `window.blockState = {}` | `block.blockConfig = {}` |
| Direct DOM access | `document.querySelector('.block')` | `block.querySelector('.item')` |
| Tight coupling | Block imports another block | Blocks use data attributes |
| Missing validation | Assume input is valid | Validate with defaults |
| Memory leaks | Never remove listeners | Clean up on destroy |
| Async issues | Promise without error handler | try/catch or .catch() |
| Naming confusion | `data`, `config`, `obj` | `blockConfig`, `itemData` |

---

## Complete Block Development Workflow (Production Pattern)

This workflow uses the patterns from the production text-callout block. See [16-BLOCK_DEVELOPMENT_TEMPLATE.md](16-BLOCK_DEVELOPMENT_TEMPLATE.md) for the full step-by-step template.

### Phase 1: Define JSON Configuration

1. Create `blocks/my-block/_my-block.json`
2. Define `definitions` with template defaults
3. Define `models` with 3 tabs: General, Appearance, Analytics
4. Use `boolean` (not `checkbox`) for toggles
5. Use `condition` (not `visible`) with JSON Logic for conditional fields
6. Use `classes` and `classes_*` for style variants
7. Define `filters` (empty `components: []` if no restrictions)

### Phase 2: Map Row Positions

Document which JSON fields create rows and their positions:
- `tab` fields → NO row
- `classes` and `classes_*` fields → NO row (applied as CSS classes)
- All other fields → create rows in order

### Phase 3: Write JavaScript

```javascript
import { applyTracking } from '../../scripts/dataLayer.js';
import { getTextFromRow, getHtmlFromRow, getBooleanFromRow } from '../../scripts/utilities/block-helpers.js';

// 1. Document row layout
/**
 * 0: id, 1: title, 2: text, ...
 */

// 2. extractConfig using helpers
function extractConfig(block) {
  if (!block) return {};
  const rows = [...block.children];
  return { /* position-based extraction */ };
}

// 3. Build DOM
function buildMyBlock(block, config) {
  // Create elements, set analytics data attrs
  block.textContent = '';
  block.appendChild(inner);
}

// 4. Event listeners
function appendEvents(config) { /* ... */ }

// 5. Decorate entry point
export default function decorate(block) {
  const config = extractConfig(block);
  buildMyBlock(block, config);
  appendEvents(config);
  applyTracking(block);
}
```

### Phase 4: Write CSS

> Use basic CSS only — no SCSS. Use **`var(--color-*)`**, **`var(--space-*)`**, and **type tokens** from **`styles/styles.css`** / **`styles/colors.css`** in block CSS (**`30-block-css-design-tokens.mdc`**).

- Use hyphenated class names: `my-block-inner`, `my-block-title`
- Target style variants via classes on block: `.my-block.cta-primary-filled`
- Mobile-first responsive design

### Phase 5: Test

- Create `demo.html` for visual testing
- Test all style variants
- Test analytics data attributes
- Test with missing/empty fields

---

## Production Conventions Checklist

Based on the text-callout production reference:

- [ ] JSON uses `boolean` (not `checkbox`) for toggles
- [ ] JSON uses `condition` with JSON Logic (not `visible`)
- [ ] JSON `select` fields include `valueType: "string"`
- [ ] Template defaults only include fields with meaningful values
- [ ] JS imports helpers from `block-helpers.js`
- [ ] JS uses `[...block.children]` for row access
- [ ] JS documents row position map in JSDoc
- [ ] JS uses `block.textContent = ''` to clear markup
- [ ] JS sets analytics data attributes during build
- [ ] JS calls `applyTracking(block)` at end of decorate
- [ ] JS `decorate` is synchronous (not async)
- [ ] CSS uses hyphenated class names (not BEM `__`/`--`)
- [ ] CSS uses basic CSS (no SCSS, no variables, no tokens)
- [ ] JSON file uses underscore prefix (`_block-name.json`)
- [ ] All files match folder name

---

## References

- [03-BLOCK_JAVASCRIPT_PATTERN.md](03-BLOCK_JAVASCRIPT_PATTERN.md) - JS pattern
- [05-CSS_STYLING_APPROACH.md](05-CSS_STYLING_APPROACH.md) - CSS standards
- [06-BLOCK_DESIGN_PHILOSOPHY.md](06-BLOCK_DESIGN_PHILOSOPHY.md) - Design principles
- [09-ANALYTICS_PATTERN.md](09-ANALYTICS_PATTERN.md) - Analytics data layer
- [10-PROJECT_STRUCTURE.md](10-PROJECT_STRUCTURE.md) - File organization
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](16-BLOCK_DEVELOPMENT_TEMPLATE.md) - Step-by-step template
- `text_callout_block/` - Production reference implementation
