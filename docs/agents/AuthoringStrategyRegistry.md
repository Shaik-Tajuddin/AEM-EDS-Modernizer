# AuthoringStrategyRegistry

> Phase 2 service that selects an `AuthoringStrategy` per
> project. There are 4 built-in strategies; operators can add
> custom ones.

- **Stage:** consulted by `AuthoringAgent` (in `AUTHORING`)
- **Phase:** 2
- **Service name:** `AuthoringStrategyRegistry`

## Built-in strategies

| Strategy | When to use |
|---|---|
| `UNIVERSAL_EDITOR` | Default. Creates UE-compatible page structures. |
| `DOC_BASED` | Doc-based authoring for sites that prefer document-style editing. |
| `EXISTING_REPO` | Reuses an existing target repo (no new pages created in AEM). |
| `CUSTOM_ADAPTER` | Plug in a custom authoring backend. |

## Interface

```java
public interface AuthoringStrategy {
    String name();
    AuthoringResult apply(AuthoringContext ctx);
    boolean supports(AemPageRecord page);
}
```

## Custom strategies

Operators can register a custom strategy by:

1. Implementing the `AuthoringStrategy` interface.
2. Calling `registry.register(strategy)` at startup.
3. Setting the project's `authoringStrategy` to the new
   strategy's name.

## Phase 2 dashboard

The `#/authoring` view (extended in Phase 2) shows the
selected strategy, the strategies tried (in order of
preference), and the per-page authoring result.

## Related

- [AuthoringAgent](AuthoringAgent.md) — the agent that uses
  the strategy.
- [ADR 0001](../adr/0001-phase2-advanced-features.md) — the
  Phase 2 decision record.
