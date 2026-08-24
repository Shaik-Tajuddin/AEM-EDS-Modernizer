# Secrets

The secret model. Per Master §60, secrets are **references
only** (`env:VAR_NAME`, `secretsmanager:path`, `aem:/path`).
The modernizer resolves the reference at use time and never
holds the raw value in memory longer than necessary.

## Where secrets are needed

| Secret | Used by | Where it lives |
|---|---|---|
| AEM IMS client ID + secret | `AemClient` | env var or AEM Secret Service |
| GitHub App private key + installation ID | `GitHubClient` | env var or AEM Secret Service |
| GitHub PAT (alternative) | `GitHubClient` | env var or AEM Secret Service |
| Figma PAT | `FigmaClient` | env var or AEM Secret Service |
| Anthropic API key | `AnthropicProvider` | env var or AEM Secret Service |
| OpenAI API key | `OpenAIProvider` | env var or AEM Secret Service |
| Google API key | `GeminiProvider` | env var or AEM Secret Service |

## Secret reference format

```
env:VAR_NAME
secretsmanager:path
aem:/path/to/secret
```

The `SecretProvider` interface resolves the reference:

```java
public interface SecretProvider {
    String resolve(String reference);
}
```

## Implementations

| Class | Where it runs | Backend |
|---|---|---|
| `EnvSecretProvider` | Standalone, AEM Cloud (default) | OS environment variables |
| `AemSecretProvider` (planned) | AEM Cloud | AEM Secret Service |
| `VaultSecretProvider` (planned) | Standalone, AEM Cloud (optional) | HashiCorp Vault |

## Token caching

For tokens that have a TTL (e.g. IMS access tokens, GitHub
installation tokens), the `SecretProvider` (or the
`Connector`) caches the resolved value in memory until
5 minutes before expiry. The cache is per-secret,
per-process.

## Redaction

The `Redactor` strips the following patterns from every log
line and every API response:

- GitHub tokens: `ghp_*`, `gho_*`, `ghu_*`, `ghs_*`, `ghr_*`
- Anthropic keys: `sk-ant-*`, `sk-ant-*-*`
- OpenAI keys: `sk-*`
- Google keys: `AIza*`
- Figma tokens: `figd_*`
- HTTP auth headers: `Bearer ...`
- Basic-auth URLs: `https://user:pass@host`

See [REDACTOR.md](REDACTOR.md) for the full redactor
documentation.

## Rotation

A secret rotation requires:

1. Issue a new secret in the source system (e.g. create a
   new IMS service account).
2. Update the secret reference (env var or AEM Secret
   Service entry).
3. Restart the AEM Author instance (or restart the
   standalone runtime).

The old secret remains valid until explicitly revoked.

## Revocation

If a secret is compromised:

1. Revoke it in the source system.
2. Issue a new secret.
3. Update the secret reference.
4. Audit the access logs for the time window of the
   compromise.

The modernizer has no way to detect a compromised secret
on its own; the source system's API will start returning
401/403, which the modernizer reports as a `CRITICAL`
issue.

## See also

- [../adr/0008-secrets-as-references-only.md](../adr/0008-secrets-as-references-only.md) —
  the decision record.
- [REDACTOR.md](REDACTOR.md) — the redactor.
- [../aem/IMS_AUTH.md](../aem/IMS_AUTH.md) — the IMS auth
  flow.
