# Block Design Philosophy - Principles and Approach

## Core Design Philosophy

Our block system is built on three pillars:

1. **Highly Modular** - Independent, composable building blocks
2. **Highly Authorable** - Rich authoring without code
3. **Highly Styleable** - Clean styling with basic CSS

See: [05-CSS_STYLING_APPROACH.md](05-CSS_STYLING_APPROACH.md) for CSS guidelines

## Modularity Principle

### **What Makes a Block Modular?**

**Characteristic 1: Self-Contained**
- Block has all code needed to function
- Minimal external dependencies
- Can be used independently
- Clear input/output contracts

```javascript
// Good: Block imports only what it needs
import { applyTracking } from '../../scripts/dataLayer.js';
import { getTextFromRow, getHtmlFromRow } from '../../scripts/utilities/block-helpers.js';

// Bad: Block imports block-specific utilities
import { someOtherBlockHelper } from '../blocks/other-block/utils.js';
```

**Characteristic 2: Composable**
- Blocks can work together
- Communication via data attributes, not direct calls
- No tight coupling between blocks

**Characteristic 3: Reusable**
- Code patterns repeated across blocks
- Shared utilities extracted
- Common patterns in JSON models

```
// Shared utilities extracted
scripts/
├── dataLayer.js             // Analytics tracking
└── utilities/
    └── block-helpers.js     // DOM extraction helpers
```

**Characteristic 4: Replaceable**
- One block can replace another
- Interface consistent (config, behavior)
- No side effects on replacement

### **How to Achieve Modularity**

✓ **Do**:
- Keep blocks small and focused
- Extract reusable utilities
- Use standard patterns (extractConfig, buildBlock, etc.)
- Use data-attribute-driven analytics
- Keep config structure consistent
- Document block expectations
- Use composition over inheritance

✗ **Don't**:
- Create blocks that depend on each other
- Duplicate code across blocks
- Create god-blocks (too many responsibilities)
- Direct DOM manipulation on other blocks
- Share state between blocks

## Authoring Principle

### **Core Concept: Zero Code Required**

Authors should never need to touch code. All customization happens through:
- Text inputs and dropdowns
- Rich text editors
- Asset pickers
- Tab organization
- Sensible defaults

### **Tab Organization**

Every block has three tabs:

**Tab 1: General**
- Core content fields
- Block ID (first field always)
- Main content (RTE for rich content, text inputs for simple)
- Core configuration

```json
{
  "component": "tab",
  "name": "tabGeneral",
  "label": "General"
},
{
  "component": "text",
  "name": "id",
  "label": "Block ID"
},
{
  "component": "richtext",
  "name": "title",
  "label": "Title"
},
{
  "component": "richtext",
  "name": "text",
  "label": "Content"
}
```

**Tab 2: Appearance**
- Style variant selection via `classes` field
- Alignment via `classes_*` field
- Other visual properties

```json
{
  "component": "tab",
  "name": "tabAppearance",
  "label": "Appearance"
},
{
  "component": "select",
  "name": "classes",
  "label": "Style",
  "valueType": "string",
  "options": [
    { "name": "Primary", "value": "callout-primary" },
    { "name": "Secondary", "value": "callout-secondary" }
  ]
}
```

**Tab 3: Analytics**
- Tracking configuration
- In-view tracking (boolean)
- Click tracking (boolean)
- Conditional metadata fields

```json
{
  "component": "tab",
  "name": "tabAnalytics",
  "label": "Analytics"
},
{
  "component": "boolean",
  "name": "trackInview",
  "label": "Track In-View",
  "valueType": "boolean"
},
{
  "component": "text",
  "name": "trackInview_meta",
  "label": "In-View Metadata (JSON)",
  "condition": { "===": [{ "var": "trackInview" }, true] }
}
```

### **Authoring Best Practices**

✓ **Do**:
- Provide defaults for fields with meaningful values
- Organize fields logically in tabs
- Use descriptive labels and help text
- Support RTE where formatting needed
- Hide complex fields conditionally with `condition`
- Use `boolean` component (not checkbox)
- Validate inputs in JavaScript

✗ **Don't**:
- Require authors to add CSS classes
- Use unclear field names
- Expose technical implementation
- Include empty fields in template defaults
- Use `visible` instead of `condition`
- Use `checkbox` instead of `boolean`

## Styling Philosophy

### **Basic CSS with Direct Values**

All styling uses basic CSS with direct, explicit values:

```css
/* Good: Direct, clear values */
.text-callout-title {
  color: #1a1a1a;
  font-size: 1.5rem;
  padding: 0 0 1rem 0;
}

/* Good: Variant via block class */
.text-callout.callout-primary .brand-cta {
  background-color: #0066cc;
}

.text-callout.callout-secondary .brand-cta {
  background-color: #6c757d;
}
```

```css
/* GOOD: project tokens (see styles/styles.css, styles/colors.css) */
.text-callout-title {
  color: var(--color-base-text);
  font-size: var(--body-font-size-m);
}

/* BAD: SCSS — do NOT use */
.text-callout-title {
  color: $text-primary;
  @include typography('heading');
}
```

### **Variant System**

Blocks support **variants** through CSS classes automatically applied by AEM based on `classes` and `classes_*` JSON fields:

```css
/* CSS targets the variant class on the outer block div */
.simple-cta.cta-primary-filled .brand-cta {
  background-color: #0066cc;
  color: #ffffff;
}

.simple-cta.cta-primary-outlined .brand-cta {
  background-color: transparent;
  color: #0066cc;
  border-color: #0066cc;
}
```

### **Hyphenated Class Naming**

Use hyphenated names (NOT BEM):

| ✅ Correct | ❌ Wrong |
|-----------|---------|
| `.text-callout-title` | `.text-callout__title` |
| `.hero-content` | `.hero__content` |
| `.simple-cta-text` | `.simple-cta__text` |

## Block Lifecycle

### **1. Authoring Phase**
- Author creates block in Universal Editor
- Fills in General tab (content)
- Customizes Appearance tab (styling via `classes`)
- Configures Analytics tab (tracking booleans + meta)
- Previews in editor

### **2. Publishing Phase**
- Content published to Git
- Serialized to HTML
- Stored in repository

### **3. Rendering Phase**
- Page load triggers block decoration
- JavaScript extracts config via `[...block.children]`
- Block structure built with `block.textContent = ''`
- Analytics data attributes set during build
- `applyTracking(block)` wires up observers/listeners

### **4. Interaction Phase**
- User interacts (click, scroll, etc.)
- Analytics events pushed to `window.dataLayer.events[]`
- Actions performed

## Content Modeling Strategy

### **Element Grouping**

Related fields are grouped using underscore naming:

```json
{
  "component": "boolean",
  "name": "trackInview",
  "label": "Track In-View"
},
{
  "component": "text",
  "name": "trackInview_meta",
  "label": "Metadata",
  "condition": { "===": [{ "var": "trackInview" }, true] }
}
```

### **RTE vs. Text Input**

**Use RTE (`richtext`) When**:
- Authors need formatting (bold, italic)
- Links required
- Variable heading levels needed
- Complex content expected

**Use Text Input (`text`) When**:
- Simple, single-line text
- No formatting needed
- Examples: IDs, labels, metadata

## File Naming Convention

Every block follows this file structure:

```
blocks/block-name/
├── block-name.js          # Block JavaScript
├── block-name.css         # Block CSS (basic CSS only)
└── _block-name.json       # Block JSON (underscore prefix required)
```

**Rules**:
1. JSON files MUST have underscore prefix: `_text-callout.json`
2. All files MUST match the folder name: `text-callout.*`
3. CSS files only — no SCSS

## Performance Considerations

### **Three-Phase Loading**

**Eager Loading** — Hero, above-the-fold content, critical images
**Lazy Loading** — Content below fold, triggered on scroll
**Delayed Loading** — Forms, modals, triggered on interaction

### **Optimization Strategies**

✓ **Do**:
- Load images responsively (multiple sizes)
- Lazy load below-fold blocks
- Use basic CSS (fast to parse, no compilation needed)
- Minimize JavaScript execution
- Defer non-critical JavaScript
- Optimize images before publication

✗ **Don't**:
- Load all assets immediately
- Use oversized images
- Animate every element
- Block rendering with JavaScript
- Load unused blocks

## Philosophy Summary

| Aspect | Principle | Implementation |
|--------|-----------|-----------------|
| **Modularity** | Independent, composable | Clean interfaces, data-attribute communication |
| **Authoring** | Zero code required | Rich UI, sensible defaults |
| **Styling** | Basic CSS, direct values | No variables, no tokens, no SCSS |
| **Performance** | Lazy loading, optimization | Three-phase strategy |
| **Accessibility** | WCAG compliance | Semantic HTML, testing |
| **Consistency** | Standard patterns | Shared utilities, conventions |
| **File Naming** | Predictable, matching | `_block-name.json`, files match folder |

## References

- [01-FUNDAMENTALS.md](01-FUNDAMENTALS.md) - Core concepts
- [02-JSON_CONFIGURATION.md](02-JSON_CONFIGURATION.md) - Configuration
- [03-BLOCK_JAVASCRIPT_PATTERN.md](03-BLOCK_JAVASCRIPT_PATTERN.md) - JS pattern
- [05-CSS_STYLING_APPROACH.md](05-CSS_STYLING_APPROACH.md) - CSS styling
- [07-FOUNDATIONAL_BLOCKS.md](07-FOUNDATIONAL_BLOCKS.md) - Block list
- [12-DEVELOPMENT_PATTERNS.md](12-DEVELOPMENT_PATTERNS.md) - Best practices
