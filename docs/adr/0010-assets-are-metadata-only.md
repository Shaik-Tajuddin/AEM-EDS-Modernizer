# ADR 0010 — Assets Are Metadata-Only, Never Binaries (Master §20)

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Asset policy, data flow

## Context

AEM DAM (Digital Asset Manager) holds millions of binary assets
(images, videos, PDFs). A naive migration to EDS would download
every binary, upload it somewhere, and rewrite every reference.
This is:

- **Expensive** — bandwidth, storage, and CDN cost.
- **Slow** — the migration time scales with asset count, not
  content count.
- **Risky** — a binary download is a one-way door; a corrupted
  download is hard to detect.
- **Off-policy** — Master §20 says the platform must not
  download asset binaries by default.

## Decision

The modernizer never downloads asset binaries. Asset references
in AEM content are preserved as the original AEM path
(`/content/dam/wknd/images/hero.jpg`) and rewritten in the EDS
section model as the corresponding AEM-published URL
(`https://author-p1234-e5678.adobeaemcloud.com/content/dam/wknd/images/hero.jpg`).

For every asset reference, the modernizer validates:

| Check | What it catches | Severity |
|---|---|---|
| Asset exists in AEM DAM | `404` references | CRITICAL |
| Asset is resolvable (HEAD 200) | Network or auth issues | HIGH |
| Asset is authorised (operator has read access) | Permission drift | HIGH |
| Asset is target-compatible (MIME type is in the allowlist) | Unsuported formats (e.g. `image/tiff`) | HIGH |
| Asset has alt text (or `alt=""` for decorative) | A11y regressions | MEDIUM |

Each check produces an `AssetRecord` with the result, the
classification (`REFERENCED_FROM_AEM`, `BROKEN_REFERENCE`, etc.),
and a flag indicating whether the check passed.

The dashboard's `#/assets` view shows every preserved reference
and its validation result.

**Asset binaries downloaded: 0 (always).**
**Asset binaries uploaded: 0 (always).**

## Consequences

### Positive

- **The migration is fast.** Asset references are strings; no
  bytes are moved. A 1000-page site with 5000 asset references
  completes in the same time as one with 100 references.
- **AEM DAM remains the source of truth.** If an asset is
  updated in AEM, the next publish reflects the new binary
  without any re-migration.
- **The platform is auditable.** Every reference is a record;
  the dashboard shows the full inventory.
- **CDN caching stays effective.** AEM's CDN already caches the
  asset URL; we don't introduce a second hop.

### Negative

- **The migrated site depends on AEM availability.** If AEM
  Author is down, the asset URL returns a 404. Mitigated by AEM
  Cloud's 99.9% SLA and by the operator's option to pre-warm
  the CDN.
- **AEM URLs are ugly.** `https://author-p1234...` is longer
  than `https://cdn.example.com/...`. Mitigated by the
  `URL_MAPPING` config (planned for Phase 3) that rewrites the
  asset host to a customer-friendly CDN.

## Alternatives considered

- **Download and re-host assets** (rejected per Master §20 and
  the cost / risk reasons).
- **Hybrid: download only above a size threshold** (e.g.
  > 1 MB): rejected because it creates a confusing two-tier
  asset model and a one-way door for the downloaded assets.

## Related

- [../migration/ASSET_POLICY.md](../migration/ASSET_POLICY.md) —
  the full asset policy.
- [ADR 0011](0011-virtual-diff-not-real-git.md) — the virtual
  diff that shows the preserved references.
