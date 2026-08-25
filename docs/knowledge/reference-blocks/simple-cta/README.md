# Simple CTA Block

> Learning implementation aligned with production patterns from `text-callout` block.

## File Structure

```
simple-cta/
├── simple-cta.js              # Block JavaScript
├── simple-cta.css             # Block CSS (basic CSS, no variables)
├── _simple-cta.json           # Block JSON (underscore prefix required)
├── simple-cta-example.html    # AEM-generated HTML example
├── demo.html                  # Standalone demo
└── simple-cta-with-helpers.js  # Refactored example with helpers
```

### File Naming Conventions
- **JSON files**: Must have underscore prefix → `_simple-cta.json`
- **All files**: Must match the folder name → `simple-cta.*`
- **CSS files**: Basic `.css` only (no SCSS); use **`var(--color-*)`**, **`var(--space-*)`**, and type tokens from **`styles/`** per **`30-block-css-design-tokens.mdc`**
- **HTML example**: Must be included — shows assumed AEM-generated markup

## Style Variants

Applied via `classes` field in JSON (auto-applied by AEM to outer block div):
- `cta-primary-filled` (default)
- `cta-primary-outlined`
- `cta-secondary-filled`
- `cta-secondary-outlined`

## Alignment Classes

Applied via `classes_simpleCTAAlign` field in JSON:
- `simple-cta-align-center`
- `simple-cta-align-right`

## Architecture

- **JavaScript**: Follows `extractConfig() → buildSimpleCta() → appendEvents()` pattern
- **CSS**: Basic CSS consuming **global** design tokens from **`styles/`** (colors, spacing, typography)
- **JSON**: Uses `definitions`, `models`, `filters` structure with tab organisation

## References

- [text_callout_block/](../text_callout_block/) — Production reference
- [BLOCK_CREATION_STANDARDS.md](../BLOCK_CREATION_STANDARDS.md) — Required deliverables for every block
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](../AEM_EDS_Documentation/16-BLOCK_DEVELOPMENT_TEMPLATE.md)
