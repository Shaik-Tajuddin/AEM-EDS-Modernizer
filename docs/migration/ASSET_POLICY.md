# Asset Policy

The asset metadata-only policy. Per Master §20, the modernizer
never downloads asset binaries by default.

## What the modernizer does with assets

For every asset reference on every eligible `AemPageRecord`,
the modernizer:

1. **Validates the reference** (HEAD 200, MIME in allowlist,
   authorised, has alt text).
2. **Preserves the AEM path** (`/content/dam/wknd/images/hero.jpg`).
3. **Rewrites the reference in the EDS section model** to the
   AEM-published URL
   (`https://author-p1234-e5678.adobeaemcloud.com/content/dam/wknd/images/hero.jpg`).

That's it. The asset binary stays in AEM DAM.

## What the modernizer does not do

- **No binary download.** No bytes leave AEM.
- **No binary upload.** No bytes are written to EDS or to
  the EDS CDN.
- **No DAM republish.** AEM DAM is the source of truth; the
  modernizer does not touch it.

## Why this is the right policy

- **Speed.** A 1000-page site with 5000 asset references
  completes in the same time as one with 100 references.
  Asset references are strings, not bytes.
- **Cost.** Bandwidth, storage, and CDN cost are
  zero. The AEM DAM's CDN is reused.
- **AEM DAM as the source of truth.** If an asset is
  updated in AEM, the next publish reflects the new binary
  without any re-migration.
- **Reversible.** If the operator decides not to migrate
  after all, the assets are untouched.

## Validations

For every asset, the modernizer checks:

| Check | Severity if failed |
|---|---|
| Asset exists in AEM DAM | CRITICAL |
| Asset is resolvable (HEAD 200) | HIGH |
| Asset is authorised (operator has read) | HIGH |
| Asset is target-compatible (MIME in allowlist) | HIGH |
| Asset has alt text (or `alt=""` for decorative) | MEDIUM |

Failed checks produce `IssueRecord`s and a per-asset
classification (`REFERENCED_FROM_AEM`, `BROKEN_REFERENCE`,
`UNAUTHORISED`, `UNSUPPORTED_MIME`, `MISSING_ALT`).

The dashboard's `#/assets` view shows every preserved
reference, its classification, and a flag indicating whether
the validation passed.

## URL rewriting

The AEM URL pattern is:

```
{author-host}/content/dam/{site}/images/{image}.jpg
```

The modernizer rewrites this in the EDS section model as
the same URL. The operator can override the host with a
URL mapping (planned for Phase 3) to point to a
customer-friendly CDN.

## When to deviate from the policy

The policy is the default. The operator can deviate by
configuring the `AssetPolicy` to one of:

- `METADATA_ONLY` (default) — preserve references only.
- `DOWNLOAD_AND_REHOST` — download and upload to the
  configured asset CDN. **Not supported in the MVP.**

The deviation is recorded on the `ProjectRecord` and
surfaced in the dashboard.

## See also

- [../adr/0010-assets-are-metadata-only.md](../adr/0010-assets-are-metadata-only.md) —
  the decision record.
- [../agents/AssetAnalysisAgent.md](../agents/AssetAnalysisAgent.md) —
  the agent.
