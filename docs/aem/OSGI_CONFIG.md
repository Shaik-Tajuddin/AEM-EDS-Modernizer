# OSGi Configuration

The AEM → EDS Modernizer reads the following OSGi
configurations at runtime. All configurations are
**factory configurations** (one per project), so multiple
projects can coexist with different settings.

## `AemEdsModernizerProject` (factory)

One configuration per project, with PID
`com.adobe.aem.modernizer.osgi.AemEdsModernizerProject~{name}`.

| Property | Type | Required | Description |
|---|---|---|---|
| `projectId` | String | yes | The project UUID |
| `aemAuthorUrl` | String | yes | AEM Author URL (e.g. `https://author-pXXXX-eYYYY.adobeaemcloud.com`) |
| `aemPublishUrl` | String | yes | AEM Publish URL |
| `aemAuthReference` | String | yes | Secret reference for IMS auth (e.g. `aem:/path/to/secret`) |
| `contentRoot` | String | yes | AEM content root (e.g. `/content/wknd`) |
| `markerProperty` | String | no | The marker property name (default: `modernizer.migrate`) |
| `markerValue` | String | no | The marker property value (default: `true`) |
| `markerPolicy` | String | no | `MARKED_ONLY` / `MARKED_AND_EXPLICIT_SELECTION` / `EXPLICIT_SELECTION_ONLY` (default: `MARKED_ONLY`) |
| `aiProvider` | String | no | The default AI provider (default: `mock` in standalone, `anthropic` in real) |
| `aiRoutingPolicy` | String | no | JSON-encoded `AiRoutingPolicy` (overrides the default) |
| `rolloutPolicy` | String | no | JSON-encoded `RolloutPolicy` (overrides the default) |
| `authoringStrategy` | String | no | `UNIVERSAL_EDITOR` / `DOC_BASED` / `EXISTING_REPO` / `CUSTOM_ADAPTER` (default: `UNIVERSAL_EDITOR`) |

## `AemEdsModernizerService` (singleton)

Singleton service configuration, PID
`com.adobe.aem.modernizer.osgi.AemEdsModernizerService`.

| Property | Type | Default | Description |
|---|---|---|---|
| `mockMode` | Boolean | `false` | If true, the modernizer uses `MockAemClient` etc. even in AEM Cloud. Useful for testing. |
| `localAiOnly` | Boolean | `false` | If true, refuses to dispatch to any external AI provider. |
| `urlGuardPolicy` | String | `PRODUCTION` | `PRODUCTION` / `STRICT` / `DEVELOPMENT` / `UNRESTRICTED` |
| `aemConcurrency` | Integer | `10` | Max concurrent AEM API calls |
| `aiMaxRetries` | Integer | `3` | Max AI call retries with exponential backoff |
| `maxRepairAttempts` | Integer | `5` | Max repair attempts per failed validation |
| `dashboardTitle` | String | `AEM → EDS Modernizer` | The dashboard title (shown in the header) |
| `dashboardTheme` | String | `auto` | `auto` / `light` / `dark` |

## Repo Init

The `ui.config` content package contains a Repo Init script
that creates the configuration nodes on first install. See
[REPO_INIT.md](REPO_INIT.md).

## Where to put a project config

In the OSGi web console (`/system/console/configMgr`), click
on the `AemEdsModernizerProject` factory and create a new
configuration. The `projectId` must be unique.

## Related

- [DEPLOY.md](DEPLOY.md) — full deployment runbook.
- [REPO_INIT.md](REPO_INIT.md) — Repo Init scripts.
