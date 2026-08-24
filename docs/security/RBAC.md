# RBAC

Role-based access control. The modernizer inherits AEM's
ACLs; no separate auth layer is required.

## How it works

The dashboard is a Sling Servlet. Sling enforces the ACLs
that apply to the servlet's path. A user with read access
to `/bin/aem-eds-modernizer/*` can see the dashboard; a
user with write access can trigger migrations.

In the standalone runtime, there is no authentication (the
runtime is loopback-only). For local development, the
operator can wrap the runtime in a reverse proxy that
enforces basic auth or a header check.

## Default ACLs

The default ACLs are set by the `ui.content` package's
Repo Init script. The default groups are:

| Group | Permission | Description |
|---|---|---|
| `content-authors` | read | Can see the dashboard, can see all jobs, can trigger dry runs |
| `content-editors` | read + write | Can trigger real migrations, can resume / cancel jobs |
| `modernizer-admins` | read + write + delete | Can configure the modernizer (OSGi config, projects) |

The default group mappings:

- `content-authors` = AEM `authors` group + the
  `modernizer-readers` AEM group.
- `content-editors` = AEM `editors` group.
- `modernizer-admins` = AEM `administrators` group.

## Per-project ACLs

Each project has its own ACLs. A user with read access to
`/content/aem-eds-modernizer/projects/{projectId}` can see
the project; a user with write access can trigger
migrations on it.

The per-project ACLs are set by the project creator (or by
the `modernizer-admins` group).

## How RBAC interacts with the dashboard

The dashboard's API endpoints respect the ACLs. A user
without read access to a project sees a 404 (not a 403, to
avoid information disclosure).

For the API:

```bash
# As a content-author
curl -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects
# Returns: [{"id": "...", "name": "..."}] (only the projects the user can see)

# As a content-author
curl -X POST -u user:pass https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects/{id}/migrate
# Returns: 403 Forbidden (the user has read-only access)
```

## Custom roles

Operators can define custom roles by:

1. Creating a new AEM group (e.g. `modernizer-viewers`).
2. Granting the group read access to the relevant paths.
3. Adding the group to the OSGi config
   (`AemEdsModernizerService.readOnlyGroups`).

The dashboard's API respects the new role automatically.

## See also

- [../aem/OSGI_CONFIG.md](../aem/OSGI_CONFIG.md) — the
  OSGi configuration.
- [../aem/REPO_INIT.md](../aem/REPO_INIT.md) — the Repo
  Init script that sets the default ACLs.
