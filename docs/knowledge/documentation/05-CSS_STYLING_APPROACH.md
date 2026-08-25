# CSS Styling Approach — Basic CSS

## Overview

All AEM EDS blocks use **basic CSS** (no SCSS). **Colors**, **spacing**, and **typography** (sizes, families, weights) use **CSS variables** from **`styles/`** in **`blocks/**/*.css`** wherever applicable: **`var(--color-*)`** from **`styles/colors.css`**, and **`var(--space-*)`**, **`var(--body-font-size-*)`**, **`var(--heading-font-size-*)`**, **`var(--font-family-*)`**, **`var(--font-weight-*)`** from **`styles/styles.css`**. **Literal** sRGB values for the palette live **only** in **`styles/colors.css`**; new global type stops belong in **`styles/styles.css`** `:root` (and media overrides when needed). See **`.cursor/rules/30-block-css-design-tokens.mdc`** and **`04-block-css-pattern.mdc`**.

> **Production Reference**: See `text_callout_block/text-callout.css` for the canonical example.

### What We Use
- ✅ Basic `.css` files
- ✅ **`var(--color-*)`** for ink, borders, fills, shadows, outlines in blocks (add new stops in **`styles/colors.css`** when no close token exists)
- ✅ **`var(--space-*)`** where it matches the spacing scale; other lengths may stay literal when there is no token
- ✅ **`var(--body-font-size-*)`**, **`var(--heading-font-size-*)`**, **`var(--font-family-*)`**, **`var(--font-weight-*)`** from **`styles/styles.css`** for typography in blocks when applicable
- ✅ Standard `@media` queries for responsive design
- ✅ Hyphenated class names (e.g., `.text-callout-title`)

### What We Do NOT Use
- ❌ SCSS (`.scss` files, `$variables`, `@include mixins`, nesting)
- ❌ Raw **`#…` / `rgb()` / `hsl()`** for UI colors **inside** **`blocks/**/*.css`**
- ❌ Declaring **new** block-local **`--my-block-*`** for colors or global type scales in **`blocks/`** (define them in **`styles/colors.css`** or **`styles/styles.css`** instead)
- ❌ Figma token pipelines or CSS-in-JS

---

## File Naming

CSS files must match the folder name:

```
blocks/text-callout/
├── text-callout.js
├── text-callout.css        ← matches folder name
└── _text-callout.json      ← underscore prefix for JSON
```

---

## CSS Structure for a Block

Every block CSS file follows this structure:

```css
/* Block Name Styles */
/* Mobile-first responsive design with basic CSS */

/* ============================================================================
   BLOCK CONTAINER
   ============================================================================ */
.block-name {
  display: flex;
  padding: 3rem 1.5rem;
  background-color: #ffffff;
  color: #1a1a1a;
}

@media (min-width: 768px) {
  .block-name {
    padding: 4rem 3rem;
  }
}

@media (min-width: 1024px) {
  .block-name {
    padding: 4rem;
  }
}

/* ============================================================================
   INNER ELEMENTS
   ============================================================================ */
.block-name-inner {
  max-width: 640px;
  width: 100%;
}

.block-name-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 1rem 0;
}

@media (min-width: 768px) {
  .block-name-title {
    font-size: 1.75rem;
  }
}

.block-name-text {
  font-size: 1rem;
  line-height: 1.75;
  color: #4a4a4a;
  margin: 0 0 2rem 0;
}

/* ============================================================================
   CTA / INTERACTIVE ELEMENTS
   ============================================================================ */
.block-name .brand-cta {
  display: inline-block;
  padding: 1rem 3rem;
  background-color: #0066cc;
  color: #ffffff;
  border: 2px solid transparent;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
}

.block-name .brand-cta:hover {
  background-color: #0052a3;
}

/* ============================================================================
   STYLE VARIANTS (applied by AEM via classes field)
   ============================================================================ */
.block-name.variant-name .brand-cta {
  background-color: #6c757d;
}

/* ============================================================================
   ALIGNMENT CLASSES (applied by AEM via classes_* field)
   ============================================================================ */
.block-name.block-name-align-center {
  justify-content: center;
  text-align: center;
}

/* ============================================================================
   ACCESSIBILITY
   ============================================================================ */
@media (prefers-reduced-motion: reduce) {
  .block-name .brand-cta {
    transition: none;
  }
}

@media print {
  .block-name {
    padding: 1rem;
    page-break-inside: avoid;
  }
}
```

---

## Class Naming Convention

Use **hyphenated names** based on the block name:

| Element | Class Name |
|---------|------------|
| Container | `.text-callout` |
| Inner wrapper | `.text-callout-inner` |
| Title | `.text-callout-title` |
| Text content | `.text-callout-text` |
| CTA element | `.brand-cta` (shared across blocks) |

**Do NOT use**:
- BEM notation (`.text-callout__title`, `.text-callout--active`)
- Generic names (`.title`, `.content`, `.wrapper`)

---

## Responsive Design

Use standard CSS media queries with mobile-first approach:

```css
/* Base: Mobile (default) */
.block-name-title {
  font-size: 1.5rem;
}

/* Tablet: 768px+ */
@media (min-width: 768px) {
  .block-name-title {
    font-size: 1.75rem;
  }
}

/* Desktop: 1024px+ */
@media (min-width: 1024px) {
  .block-name-title {
    font-size: 2rem;
  }
}
```

### Standard Breakpoints

| Name | Width | Usage |
|------|-------|-------|
| Mobile | Default | Base styles |
| Tablet | `768px` | `@media (min-width: 768px)` |
| Desktop | `1024px` | `@media (min-width: 1024px)` |
| Large | `1440px` | `@media (min-width: 1440px)` (when needed) |

---

## Style Variants

Variants are applied by AEM as CSS classes on the outer block `div` based on the `classes` and `classes_*` JSON fields.

Target variants by combining block class with variant class:

```css
/* Default style */
.simple-cta .brand-cta {
  background-color: #0066cc;
  color: #ffffff;
}

/* Primary outlined variant */
.simple-cta.cta-primary-outlined .brand-cta {
  background-color: transparent;
  color: #0066cc;
  border-color: #0066cc;
}

/* Secondary filled variant */
.simple-cta.cta-secondary-filled .brand-cta {
  background-color: #6c757d;
  color: #ffffff;
}
```

---

## Best Practices

### ✅ Do
- Use **`var(--color-*)`** for colors, **`var(--space-*)`** for spacing, and **type tokens** (**`var(--body-font-size-*)`**, **`var(--heading-font-size-*)`**, **`var(--font-family-*)`**, **`var(--font-weight-*)`**) from **`styles/`** in **`blocks/**/*.css`** wherever they fit (**`30-block-css-design-tokens.mdc`**)
- Use `rem` / `px` for one-off lengths only when there is no matching token
- Write mobile-first (base = mobile, add `@media` for larger)
- Include `prefers-reduced-motion` media query
- Include `@media print` styles
- Use `focus-visible` for keyboard focus indicators
- Keep selectors simple and flat

### ❌ Don’t
- Use raw **`#…` / `rgb()` / `hsl()`** for UI colors in **`blocks/**/*.css`**
- Invent **block-local** **`--my-block-*`** for colors or global type scales — add tokens under **`styles/`** instead
- Use SCSS syntax (`$variables`, `@include`, `&:hover`, nesting)
- Use deeply nested selectors (max 2 levels)
- Use `!important`
- Use generic class names

---

## Loading States

```css
/* Hidden until decorated */
.block-name:not(.block-name--ready) {
  opacity: 0;
  transition: opacity 0.3s ease-in-out;
}

/* Visible after decoration */
.block-name.block-name--ready {
  opacity: 1;
}

/* Error state */
.block-name.block-name--error {
  opacity: 1;
  border: 2px dashed #cc0000;
  min-height: 100px;
}

.block-name.block-name--error::after {
  content: 'Block failed to load';
  display: block;
  text-align: center;
  padding: 1.5rem;
  color: #cc0000;
  font-size: 0.875rem;
}
```

---

## References

- [text_callout_block/text-callout.css](../text_callout_block/text-callout.css) — Production reference
- [simple_cta_block/simple-cta.css](../simple_cta_block/simple-cta.css) — Example implementation
- [hero_block/hero.css](../hero_block/hero.css) — Complex block example
- [13-RESPONSIVE_DESIGN_STRATEGY.md](13-RESPONSIVE_DESIGN_STRATEGY.md) — Responsive patterns
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](16-BLOCK_DEVELOPMENT_TEMPLATE.md) — Complete block template
