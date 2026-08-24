# AdvancedFigmaIntelligenceAgent

> Phase 2 advanced Figma intelligence. Goes beyond token
> extraction to **pair Figma components with EDS blocks**,
> produce `themes.css`, and write a `figma-component-map.json`
> that the BlockGenerationAgent uses.

- **Stage:** `DESIGN_ANALYSIS`
- **Phase:** 2
- **Agent name:** `figma-intelligence`
- **Task type:** `FIGMA_INTELLIGENCE`

## Inputs

- The Figma file (via `FigmaClient`).

## Outputs

- `GeneratedFileRecord`s:
  - `styles/themes.css` — component-class theme overrides
  - `styles/figma-tokens.json` — design tokens (extends
    `tokens.css`)
  - `scripts/figma-component-map.json` — Figma component →
    EDS block mapping

## AI usage

Two AI calls per Figma file:

1. **Component extraction** — for each component, returns
   the Figma node type, the property schema, and the visual
   signature.
2. **Component-to-block pairing** — for each extracted
   component, returns the best-matching EDS block from the
   block catalogue.

The mock provider returns a deterministic pair (component
name → block name) per Figma file.

## Failure modes

- **Figma file has no components:** the agent falls back to
  the basic `FigmaAnalysisAgent` and produces only tokens.
- **AI returns no good pairing** (low confidence): the
  component is mapped to `generic-block` with
  `confidence=0.5`; the BlockGenerationAgent treats it as
  custom.

## Performance

- 2 AI calls per Figma file.
- Mock mode: < 100 ms.
- Real mode: 5-15 seconds per file.

## Related

- [FigmaAnalysisAgent](FigmaAnalysisAgent.md) — the Phase 1
  basic version.
- [ADR 0001](../adr/0001-phase2-advanced-features.md) — the
  Phase 2 decision record.
