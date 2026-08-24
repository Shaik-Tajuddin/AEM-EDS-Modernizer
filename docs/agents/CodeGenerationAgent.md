# CodeGenerationAgent

> Generates the EDS repo scaffold: `fstab.yaml`, `README.md`,
> `scripts.js`, `styles.css`, `package.json`.

- **Stage:** `BUILDING`
- **Phase:** 1
- **Agent name:** `code`
- **Task type:** `CODE`

## Inputs

- The project's repo configuration (GitHub URL, default
  branch, content root).
- The block catalogue (so the scaffold can reference the
  generated blocks).
- The Figma tokens (so the scaffold can import the design
  tokens).

## Outputs

- `GeneratedFileRecord`s for the scaffold files.

## AI usage

One AI call to customise the `README.md` (project name,
description, contact info). The rest of the scaffold is
template-based.

The mock provider returns a deterministic README.

## Failure modes

- **Repo URL malformed:** the agent records a `CRITICAL` issue
  and the migration is blocked.
- **Figma tokens missing:** the agent uses the default
  design tokens and records a `MEDIUM` issue.

## Performance

- 1 AI call + ~10 file writes.
- Total: < 1 second in mock mode.
