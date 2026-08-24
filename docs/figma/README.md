# Figma Integration

The AEM → EDS Modernizer reads Figma files to extract
design tokens (colors, typography, spacing) and — in Phase 2
— to pair Figma components with EDS blocks.

## Documents in this section

- [API_USAGE.md](API_USAGE.md) — the Figma REST API v1
  endpoints the modernizer calls.
- [TOKEN_EXTRACTION.md](TOKEN_EXTRACTION.md) — how the
  `FigmaAnalysisAgent` extracts design tokens.
- [COMPONENT_PAIRING.md](COMPONENT_PAIRING.md) — how the
  Phase 2 `AdvancedFigmaIntelligenceAgent` pairs Figma
  components with EDS blocks.
- [AUTH.md](AUTH.md) — the Figma PAT auth flow.

## How Figma fits in the architecture

The modernizer's Figma integration is read-only: it never
modifies the Figma file. The two agents
(`FigmaAnalysisAgent` and `AdvancedFigmaIntelligenceAgent`)
run in the `DESIGN_ANALYSIS` stage, after the `ANALYZING`
stage has finished. Their output (design tokens, component
map) is consumed by the `BlockGenerationAgent` and the
`CodeGenerationAgent` in the `BUILDING` stage.

```
ANALYZING → DESIGN_ANALYSIS → BUILDING → MIGRATING
                │                  ↑
                └─ tokens + map ──┘
```

## See also

- [../agents/FigmaAnalysisAgent.md](../agents/FigmaAnalysisAgent.md) —
  the Phase 1 basic agent.
- [../agents/AdvancedFigmaIntelligenceAgent.md](../agents/AdvancedFigmaIntelligenceAgent.md) —
  the Phase 2 advanced agent.
- [../security/SECRETS.md](../security/SECRETS.md) — the
  Figma PAT secret model.
