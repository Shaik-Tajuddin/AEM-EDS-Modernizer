# Deployment Checklist

The pre-deployment checklist for the AEM → EDS Modernizer.

## Pre-build

- [ ] Java 11 is installed (`java -version`).
- [ ] Maven 3.9 is installed (`mvn -version`).
- [ ] The GitHub repo is cloned.
- [ ] The Cloud Manager pipeline is configured.

## Build

- [ ] `mvn install -DskipTests` succeeds.
- [ ] `mvn -pl core test` succeeds (13 tests passing).
- [ ] `bash scripts/e2e.sh` succeeds (e2e green).
- [ ] `all/target/all-0.1.0-SNAPSHOT.zip` is produced.

## Pre-deploy

- [ ] The AEM Cloud program is provisioned.
- [ ] The IMS service account is created and granted
      access to the program.
- [ ] The GitHub App (or PAT) is created and installed.
- [ ] The Figma PAT is created.
- [ ] The AI provider API keys are created.
- [ ] The secret references are configured in the OSGi
      config (env vars or AEM Secret Service).
- [ ] The Dispatcher config is reviewed.
- [ ] The capacity plan is reviewed
      ([CAPACITY_PLANNING.md](CAPACITY_PLANNING.md)).
- [ ] The incident response procedure is in place
      ([INCIDENT_RESPONSE.md](INCIDENT_RESPONSE.md)).

## Deploy

- [ ] The `all` package is uploaded to Cloud Manager.
- [ ] The pipeline is triggered.
- [ ] The `Build` stage succeeds.
- [ ] The `Stage` stage succeeds.
- [ ] The `Production` stage is approved.
- [ ] The `Production` stage succeeds.

## Post-deploy

- [ ] The health check passes
      (`curl /bin/aem-eds-modernizer/api/health`).
- [ ] The dashboard loads in a browser.
- [ ] A demo project is created.
- [ ] A demo dry run completes.
- [ ] The dry run produces a valid estimate and virtual
      diff.
- [ ] The cost / token alerts are configured.
- [ ] The on-call rotation is updated.
- [ ] The customer is notified of the deployment.

## Rollback

If the deployment fails or causes a production issue:

- [ ] Roll back via Cloud Manager (one-click).
- [ ] Verify the rollback succeeded.
- [ ] Open an incident.
- [ ] Investigate the root cause.

## See also

- [CAPACITY_PLANNING.md](CAPACITY_PLANNING.md) — capacity
  planning.
- [INCIDENT_RESPONSE.md](INCIDENT_RESPONSE.md) — incident
  response.
- [../aem/DEPLOY.md](../aem/DEPLOY.md) — the deployment
  runbook.
