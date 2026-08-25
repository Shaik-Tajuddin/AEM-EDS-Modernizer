# Analytics Pattern - Data Layer Tracking Implementation

## Overview

AEM EDS blocks use a **data-attribute-driven analytics pattern** powered by `dataLayer.js`. Authors configure tracking through the block's JSON model, and the system uses data attributes plus a centralized `applyTracking()` utility to wire up IntersectionObserver and click listeners.

> **Production Reference**: See `utilities/dataLayer.js` for the complete implementation and `text_callout_block/text-callout.js` for usage.

Two tracking types are supported:

1. **trackInview** — Track when a block enters the viewport
2. **trackClick** — Track click events on interactive elements

---

## Architecture

```
Author configures → JSON model (boolean + text meta)
                  ↓
extractConfig   → Reads boolean/meta from row positions
                  ↓
buildBlock      → Sets data-trackinview / data-trackclick attributes
                  ↓
applyTracking() → Scans attributes, wires IntersectionObserver & click listeners
                  ↓
pushToDataLayer → Pushes events to window.dataLayer.events[]
```

**Key principle**: Blocks do NOT directly create observers or attach analytics listeners. They set data attributes during build, and `applyTracking()` handles the rest.

---

## JSON Configuration (Author-Facing)

### Model Fields

Every block includes these fields in the Analytics tab:

```json
{
  "component": "tab",
  "label": "Analytics",
  "name": "tabAnalytics"
},
{
  "component": "boolean",
  "name": "trackInview",
  "label": "Track inview",
  "description": "Enable tracking when this block comes into view."
},
{
  "component": "text",
  "name": "trackInview_meta",
  "label": "Inview analytics meta",
  "description": "Metadata to send with inview tracking events",
  "condition": { "==": [{ "var": "trackInview" }, true] }
},
{
  "component": "boolean",
  "name": "trackClick",
  "label": "Track click",
  "description": "Enable tracking for the CTA link"
},
{
  "component": "text",
  "name": "trackClick_meta",
  "label": "Click analytics meta",
  "description": "Metadata to send with click tracking events",
  "condition": { "==": [{ "var": "trackClick" }, true] }
}
```

### Key Design Decisions

| Decision | Detail |
|---|---|
| **`boolean` component** | Not `checkbox` — `boolean` is the correct production component |
| **`text` for meta** | Not `textarea` — meta is typically a JSON string, single-line is sufficient |
| **`condition` property** | Not `visible` — uses JSON Logic `{"==": [{"var": "trackInview"}, true]}` |
| **Separate meta fields** | Each tracking type has its own meta field with `_meta` suffix |

### Template Defaults

In the definition template, set boolean defaults:

```json
"template": {
  "trackInview": false,
  "trackClick": false
  // meta fields omitted — start empty
}
```

---

## dataLayer.js — Complete API Reference

### `pushToDataLayer(event, params)`

Default export. Pushes an event to `window.dataLayer.events[]`:

```javascript
import pushToDataLayer from '../../scripts/dataLayer.js';

pushToDataLayer('click', { id: 'my-block', meta: { event: 'cta_clicked' } });
// Result: window.dataLayer.events contains:
// { event: 'click', id: 'my-block', meta: {...}, timestamp: '2026-04-09T...' }
```

### `applyTracking(block)`

**Primary integration point for blocks.** Scans the block (and its descendants) for `data-trackinview` and `data-trackclick` attributes and wires up appropriate listeners.

```javascript
import { applyTracking } from '../../scripts/dataLayer.js';

export default function decorate(block) {
  const config = extractConfig(block);
  buildBlock(block, config);
  applyTracking(block);  // Always call last
}
```

**How it works**:
1. Finds all elements with `data-trackinview="true"` inside block (and block itself)
2. Reads meta from `data-trackinviewmeta` attribute, parses as JSON
3. Creates IntersectionObserver for each (fires once, then unobserves)
4. Finds all elements with `data-trackclick="true"` inside block (and block itself)
5. Reads meta from `data-trackclickmeta` attribute, parses as JSON
6. Attaches click listener to each

### `getMetaFromElement(element, metaJsonAttr)`

Reads and parses JSON from an element's data attribute:

```javascript
const meta = getMetaFromElement(element, 'trackinviewmeta');
// Reads element.dataset.trackinviewmeta, parses JSON, returns object or null
```

### `appendInViewTracker(element, meta)`

Creates an IntersectionObserver that pushes an `'inview'` event when element is 10% visible:

```javascript
appendInViewTracker(myElement, { event: 'section_viewed' });
```

### `appendClickTracker(element, meta)`

Attaches a click listener that pushes a `'click'` event:

```javascript
appendClickTracker(myButton, { event: 'cta_clicked' });
```

### `extractTrackingConfig(cell)`

Legacy helper for paragraph-based tracking cells. Returns `{ enabled, metadata }` or `null`.

---

## Block Integration Pattern

### Step 1: Extract Analytics Config

```javascript
function extractConfig(block) {
  const rows = [...block.children];
  return {
    // ... content fields ...
    trackInView: getBooleanFromRow(rows[5]),
    trackInViewMeta: getTextFromRow(rows[6]),
    trackClick: getBooleanFromRow(rows[7]),
    trackClickMeta: getTextFromRow(rows[8]),
  };
}
```

### Step 2: Set Data Attributes During Build

```javascript
function buildBlock(block, config) {
  // ... build DOM ...

  // Inview tracking on the block itself
  if (config.trackInView && config.trackInViewMeta) {
    block.setAttribute('data-trackinview', 'true');
    block.setAttribute('data-trackinviewmeta', config.trackInViewMeta);
  }

  // Click tracking on the CTA element
  if (config.trackClick && config.trackClickMeta) {
    ctaEl.setAttribute('data-trackclick', 'true');
    ctaEl.setAttribute('data-trackclickmeta', config.trackClickMeta);
  }
}
```

**Placement guidelines**:
- `data-trackinview` → typically on the **block element** itself
- `data-trackclick` → typically on the **clickable element** (CTA, button, link)

### Step 3: Call applyTracking

```javascript
export default function decorate(block) {
  const config = extractConfig(block);
  buildBlock(block, config);
  appendEvents(config);
  applyTracking(block);  // Always last — scans built DOM for data attributes
}
```

---

## Data Layer Structure

```javascript
window.dataLayer = {
  events: [
    {
      event: 'inview',
      id: 'hero-block',
      meta: { event: 'hero_viewed', section: 'top' },
      timestamp: '2026-04-09T10:30:00.000Z'
    },
    {
      event: 'click',
      id: 'hero-cta',
      meta: { event: 'cta_clicked', variant: 'primary' },
      timestamp: '2026-04-09T10:30:15.000Z'
    }
  ]
};
```

Events are pushed via `pushToDataLayer()` and accumulate in `window.dataLayer.events[]`.

---

## Meta Format

The meta field value should be a **valid JSON string** that the author enters:

```
{"event": "cta_clicked", "variant": "primary", "section": "hero"}
```

`getMetaFromElement()` parses this JSON. If parsing fails, it logs a warning and returns `null` (tracking is silently skipped).

---

## Best Practices

### ✓ Do
- Use `boolean` component for tracking toggles
- Use `text` component for tracking meta
- Set data attributes during `buildBlock`
- Call `applyTracking(block)` at end of `decorate`
- Place `data-trackinview` on the block element
- Place `data-trackclick` on the clickable element
- Provide clear descriptions for meta fields
- Use `condition` for conditional visibility

### ✗ Don't
- Use `checkbox` — use `boolean`
- Use mitt/emitter for analytics events
- Manually create IntersectionObserver in blocks
- Directly call `pushToDataLayer` from blocks
- Use `visible` — use `condition` with JSON Logic
- Assume meta JSON is always valid
- Forget to call `applyTracking()` after building DOM

---

## Summary

| Tracking | Data Attribute | Meta Attribute | Observer Type |
|---|---|---|---|
| Inview | `data-trackinview="true"` | `data-trackinviewmeta` | IntersectionObserver (10% threshold) |
| Click | `data-trackclick="true"` | `data-trackclickmeta` | Click event listener (capture: true) |

## References

- [02-JSON_CONFIGURATION.md](02-JSON_CONFIGURATION.md) - JSON model fields
- [03-BLOCK_JAVASCRIPT_PATTERN.md](03-BLOCK_JAVASCRIPT_PATTERN.md) - JS integration
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](16-BLOCK_DEVELOPMENT_TEMPLATE.md) - Step-by-step guide
- `utilities/dataLayer.js` - Full source code
- `text_callout_block/text-callout.js` - Production usage
