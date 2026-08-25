# Text Callout Block — Production Reference

> **This is the canonical production reference implementation.** All new blocks should follow this pattern.

## File Structure

```
text-callout/
├── text-callout.js              # Block JavaScript
├── text-callout.css             # Block CSS (basic CSS, no variables)
├── _text-callout.json           # Block JSON (underscore prefix required)
├── text-callout-example.html    # AEM-generated HTML example
└── README.md                    # This file
```

### File Naming Conventions
- **JSON files**: Must have underscore prefix → `_text-callout.json`
- **All files**: Must match the folder name → `text-callout.*`
- **CSS files**: Basic `.css` only (no SCSS); use **`var(--color-*)`**, **`var(--space-*)`**, and type tokens from **`styles/`** per **`30-block-css-design-tokens.mdc`**
- **HTML example**: Must be included — shows assumed AEM-generated markup

## Key Patterns (JSON)

| Pattern | Implementation |
|---------|---------------|
| Boolean toggles | `"component": "boolean"` (NOT checkbox) |
| Conditional visibility | `"condition"` with JSON Logic (NOT visible) |
| Tab organization | `tab` components: tabGeneral, tabAppearance |
| Style variants | `classes` and `classes_*` fields (auto-applied by AEM) |
| Select fields | Must include `"valueType": "string"` |
| Template defaults | Only include fields with meaningful defaults |
| Filters | `"components": []` (empty array) |

## Key Patterns (JavaScript)

| Pattern | Implementation |
|---------|---------------|
| Imports | `block-helpers.js` for extraction |
| Row access | `[...block.children]` (NOT querySelectorAll) |
| Extraction | Position-based with helper functions |
| CTA logic | Separate `buildCtaElement()` — reuse AEM anchor or create button |
| Class naming | Hyphenated: `text-callout-inner`, `text-callout-title` (NOT BEM) |
| Clear markup | `block.textContent = ''` |
| Decorate | Synchronous function (NOT async) |
| JS flow | `extractConfig() → buildTextCallout() → appendEvents()` |

## Key Patterns (CSS)

| Pattern | Implementation |
|---------|---------------|
| Approach | Basic CSS with direct values (no variables, no tokens) |
| Class names | Hyphenated: `.text-callout-title` |
| Variants | Target via block class: `.text-callout.callout-primary .brand-cta` |
| Responsive | Standard `@media (min-width: ...)` queries |
| Mobile-first | Base styles for mobile, progressive enhancement |

## References

- [02-JSON_CONFIGURATION.md](../AEM_EDS_Documentation/02-JSON_CONFIGURATION.md)
- [03-BLOCK_JAVASCRIPT_PATTERN.md](../AEM_EDS_Documentation/03-BLOCK_JAVASCRIPT_PATTERN.md)
- [05-CSS_STYLING_APPROACH.md](../AEM_EDS_Documentation/05-CSS_STYLING_APPROACH.md)
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](../AEM_EDS_Documentation/16-BLOCK_DEVELOPMENT_TEMPLATE.md)
- [BLOCK_CREATION_STANDARDS.md](../BLOCK_CREATION_STANDARDS.md) — required deliverables for every block
