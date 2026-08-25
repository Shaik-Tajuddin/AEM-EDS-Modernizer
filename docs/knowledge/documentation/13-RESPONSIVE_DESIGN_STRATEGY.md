# Responsive Design Strategy - Mobile-First Approach

## Overview

All blocks follow a **mobile-first, progressive enhancement** strategy for responsive design using **basic CSS media queries**.

See: [05-CSS_STYLING_APPROACH.md](05-CSS_STYLING_APPROACH.md) for CSS implementation

## Core Principles

### **1. Mobile-First**

**Definition**: Base styles target mobile devices, media queries add styles for larger screens.

**Benefits**:
- ✓ Smaller CSS on mobile (no override needed)
- ✓ Simpler CSS logic (progressive enhancement)
- ✓ Better performance
- ✓ Forces focus on mobile UX

**Example**:
```css
/* Base (mobile) */
.text-callout {
  font-size: 0.875rem;
  padding: 1rem;
  display: flex;
  flex-direction: column;
}

/* Tablet and up */
@media (min-width: 768px) {
  .text-callout {
    font-size: 1rem;
    padding: 1.5rem;
    flex-direction: row;
  }
}

/* Desktop and up */
@media (min-width: 1024px) {
  .text-callout {
    font-size: 1.125rem;
    padding: 2rem;
  }
}
```

### **2. Progressive Enhancement**

**Definition**: Enhance experience for capable devices without breaking on limited ones.

**Example**:
```css
/* Works everywhere */
.hero-image {
  width: 100%;
  height: auto;
}

/* Enhanced on modern browsers */
@supports (aspect-ratio: 1) {
  .hero-image {
    aspect-ratio: 16 / 9;
    object-fit: cover;
  }
}
```

### **3. Content-Driven Layout**

**Definition**: Layout adapts to content, not viewport size alone.

```css
/* Use min-width for flexibility */
@supports (display: grid) {
  .cards-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 1.5rem;
  }
}

/* Fallback for non-grid browsers */
.cards-grid {
  display: flex;
  flex-wrap: wrap;
}
```

## Breakpoints

### **Standard Breakpoints**

| Name | Width | CSS |
|------|-------|-----|
| Mobile | Default | Base styles (no media query) |
| Tablet | `768px` | `@media (min-width: 768px)` |
| Desktop | `1024px` | `@media (min-width: 1024px)` |
| Large | `1440px` | `@media (min-width: 1440px)` (when needed) |

### **When to Use Each**

| Breakpoint | Target Devices | Use Case |
|-----------|----------------|----------|
| Mobile (default) | Phones | Single column, full width |
| Tablet (768px+) | Tablets, large phones | Two columns, increased padding |
| Desktop (1024px+) | Desktops | Three columns, horizontal layout |
| Large (1440px+) | Large monitors | Maximum width containers |

### **Don't Create Arbitrary Breakpoints**

❌ **Avoid**:
```css
@media (min-width: 600px) { ... }
@media (min-width: 900px) { ... }
@media (min-width: 1400px) { ... }
```

✅ **Use standard breakpoints**:
```css
@media (min-width: 768px) { ... }
@media (min-width: 1024px) { ... }
```

## Common Responsive Patterns

### **Pattern 1: Flexible Columns**

```css
.cards-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

@media (min-width: 768px) {
  .cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1024px) {
  .cards-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
```

### **Pattern 2: Stack to Side-by-Side**

```css
.hero {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.hero-image-wrapper {
  width: 100%;
}

@media (min-width: 768px) {
  .hero {
    flex-direction: row;
  }

  .hero-image-wrapper {
    width: 50%;
    flex-shrink: 0;
  }
}
```

### **Pattern 3: Hidden Content**

```css
.hero-image-mobile {
  display: block;
}

.hero-image-desktop {
  display: none;
}

@media (min-width: 768px) {
  .hero-image-mobile {
    display: none;
  }

  .hero-image-desktop {
    display: block;
  }
}
```

### **Pattern 4: Font Scaling**

```css
.hero-headline {
  font-size: 1.75rem;
  line-height: 1.2;
}

@media (min-width: 768px) {
  .hero-headline {
    font-size: 2.25rem;
  }
}

@media (min-width: 1024px) {
  .hero-headline {
    font-size: 3rem;
  }
}
```

### **Pattern 5: Spacing Adjustment**

```css
.text-callout {
  padding: 1.5rem 1rem;
}

@media (min-width: 768px) {
  .text-callout {
    padding: 3rem 2rem;
  }
}

@media (min-width: 1024px) {
  .text-callout {
    padding: 4rem 3rem;
  }
}
```

### **Pattern 6: Image Optimization**

```css
.hero-image {
  width: 100%;
  height: auto;
}

@media (min-width: 1024px) {
  .hero-image {
    width: 80%;
    margin: 0 auto;
  }
}
```

```html
<!-- HTML: picture element for responsive images -->
<picture>
  <source media="(min-width: 1024px)" srcset="/image-wide.jpg">
  <source media="(min-width: 768px)" srcset="/image-medium.jpg">
  <img src="/image-mobile.jpg" alt="Description">
</picture>
```

## Typography Scaling

Use direct font sizes that scale with breakpoints:

```css
/* Mobile base */
.block-name-title {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.2;
}

/* Tablet */
@media (min-width: 768px) {
  .block-name-title {
    font-size: 1.75rem;
  }
}

/* Desktop */
@media (min-width: 1024px) {
  .block-name-title {
    font-size: 2rem;
  }
}
```

## Touch vs. Click Interactions

```css
/* Larger touch targets on mobile */
.hero-cta-button {
  padding: 1rem 3rem;
  min-height: 44px; /* Touch-friendly */
}

/* Hover states work on desktop */
.hero-cta-button:hover {
  background-color: #2a2a2a;
}
```

## Testing Responsive Design

### **Breakpoint Testing Checklist**

- [ ] Mobile (320px): Content readable, single column, touch-friendly
- [ ] Tablet (768px): Two-column layouts work, improved spacing
- [ ] Desktop (1024px): Full layouts, optimized spacing
- [ ] Large (1440px+): Max-width containers, good proportions

### **Common Issues**

| Issue | Solution |
|-------|----------|
| Text too small | Use min font size, don't go below 0.75rem |
| Images stretched | Use `max-width: 100%` and `height: auto` |
| Overflow on mobile | Check for hardcoded widths |
| Buttons too small | Touch target minimum 44x44px |
| Horizontal scroll | Check for overflow |

## Summary: Mobile-First Checklist

- [ ] Base CSS targets mobile (320px+)
- [ ] Media queries add styles for larger screens
- [ ] Three breakpoints: 768px, 1024px, 1440px
- [ ] No arbitrary breakpoints
- [ ] Typography scales across breakpoints
- [ ] Touch targets ≥ 44x44px on mobile
- [ ] Images responsive with srcset/picture
- [ ] No horizontal scrolling
- [ ] Uses basic CSS only (no SCSS, no variables)
- [ ] Tested on actual devices

## References

- [05-CSS_STYLING_APPROACH.md](05-CSS_STYLING_APPROACH.md) - CSS approach
- [12-DEVELOPMENT_PATTERNS.md](12-DEVELOPMENT_PATTERNS.md) - Standards
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](16-BLOCK_DEVELOPMENT_TEMPLATE.md) - Block template
