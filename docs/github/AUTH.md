# GitHub Authentication

The modernizer supports two GitHub auth modes: **Personal
Access Token (PAT)** and **GitHub App**. This document
describes the trade-offs and how to configure each.

## PAT vs GitHub App

| | PAT | GitHub App |
|---|---|---|
| Setup | Simple (generate a token, paste it) | More complex (register an app, install it) |
| Granular permissions | No (all permissions of the user) | Yes (per-repository, read-only / read-write) |
| Rate limit | 5000 req / hour per user | 5000 req / hour per installation |
| Auditability | Token tied to a user | Token tied to an installation |
| Best for | Single-repo, single-user | Multi-repo, multi-user, enterprise |

**Recommendation: use a GitHub App for production.**

## GitHub App setup

1. Go to GitHub Settings → Developer settings → GitHub Apps
   → New GitHub App.
2. Set the homepage URL (e.g. `https://example.com`).
3. Set the callback URL (e.g. `https://example.com/oauth/callback`).
4. Set the webhook URL (e.g. `https://example.com/webhook`).
5. Set the repository permissions:
   - `Contents`: Read & Write
   - `Pull requests`: Read & Write
   - `Metadata`: Read-only (default)
6. Set the user permissions: none (the app acts on its
   own behalf).
7. Subscribe to events: `Push`, `Pull request`.
8. Save, then generate a private key (PEM).
9. Install the app on the target repository (or org).

## Where the credentials live

```yaml
# GitHub App:
githubAppId: "..."
githubAppPrivateKeyReference: "env:GITHUB_APP_PRIVATE_KEY"  # PEM
githubInstallationId: "..."

# PAT:
githubPatReference: "env:GITHUB_PAT"
```

The credentials are **never** stored in source, JCR, or
browser.

## How the request is authenticated

For the **GitHub App**:

```
1. Generate a JWT signed with the private key
2. POST https://api.github.com/app/installations/{installation_id}/access_tokens
   Authorization: Bearer {JWT}
3. Receive an installation access token (1 hour TTL)
4. Use the installation access token in subsequent requests
   Authorization: token {installation_token}
```

For the **PAT**:

```
Authorization: token {pat}
```

The `GitHubClient` handles the JWT generation and the
installation token caching automatically.

## Failure modes

- **401 from GitHub:** the credentials are wrong or the
  token has been revoked; the modernizer records a
  `CRITICAL` issue.
- **403 from GitHub:** the app does not have the required
  permission; the modernizer records a `CRITICAL` issue
  with the GitHub response.
- **422 from GitHub:** the request body is invalid (e.g.
  branch name is invalid); the modernizer records a
  `HIGH` issue with the GitHub response.

## Related

- [../security/SECRETS.md](../security/SECRETS.md) — the
  full secret model.
- [BRANCH_POLICY.md](BRANCH_POLICY.md) — the branch naming
  convention.
