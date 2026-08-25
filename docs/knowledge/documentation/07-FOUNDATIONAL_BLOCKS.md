# Foundational Blocks - 19 Block Specification

## Overview

The block system includes 19 foundational blocks that cover all common use cases for building static websites. These blocks are designed to be modular, composable, and highly authorable.

See: [08-BLOCK_SPECIFICATIONS.md](08-BLOCK_SPECIFICATIONS.md) for detailed specifications of key blocks

## Block Categories and List

### **Category 1: Content & Text Blocks**

#### **1. Text Block**
- **Purpose**: Display formatted text content
- **Key Features**: RTE content, color variants, size options, alignment
- **Authoring**: General (ID, RTE content) | Appearance (color, size, alignment) | Analytics
- **Appearance Options**: Color (default, inverse, primary, secondary, tertiary), Size (small, caption, default, large), Alignment (left, center, right)
- **Use Cases**: Body paragraphs, descriptions, callouts
- **Notes**: RTE allows formatting but alignment done through Appearance tab (not RTE)

#### **2. Title Block**
- **Purpose**: Display headings and titles
- **Key Features**: RTE-based heading level selection, color variants, size options
- **Authoring**: General (ID, RTE title) | Appearance (color, size) | Analytics
- **Appearance Options**: Color (default, inverse, primary, secondary, tertiary), Size options for different heading levels
- **Use Cases**: Page headings, section titles, card titles
- **Notes**: Heading level selected via RTE formatting (paragraph→h1, h2, etc.)

#### **3. Table Block**
- **Purpose**: Display structured data
- **Key Features**: Tabular content, headers, responsive design
- **Authoring**: General (ID, table content) | Appearance (styling) | Analytics
- **Appearance Options**: Color scheme, padding, border styles
- **Use Cases**: Data tables, pricing tables, comparison matrices
- **Notes**: Markdown table syntax or spreadsheet input

#### **4. Quote Block** (Phase 2)
- **Purpose**: Display testimonials and quotes
- **Key Features**: Quote text, attribution, styling
- **Authoring**: General (ID, quote text, attribution) | Appearance (styling) | Analytics
- **Appearance Options**: Background color, border style
- **Use Cases**: Testimonials, featured quotes, callouts

### **Category 2: Media Blocks**

#### **5. Image Block**
- **Purpose**: Display images with advanced options
- **Key Features**: Desktop/mobile variants, captions, overlays, width control
- **Authoring**: General (ID, mobile image, desktop image, caption, alt text) | Appearance (alignment, overlay position) | Analytics
- **Appearance Options**: Alignment (left, center, right), Overlay position (top-left, top-right, bottom-left, bottom-right)
- **Advanced Options**: Image width (number, pixels), overlay RTE, caption alignment
- **Use Cases**: Content images, hero images, product images
- **Technical**: Responsive images with srcset, lazy loading, fallback patterns

#### **6. Video Block**
- **Purpose**: Embed videos from multiple sources
- **Key Features**: Local DAM videos, YouTube/Vimeo embedding
- **Authoring**: General (ID, video source, video URL, poster image) | Appearance (sizing) | Analytics
- **Video Sources**: Local video file (DAM), YouTube URL, Vimeo URL
- **Appearance Options**: Width, height, autoplay, controls
- **Use Cases**: Product videos, tutorials, promotional videos
- **Notes**: Support both embedded and linked videos

#### **7. Hero Block** (Phase 2)
- **Purpose**: Large impactful section with background
- **Key Features**: Background image/video, headline, CTA, overlay
- **Authoring**: General (ID, background, headline, subheadline, CTA) | Appearance (overlay opacity, text position) | Analytics
- **Appearance Options**: Background overlay (color/opacity), text position
- **Use Cases**: Landing page hero sections, campaign pages
- **Technical**: Background image optimization, lazy loading

### **Category 3: Interactive Blocks**

#### **8. CTA Block**
- **Purpose**: Individual call-to-action button
- **Key Features**: Text, URL, multiple variants, styling
- **Authoring**: General (ID, CTA text, URL, target) | Appearance (variant, size) | Analytics
- **Variants**: primary, secondary, tertiary (4-5 total variants)
- **Appearance Options**: Variant (primary, secondary, tertiary), Size (small, default, large)
- **Use Cases**: Links, action buttons, navigation
- **Notes**: Can be composed into CTA Groups

#### **9. CTA Group Block**
- **Purpose**: Multiple CTAs rendered together
- **Key Features**: Container for multiple CTA blocks, flexible layout
- **Authoring**: General (ID, layout direction) | Appearance (spacing, alignment) | Analytics
- **Layout Options**: Horizontal, vertical, grid
- **Use Cases**: Multiple action buttons, button groups
- **Technical**: Composed of individual CTA blocks

#### **10. Button Block** (May merge with CTA)
- **Purpose**: Styled button element
- **Key Features**: Similar to CTA but button-specific
- **Authoring**: General (ID, text, action) | Appearance (style, size) | Analytics
- **Use Cases**: Form submissions, modal triggers
- **Note**: May be merged with CTA block for consistency

#### **11. Form Block**
- **Purpose**: Multi-field form with validation
- **Key Features**: Multiple input types, validation, submission handling
- **Authoring**: General (ID, form fields, submission) | Appearance (layout, styling) | Analytics
- **Field Types**: Text, email, password, textarea, select, checkbox, radio
- **Validation**: Required fields, email validation, custom rules
- **Submission**: Direct submission, webhook integration, email
- **Use Cases**: Contact forms, sign-up forms, feedback forms
- **Technical**: Form validation, error handling, submission tracking

#### **12. Carousel Block**
- **Purpose**: Rotating content carousel
- **Key Features**: Multiple slides, navigation, auto-play
- **Authoring**: General (ID, slides multifield) | Appearance (size, animation) | Analytics
- **Slide Content**: Image, title, description, link
- **Navigation**: Dots, arrows, keyboard support
- **Appearance Options**: Auto-play (yes/no), delay (ms), animation style
- **Use Cases**: Image galleries, testimonial carousels, featured content
- **Technical**: Touch support, keyboard navigation, accessibility

#### **13. Accordion Block**
- **Purpose**: Collapsible content sections
- **Key Features**: Multiple sections, toggle expand/collapse, single/multi-expand
- **Authoring**: General (ID, items multifield) | Appearance (styling) | Analytics
- **Items**: Title, content, initially expanded
- **Appearance Options**: Color, border style
- **Use Cases**: FAQs, expandable sections, content organization
- **Technical**: Accessibility (ARIA), keyboard navigation

#### **14. Tabs Block**
- **Purpose**: Tabbed content interface
- **Key Features**: Multiple tabs, content switching, lazy loading
- **Authoring**: General (ID, tabs multifield) | Appearance (styling) | Analytics
- **Tabs**: Tab name, content
- **Appearance Options**: Tab styling, active state styling
- **Use Cases**: Feature comparisons, documentation, product variations
- **Technical**: Accessible tab pattern, keyboard navigation

#### **15. Modal Block**
- **Purpose**: Modal dialog with overlay
- **Key Features**: Trigger mechanism, content, actions
- **Authoring**: General (ID, trigger type, content) | Appearance (sizing) | Analytics
- **Trigger Types**: Button, link, page load, event
- **Content**: Text, images, forms
- **Actions**: Close, submit, navigate
- **Use Cases**: Promotions, confirmations, lightboxes
- **Technical**: Focus management, escape key handling, backdrop

### **Category 4: Layout & Navigation Blocks**

#### **16. Breadcrumb Block**
- **Purpose**: Hierarchical navigation trail
- **Key Features**: Auto-generated path, custom links
- **Authoring**: General (ID, breadcrumb items) | Appearance (styling) | Analytics
- **Items**: Label, URL
- **Appearance Options**: Styling, separator style
- **Use Cases**: Site navigation, page location indication
- **Technical**: Structured data (Schema.org), accessibility

#### **17. Pagination Block**
- **Purpose**: Multi-page navigation
- **Key Features**: Previous/next buttons, page numbers, results per page
- **Authoring**: General (ID, total pages, current page) | Appearance (styling) | Analytics
- **Appearance Options**: Styling, alignment
- **Use Cases**: Search results, content listing, archive pages
- **Technical**: URL parameter handling, state management

#### **18. Divider Block**
- **Purpose**: Visual separator and spacer
- **Key Features**: Visual line or spacing, configurable height
- **Authoring**: General (ID, type) | Appearance (styling, spacing) | Analytics
- **Types**: Line, spacing, decorative
- **Appearance Options**: Style (solid, dashed, dotted), color, height, spacing
- **Use Cases**: Section separation, visual hierarchy
- **Notes**: Can be used purely for spacing

### **Category 5: Advanced Blocks**

#### **19. Fragment Block**
- **Purpose**: Load and embed HTML from other pages
- **Key Features**: Dynamic content loading, reusable components
- **Authoring**: General (ID, source URL) | Appearance (sizing) | Analytics
- **Source**: Path to HTML file to load
- **Use Cases**: Code reuse, shared components, dynamic content
- **Technical**: Fetch API, DOM insertion, script execution

#### **20. Custom Column Section** (Bonus)
- **Purpose**: Flexible multi-column layout
- **Key Features**: Configurable columns, responsive
- **Authoring**: General (ID, column count) | Appearance (gap, alignment) | Analytics
- **Configuration**: Mobile columns, tablet columns, desktop columns
- **Appearance Options**: Gap between columns, alignment, justification
- **Use Cases**: Feature layouts, service showcases, team displays

## Block Status and Phases

### **Phase 1: Core Blocks** (Priority)

These 16 blocks cover all common website needs:

1. Text Block - ✅ Essential
2. Title Block - ✅ Essential
3. Image Block - ✅ Essential
4. CTA Block - ✅ Essential
5. Form Block - ✅ Essential
6. Table Block - ✅ Common
7. Accordion Block - ✅ Common
8. Tabs Block - ✅ Common
9. Video Block - ✅ Common
10. Carousel Block - ✅ Common
11. Breadcrumb Block - ✅ Common
12. Pagination Block - ✅ Navigation
13. Divider Block - ✅ Utility
14. Fragment Block - ✅ Advanced
15. CTA Group Block - ✅ Composition
16. Custom Column Section - ✅ Layout

### **Phase 2: Enhanced Blocks**

Additional blocks for extended functionality:

- Hero Block - Large impactful sections
- Quote Block - Testimonials and featured quotes
- Button Block - Specialized button handling (may merge with CTA)
- Modal Block - Dialog and overlay functionality
- Advanced Form Validation - Custom validation rules
- Grid Block - Advanced layout controls

### **Phase 3: Automation**

Blocks generated from design system:

- Figma-generated blocks
- Design token-based variants
- Auto-generated responsive variants

## Common Features Across All Blocks

### **Every Block Includes**

**Authoring Interface**:
- ✅ General tab (content, ID field)
- ✅ Appearance tab (colors, sizes, alignment)
- ✅ Analytics tab (tracking configuration)

**JavaScript Implementation**:
- ✅ extractConfig() for data extraction
- ✅ buildBlock() for preparation
- ✅ appendEventListeners() for interactions
- ✅ Configuration attachment to element

**Styling**:
- ✅ Design token integration
- ✅ Mobile-first responsive
- ✅ Variant support (color, size)
- ✅ Accessibility compliance

**Analytics**:
- ✅ trackInView configuration
- ✅ trackClick for interactions
- ✅ Custom metadata support
- ✅ Event emission via mitt

## Implementation Strategy

### **Order of Implementation**

1. **Week 1**: Text, Title, Image (Content blocks)
2. **Week 2**: CTA, Form (Interactive blocks)
3. **Week 3**: Table, Accordion, Tabs (Complex blocks)
4. **Week 4**: Video, Carousel, Breadcrumb, Pagination (Advanced)
5. **Week 5**: Divider, Fragment, CTA Group, Custom Column (Utility)
6. **Week 6**: Testing, documentation, refinement

### **Per-Block Checklist**

For each block implementation:

- [ ] Define component model (General, Appearance, Analytics tabs)
- [ ] Create definition with metadata
- [ ] Implement JavaScript with standard pattern
- [ ] Create SCSS with design tokens
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Test authoring experience
- [ ] Document block spec (see 08-BLOCK_SPECIFICATIONS.md)
- [ ] Add to documentation
- [ ] Get stakeholder approval

## Block Naming Conventions

### **Class Names**
```scss
.block-text           // Block wrapper
.block-title          // Block wrapper
.block-image          // Block wrapper
.block-cta            // Block wrapper

// Variants
.text-primary         // Color variant
.text-large           // Size variant
.text-align-center    // Alignment variant
```

### **Data Attributes**
```javascript
data-id              // Block instance ID
data-config          // Configuration JSON
data-variant         // Variant name
data-tracking        // Analytics tracking
```

### **JavaScript Exports**
```javascript
// Each block exports decorate function
export default function decorate(block) { }

// Can also export helper functions for testing
export { extractConfig, buildBlock };
```

## Dependencies Between Blocks

### **Composition Structure**

```
CTA Group Block
├── CTA Block (multiple)

Custom Column Section
├── Any block (flexible)

Modal Block
├── Form or other blocks

Fragment Block
└── External HTML (no dependency)
```

### **No Direct Dependencies**

Blocks communicate via events, not direct function calls:

```javascript
// Block A emits
emitter.emit('event-name', data);

// Block B listens
emitter.on('event-name', (data) => { ... });
```

## Success Criteria

### **Per Block**
- ✓ Passes authoring testing
- ✓ Responsive on mobile/tablet/desktop
- ✓ Meets accessibility standards
- ✓ Analytics tracking works
- ✓ Performance benchmarks met
- ✓ Documentation complete
- ✓ Unit tests pass
- ✓ Integration tests pass

### **Overall System**
- ✓ All 19 blocks implemented
- ✓ Consistent authoring experience
- ✓ Complete design token system
- ✓ Full documentation
- ✓ Team training materials
- ✓ Ready for production use

## References

- [00-HOLISTIC_VISION.md](00-HOLISTIC_VISION.md) - Project overview
- [08-BLOCK_SPECIFICATIONS.md](08-BLOCK_SPECIFICATIONS.md) - Detailed specs for key blocks
- [02-JSON_CONFIGURATION.md](02-JSON_CONFIGURATION.md) - JSON structure
- [03-BLOCK_JAVASCRIPT_PATTERN.md](03-BLOCK_JAVASCRIPT_PATTERN.md) - JS pattern
- [06-BLOCK_DESIGN_PHILOSOPHY.md](06-BLOCK_DESIGN_PHILOSOPHY.md) - Design principles
- [10-PROJECT_STRUCTURE.md](10-PROJECT_STRUCTURE.md) - Code organization
