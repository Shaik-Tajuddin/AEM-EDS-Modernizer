# BlockGenerationAgent

> Generates the EDS blocks (`blocks/{name}/{name}.js`,
> `blocks/{name}/{name}.css`) for every distinct target block
> in the mapping.

- **Stage:** `BUILDING`
- **Phase:** 1
- **Agent name:** `block-generation`
- **Task type:** `BLOCK_GENERATION`

## Inputs

- The `ComponentMappingRecord`s.
- The Figma component map (if `AdvancedFigmaIntelligenceAgent`
  ran).
- The EDS block catalogue.

## Outputs

- One `GeneratedFileRecord` per `.js` and one per `.css` per
  distinct block.
- `operation=CREATE`, `stage=BUILDING`, `path=blocks/{name}/{name}.{js,css}`.

## AI usage

One AI call per distinct block. The AI receives the
component-to-block mapping, the Figma component schema, and
the existing block patterns. It returns the JS (decoration
function) and CSS (block-scoped styles).

The mock provider returns a deterministic JS + CSS skeleton
per block.

## Failure modes

- **No EDS block for a component:** the agent creates a
  `generic-block` with placeholder content and records a
  `MEDIUM` issue.
- **AI returns invalid JS:** the agent retries; if still
  failing, writes a skeleton that logs a warning at runtime.

## Performance

- 3-10 AI calls per project (one per distinct block).
- Mock mode: < 100 ms.
- Real mode: 3-10 seconds per block.

## Related

- [CodeGenerationAgent](CodeGenerationAgent.md) — runs in the
  same stage, produces the repo scaffold.
