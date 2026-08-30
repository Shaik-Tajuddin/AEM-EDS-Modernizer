# AEM Integration

The AEM → EDS Modernizer talks to AEM as a Cloud Service for
two purposes:

1. **Source** — read the content tree, components, assets,
   templates, MSM, content fragments.
2. **Target** — create Universal Editor–compatible page
   structures in the target AEM.

This section collects the AEM-specific documentation: the
deployment guide, the OSGi configuration, the Dispatcher
config, and the IMS auth flow.

## Documents in this section

- [DEPLOY.md](DEPLOY.md) — full Cloud Manager pipeline
  configuration and deployment runbook.
- [OSGI_CONFIG.md](OSGI_CONFIG.md) — every OSGi configuration
  the modernizer reads at runtime.
- [DISPATCHER.md](DISPATCHER.md) — Apache HTTP Server +
  Dispatcher vhost config for serving the dashboard.
- [IMS_AUTH.md](IMS_AUTH.md) — IMS OAuth Server-to-Server flow
  for the AEM Author / Publish connectors.
- [CONNECTORS.md](CONNECTORS.md) — the `AemClient` interface,
  the real and mock implementations, and the URL guard.
- [REPO_INIT.md](REPO_INIT.md) — the `ui.config` package
  layout, the Repo Init scripts, and the `JcrStore` persistence
  path under `/var/aem-eds-modernizer/projects/`.

## How AEM fits in the architecture

The modernizer is **deployed as part of the AEM application**,
not as a sidecar service. The dashboard is served from AEM
Author at `/bin/aem-eds-modernizer/*`; the API at
`/bin/aem-eds-modernizer/api/*`. The Dispatcher caches the
dashboard HTML and proxies the API back to AEM Author.

See [../architecture/RUNTIME_TOPOLOGY.md](../architecture/RUNTIME_TOPOLOGY.md)
for the full deployment topology.
