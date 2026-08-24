# Token Extraction

How the `FigmaAnalysisAgent` (Phase 1) extracts design
tokens from a Figma file.

## What gets extracted

| Token type | Source | Output |
|---|---|---|
| Colors | Figma color styles | `styles/tokens.css` (`--color-*`) |
| Typography | Figma text styles | `styles/tokens.css` (`--font-*`, `--type-*`) |
| Spacing | Figma spacing tokens | `styles/tokens.css` (`--space-*`) |
| Border radius | Figma corner radius | `styles/tokens.css` (`--radius-*`) |
| Shadow | Figma effect styles | `styles/tokens.css` (`--shadow-*`) |

## The output

`styles/tokens.css`:

```css
:root {
  --color-primary: #1473e6;
  --color-secondary: #2680eb;
  --color-accent: #f15a29;
  --color-background: #ffffff;
  --color-text: #2c2c2c;

  --font-family-sans: "Adobe Clean", sans-serif;
  --font-family-serif: "Adobe Clean Serif", serif;
  --font-size-100: 0.875rem;
  --font-size-200: 1rem;
  --font-size-300: 1.25rem;
  --font-size-400: 1.5rem;
  --font-size-500: 2rem;

  --space-100: 0.5rem;
  --space-200: 1rem;
  --space-300: 1.5rem;
  --space-400: 2rem;
  --space-500: 3rem;

  --radius-100: 4px;
  --radius-200: 8px;

  --shadow-100: 0 1px 2px rgba(0, 0, 0, 0.1);
  --shadow-200: 0 4px 8px rgba(0, 0, 0, 0.1);
}
```

## How the agent extracts the tokens

1. Call `GET /v1/files/{key}/styles` to get the published
   style IDs.
2. For each style, call `GET /v1/files/{key}/nodes?ids={ids}`
   to get the style definition (color, typography, etc.).
3. Convert the Figma-style values to CSS custom properties.
4. Write `tokens.css` as a `GeneratedFileRecord`.

## Phase 2: themes.css

The Phase 2 `AdvancedFigmaIntelligenceAgent` adds a
`themes.css` file that contains **component-class theme
overrides**. For example:

```css
.hero {
  --hero-bg: var(--color-primary);
  --hero-text: var(--color-background);
}
.cards {
  --cards-gap: var(--space-300);
}
```

The `BlockGenerationAgent` reads `themes.css` and merges it
into each block's CSS.

## AI usage

The token extraction is deterministic; no AI call. The
mapping from Figma style to CSS custom property is rule-based.

The Phase 2 component pairing uses AI. See
[COMPONENT_PAIRING.md](COMPONENT_PAIRING.md).

## Related

- [COMPONENT_PAIRING.md](COMPONENT_PAIRING.md) — the
  Phase 2 component pairing.
- [../agents/FigmaAnalysisAgent.md](../agents/FigmaAnalysisAgent.md) —
  the Phase 1 agent.
