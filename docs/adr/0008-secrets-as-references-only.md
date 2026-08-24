# ADR 0008 — Secrets as References Only (no raw keys)

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Security

## Context

A migration system needs API keys for: AEM (IMS service token),
GitHub (PAT or GitHub App), Figma (PAT), and the AI providers
(Anthropic, OpenAI, Gemini keys, or local Ollama endpoints).

These secrets must never be:

- Hard-coded in source.
- Persisted to JCR / PostgreSQL / files.
- Logged to stdout / Sling logs / browser console.
- Sent in API responses.

Per Master §60, secrets are **references only**:
`env:ANTHROPIC_API_KEY`, `secretsmanager:prod/...`, etc. The
modernizer resolves the reference at use time and never holds
the raw value in memory longer than necessary.

## Decision

The `SecretProvider` interface has a single method:

```java
String resolve(String reference);
```

Implementations:

- `EnvSecretProvider` (production default) — resolves
  `env:VAR_NAME` by reading from the JVM environment.
- (Future) `VaultSecretProvider` — resolves
  `secretsmanager:path` via the AWS SDK.
- (Future) `AemSecretProvider` — resolves
  `aem:/path/to/secret` from the AEM secret service.

The `Redactor` strips the following patterns from every log line
and every JSON response:

- `ghp_*`, `gho_*`, `ghu_*`, `ghs_*`, `ghr_*` (GitHub tokens)
- `sk-ant-*`, `sk-ant-*-*` (Anthropic)
- `sk-*` (OpenAI)
- `AIza*` (Google)
- `Bearer ...` (HTTP auth header)
- `figd_*` (Figma)
- Basic-auth URLs (`https://user:pass@host`)

The `Redactor` runs in the request thread *before* logging and
*before* the response is serialised.

## Consequences

### Positive

- **No secret in source, ever.** A developer adding
  `sk-ant-...` to a config file triggers a code review red flag.
- **No secret in JCR.** A JCR dump (e.g. for support) cannot
  leak the keys.
- **No secret in logs.** Even if a log line includes a
  misformatted request body, the `Redactor` strips the key.
- **No secret in browser.** The API never returns resolved
  secrets; only references.

### Negative

- **Secret rotation requires a redeploy.** A rotation of
  `ANTHROPIC_API_KEY` requires restarting the AEM Author
  instance (or restarting the standalone runtime). For the
  MVP this is acceptable.
- **`EnvSecretProvider` does not work in serverless contexts**
  where env vars are not stable. Mitigated by the planned
  `VaultSecretProvider` and `AemSecretProvider`.

## Alternatives considered

- **Direct env var reads in each provider**: rejected because it
  scatters the secret-handling logic and makes the `Redactor`
  impossible to apply uniformly.
- **A "secret store" JCR node**: rejected because it puts
  secrets in JCR (the very thing we want to avoid).

## Related

- [../security/SECRETS.md](../security/SECRETS.md) — full secret
  model and operational runbook.
- [ADR 0009](0009-ssrf-protection-on-every-url.md) — the URL
  guard that pairs with the secret model.
- [ADR 0014](0014-redactor-on-every-log-and-response.md) — the
  redactor that enforces "no secret in logs".
