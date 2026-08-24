# Figma Authentication

The Figma REST API uses a **Personal Access Token (PAT)**.
This document describes how the modernizer stores the PAT
and how it makes authenticated requests.

## How to get a PAT

1. Open the Figma desktop app or web app.
2. Go to Account Settings → Personal Access Tokens.
3. Click "Create new token".
4. Select the file scope: "All files" or specific files.
5. Copy the token (it starts with `figd_`).

## Where the PAT lives

The PAT is **never** stored in source, JCR, or browser. It
lives in an env var or in the AEM Secret Service:

```yaml
# In the env:
FIGMA_PAT=figd_...

# In the OSGi config:
figmaPatReference: "env:FIGMA_PAT"
```

## How the request is authenticated

```http
GET /v1/files/{key} HTTP/1.1
Host: api.figma.com
X-Figma-Token: figd_...
```

The `X-Figma-Token` header is set by the `FigmaClient`. The
PAT is read from the secret reference at request time.

## Token rotation

Figma PATs do not expire automatically, but the operator
can rotate them. To rotate:

1. Create a new PAT in Figma.
2. Update the secret reference with the new PAT.
3. Restart the AEM Author instance (or restart the
   standalone runtime).

The old PAT remains valid until explicitly revoked in
Figma.

## Token revocation

If a PAT is compromised:

1. Revoke it in Figma (Account Settings → Personal Access
   Tokens → Revoke).
2. Create a new PAT.
3. Update the secret reference.

The modernizer has no way to detect a compromised PAT; the
Figma API will start returning 403s, which the modernizer
reports as a `CRITICAL` issue.

## Related

- [../security/SECRETS.md](../security/SECRETS.md) — the
  full secret model.
- [API_USAGE.md](API_USAGE.md) — the Figma API endpoints.
