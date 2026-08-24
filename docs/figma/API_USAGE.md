# Figma API Usage

The Figma REST API v1 endpoints the modernizer calls.

## Endpoints

| Endpoint | Purpose | Phase |
|---|---|---|
| `GET /v1/files/{key}` | Fetch the full Figma file | 1 + 2 |
| `GET /v1/files/{key}/components` | Fetch the published components | 2 |
| `GET /v1/files/{key}/styles` | Fetch the published styles | 1 + 2 |
| `GET /v1/images/{key}?ids={node-ids}&format=png&scale=2` | Render nodes to PNG | 1 + 2 |

## Rate limits

The Figma API has rate limits per plan:

| Plan | Requests / minute |
|---|---|
| Free | 60 |
| Professional | 120 |
| Organization | 300 |
| Enterprise | Custom (typically 1000+) |

The modernizer respects the `X-Figma-Plan-Usage` and
`X-Figma-Plan-Tier` response headers and throttles requests
to stay within the limit. A `429` response triggers an
exponential backoff.

## Caching

The modernizer caches the file response in memory for the
duration of the migration job. The cache key is
`figma:{file-key}:v{version}`. If the Figma file is updated
mid-migration, the cached version is used (and a `MEDIUM`
issue is recorded).

## Failure modes

- **403 from Figma:** the PAT is invalid or the file is
  restricted; the modernizer records a `CRITICAL` issue.
- **404 from Figma:** the file key is wrong; the modernizer
  records a `CRITICAL` issue.
- **429 from Figma:** the modernizer backs off and retries
  with exponential backoff (up to 3 times).

## Related

- [AUTH.md](AUTH.md) — the Figma PAT auth flow.
- [TOKEN_EXTRACTION.md](TOKEN_EXTRACTION.md) — the token
  extraction logic.
- [COMPONENT_PAIRING.md](COMPONENT_PAIRING.md) — the
  component pairing logic (Phase 2).
