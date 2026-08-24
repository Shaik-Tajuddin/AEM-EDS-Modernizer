# ADR 0014 — Redactor on Every Log and Response

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Security

## Context

Secrets leak in three places:

1. **Source code.** Mitigated by ADR 0008 (secrets as references
   only).
2. **Logs.** An AI request body that includes a customer
   payload, a GitHub response that includes a private repo
   name, an AEM URL with a basic-auth credential — all of these
   can end up in a log line and persist in Cloud Manager logs
   for weeks.
3. **API responses.** A misconfigured endpoint that returns the
   `AiRoutingPolicy` with embedded secret references may
   accidentally include a resolved secret. A `/test-connections`
   endpoint that returns the raw auth header is a similar risk.

The redactor is the safety net for (2) and (3). It runs on
every log line and every API response.

## Decision

`Redactor.redact(string)` is a stateless, fast string operation
that replaces the following patterns with `[REDACTED]`:

- GitHub tokens: `ghp_*`, `gho_*`, `ghu_*`, `ghs_*`, `ghr_*`
- Anthropic keys: `sk-ant-*`, `sk-ant-*-*`
- OpenAI keys: `sk-*` (anything starting with `sk-` and
  matching the OpenAI prefix regex)
- Google keys: `AIza*`
- Figma tokens: `figd_*`
- HTTP auth headers: `Bearer ...`
- Basic-auth URLs: `https://user:pass@host`

The redactor is called:

- In every SLF4J appender (via an `MDC` filter and a custom
  appender wrapper).
- In the `ApiRouter` before serialising the response.
- In the `MigrationReportService` before writing the report
  JSON.

The redactor's patterns are compiled once at startup and
applied in a single pass over the input string.

## Consequences

### Positive

- **No secret in logs.** Even if a log line includes a
  misformatted request body, the redactor strips the key.
- **No secret in API responses.** A misconfigured endpoint
  cannot accidentally expose a secret to the dashboard.
- **No secret in the migration report.** The report is
  downloadable as JSON; the redactor ensures the JSON is safe
  to share with the customer.
- **The redactor is on the hot path but cheap.** A 10 KB log
  line is redacted in < 1 ms.

### Negative

- **False positives are possible.** A legitimate string that
  happens to match `Bearer ...` (e.g. a documentation snippet)
  is redacted. Mitigated by the redactor being conservative
  (only well-known prefixes are matched).
- **The redactor does not understand structured logs.** A JSON
  log line with a key `apiKey` is not automatically
  redacted unless the value matches a known pattern. The MVP
  is fine with this; a follow-up can add a JSON-aware
  redactor.
- **Custom secret formats are not covered.** A customer that
  uses a non-standard secret format (e.g. a custom AI
  provider with a `xyz_*` token) must add a custom redactor
  pattern.

## Alternatives considered

- **Static analysis to prevent secrets in source** (rejected
  as a primary mechanism: it's the right first line but
  not enough).
- **A separate log-redaction proxy** (e.g. Vector, Fluentd):
  rejected because it adds an operational dependency for
  something the application can do in-process.

## Related

- [../security/REDACTOR.md](../security/REDACTOR.md) — full
  redactor documentation.
- [ADR 0008](0008-secrets-as-references-only.md) — the secret
  model that pairs with the redactor.
- [ADR 0009](0009-ssrf-protection-on-every-url.md) — the URL
  guard that pairs with both.
