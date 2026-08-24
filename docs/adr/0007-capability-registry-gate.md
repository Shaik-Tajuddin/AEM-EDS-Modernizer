# ADR 0007 — Capability Registry Gate on AI Dispatch

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** AI integration

## Context

Different AI models support different capabilities: vision
(`CAP_VISION`), structured output (`CAP_STRUCTURED`), code
generation (`CAP_CODE`), tool use (`CAP_TOOL_USE`), long context
(`CAP_LONG_CONTEXT`), and local-only execution (`CAP_LOCAL`).

A common failure mode in AI-driven systems is a silent fallback:
the agent wants `CAP_VISION`, the configured model doesn't have
it, the gateway falls back to a text-only model, the agent
receives garbage, and the operator sees a confusing failure
later. Master §60 explicitly requires that the gateway *refuse*
to dispatch if the capability check fails.

## Decision

`AiGateway.dispatch(request)` runs a capability check before
calling the provider:

1. Read the `requiredCapabilities` from the `ChatRequest`.
2. Look up the target model in `CapabilityRegistry`.
3. If any required capability is missing, return a
   `ChatResponse` with `success=false` and a clear error
   message: `"model {provider}/{model} lacks capability {cap};
   required by agent {agent} for task {taskType}"`.
4. If all capabilities are present, dispatch.

The `CapabilityRegistry` is pre-seeded with 9 models:

| Model | Caps |
|---|---|
| `claude-sonnet-4-5` | chat, structured, code, vision, tool_use, long_context |
| `claude-opus-4-1` | chat, structured, code, vision, tool_use, long_context |
| `gpt-4o` | chat, structured, code, vision, tool_use |
| `gpt-4o-mini` | chat, structured, code |
| `gemini-1.5-pro` | chat, structured, code, vision, long_context |
| `llama3.2` | chat, structured, local |
| `codellama` | chat, code, local |
| `qwen2.5-coder:32b` | chat, code, local |
| `llava` | chat, vision, local |
| `mock-general-1` | chat, structured, code, vision, local (added at startup in mock mode) |

## Consequences

### Positive

- **Fail fast, fail loud.** A misconfigured routing policy is
  caught at the gateway, not buried in an agent's
  incomprehensible response.
- **The dashboard surfaces the failure.** The
  `GET /api/projects/{id}/ai-usage` endpoint shows the failed
  dispatch with the reason.
- **No silent fallback.** The Master Prompt is explicit; we
  honour it.

### Negative

- **Operators must understand capability flags.** A new model
  added to the registry must declare its capabilities correctly
  or it will be unusable for any non-trivial task.
- **Capability drift.** A model that loses a capability in a
  new version (e.g. OpenAI deprecating vision in a model) will
  start failing on tasks that previously worked. Mitigated by
  the model version being explicit in the routing policy.

## Alternatives considered

- **Soft fallback** (silently try a different model): rejected
  per Master §60.
- **No capability check** (let the provider fail): rejected
  because the provider's failure is too late and too cryptic.

## Related

- [ADR 0003](0003-ai-gateway-with-routing-policy.md) — the
  gateway that runs the check.
- [ADR 0008](0008-secrets-as-references-only.md) — secret model
  that works alongside the capability gate.
