# Block Specifications - Detailed Implementation Guide

## Overview

This document provides detailed specifications for key foundational blocks. Use these as templates for implementing other blocks.

See: [07-FOUNDATIONAL_BLOCKS.md](07-FOUNDATIONAL_BLOCKS.md) for complete block list

## Block 1: Text Block

### **Purpose**
Display formatted text content with styling options.

### **Use Cases**
- Body paragraphs
- Descriptions
- Content sections
- Callout boxes

### **Component Model**

**File**: `models/text.json` (after merge processing)

```json
{
  "id": "text",
  "fields": [
    {
      "component": "tab",
      "name": "general",
      "label": "General"
    },
    {
      "component": "text",
      "name": "id",
      "label": "Block ID",
      "description": "Unique identifier for this block"
    },
    {
      "component": "richtext",
      "name": "content",
      "label": "Content",
      "description": "Text content with formatting"
    },
    {
      "component": "tab",
      "name": "appearance",
      "label": "Appearance"
    },
    {
      "component": "select",
      "name": "color",
      "label": "Text Color",
      "value": "default",
      "options": ["default", "inverse", "primary", "secondary", "tertiary"]
    },
    {
      "component": "select",
      "name": "size",
      "label": "Text Size",
      "value": "default",
      "options": ["small", "caption", "default", "large"]
    },
    {
      "component": "select",
      "name": "alignment",
      "label": "Text Alignment",
      "value": "left",
      "options": ["left", "center", "right"]
    },
    {
      "component": "tab",
      "name": "analytics",
      "label": "Analytics"
    },
    {
      "component": "checkbox",
      "name": "trackInView",
      "label": "Track In-View Events"
    },
    {
      "component": "textarea",
      "name": "trackInView_meta",
      "label": "In-View Metadata (JSON)",
      "description": "Enter valid JSON for tracking data",
      "visible": "{trackInView}"
    }
  ]
}
```

### **Definition**

**File**: `definitions/text.json`

```json
{
  "title": "Text Block",
  "description": "Display formatted text content",
  "icon": "text",
  "group": "text",
  "tags": ["content", "typography"],
  "status": "stable"
}
```

### **AEM Generated HTML Example**

```html
<div class="block block-text" data-id="text-123">
  <div>
    <div><p>This is body text content.</p></div>
  </div>
  <div>
    <div>primary</div>
  </div>
  <div>
    <div>default</div>
  </div>
  <div>
    <div>left</div>
  </div>
  <div>
    <div>true</div>
  </div>
  <div>
    <div>{"event": "text-viewed"}</div>
  </div>
</div>
```

### **JavaScript Implementation**

```javascript
// blocks/text/text.js
import { emitter } from '../../utils/event-emitter.js';
import { trackInView } from '../../utils/analytics.js';

function extractConfig(block) {
  const config = {
    mainEl: block,
  };

  const rows = block.querySelectorAll('div > div');

  // Extract content
  config.content = rows[0];
  
  // Extract appearance
  config.color = rows[1]?.textContent?.trim() || 'default';
  config.size = rows[2]?.textContent?.trim() || 'default';
  config.alignment = rows[3]?.textContent?.trim() || 'left';

  // Extract analytics
  config.trackInView = rows[4]?.textContent?.trim() === 'true';
  config.trackInView_meta = rows[5]?.textContent?.trim() || '{}';

  return config;
}

function buildBlock(config) {
  // Add variant classes
  config.classes = [];
  
  if (config.color !== 'default') {
    config.classes.push(`text-${config.color}`);
  }
  
  if (config.size !== 'default') {
    config.classes.push(`text-${config.size}`);
  }
  
  if (config.alignment !== 'left') {
    config.classes.push(`text-align-${config.alignment}`);
  }

  return config;
}

function appendEventListeners(config) {
  if (config.trackInView) {
    const metadata = JSON.parse(config.trackInView_meta);
    trackInView(config.mainEl, metadata);
  }
}

export default function decorate(block) {
  let config = extractConfig(block);
  config = buildBlock(config);
  appendEventListeners(config);

  // Apply variant classes
  config.classes.forEach(cls => block.classList.add(cls));

  // Attach config for future use
  block.blockConfig = config;
}

export { extractConfig, buildBlock };
```

### **SCSS Styling**

```scss
// blocks/text/text.scss
@import '../../styles/tokens/semantic';
@import '../../styles/mixins/responsive';

.block-text {
  @include padding($spacing-md);

  // Base typography
  p {
    @include text-default;
    color: $text-default;
    margin: 0 0 $spacing-md 0;

    &:last-child {
      margin-bottom: 0;
    }
  }

  // Responsive padding
  @include respond-to('tablet') {
    @include padding($spacing-lg);
  }

  // Color variants
  &.text-primary {
    color: $text-primary;
  }

  &.text-secondary {
    color: $text-secondary;
  }

  &.text-inverse {
    color: $text-inverse;
    background-color: $bg-inverse;
  }

  // Size variants
  &.text-small {
    @include text-small;
  }

  &.text-large {
    @include text-large;
  }

  &.text-caption {
    @include text-caption;
  }

  // Alignment
  &.text-align-center {
    text-align: center;
  }

  &.text-align-right {
    text-align: right;
  }
}
```

---

## Block 2: Title Block

### **Purpose**
Display headings with flexible level selection and styling options.

### **Use Cases**
- Page titles
- Section headings
- Card titles
- Feature headlines

### **Component Model**

```json
{
  "id": "title",
  "fields": [
    {
      "component": "tab",
      "name": "general",
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
      "label": "Title",
      "description": "Use formatting to set heading level (paragraph=h1, etc.)"
    },
    {
      "component": "tab",
      "name": "appearance",
      "label": "Appearance"
    },
    {
      "component": "select",
      "name": "color",
      "label": "Title Color",
      "value": "default",
      "options": ["default", "inverse", "primary", "secondary", "tertiary"]
    },
    {
      "component": "select",
      "name": "size",
      "label": "Title Size",
      "value": "h2",
      "options": ["h1", "h2", "h3", "h4"]
    },
    {
      "component": "select",
      "name": "alignment",
      "label": "Alignment",
      "value": "left",
      "options": ["left", "center", "right"]
    },
    {
      "component": "tab",
      "name": "analytics",
      "label": "Analytics"
    },
    {
      "component": "checkbox",
      "name": "trackInView",
      "label": "Track In-View"
    },
    {
      "component": "textarea",
      "name": "trackInView_meta",
      "label": "In-View Metadata (JSON)",
      "visible": "{trackInView}"
    }
  ]
}
```

### **JavaScript Implementation**

```javascript
// blocks/title/title.js
import { trackInView } from '../../utils/analytics.js';

function extractConfig(block) {
  const config = { mainEl: block };
  const rows = block.querySelectorAll('div > div');

  // Extract heading element from RTE
  const headingEl = rows[0]?.querySelector('h1, h2, h3, h4, h5, h6') || 
                    rows[0]?.querySelector('p');
  
  config.titleElement = headingEl;
  config.level = headingEl?.tagName?.toLowerCase() || 'h2';

  // Extract styling
  config.color = rows[1]?.textContent?.trim() || 'default';
  config.size = rows[2]?.textContent?.trim() || 'h2';
  config.alignment = rows[3]?.textContent?.trim() || 'left';

  // Extract analytics
  config.trackInView = rows[4]?.textContent?.trim() === 'true';
  config.trackInView_meta = rows[5]?.textContent?.trim() || '{}';

  return config;
}

function buildBlock(config) {
  config.classes = [];
  
  // Add size class matching heading level
  config.classes.push(`heading-${config.size}`);
  
  if (config.color !== 'default') {
    config.classes.push(`heading-${config.color}`);
  }
  
  if (config.alignment !== 'left') {
    config.classes.push(`text-align-${config.alignment}`);
  }

  return config;
}

function appendEventListeners(config) {
  if (config.trackInView) {
    const metadata = JSON.parse(config.trackInView_meta);
    trackInView(config.mainEl, metadata);
  }
}

export default function decorate(block) {
  let config = extractConfig(block);
  config = buildBlock(config);
  appendEventListeners(config);

  config.classes.forEach(cls => block.classList.add(cls));
  block.blockConfig = config;
}
```

### **SCSS Styling**

```scss
// blocks/title/title.scss
@import '../../styles/tokens/semantic';
@import '../../styles/mixins/responsive';
@import '../../styles/mixins/typography';

.block-title {
  @include padding($spacing-md);

  h1, h2, h3, h4, h5, h6 {
    margin: 0;
  }

  // Size classes
  &.heading-h1 {
    h1, h2, h3, h4, h5, h6 {
      @include heading-h1;
    }
  }

  &.heading-h2 {
    h1, h2, h3, h4, h5, h6 {
      @include heading-h2;
    }
  }

  &.heading-h3 {
    h1, h2, h3, h4, h5, h6 {
      font-size: $font-size-large;
      font-weight: $font-weight-bold;
    }
  }

  // Color variants
  &.heading-primary {
    color: $text-primary;
  }

  &.heading-inverse {
    color: $text-inverse;
  }

  // Alignment
  &.text-align-center {
    text-align: center;
  }

  &.text-align-right {
    text-align: right;
  }
}
```

---

## Block 3: Image Block

### **Purpose**
Display responsive images with captions, overlays, and advanced options.

### **Key Features**
- Desktop and mobile image variants
- Caption support
- Overlay positioning (4 corners)
- Image width control
- Fallback pattern support

### **Component Model**

```json
{
  "id": "image",
  "fields": [
    {
      "component": "tab",
      "name": "general",
      "label": "General"
    },
    {
      "component": "text",
      "name": "id",
      "label": "Block ID"
    },
    {
      "component": "asset",
      "name": "imageMobile",
      "label": "Mobile Image"
    },
    {
      "component": "asset",
      "name": "imageDesktop",
      "label": "Desktop Image"
    },
    {
      "component": "text",
      "name": "alt",
      "label": "Alt Text",
      "description": "Accessibility text"
    },
    {
      "component": "text",
      "name": "caption",
      "label": "Caption"
    },
    {
      "component": "number",
      "name": "imageWidth",
      "label": "Image Width (px)",
      "description": "Set width, leave blank for auto"
    },
    {
      "component": "tab",
      "name": "appearance",
      "label": "Appearance"
    },
    {
      "component": "select",
      "name": "alignment",
      "label": "Alignment",
      "value": "left",
      "options": ["left", "center", "right"]
    },
    {
      "component": "select",
      "name": "captionAlignment",
      "label": "Caption Alignment",
      "value": "left",
      "options": ["left", "center", "right"]
    },
    {
      "component": "tab",
      "name": "overlay",
      "label": "Overlay"
    },
    {
      "component": "richtext",
      "name": "overlay",
      "label": "Overlay Disclaimer",
      "description": "Optional overlay text"
    },
    {
      "component": "select",
      "name": "overlayPosition",
      "label": "Overlay Position",
      "value": "top-left",
      "options": ["top-left", "top-right", "bottom-left", "bottom-right"]
    },
    {
      "component": "tab",
      "name": "analytics",
      "label": "Analytics"
    },
    {
      "component": "checkbox",
      "name": "trackInView",
      "label": "Track In-View"
    },
    {
      "component": "textarea",
      "name": "trackInView_meta",
      "label": "In-View Metadata (JSON)",
      "visible": "{trackInView}"
    }
  ]
}
```

### **JavaScript Implementation**

```javascript
// blocks/image/image.js
import { trackInView } from '../../utils/analytics.js';
import { getImageURL } from '../../utils/image-helpers.js';

function extractConfig(block) {
  const config = { mainEl: block };
  const rows = block.querySelectorAll('div > div');

  // Extract images
  const mobileImg = rows[0]?.querySelector('img');
  const desktopImg = rows[1]?.querySelector('img');
  
  config.imageMobile = mobileImg?.src || '';
  config.imageDesktop = desktopImg?.src || config.imageMobile;
  config.alt = mobileImg?.alt || '';

  // Extract metadata
  config.caption = rows[2]?.textContent?.trim() || '';
  config.imageWidth = parseInt(rows[3]?.textContent?.trim() || '0', 10);

  // Extract appearance
  config.alignment = rows[4]?.textContent?.trim() || 'left';
  config.captionAlignment = rows[5]?.textContent?.trim() || 'left';

  // Extract overlay
  const overlayEl = rows[6]?.querySelector('p');
  config.overlay = overlayEl?.innerHTML || '';
  config.overlayPosition = rows[7]?.textContent?.trim() || 'top-left';

  // Extract analytics
  config.trackInView = rows[8]?.textContent?.trim() === 'true';
  config.trackInView_meta = rows[9]?.textContent?.trim() || '{}';

  return config;
}

function buildBlock(config) {
  // Prepare image URLs with fallback
  config.mobileImageURL = getImageURL(config.imageMobile, config.imageWidth);
  config.desktopImageURL = getImageURL(config.imageDesktop, config.imageWidth);

  // Generate srcset for responsive images
  config.srcset = `${config.mobileImageURL} 600w, ${config.desktopImageURL} 1200w`;

  config.classes = [];
  if (config.alignment !== 'left') {
    config.classes.push(`image-align-${config.alignment}`);
  }

  return config;
}

function appendEventListeners(config) {
  if (config.trackInView) {
    const metadata = JSON.parse(config.trackInView_meta);
    trackInView(config.mainEl, metadata);
  }
}

export default function decorate(block) {
  let config = extractConfig(block);
  config = buildBlock(config);
  appendEventListeners(config);

  // Build responsive image
  const picture = document.createElement('picture');
  
  // Mobile source
  if (config.imageMobile !== config.imageDesktop) {
    const mobileSource = document.createElement('source');
    mobileSource.media = '(max-width: 767px)';
    mobileSource.srcset = config.mobileImageURL;
    picture.appendChild(mobileSource);
  }

  // Desktop source
  const desktopSource = document.createElement('source');
  desktopSource.srcset = config.desktopImageURL;
  picture.appendChild(desktopSource);

  // Image element
  const img = document.createElement('img');
  img.src = config.desktopImageURL;
  img.alt = config.alt;
  if (config.imageWidth) {
    img.width = config.imageWidth;
  }
  picture.appendChild(img);

  // Replace content
  const pictureWrapper = document.createElement('div');
  pictureWrapper.className = 'image-wrapper';
  pictureWrapper.appendChild(picture);

  // Add overlay if present
  if (config.overlay) {
    const overlayDiv = document.createElement('div');
    overlayDiv.className = `image-overlay overlay-${config.overlayPosition}`;
    overlayDiv.innerHTML = config.overlay;
    pictureWrapper.appendChild(overlayDiv);
  }

  // Add caption if present
  if (config.caption) {
    const captionDiv = document.createElement('figure');
    captionDiv.className = 'image-caption';
    const figCaption = document.createElement('figcaption');
    figCaption.className = `caption-align-${config.captionAlignment}`;
    figCaption.textContent = config.caption;
    captionDiv.appendChild(figCaption);
    pictureWrapper.appendChild(captionDiv);
  }

  // Clear original content and add new
  block.innerHTML = '';
  block.appendChild(pictureWrapper);

  // Apply classes
  config.classes.forEach(cls => block.classList.add(cls));
  block.blockConfig = config;
}

export { extractConfig, buildBlock };
```

### **SCSS Styling**

```scss
// blocks/image/image.scss
@import '../../styles/tokens/semantic';
@import '../../styles/mixins/responsive';

.block-image {
  @include padding($spacing-md);

  .image-wrapper {
    position: relative;
    display: flex;
    flex-direction: column;
  }

  picture, img {
    display: block;
    width: 100%;
    height: auto;
  }

  // Overlay styles
  .image-overlay {
    position: absolute;
    padding: $spacing-md;
    background-color: rgba(0, 0, 0, 0.7);
    color: white;
    border-radius: $radius-medium;

    &.overlay-top-left {
      top: $spacing-md;
      left: $spacing-md;
    }

    &.overlay-top-right {
      top: $spacing-md;
      right: $spacing-md;
    }

    &.overlay-bottom-left {
      bottom: $spacing-md;
      left: $spacing-md;
    }

    &.overlay-bottom-right {
      bottom: $spacing-md;
      right: $spacing-md;
    }
  }

  // Caption styles
  .image-caption {
    margin: $spacing-md 0 0 0;
    padding: 0;

    figcaption {
      @include text-small;
      color: $text-secondary;

      &.caption-align-center {
        text-align: center;
      }

      &.caption-align-right {
        text-align: right;
      }
    }
  }

  // Alignment variants
  &.image-align-center {
    display: flex;
    justify-content: center;
  }

  &.image-align-right {
    display: flex;
    justify-content: flex-end;
  }

  @include respond-to('desktop') {
    @include padding($spacing-lg);
  }
}
```

---

## Testing Template

For each block, create tests:

```javascript
// blocks/text/__tests__/text.test.js
import { describe, test, expect } from 'vitest';
import decorate, { extractConfig, buildBlock } from '../text.js';

describe('Text Block', () => {
  test('extractConfig returns correct data', () => {
    const block = document.createElement('div');
    block.innerHTML = `
      <div><div><p>Test content</p></div></div>
      <div><div>primary</div></div>
      <div><div>large</div></div>
      <div><div>center</div></div>
      <div><div>true</div></div>
      <div><div>{"event": "test"}</div></div>
    `;

    const config = extractConfig(block);
    
    expect(config.color).toBe('primary');
    expect(config.size).toBe('large');
    expect(config.alignment).toBe('center');
    expect(config.trackInView).toBe(true);
  });

  test('buildBlock adds correct classes', () => {
    const config = {
      color: 'primary',
      size: 'large',
      alignment: 'center',
      classes: [],
    };

    const result = buildBlock(config);
    
    expect(result.classes).toContain('text-primary');
    expect(result.classes).toContain('text-large');
    expect(result.classes).toContain('text-align-center');
  });
});
```

## Summary

This document shows detailed specifications for:

1. **Text Block** - Basic content with styling
2. **Title Block** - Heading with flexible levels
3. **Image Block** - Advanced image handling

Use these as templates when implementing other blocks. Follow the same:

- Component model structure (General/Appearance/Analytics tabs)
- JavaScript pattern (extractConfig, buildBlock, appendEventListeners)
- SCSS organization with design tokens
- Testing approach

## References

- [03-BLOCK_JAVASCRIPT_PATTERN.md](03-BLOCK_JAVASCRIPT_PATTERN.md) - JS pattern details
- [02-JSON_CONFIGURATION.md](02-JSON_CONFIGURATION.md) - JSON structure
- [04-DESIGN_TOKENS_SYSTEM.md](04-DESIGN_TOKENS_SYSTEM.md) - Token usage
- [05-SCSS_STYLING_APPROACH.md](05-SCSS_STYLING_APPROACH.md) - SCSS patterns
- [07-FOUNDATIONAL_BLOCKS.md](07-FOUNDATIONAL_BLOCKS.md) - All block list
