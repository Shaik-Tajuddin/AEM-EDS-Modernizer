# Redactor

The `Redactor` strips secret patterns from every log line
and every API response. It is the safety net for the
secret model.

## Why we need it

Even with the secret model (references only, no raw keys in
source), secrets can still leak:

- An AI request body includes a customer payload that
  happens to contain a key.
- A GitHub response includes a private repo name that
  embeds a token.
- An AEM URL with a basic-auth credential is logged by
  accident.

The `Redactor` catches these leaks by pattern-matching on
known secret formats.

## What it strips

| Pattern | Example | Source |
|---|---|---|
| `ghp_*` | `ghp_1234567890abcdef...` | GitHub PAT |
| `gho_*` | `gho_1234567890abcdef...` | GitHub OAuth |
| `ghu_*` | `ghu_1234567890abcdef...` | GitHub user token |
| `ghs_*` | `ghs_1234567890abcdef...` | GitHub server token |
| `ghr_*` | `ghr_1234567890abcdef...` | GitHub refresh token |
| `sk-ant-*`, `sk-ant-*-*` | `sk-ant-api03-...` | Anthropic |
| `sk-*` | `sk-...` | OpenAI |
| `AIza*` | `AIza...` | Google |
| `figd_*` | `figd_...` | Figma |
| `Bearer ...` | `Bearer eyJhbGc...` | HTTP auth header |
| `https://user:pass@host` | `https://alice:secret@example.com` | Basic-auth URL |

## Where it runs

The `Redactor` is called:

- In every SLF4J appender (via a custom appender wrapper).
- In the `ApiRouter` before serialising the response.
- In the `MigrationReportService` before writing the report
  JSON.

The redactor's patterns are compiled once at startup and
applied in a single pass over the input string.

## How to add a new pattern

To add a new pattern (e.g. for a custom AI provider):

1. Add a new pattern to `Redactor.PATTERNS`.
2. Add a test case to `RedactorTest`.
3. Update this document.

The pattern must be a `java.util.regex.Pattern` with a
group named `secret` (so the replacement is unambiguous).

## Performance

The redactor is on the hot path. A 10 KB log line is
redacted in < 1 ms. The patterns are compiled once at
startup; the input is matched against the patterns in a
single pass.

## False positives

A legitimate string that happens to match a pattern (e.g.
a documentation snippet with `Bearer ...`) is redacted.
Mitigated by the redactor being conservative: only
well-known prefixes are matched, and the patterns are
anchored.

## Structured logs

The MVP redactor is string-based. A JSON log line with a
key `apiKey` is not automatically redacted unless the value
matches a known pattern. A follow-up adds a JSON-aware
redactor that traverses the JSON tree.

## See also

- [../adr/0014-redactor-on-every-log-and-response.md](../adr/0014-redactor-on-every-log-and-response.md) —
  the decision record.
- [SECRETS.md](SECRETS.md) — the secret model.
