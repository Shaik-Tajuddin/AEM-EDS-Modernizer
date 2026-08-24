# Repo Init

The `ui.config` content package contains a Repo Init script
that creates the modernizer's configuration nodes on first
install. Repo Init is the AEM-recommended way to bootstrap
JCR content from a content package.

## The script

```java
// /apps/aem-eds-modernizer/configs/org.apache.sling.jcr.repoinit.RepositoryInitializer~modernizer.config
create path /content/aem-eds-modernizer
create path /content/aem-eds-modernizer/projects
create path /content/aem-eds-modernizer/projects/seed

set properties on /content/aem-eds-modernizer/projects/seed
  set sling:resourceType "aem-eds-modernizer/components/project"
  set projectId "{UUID}"
  set projectName "WKND Modernization (seed)"
  set contentRoot "/content/wknd"
  set markerProperty "modernizer.migrate"
  set markerValue "true"
  set markerPolicy "MARKED_AND_EXPLICIT_SELECTION"
end

create service user modernizer-service with path /home/users/system/modernizer
set ACL on /content/aem-eds-modernizer
  allow jcr:read,rep:write for modernizer-service
end
```

## Service user

The `modernizer-service` user is created with the minimum
required permissions:

- `jcr:read` on `/content/{site}` (the modernizer needs to
  read the content tree).
- `rep:write` on `/content/aem-eds-modernizer` (the
  modernizer needs to write its own state).

## Idempotency

The Repo Init script is **idempotent**: re-running it on an
existing install is a no-op. The `create path` and `create
service user` commands are no-ops if the path / user already
exists.

## Where to find the script

In the AEM Author web console
(`/system/console/status-repoinit`), the running Repo Init
scripts are listed. The modernizer's script is at
`/apps/aem-eds-modernizer/configs/org.apache.sling.jcr.repoinit.RepositoryInitializer~modernizer.config`.

## Related

- [OSGI_CONFIG.md](OSGI_CONFIG.md) — the OSGi configurations
  the modernizer reads.
