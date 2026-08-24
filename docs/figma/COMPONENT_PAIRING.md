# Component Pairing (Phase 2)

How the `AdvancedFigmaIntelligenceAgent` pairs Figma
components with EDS blocks.

## The pairing problem

A Figma file may have 50+ components. The EDS site may have
10+ blocks. We need to answer: "which Figma component maps
to which EDS block?"

The answer is not 1:1. A Figma `Hero` component may map to
the EDS `hero` block, but a Figma `Card with image + title
+ body + CTA` may map to the EDS `cards` block (one card per
item).

## The algorithm

1. Fetch the Figma components
   (`GET /v1/files/{key}/components`).
2. For each component, extract:
   - The component name (e.g. `Hero / Default`).
   - The property schema (e.g. `heading: string`, `image:
     image`).
   - The visual signature (rendered as a 64×64 PNG).
3. Ask the AI to pair the component with the best-matching
   EDS block, given:
   - The component's name, property schema, and visual
     signature.
   - The EDS block catalogue (from the local block
     registry).
   - The component-to-block mapping rules (heuristics).
4. Record the pairing with a confidence score.

## Confidence threshold

- `confidence >= 0.8` — the pairing is used; the
  `BlockGenerationAgent` references the pairing.
- `confidence < 0.8` — the pairing is recorded but flagged
  for manual review; the `BlockGenerationAgent` uses the
  generic block.

## Output

`scripts/figma-component-map.json`:

```json
{
  "version": "1.0",
  "generatedAt": "2026-08-24T08:00:00.000Z",
  "components": [
    {
      "figmaId": "1:23",
      "figmaName": "Hero / Default",
      "edsBlock": "hero",
      "confidence": 0.95,
      "propertyMapping": {
        "heading": "heading",
        "subheading": "subheading",
        "image": "image",
        "ctaText": "ctaText",
        "ctaUrl": "ctaUrl"
      }
    },
    {
      "figmaId": "1:42",
      "figmaName": "Card with image + title + body + CTA",
      "edsBlock": "cards",
      "confidence": 0.88,
      "propertyMapping": {
        "image": "items[].image",
        "title": "items[].title",
        "body": "items[].body",
        "ctaText": "items[].ctaText"
      }
    }
  ]
}
```

## How the BlockGenerationAgent uses the map

When generating a block, the `BlockGenerationAgent` reads
`figma-component-map.json` and looks for a matching
component. If found, the AI's block generation prompt
includes the property mapping, so the generated block
honours the Figma component's property schema.

## Failure modes

- **AI returns no good pairing** (all confidences < 0.5):
  the agent uses the generic block and records a `HIGH`
  issue.
- **Figma file has no published components:** the agent
  records a `MEDIUM` issue and falls back to the basic
  `FigmaAnalysisAgent`.

## Related

- [TOKEN_EXTRACTION.md](TOKEN_EXTRACTION.md) — the token
  extraction (Phase 1).
- [../agents/AdvancedFigmaIntelligenceAgent.md](../agents/AdvancedFigmaIntelligenceAgent.md) —
  the agent.
