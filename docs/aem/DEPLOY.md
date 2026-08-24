# Deploy to AEM Cloud

This is the full Cloud Manager pipeline configuration and
deployment runbook for the AEM → EDS Modernizer.

## Pre-requisites

1. An AEM Cloud Service program (Author + Publish tier).
2. A Cloud Manager pipeline configured (the AEM archetype
   provides a starter pipeline).
3. A GitHub repository for the modernizer source.
4. (For real mode) IMS credentials for the AEM Author and
   Publish endpoints.

## Build

```bash
mvn install -DskipTests
```

This produces:

- `core/target/core-0.1.0-SNAPSHOT.jar` — the OSGi bundle
- `core/target/core-0.1.0-SNAPSHOT-standalone.jar` — fat jar
  for the standalone runtime
- `ui.apps/target/ui.apps-0.1.0-SNAPSHOT.zip` — immutable
  content (HTL, clientlibs, OSGi install hook)
- `ui.config/target/ui.config-0.1.0-SNAPSHOT.zip` — OSGi
  configurations and Repo Init
- `ui.content/target/ui.content-0.1.0-SNAPSHOT.zip` —
  mutable baseline content (sample projects, dashboard
  pages)
- `dispatcher/target/dispatcher-0.1.0-SNAPSHOT.zip` —
  Dispatcher config
- `all/target/all-0.1.0-SNAPSHOT.zip` — single deployable
  container (depends on the four above)

## Cloud Manager pipeline

The `all` content package is the single deployable artefact.
A typical Cloud Manager pipeline:

```yaml
# .cloudmanager/config.yaml
pipelines:
  - name: modernizer-prod
    trigger: MANUAL
    stages:
      - name: Build
        type: BUILD
      - name: Stage
        type: DEPLOY
        deployTarget: stage
      - name: Production
        type: DEPLOY
        deployTarget: production
        requiresApproval: true
```

The `Build` stage runs `mvn install` (or
`mvn verify` for the test pipeline). The `Stage` and
`Production` stages deploy the `all-0.1.0-SNAPSHOT.zip` via
Cloud Manager.

## Post-deploy verification

1. Open `https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer`
   in a browser. The dashboard should load.
2. Click "Create demo project". The seed project should be
   created.
3. Click "Run Dry Run". The full state machine should run
   and the dashboard should populate.

## Rollback

If a deployment fails, Cloud Manager can roll back to the
previous version. The rollback is safe because:

- The modernizer is a new component; rolling back removes
  the dashboard.
- The OSGi bundle is the only thing that needs to be
  removed; no AEM content is modified by the modernizer.
- The `ui.content` package adds sample projects, which can
  be removed via `https://author-pXXXX.../crx/de/index.jsp`.

## Smoke test against a real AEM tenant

To verify the deployment without running a full migration:

```bash
curl https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/health
# {"status":"ok","time":1700000000000}

curl https://author-pXXXX-eYYYY.adobeaemcloud.com/bin/aem-eds-modernizer/api/projects
# [{"id":"...","name":"WKND Modernization (seed)","createdAt":"..."}]
```

## Related

- [OSGI_CONFIG.md](OSGI_CONFIG.md) — the OSGi configuration
  the modernizer reads at runtime.
- [DISPATCHER.md](DISPATCHER.md) — the Dispatcher vhost
  config.
- [IMS_AUTH.md](IMS_AUTH.md) — the IMS auth flow.
