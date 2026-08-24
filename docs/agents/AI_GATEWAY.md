# AI Gateway

> The single entry point for every AI call. Per Master §60, **agents
> never instantiate a provider SDK directly**. They all go through
> `AiGateway`.

## Where it lives

`com.adobe.aem.modernizer.ai.AiGateway` in the `core` module.

## How it works

```
Agent
  │ chat(request)
  ▼
AiGateway
  │ 1. Look up the agent's routing in AiRoutingPolicy
  │ 2. Verify the target model has the required ModelCapability flags
  │ 3. Check the budget (maxCostMicros, maxRequests, maxTokens)
  │ 4. Call the provider adapter's chat(request)
  │ 5. On failure, retry with exponential backoff (up to maxRetries)
  │ 6. Record the result in AIDecision + BenchmarkService
  ▼
Provider Adapter (Anthropic | OpenAI | Gemini | Ollama | Mock)
```

## Public API

```java
public class AiGateway {
    public ChatResponse dispatch(ChatRequest request);
    public CapabilityRegistry capabilities();
    public void register(AiProvider provider);
    public void recordDecision(AIDecision decision);
    public RoutingPolicySnapshot routingSnapshot();
}
```

## Routing strategies

- `SINGLE_PROVIDER` — one provider for everything
- `MULTI_PROVIDER` — per-agent override (default)
- `COST_OPTIMIZED` — pick the cheapest provider that satisfies capabilities
- `QUALITY_OPTIMIZED` — pick the highest-capability provider

When `LOCAL_AI_ONLY=true` is set, the gateway refuses to dispatch
to any external provider and the migration fails closed rather
than silently falling back.

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

## Mock mode

When the modernizer is started with `MOCK_MODE=true` (the
default for the standalone runtime), the `MockAiProvider`
produces realistic structured responses for every `taskType`:

- `MAPPING` — returns a `ComponentMappingRecord` with a
  confidence between 0.5 and 0.99
- `PLANNING` — returns a `MigrationPlan` with 3-10 stages
- `BLOCK_GENERATION` — returns EDS block JS + CSS skeletons
- `CODE` — returns repo scaffold fragments
- `CONTENT_MIGRATION` — returns EDS section model `.md`
- `REPAIR` — returns a CSS / JS patch
- `FIGMA` — returns themes.css + figma-tokens.json
- `VISUAL_VALIDATION` — returns a score between 0.84 and 1.0
- `ACCESSIBILITY` — returns an axe-core-shaped JSON
- `ROLLOUT` — returns a 6-stage rollout plan

The mock is deterministic per (seed, taskType), so the e2e
produces a stable output every run.

## Related

- [ADR 0003](../adr/0003-ai-gateway-with-routing-policy.md) —
  the routing policy decision record.
- [ADR 0007](../adr/0007-capability-registry-gate.md) — the
  capability gate.
- [ADR 0008](../adr/0008-secrets-as-references-only.md) — the
  secret model that flows through the gateway.
