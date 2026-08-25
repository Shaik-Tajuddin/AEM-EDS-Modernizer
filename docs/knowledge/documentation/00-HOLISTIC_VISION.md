# AEM EDS Block System - Holistic Vision

## Project Overview

Create a **comprehensive AEM EDS-based design system** that enables building **any static website** through **configuration and composition**, not code. This is a custom block repository built on top of the AEM + Universal Editor implementation approach.

## Ultimate Vision

A **production-ready design system** with 19 foundational blocks that provides:

### **What This System Enables**
- **Configuration-driven development** - Authors create content through visual editors, not code
- **Composable architecture** - Combine basic blocks to create complex layouts
- **Basic CSS styling** - Simple, direct CSS with no abstraction layers
- **Authoring-first approach** - Content creators and marketers own the workflow
- **Highly modular blocks** - Each block is self-contained and reusable

## Core Components

### **1. Design System Foundation**
- **19 foundational blocks** designed for any static website
- **Basic CSS styling** with direct values (no variables, no tokens, no SCSS)
- **Responsive-first approach** supporting mobile, tablet, and desktop
- **Hyphenated class naming** convention (e.g., `text-callout-title`)

### **2. Block System**
- **19 custom blocks** replacing or enhancing OOTB blocks
- **Standardized implementation pattern** across all blocks
- **Deep authoring capabilities** with General, Appearance, and Analytics tabs
- **Rich data modeling** through JSON configuration
- **Data-attribute-driven analytics** using `dataLayer.js`

### **3. JSON Configuration Layer**
- **Component models** define authoring fields and UI structure
- **Underscore-prefixed JSON files** (`_block-name.json`)
- **File names match folder names** (e.g., `text-callout/text-callout.js`, `_text-callout.json`)
- **Conditional field visibility** using `condition` with JSON logic
- **Asset management** for DAM integration

### **4. Analytics System**
- **Data-attribute-driven tracking**: `data-trackinview`, `data-trackclick`
- **Author-configurable metrics** through JSON metadata
- **Centralized `applyTracking(block)`** from `dataLayer.js`
- **`window.dataLayer.events[]`** for analytics platform integration

### **5. Development Tooling**
- **Improved project structure** based on xwalk boilerplate
- **Basic CSS** — no build pipeline needed for styles
- **Local development environment** with live reload
- **Testing and validation** infrastructure

## Implementation Approach

### **Implementation Type**
We're using the **AEM + Universal Editor** approach, which means:
- Authors use AEM's Universal Editor for content creation
- Blocks are defined in JSON (models, definitions, filters)
- JavaScript brings blocks to life with interactivity
- Basic CSS provides styling (no SCSS, no variables, no tokens)
- Everything is version-controlled in Git

### **Architecture Principles**

**1. Flat Structure**
- Sections contain blocks
- Blocks don't nest in other blocks (with one-level exceptions)
- Sections cannot nest in other sections
- Simple, flat content hierarchy

**2. Authoring-Centric Design**
- All customization available through authoring UI
- No CSS classes added by authors
- Visual editors for all field types
- Rich text editing where appropriate

**3. Composability**
- Blocks work independently or together
- Data-attribute communication patterns
- Fragment blocks for code reuse
- Consistent patterns across all blocks

**4. Performance-Focused**
- Three-phase loading (Eager, Lazy, Delayed)
- Basic CSS for minimal file size
- Image optimization
- Critical rendering path optimization

## Foundational Blocks (19 Total)

### **Content & Text Blocks**
1. **Text** - RTE-based content with color, size, and alignment options
2. **Title** - Heading block with level selection via RTE formatting
3. **Table** - Structured data presentation
4. **Quote** - Attribution and styling for quoted content (Phase 2)

### **Media Blocks**
5. **Image** - Desktop/mobile image variants, captions, overlays, width control
6. **Video** - Local DAM + Vimeo/YouTube embedding
7. **Hero** - Hero section with background image/video (Phase 2)

### **Interactive Blocks**
8. **CTA** - Call-to-action with 4-5 variants
9. **CTA Group** - Multiple CTAs rendered together
10. **Button** - Single button (may merge with CTA)
11. **Form** - Complex form handling with validation
12. **Carousel** - Image/content carousel with navigation
13. **Accordion** - Collapsible content sections
14. **Tabs** - Tabbed content interface
15. **Modal** - Modal dialog functionality

### **Layout & Navigation Blocks**
16. **Breadcrumb** - Hierarchical navigation
17. **Pagination** - Multi-page content navigation
18. **Divider** - Visual separator/spacer

### **Advanced Blocks**
19. **Fragment** - Load HTML from other pages for code reuse
20. **Custom Column Section** - Flexible multi-column layout

*Note: Original scope included 19 blocks; some may be refined as design evolves*

## Technology Stack

### **Frontend**
- **JavaScript (ES6+)** - Modern vanilla JS for interactivity
- **Basic CSS** - Simple stylesheets with direct values
- **HTML5** - Semantic markup
- **dataLayer.js** - Analytics tracking utility

### **AEM & Content Management**
- **AEM EDS** - Edge Delivery Services platform
- **Universal Editor** - Content authoring interface
- **DAM** - Digital asset management
- **Path Mapping** - Configuration service for content publishing

### **Development Tools**
- **npm** - Package management
- **Git** - Version control
- **Visual Studio Code** - Recommended editor

## Development Phases

### **Phase 1: Foundation (Current)**
- ✅ Project structure setup
- ✅ Basic CSS styling approach
- ✅ 19 block implementations
- ✅ Analytics system integration (dataLayer.js)
- ✅ Documentation

### **Phase 2: Enhancement**
- Hero block
- Quote block
- Advanced form validation
- Custom section types
- Extended testing

### **Phase 3: Automation**
- **Block Generator** - Automated component scaffolding
- **Component library management**
- **Page synchronization** and updates

### **Phase 4: Scaling**
- Team collaboration features
- Version management
- Template library
- Reusable component patterns
- Best practices guide

## Design Philosophy

### **Highly Modular**
- Each block is self-contained
- Minimal dependencies between blocks
- Reusable JSON patterns
- Composable building blocks

### **Highly Authorable**
- Authors should never touch code
- Rich visual editing for all options
- Sensible defaults with customization
- Clear field organization with tabs

### **Highly Styleable**
- Basic CSS with direct values for clarity
- Style variants via `classes` and `classes_*` fields
- Alignment options where appropriate
- Color and typography control through CSS

### **Performance-First**
- Minimal JavaScript per block
- Basic CSS for fast loading
- Image optimization
- Lazy loading strategy
- Core Web Vitals optimization

## Key Differentiators from OOTB

The custom block system provides:

1. **Better Authoring** - More control with custom fields
2. **Consistent UX** - Unified appearance tab across blocks
3. **Rich Analytics** - Built-in tracking via data attributes
4. **Simple Styling** - Basic CSS, easy to maintain
5. **Data-Attribute Analytics** - `dataLayer.js` integration
6. **Reusable Patterns** - JSON merge for common patterns
7. **Mobile-First** - Responsive design built-in

## File Naming Conventions

### **Critical Rules**
1. **JSON files must have underscore prefix**: `_text-callout.json`, `_hero.json`
2. **All files must match folder name**: `text-callout/text-callout.js`, `text-callout.css`, `_text-callout.json`
3. **CSS files only**: `.css` (not `.scss`)

### **Repository Structure Overview**

```
aem-eds-blocks/
├── blocks/                # Individual block implementations
│   ├── text-callout/
│   │   ├── text-callout.js
│   │   ├── text-callout.css
│   │   └── _text-callout.json
│   ├── hero/
│   │   ├── hero.js
│   │   ├── hero.css
│   │   └── _hero.json
│   └── ...
├── scripts/               # Shared utilities
│   ├── dataLayer.js
│   └── utilities/
│       └── block-helpers.js
└── docs/                  # Documentation
```

## References

- **AEM EDS Documentation**: https://www.aem.live/developer
- **Reference Repository**: https://github.com/adobe-rnd/aem-boilerplate-xwalk
- **Universal Editor**: https://www.aem.live/docs/authoring
- **CSS Styling**: See [05-CSS_STYLING_APPROACH.md](05-CSS_STYLING_APPROACH.md)
- **Block Pattern**: See [03-BLOCK_JAVASCRIPT_PATTERN.md](03-BLOCK_JAVASCRIPT_PATTERN.md)
