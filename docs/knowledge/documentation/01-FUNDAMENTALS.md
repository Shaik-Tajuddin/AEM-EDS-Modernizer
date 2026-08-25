# AEM EDS Fundamentals - Core Concepts

## What is AEM EDS?

**AEM Edge Delivery Services** (EDS) is a set of composable services that enable rapid website development and deployment at scale. It uses a **flat content structure** with **Sections** and **Blocks** instead of the traditional AEM component hierarchy.

See: [00-HOLISTIC_VISION.md](00-HOLISTIC_VISION.md) for project overview

## Three Implementation Approaches

### **1. AEM + Universal Editor** (Our Focus)
- Content authored in AEM's Universal Editor
- Blocks defined in JSON (models, definitions, filters)
- JavaScript brings blocks to life
- Full AEM integration
- Best for organizations already using AEM

### **2. Document-Based Approach**
- Content stored as document sheets (Excel, Google Sheets)
- Markdown support
- Lower barrier to entry
- Simpler structure

### **3. Hybrid Approach**
- Combination of both approaches
- Maximum flexibility

## Flat Content Structure

Unlike traditional AEM's nested component hierarchy, EDS uses a **flat, two-level structure**:

```
Page
├── Section 1
│   ├── Block 1
│   ├── Block 2
│   └── Block 3
├── Section 2
│   ├── Block 1
│   └── Block 2
└── Section 3
    └── Block 1
```

## Core Concepts

### **Sections**
**Definition**: Container for blocks; acts as a layout wrapper

**Characteristics**:
- Cannot be nested inside other sections
- Contains one or more blocks
- Typically represents a major content area (header, hero, content area, footer)
- Can have styling and configuration

**Examples**:
```
- Page Header Section (contains logo, nav blocks)
- Hero Section (contains background image, headline, CTA)
- Content Section (contains text, images)
- Footer Section (contains links, copyright)
```

**Nesting Rule**: ❌ Section cannot contain another section

### **Blocks**
**Definition**: Individual content component within a section

**Characteristics**:
- Must exist within a section
- Can have one level of nesting (blocks within blocks)
- Represents discrete content units
- Contains data and behavior

**Examples**:
```
- Text block (paragraph content)
- Image block (pictures with captions)
- CTA block (call-to-action button)
- Form block (input fields)
- Carousel block (rotating images)
```

**Nesting Rule**: ⚠️ Blocks can contain blocks, but rendering differs

### **Block Nesting - Important Caveat**

When a block is added inside another block:

```
Outer Block
└── Inner Block
```

**The rendering behavior differs**:

| Aspect | Block in Section | Block in Block |
|--------|------------------|----------------|
| HTML Structure | Direct child of section | Nested within outer block |
| JavaScript Decoration | `decorate(block)` per block | Same — each block is decorated on its own root |
| Data Attributes | All present and available | May be scoped differently |
| CSS Selector Context | Section scope | Outer block scope |
| Event Communication | Full event system | Same unless a parent explicitly coordinates children |

**Key point:** Nesting is declared in **`filters` → `components`** in JSON so authors can place blocks inside other blocks. **Runtime decoration does not require special “parent block” JavaScript** unless the design needs the outer block to **compose** inner blocks (e.g. a custom grid or selection shell). See `.cursor/rules/22-repeatable-parent-child-blocks.mdc`.

**Repeatable patterns (screenshots / requirements):** Model as a **parent** block plus a **child** block (one child instance per repeat). **Non-repeated** fields and **overall** appearance belong on the **parent**; **per-item** fields and **per-item** appearance belong on the **child**. The **parent’s `filters` → `components` must list the child block** so the child can be placed inside the parent. **Nested** child blocks may get **different row wrapper HTML** than the same block at section level — child **`extractConfig`** may use `getHtmlFromBlockRow` / `getTextFromBlockRow` when needed; the parent stays a normal block unless it integrates children. See `.cursor/rules/22-repeatable-parent-child-blocks.mdc`.

## Content Modeling

### **HTML Generation by AEM**

When content is authored through Universal Editor, AEM automatically generates HTML following a specific pattern:

```html
<section class="section">
  <div class="block block-name">
    <div>
      <div>
        <!-- Field 1 content -->
      </div>
    </div>
    <div>
      <div>
        <!-- Field 2 content -->
      </div>
    </div>
  </div>
</section>
```

**Structure Explanation**:
- Root: `<section class="section">`
- Block wrapper: `<div class="block block-name">`
- Each field wrapped in two `<div>` layers (inner and outer)
- Content from authored fields in innermost divs

### **Element Grouping with Underscores**

Fields with underscore-separated names are grouped together:

```json
{
  "id": "block-id-123",
  "trackInView": true,
  "trackInView_meta": "{\"event\": \"view\"}"
}
```

Generates HTML:
```html
<div class="block" data-id="block-id-123">
  <div>
    <div>trackInView content</div>
  </div>
  <div>
    <div>trackInView_meta content</div>
  </div>
</div>
```

The `trackInView` and `trackInView_meta` fields are semantically linked and grouped in the authoring UI.

## Content Publishing Flow

### **Three Key Phases**

1. **Authoring Phase** (AEM)
   - Content created in Universal Editor
   - Stored in AEM repository
   - Validation and preview

2. **Publishing Phase**
   - Content published to Git repository
   - Automatic serialization to markdown/JSON
   - Triggered by publish workflow

3. **Delivery Phase**
   - Content served via CDN
   - JavaScript enhanced on client
   - Real-time updates possible

## Block Decoration

### **What is Decoration?**

The JavaScript process of taking AEM's generated HTML and transforming it into an interactive, functional block.

**Process**:
1. Block HTML received from server
2. JavaScript extracts data (extractConfig)
3. Config attached to block element
4. Block structure rebuilt if needed (buildBlock)
5. Event listeners added (appendEventListeners)
6. DOM manipulated to final state

**Timing**: Happens on page load, before user interaction

### **Instrumentation**

For authoring-side functionality, special data attributes are added by AEM:

```html
<div class="block block-cta" data-path="/blocks/cta" data-model="cta">
  <!-- Block content -->
</div>
```

When replacing the main block element, these attributes must be moved to the new element using `moveInstrumentation()`.

**Important**: This only affects authoring experience, not publish-side.

## Data Attributes

### **Standard Data Attributes**

| Attribute | Purpose | Example |
|-----------|---------|---------|
| `data-path` | Block path in repository | `/blocks/cta` |
| `data-model` | Component model name | `cta` |
| `id` | Block instance ID | `block-123` |
| `class` | Block name, `block` (from `decorateBlock`), and `eds-block-*` marker | `cta block eds-block-cta` |

### **Custom Data Attributes**

Blocks can add custom data attributes for tracking or functionality:

```html
<div class="block block-carousel" data-auto-play="true" data-delay="5000">
  <!-- Carousel content -->
</div>
```

## Key Technical Patterns

### **extractConfig**
Extracts authored data from AEM-generated HTML and returns a configuration object:

```javascript
const config = extractConfig(block);
// Returns: { id, title, description, image, ... }
```

See: [03-BLOCK_JAVASCRIPT_PATTERN.md](03-BLOCK_JAVASCRIPT_PATTERN.md)

### **moveInstrumentation**
Moves AEM's authoring instrumentation to a new element:

```javascript
const newMainElement = document.createElement('div');
moveInstrumentation(block, newMainElement);
```

This ensures Universal Editor can still edit the block.

## Component Models

**Definition**: JSON files that define:
- What fields authors can edit
- How those fields are presented in the UI
- What validation rules apply
- What values are allowed

**Structure**:
```json
{
  "id": "block-name",
  "fields": [
    { "component": "text", "name": "title", "label": "Title" },
    { "component": "richtext", "name": "content", "label": "Content" }
  ]
}
```

See: [02-JSON_CONFIGURATION.md](02-JSON_CONFIGURATION.md) for detailed explanation

## Filters

**Definition**: Rules that control which blocks are allowed in a section or block

**Purpose**:
- Enforce content structure
- Prevent invalid combinations
- Guide authors on available options

**Example**:
```json
{
  "groups": ["text", "media"],
  "blocksAllowed": ["text", "image", "video"]
}
```

See: [02-JSON_CONFIGURATION.md](02-JSON_CONFIGURATION.md) for detailed explanation

## Page Structure Example

```
Home Page
│
├── Header Section
│   ├── Logo Block
│   └── Navigation Block
│
├── Hero Section
│   ├── Image Block
│   ├── Title Block
│   └── CTA Block
│
├── Content Section
│   ├── Text Block
│   ├── Image Block
│   ├── Text Block
│   └── CTA Block
│
├── Features Section
│   ├── Text Block (intro)
│   ├── Feature Block (icon + text)
│   ├── Feature Block (icon + text)
│   └── Feature Block (icon + text)
│
└── Footer Section
    ├── Footer Links Block
    └── Copyright Block
```

## Section Styling

Sections can be styled with classes:

```html
<section class="section section-dark">
  <div class="block block-text">
    <!-- Content -->
  </div>
</section>
```

This allows different sections to have different background colors, padding, or layouts.

## Responsive Behavior

EDS is mobile-first:

1. **Base CSS** targets mobile (320px+)
2. **Tablet media queries** add styles for tablet (768px+)
3. **Desktop media queries** add styles for desktop (1200px+)

See: [13-RESPONSIVE_DESIGN_STRATEGY.md](13-RESPONSIVE_DESIGN_STRATEGY.md) for detailed approach

## Performance Considerations

### **Three-Phase Loading**

1. **Eager Loading**
   - Critical blocks loaded immediately
   - Example: Hero, above-the-fold content

2. **Lazy Loading**
   - Blocks loaded as user scrolls
   - Example: Content below fold

3. **Delayed Loading**
   - Loaded only on interaction
   - Example: Forms, modals, carousels

## Summary

**Key Takeaways**:
1. ✓ EDS uses flat structure: Sections contain Blocks
2. ✓ Sections cannot nest; Blocks can nest (with caveats)
3. ✓ AEM generates HTML automatically
4. ✓ JavaScript decorates HTML with functionality
5. ✓ JSON models define authoring experience
6. ✓ Element grouping via underscores
7. ✓ Instrumentation moves for authoring
8. ✓ Mobile-first responsive approach
9. ✓ Three-phase loading for performance
10. ✓ Event-driven architecture between blocks

## Next Steps

- New block/section requests: discover existing **`blocks/*/`** and **`models/_section.json`** first — see [02-JSON_CONFIGURATION.md](02-JSON_CONFIGURATION.md) (*New blocks and sections (discovery)*) and **`.cursor/rules/28-new-component-discovery.mdc`**
- Understand JSON configuration: [02-JSON_CONFIGURATION.md](02-JSON_CONFIGURATION.md)
- Learn block JavaScript pattern: [03-BLOCK_JAVASCRIPT_PATTERN.md](03-BLOCK_JAVASCRIPT_PATTERN.md)
- Review block design philosophy: [06-BLOCK_DESIGN_PHILOSOPHY.md](06-BLOCK_DESIGN_PHILOSOPHY.md)
