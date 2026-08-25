# Hero Block

> Reference block implementation for hero sections.

## File Structure

```
hero/
├── hero.js                 # Block JavaScript
├── hero.css                # Block CSS (basic CSS, no variables)
├── _hero.json              # Block JSON (underscore prefix required)
├── demo.html               # Standalone demo
└── hero-image.png          # Demo image
```

### File Naming Conventions
- **JSON files**: Must have underscore prefix → `_hero.json`
- **All files**: Must match the folder name → `hero.*`
- **CSS files**: Basic `.css` only (no SCSS); use **`var(--color-*)`**, **`var(--space-*)`**, and type tokens from **`styles/`** per **`30-block-css-design-tokens.mdc`**

## Style Variants

Applied via `classes` field in JSON (auto-applied by AEM to outer block div):
- Default (light background)
- `dark` — Dark theme
- `light` — Light theme with blue CTA
- `gradient` — Gradient background

## Features

- Eyebrow text with uppercase styling
- Main headline with serif typography
- Description body text
- CTA button with optional location input
- Responsive images (separate mobile/desktop)
- Disclaimer text
- Analytics tracking (inView + click)

## Architecture

- **JavaScript**: Follows `extractConfig` → `buildHero` → `appendEvents` → `applyTracking` pattern
- **CSS**: Basic CSS consuming **global** design tokens from **`styles/`** (colors, spacing, typography)
- **JSON**: Uses `definitions`, `models`, `filters` structure
- **Analytics**: Data-attribute-driven via `dataLayer.js`
- **Class naming**: Hyphenated (e.g., `hero-content`, `hero-headline`, NOT BEM)

> **Note**: The hero.json file uses an older `groups` structure that should be migrated to the `definitions/models/filters` flat structure per the text-callout production pattern.

## References

- [text_callout_block/](../text_callout_block/) — Production reference
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](../AEM_EDS_Documentation/16-BLOCK_DEVELOPMENT_TEMPLATE.md)
