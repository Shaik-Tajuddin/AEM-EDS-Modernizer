# AEM Element Grouping Pattern Analysis

## Core Concept

**Element grouping** in AEM is a technique for concatenating multiple semantic elements into a single cell in the block's table representation. This is achieved using **underscore naming conventions** to logically group related fields together.

## The Naming Pattern

The pattern uses an underscore (`_`) as a delimiter to group fields:

```
[groupName]_[fieldName]
```

### How It Works

1. **Group Prefix**: All fields sharing the same prefix (before the underscore) are grouped together
2. **Field Suffix**: The part after the underscore identifies the specific field within that group
3. **Single Cell Rendering**: All fields with the same prefix are rendered into a single cell in the table, even though they're defined as separate properties

### Example from Documentation

In the teaser component example:
- `teaserText_subtitle`
- `teaserText_title`
- `teaserText_titleType`
- `teaserText_description`
- `teaserText_cta1`
- `teaserText_cta1Text`
- `teaserText_cta2`
- `teaserText_cta2Text`
- `teaserText_cta2Type`

All fields prefixed with `teaserText_` are grouped together and rendered as a **single cell** in the table, containing:
- Subtitle (paragraph)
- Title (h2)
- Description (paragraph)
- Multiple CTAs (links)

## Application to Analytics Configuration Fields

Based on the element grouping pattern, your analytics configuration fields should follow this structure:

### Correct Naming Pattern

```
analytics_inview
analytics_inview_data
analytics_singleClick
analytics_singleClick_data
analytics_multiClick
analytics_multiClick_items
```

Or alternatively (if grouping by event type):

```
inview_enabled
inview_data
singleClick_enabled
singleClick_data
multiClick_enabled
multiClick_items
```

### How It Applies

1. **Grouped by Event Type**: Fields related to the same event (e.g., `inview`) share the same prefix
2. **Single Cell Rendering**: All `inview_*` fields would render in one cell, all `singleClick_*` fields in another, etc.
3. **Semantic Relationship**: The underscore clearly indicates that `inview_data` is the data payload for the `inview` event

### Recommended Structure

For your analytics configuration, use this pattern:

```json
{
  "inview": true,
  "inview_data": { /* tracking data */ },
  "singleClick": true,
  "singleClick_data": { /* tracking data */ },
  "multiClick": true,
  "multiClick_items": [ /* array of items */ ]
}
```

**In component-models.json**, define them with the underscore prefix:

```json
{
  "id": "analytics",
  "fields": [
    {
      "component": "checkbox",
      "name": "inview",
      "label": "Track Inview"
    },
    {
      "component": "text-area",
      "name": "inview_data",
      "label": "Inview Data"
    },
    {
      "component": "checkbox",
      "name": "singleClick",
      "label": "Track Single Click"
    },
    {
      "component": "text-area",
      "name": "singleClick_data",
      "label": "Single Click Data"
    },
    {
      "component": "checkbox",
      "name": "multiClick",
      "label": "Track Multi Click"
    },
    {
      "component": "text-area",
      "name": "multiClick_items",
      "label": "Multi Click Items"
    }
  ]
}
```

## Key Takeaway

The underscore naming convention (`prefix_fieldName`) is the mechanism AEM uses to:
- **Logically group** related fields in the content model
- **Render them together** in a single table cell
- **Maintain semantic relationships** between related properties
- **Keep the content structure clean** while supporting complex data hierarchies

This pattern is essential for creating intuitive authoring experiences where related fields are visually and logically grouped together.
