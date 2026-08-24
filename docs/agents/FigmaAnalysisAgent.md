# FigmaAnalysisAgent

> Reads a Figma file and extracts the design tokens (colors,
> typography, spacing) that the EDS code consumes.

- **Stage:** `DESIGN_ANALYSIS`
- **Phase:** 1
- **Agent name:** `figma-analysis`
- **Task type:** `FIGMA`

## Inputs

- The project's `figmaUrl` (e.g.
  `https://www.figma.com/design/{key}/{name}`).

## Outputs

- `GeneratedFileRecord`s: `styles/tokens.css`,
  `scripts/figma-tokens.json`.

## AI usage

The agent calls `AiGateway.dispatch(...)` with `taskType=FIGMA`
and the Figma file's node tree. The AI returns a structured
tokens object (colors, font families, font sizes, spacing
scale).

## Failure modes

- **Figma file not accessible:** the agent records a `CRITICAL`
  issue. The migration can continue without design tokens but
  the dashboard shows a warning.
- **Figma file has no published styles:** the agent records a
  `MEDIUM` issue and uses the raw node tree as fallback.

## Related

- [AdvancedFigmaIntelligenceAgent](AdvancedFigmaIntelligenceAgent.md) —
  the Phase 2 advanced version that adds component-block
  pairing.
