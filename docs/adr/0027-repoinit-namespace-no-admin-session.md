# ADR-0027 — RepoInit namespace registration, drop admin session

- **Status:** Accepted
- **Date:** 2026-08-30
- **Supersedes:** `JcrStore.registerNamespace()` with `repository.login(new SimpleCredentials("admin", ...))`
- **Scope:** `core/.../persistence/JcrStore.java`, `ui.config` Repo Init

## Context

The `JcrStore` implementation registered the `eds` JCR namespace
programmatically on `@Activate` using an admin session:

```java
session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
session.getWorkspace().getNamespaceRegistry().registerNamespace("eds", NS_URI);
```

This was necessary because the `modernizer-service` user does not hold
the repository-wide `jcr:namespaceManagement` privilege. However:

1. **Cloud Manager quality gates flag `loginAdministrative` usage.**
   It fails the Cloud Manager build analyzer and is an audit finding.
2. **Unnecessary privilege.** The `JcrStore` service is project-scoped;
   an admin session is repo-wide and grants far more than namespace
   registration.
3. **Non-declarative.** Namespace registration happens imperatively in
   Java code rather than in the version-controlled Repo Init script.
   If the bundle restarts before the admin login succeeds, the
   namespace is missing and project saves fail.

## Decision

Delete the admin-session namespace registration from `JcrStore.activate()`.
Replace with one line of Repo Init in `ui.config`:

```
register namespace (eds) https://www.adobe.com/aem-eds-modernizer/1.0
```

Repo Init runs at startup with the privileges it needs, is declarative,
is version-controlled, and is the Adobe-sanctioned path for this
operation.

### Least-privilege ACLs

While updating Repo Init, pin the service user's ACLs to least
privilege:

```
create service user modernizer-service with path /home/users/system/aem-eds-modernizer
set principal ACL for modernizer-service
  allow jcr:read on /content
  allow jcr:read on /conf
  allow jcr:read on /apps
  allow jcr:all  on /var/aem-eds-modernizer
end
```

The `jcr:all` on `/var/aem-eds-modernizer` grants create, read,
update, delete for project nodes. The read-only grants on `/content`,
`/conf`, and `/apps` cover discovery and template resolution.

### Namespace necessity

An open question: **is a custom namespace needed at all?**
`eds:projectId` on an `nt:unstructured` node buys namespace hygiene
and little else, at the cost of a repo-wide mutation. An unprefixed
`projectId` property, or `sling:` / plain names, would work
identically for an `nt:unstructured` node that accepts any property
name.

**Decision:** keep the `eds` namespace for now. It provides a clear
visual indicator in CRX/DE that these properties belong to the
modernizer, and it prevents accidental collision with AEM or Sling
reserved names. If it becomes a burden, dropping it is a one-line
Repo Init removal + a property-rename migration.

## Consequences

### Positive

- No admin session — passes Cloud Manager quality gates.
- Namespace registration is declarative and version-controlled.
- Service user holds least privilege.
- Bundle activation no longer depends on admin-login success.

### Negative

- Namespace is created at repo init time, not at bundle activation.
  If the bundle is installed before Repo Init runs, project saves
  fail until the next startup. This is the standard AEM startup
  ordering and is mitigated by installing `ui.config` before `core`.

## Related

- [ADR-0026](0026-project-state-under-var-not-conf.md) —
  the `/var` move (run alongside this change; ACLs are combined).
- [REPO_INIT.md](../aem/REPO_INIT.md) — the Repo Init documentation.
