# Capability Gate

The AI capability registry gate. Per Master §60, the
`AiGateway` refuses to dispatch a task to a model that
lacks the required capabilities.

## Capability flags

| Flag | What it means |
|---|---|
| `CAP_CHAT` | basic text chat |
| `CAP_STRUCTURED` | structured JSON output (with a `jsonSchema`) |
| `CAP_CODE` | code generation / understanding |
| `CAP_VISION` | image inputs |
| `CAP_TOOL_USE` | tool / function calling |
| `CAP_LONG_CONTEXT` | context > 100k tokens |
| `CAP_LOCAL` | runs locally (no external API) |

## Pre-seeded models

| Model | Provider | Capabilities |
|---|---|---|
| `claude-sonnet-4-5` | Anthropic | chat, structured, code, vision, tool_use, long_context |
| `claude-opus-4-1` | Anthropic | chat, structured, code, vision, tool_use, long_context |
| `gpt-4o` | OpenAI | chat, structured, code, vision, tool_use |
| `gpt-4o-mini` | OpenAI | chat, structured, code |
| `gemini-1.5-pro` | Google | chat, structured, code, vision, long_context |
| `llama3.2` | Ollama | chat, structured, local |
| `codellama` | Ollama | chat, code, local |
| `qwen2.5-coder:32b` | Ollama | chat, code, local |
| `llava` | Ollama | chat, vision, local |
| `mock-general-1` | Mock | chat, structured, code, vision, local (added at startup in mock mode) |

## How the gate works

`AiGateway.dispatch(request)` runs:

1. Look up the agent's routing in `AiRoutingPolicy`.
2. Read the `requiredCapabilities` from the `ChatRequest`.
3. Look up the target model in `CapabilityRegistry`.
4. If any required capability is missing, return a
   `ChatResponse` with `success=false` and a clear error
   message.
5. If all capabilities are present, dispatch.

## Why we need it

Without the gate, a misconfigured routing policy (e.g.
assigning `gpt-4o-mini` to a vision-required task) would
silently fail: the agent receives garbage and the operator
sees a confusing failure later. The gate fails fast and
loud.

## How to add a new model

To add a new model (e.g. a new Anthropic model):

1. Add the model to `CapabilityRegistry` at startup:
   ```java
   capabilities().add(new ModelCapability("anthropic", "claude-new-1", 200_000)
       .add(ModelCapability.CAP_CHAT)
       .add(ModelCapability.CAP_STRUCTURED)
       .add(ModelCapability.CAP_VISION));
   ```
2. Add the model to the `AiRoutingPolicy` (or to a
   per-agent override).
3. Add a test case to `CapabilityRegistryTest`.

The model is then usable by any agent that requests a
capability the model has.

## See also

- [../adr/0007-capability-registry-gate.md](../adr/0007-capability-registry-gate.md) —
  the decision record.
- [../agents/AI_GATEWAY.md](../agents/AI_GATEWAY.md) — the
  full gateway documentation.
