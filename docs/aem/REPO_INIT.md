# Repo Init

The `ui.config` content package contains a Repo Init script
that creates the modernizer's service user and ACLs on first
install. Repo Init is the AEM-recommended way to bootstrap
JCR content from a content package.

## The script

```json
// ui.config/.../org.apache.sling.jcr.repoinit.RepositoryInitializer~aem-eds-modernizer.cfg.json
{
  "scripts": [
    "create service user modernizer-service with path /home/users/system/aem-eds-modernizer\nset principal ACL for modernizer-service\n  allow jcr:read,rep:write on /content/aem-eds-modernizer\n  allow jcr:read on /apps/aem-eds-modernizer\n  allow jcr:read,rep:write on /conf/aem-eds-modernizer\nend"
  ]
}
```

## Service user

The `modernizer-service` user is created with the minimum
required permissions:

- `jcr:read,rep:write` on `/content/aem-eds-modernizer` —
  the modernizer writes job/event state under this path.
- `jcr:read` on `/apps/aem-eds-modernizer` —
  the modernizer reads its component definitions.
- `jcr:read,rep:write` on `/conf/aem-eds-modernizer` —
  `JcrStore` persists project records as `nt:unstructured`
  nodes under `/conf/aem-eds-modernizer/<Project ID>` with
  `eds:*` namespaced properties.

## JCR namespace registration

`JcrStore` uses the `eds` namespace prefix for all project
properties (`eds:projectId`, `eds:name`, `eds:aemAuthorUrl`,
etc.). The namespace `eds` → `https://www.adobe.com/aem-eds-modernizer/1.0`
is **not** declared via Repo Init (Repo Init has no namespace
registration command). Instead, `JcrStore` registers it
programmatically on `@Activate` using an admin session, since
namespace registration requires the repository-wide
`jcr:namespaceManagement` privilege that the scoped
`modernizer-service` user does not hold.

If the namespace registration fails, `JcrStore` logs an error
and falls back to in-memory-only persistence for that
activation cycle. The namespace is idempotent — re-registering
an existing prefix/URI pair is a no-op.

## JcrStore persistence path

Every `saveProject()` call creates or updates an
`nt:unstructured` node at:

```
/conf/aem-eds-modernizer/<sanitized-project-id>
```

with properties:

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

## Idempotency

The Repo Init script is **idempotent**: re-running it on an
existing install is a no-op. The `create service user` and
`set principal ACL` commands are no-ops if the user/ACL
already exists.

## Where to find the script

In the AEM Author web console
(`/system/console/status-repoinit`), the running Repo Init
scripts are listed. The modernizer's script is at
`/apps/aem-eds-modernizer/osgiconfig/config/org.apache.sling.jcr.repoinit.RepositoryInitializer~aem-eds-modernizer.cfg.json`.

## Related

- [OSGI_CONFIG.md](OSGI_CONFIG.md) — the OSGi configurations
  the modernizer reads.
