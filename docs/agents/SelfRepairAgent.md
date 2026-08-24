# SelfRepairAgent

> Phase 1 basic self-repair. For every failed validation, the
> agent asks the AI to propose a fix, applies the fix, and
> re-validates. Capped at 5 attempts.

- **Stage:** `REPAIRING`
- **Phase:** 1
- **Agent name:** `self-repair`
- **Task type:** `REPAIR`

## Inputs

- The failed `ValidationResultRecord`s for the current job.

## Outputs

- Updated `ValidationResultRecord`s (score bumped if repair
  succeeded).
- Updated `GeneratedFileRecord`s (the patched files).

## AI usage

One AI call per failed validation per attempt. The AI
receives the validation result, the relevant generated file,
and a request for a fix. Returns a `patch` object with
`{file, before, after}`.

The mock provider returns a deterministic CSS comment
patch.

## Failure modes

- **AI returns invalid patch:** the agent retries up to 5
  times; if still failing, the issue is left open.
- **Re-validation still fails:** the issue is left open with
  `repairAttempts=5`.

## Related

- [AdvancedRepairAgent](AdvancedRepairAgent.md) — the
  Phase 2 version with failure-mode classification and
  per-attempt evidence.
