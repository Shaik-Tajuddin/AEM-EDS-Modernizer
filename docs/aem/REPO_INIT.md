# Repo Init

The `ui.config` content package contains a Repo Init script
that registers the `eds` JCR namespace, creates the modernizer's
service user, and sets ACLs on first install. Repo Init is the
AEM-recommended way to bootstrap JCR content from a content
package.

## The script

```json
// ui.config/.../org.apache.sling.jcr.repoinit.RepositoryInitializer~aem-eds-modernizer.cfg.json
{
  "scripts": [
    "register namespace (eds) https://www.adobe.com/aem-eds-modernizer/1.0\n\ncreate service user modernizer-service with path /home/users/system/aem-eds-modernizer\nset principal ACL for modernizer-service\n  allow jcr:read on /content\n  allow jcr:read on /conf\n  allow jcr:read on /apps\n  allow jcr:all on /var/aem-eds-modernizer\nend"
  ]
}
```

## JCR namespace registration

The `eds` namespace prefix is registered declaratively via
Repo Init at startup:

```
register namespace (eds) https://www.adobe.com/aem-eds-modernizer/1.0
```

This replaces the previous programmatic registration that used an
admin session in `JcrStore.activate()`. Repo Init runs with the
privileges it needs, is declarative, is version-controlled, and
passes Cloud Manager quality gates.

## Service user

The `modernizer-service` user is created with least privilege:

- `jcr:read` on `/content` —
  the modernizer reads the content tree for discovery.
- `jcr:read` on `/conf` —
  the modernizer reads context-aware configuration.
- `jcr:read` on `/apps` —
  the modernizer reads its component definitions.
- `jcr:all` on `/var/aem-eds-modernizer` —
  `JcrStore` persists project records as `nt:unstructured`
  nodes under `/var/aem-eds-modernizer/projects/{yyyy}/{MM}/`
  with `eds:*` namespaced properties.

## JcrStore persistence path

Every `saveProject()` call creates or updates an
`nt:unstructured` node at:

```
/var/aem-eds-modernizer/projects/{yyyy}/{MM}/{escapedProjectId}
```

The path is **date-sharded** by year/month (UTC) to prevent the
flat-child Oak performance cliff that occurs with 50k+ children
under a single parent. Node names are escaped via
`Text.escapeIllegalJcrChars()` so project IDs containing colons,
slashes, or spaces are safe. The original project ID is stored
as the `eds:projectId` property.

Properties on each project node:

| Property                  | Type   | Description                          |
| ------------------------- | ------ | ------------------------------------ |
| `jcr:title`               | String | Project display name                 |
| `eds:projectId`           | String | Unique project identifier            |
| `eds:name`                | String | Project name                         |
| `eds:aemAuthorUrl`        | String | AEM Author instance URL              |
| `eds:aemPublishUrl`       | String | AEM Publish instance URL             |
| `eds:contentRoot`         | String | JCR content root path                |
| `eds:pageScope`           | String | Page scope filter                    |
| `eds:edsGitRepoUrl`       | String | EDS Git repository URL               |
| `eds:edsBranch`           | String | EDS Git branch                       |
| `eds:figmaUrl`            | String | Figma file URL                       |
| `eds:markerProperty`      | String | Marker property name                 |
| `eds:markerValue`         | String | Marker property value                |
| `eds:authoringStrategy`   | String | Authoring strategy (e.g. UNIVERSAL_EDITOR) |
| `eds:aiProvider`          | String | AI provider name                     |
| `eds:aiModel`             | String | AI model name                        |
| `eds:maxBudgetUsd`        | Double | Maximum budget in USD                |
| `eds:maxRepairAttempts`   | Long   | Maximum self-repair attempts         |
| `eds:createdAt`           | Long   | Creation timestamp (epoch ms)        |
| `eds:updatedAt`           | Long   | Last update timestamp (epoch ms)     |
| `eds:properties`          | String | JSON-encoded additional properties   |

## Why `/var` and not `/conf`

| Concern | `/conf` | `/var` |
|---------|---------|--------|
| Package-managed | Yes — a content-package filter with `mode="replace"` deletes runtime data | No — `/var` is not package-managed |
| Replicated to publish | Yes | No |
| CA-config resolution | Yes — project nodes pollute the lookup path | No |
| Semantics | Configuration authored by humans | Runtime-generated content |
| CRX/DE browsable | Yes | Yes |

## Idempotency

The Repo Init script is **idempotent**: re-running it on an
existing install is a no-op. The `register namespace`,
`create service user`, and `set principal ACL` commands are
no-ops if they already exist.

## Where to find the script

In the AEM Author web console
(`/system/console/status-repoinit`), the running Repo Init
scripts are listed. The modernizer's script is at
`/apps/aem-eds-modernizer/osgiconfig/config/org.apache.sling.jcr.repoinit.RepositoryInitializer~aem-eds-modernizer.cfg.json`.

## Related

- [OSGI_CONFIG.md](OSGI_CONFIG.md) — the OSGi configurations
  the modernizer reads.
- [ADR-0026](../adr/0026-project-state-under-var-not-conf.md) —
  the decision record for the `/var` move.
- [ADR-0027](../adr/0027-repoinit-namespace-no-admin-session.md) —
  the decision record for RepoInit namespace registration.
