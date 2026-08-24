# ADR 0003 — AI Gateway with Routing Policy (Master §60)

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** AI integration architecture

## Context

The platform uses four AI provider families: Anthropic, OpenAI,
Google Gemini, and Ollama. Each has multiple models with different
capabilities (chat, structured output, code generation, vision,
tool use, long context) and different costs.

Agents should be able to:

- Use a different model than the platform default for their specific
  task (e.g. `qwen2.5-coder:32b` for code generation, `claude-sonnet-4-5`
  for planning).
- Fail safely if the configured model lacks the required capability
  (no silent fallback to a worse model).
- Be reconfigured without a code change (e.g. switch an agent to
  Ollama for privacy-sensitive data).

## Decision

All AI calls go through a single `AiGateway` class. The gateway
uses an `AiRoutingPolicy` data class to decide which provider and
model to dispatch to:

```java
public class AiRoutingPolicy {
    String strategy;             // SINGLE_PROVIDER, MULTI_PROVIDER, COST_OPTIMIZED, QUALITY_OPTIMIZED
    String defaultProvider;
    String defaultModel;
    Map<String, PerAgent> perAgent;   // override per agent
    long maxCostMicros;
    int  maxRequests;
    long maxTokens;
}
```

The gateway's dispatch algorithm:

1. Look up the agent's `perAgent` entry; fall back to
   `defaultProvider` / `defaultModel`.
2. Verify the target model has every `ModelCapability` flag the
   request needs.
3. Check the budget (`maxCostMicros`, `maxRequests`, `maxTokens`).
4. Call the provider adapter's `chat(request)`.
5. On failure, retry with exponential backoff up to
   `maxRetries`.
6. Record the result in `AIDecision` and `BenchmarkService`.

If any check fails, the call is refused; the agent receives a
`ChatResponse` with `success=false` and a clear error.

## Consequences

### Positive

- **One place to change AI behaviour.** Operators update the
  routing policy (in the dashboard's `#/ai-config` view or as
  JSON in a config file); no agent code change.
- **Capability gate is enforced at the gateway.** A misconfigured
  routing policy (e.g. assigning a non-vision model to a
  vision-required task) fails fast with a clear error.
- **Cost / token budgets are explicit.** A runaway agent cannot
  blow the cost limit silently.
- **The same gateway works in mock mode.** `MockAiProvider`
  produces realistic structured responses, so the full state
  machine can be exercised without API keys.

### Negative

- **Indirection.** New developers need to learn the gateway's
  routing algorithm. Mitigated by the `AiRoutingPolicy` JSON
  schema being self-documenting.
- **Capability flags are coarse.** A model with `CAP_VISION`
  set may still fail on a specific image; the gateway cannot
  detect that. Mitigated by the agent's own validation of the
  response.

## Alternatives considered

- **Direct provider instantiation in each agent** (the "easy"
  approach): rejected because it scatters provider config across
  the codebase and makes the capability gate impossible.
- **LangChain / LlamaIndex as the gateway**: rejected because the
  Master Prompt asks for a thin, AEM-shaped abstraction, not a
  third-party framework.

## Related

- [../ai/](../agents/AI_GATEWAY.md) — full agent docs.
- [ADR 0007](0007-capability-registry-gate.md) — the capability
  gate enforcement.
- [ADR 0008](0008-secrets-as-references-only.md) — how secrets
  flow through the gateway.
