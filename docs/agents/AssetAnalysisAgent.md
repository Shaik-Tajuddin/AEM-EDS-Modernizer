# AssetAnalysisAgent

> Validates every asset reference in the scope. Per Master §20,
> **assets are metadata-only**: the agent never downloads a
> binary.

- **Stage:** `ANALYZING`
- **Phase:** 1
- **Agent name:** `asset-analysis`
- **Task type:** `ASSET_ANALYSIS`

## Inputs

- Every asset reference on every eligible `AemPageRecord`
  (extracted from the page's components).
- The `AemClient` for metadata-only HEAD requests.

## Outputs

- `AssetRecord`s with: `path`, `mimeType`, `size`,
  `lastModified`, `classification`
  (`REFERENCED_FROM_AEM`, `BROKEN_REFERENCE`, etc.),
  `validationPassed`.

## Validation

For every asset, the agent checks:

| Check | Severity if failed |
|---|---|
| Asset exists in AEM DAM | CRITICAL |
| Asset is resolvable (HEAD 200) | HIGH |
| Asset is authorised (operator has read) | HIGH |
| Asset is target-compatible (MIME in allowlist) | HIGH |
| Asset has alt text (or `alt=""` for decorative) | MEDIUM |

**Asset binaries downloaded: 0 (always).**
**Asset binaries uploaded: 0 (always).**

## AI usage

None. Pure HTTP HEAD requests.

## Failure modes

- **AEM DAM unreachable:** the agent records a `CRITICAL` issue
  and the migration is blocked.
- **HEAD request rate-limited:** the agent backs off and
  retries; the throughput is throttled to the
  `AEM_CONCURRENCY` setting.

## Related

- [ADR 0010](../adr/0010-assets-are-metadata-only.md) — the
  asset policy decision record.
