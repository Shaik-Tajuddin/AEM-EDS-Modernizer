# ADR-0026 — Project state under `/var`, not `/conf`

- **Status:** Accepted
- **Date:** 2026-08-30
- **Supersedes:** `/conf/aem-eds-modernizer/<Project ID>` (README §4, JcrStore initial implementation)
- **Scope:** `core/.../persistence/JcrStore.java`, `ui.config` Repo Init, all documentation

## Context

The initial `JcrStore` implementation persisted project records under
`/conf/aem-eds-modernizer/<Project ID>`. While `/conf` is browsable in
CRX/DE (which the README values), it is the **wrong JCR subtree** for
machine-generated runtime state:

1. **Package-managed.** A content-package filter on
   `/conf/aem-eds-modernizer` with `mode="replace"` wipes every
   project record on deploy. This is the same failure family as
   `FilterXmlModeAnalysis` / `PackageOverlaps` already encountered on
   this repo — except the casualty is production run history and cost
   data.

2. **Replicated to publish.** Run history and cost data get pushed to
   the publish tier, where they have no business.

3. **CA-config resolution path.** Sling context-aware config resolves
   through `/conf`. Thousands of project nodes sit in a lookup path
   they don't belong in, slowing resolution.

4. **Semantic mismatch.** `/conf` = configuration authored by humans.
   Project state is machine-generated runtime data. Adobe documents
   `/var` as the location for runtime-generated content.

## Decision

Move project state to:

```
/var/aem-eds-modernizer/projects/{yyyy}/{MM}/{projectId}
```

### Sharding by date

A flat parent with 50k children degrades Oak query and traversal badly.
Sharding by `yyyy/MM` keeps each month's parent to a few hundred
children under normal load and makes date-scanned retention trivial.

### Node name escaping

`<Project ID>` used raw as a node name will break on spaces, colons,
slashes, or a leading digit. Use `JcrUtil.createValidName()` /
`Text.escapeIllegalJcrChars()` and store the original id as
`eds:projectId`.

### Lucene exclusion

Exclude `/var/aem-eds-modernizer` from the fulltext Lucene index.
Otherwise every generated CSS and Markdown file gets indexed for site
search, degrading index size and query performance.

### Retention job

Nothing currently deletes old runs. Add a Sling Job (or scheduler)
that prunes trees older than a configurable TTL (default 90 days).
Decide TTL before the first 10k-page run, not after.

### Artifact storage

Generated JS/CSS/Markdown belong in `nt:file` / `jcr:data` binaries,
not long String properties. This keeps the node tree small and
allows Oak to stream large artifacts without loading them into the
session.

## Consequences

### Positive

- `/var` is equally browsable in CRX/DE — the "visible in CRX/DE"
  property the README values is fully preserved.
- Runtime state survives content-package deployments.
- Publish tier does not receive project data.
- CA-config resolution is not polluted.
- Sharding prevents the flat-child Oak performance cliff.
- Date-sharded retention is trivial to implement.

### Negative

- Slightly more complex path construction (yyyy/MM shard).
- Requires a Repo Init change and ACL migration.
- Existing projects under `/conf` must be migrated once (a one-time
  upgrade step).

## Migration path

1. Deploy the new `JcrStore` that writes to `/var`.
2. Run a one-time upgrade servlet/job that copies nodes from
   `/conf/aem-eds-modernizer/*` to `/var/aem-eds-modernizer/projects/*`.
3. Remove the `/conf/aem-eds-modernizer` ACL from Repo Init in the
   next release.

## Related

- [ADR-0027](0027-repoinit-namespace-no-admin-session.md) —
  RepoInit namespace registration (run alongside this change).
- [ADR-0028](0028-batched-checkpoint-persistence.md) —
  reduces JCR write volume to the new `/var` tree.
- [ADR-0029](0029-projectstore-port-and-conformance-suite.md) —
  abstract conformance tests run against the new path.
