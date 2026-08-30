# Deploy to AEM Cloud — Dev Environment

This document explains how to deploy the AEM → EDS Modernizer to your
AEM Cloud Service Dev environment and access the dashboard at
`/content/aem-eds-modernizer/home.html`.

## What you'll deploy

The build produces **7 artefacts** in `*/target/`:

| Artefact | Size | Purpose |
|---|---|---|
| `core/target/core-0.1.0-SNAPSHOT.jar` | ~3.7 MB | The OSGi bundle containing the agents, AI gateway, REST API, dashboard SPA, and Sling Model |
| `core/target/core-0.1.0-SNAPSHOT-standalone.jar` | ~10 MB | Fat jar for local development only — **do not upload this** |
| `ui.apps/target/ui.apps-0.1.0-SNAPSHOT.zip` | ~9 KB | HTL template, page component (`/apps/aem-eds-modernizer/components/page/home`, `/apps/aem-eds-modernizer/templates/home`) |
| `ui.config/target/ui.config-0.1.0-SNAPSHOT.zip` | ~7 KB | OSGi config for the modernizer, including the Repo Init that creates the `modernizer-service` user and the ACLs |
| `ui.content/target/ui.content-0.1.0-SNAPSHOT.zip` | ~6 KB | Seed content — the `/content/aem-eds-modernizer/home` page |
| `dispatcher/target/dispatcher-0.1.0-SNAPSHOT.zip` | ~5 KB | Apache HTTP Server + Dispatcher vhost config |
| `all/target/all-0.1.0-SNAPSHOT.zip` | ~5 KB | Convenience container (no subpackage embedding — upload each separately) |

## Option A: Deploy via Cloud Manager (recommended)

1. **Open Cloud Manager** for your AEM Cloud Service program.
2. **Create a new deployment pipeline** (or use an existing one) that
   points at this Git repository. The pipeline must build with:
   ```bash
   mvn clean install
   ```
3. **Run the pipeline** on the `main` branch. Cloud Manager builds
   the artefacts and stores them in its Maven repository.
4. **Deploy to Dev** through the pipeline. Cloud Manager installs
   the packages in the correct order:
   - `core` bundle first (so the Sling Servlet is registered)
   - `ui.apps` (so the HTL template + component are present)
   - `ui.config` (so the service user and ACLs are created)
   - `ui.content` (so the `/content/aem-eds-modernizer/home` page exists)
   - `dispatcher` (so the dashboard can be served at the edge)
5. **Open the dashboard** in your browser:
   ```
   https://author-pXXXXX-eYYYYY.adobeaemcloud.com/content/aem-eds-modernizer/home.html
   ```
6. **Log in** with your AEM credentials (e.g. `admin` or any user with
   `content-authors` group membership).
7. **Click "Create demo project"** to seed a WKND-shaped sample.
8. **Click "Run Dry Run"** to drive the state machine. The dashboard
   populates with 59 pages, 15 components, ~177 URL redirects, ~669
   dependency edges, 5 rollout stages, 23 repair attempts, 34
   benchmark samples.

## Option B: Maven deploy profile (fastest for local SDK)

The Maven build has a `deploy` profile that pushes every artefact
directly to a running AEM instance via HTTP. It's the fastest way
to iterate against a local AEM SDK.

### Local SDK (default)

```bash
# 1. Start AEM SDK on :4502 (admin / admin)
# 2. Build + deploy in one command:
mvn clean install -Pdeploy
```

### Custom host / credentials

```bash
mvn clean install -Pdeploy \
    -Daem.host=https://author-pXXXX-eYYYY.adobeaemcloud.com \
    -Daem.user=modernizer-deploy \
    -Daem.password=$AEM_TOKEN
```

### What the profile does

For each module the right thing happens automatically:

| Module | What the deploy profile does |
|---|---|
| `core` | POSTs the OSGi bundle to `${aem.host}/system/console/bundles` (Felix Web Console) |
| `ui.apps` | POSTs the content package to `${aem.host}/crx/packmgr/service.jsp` with `force=true&install=true` |
| `ui.config` | Same as `ui.apps` (runs the Repo Init on install) |
| `ui.content` | Same as `ui.apps` (creates the seeded home page) |
| `dispatcher` | Same as `ui.apps` (uploads the vhost + farm) |
| `all` | Skipped (the meta container doesn't get deployed on its own) |

The reactor runs in the order `core → ui.apps → ui.config → ui.content →
dispatcher`, so the OSGi bundle is installed **first** (registering the
Sling Servlet) and the content packages follow in dependency order.

### Per-module skip flags

To skip a specific module's deploy step, set the matching flag:

```bash
# Skip deploying ui.content (build only)
mvn clean install -Pdeploy -DskipPackageDeploy=true -pl ui.content

# Skip the bundle install
mvn clean install -Pdeploy -DskipBundleDeploy=true
```

The defaults (in each module's `pom.xml`) are:

| Module | `skipBundleDeploy` | `skipPackageDeploy` |
|---|---|---|
| `core` | **false** (do deploy) | true (skip) |
| `ui.apps` | true (skip) | **false** (do deploy) |
| `ui.config` | true (skip) | **false** (do deploy) |
| `ui.content` | true (skip) | **false** (do deploy) |
| `dispatcher` | true (skip) | **false** (do deploy) |
| `all` | true (skip) | true (skip) |

### Prerequisite: `curl` on PATH

The profile invokes `curl` (the system command, not a Maven plugin).
On macOS, Linux, and most CI runners this is already present. On
Windows, install cURL or use the Git Bash shell.

## Option C: Manual install via local AEM SDK quickstart

If you can't or don't want to use the Maven deploy profile:

1. **Download and run the AEM SDK quickstart** (Author instance on
   port 4502). See
   <https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/local-development-environment-set-up/development-tools.html>.

2. **Build** the modernizer:
   ```bash
   mvn clean install
   ```

3. **Install the OSGi bundle** into the running Author:
   ```bash
   # Bundle (the OSGi bundle)
   curl -u admin:admin -F bundle=@core/target/core-0.1.0-SNAPSHOT.jar \
     http://localhost:4502/system/console/bundles
   ```

   Or use the Felix Web Console UI at
   <http://localhost:4502/system/console/bundles> → "Install/Update".

4. **Install the content packages** via the Package Manager:
   - Open <http://localhost:4502/crx/packmgr/index.jsp>
   - Upload and install each of:
     - `ui.apps/target/ui.apps-0.1.0-SNAPSHOT.zip`
     - `ui.config/target/ui.config-0.1.0-SNAPSHOT.zip` (this runs the
       Repo Init script that creates the `modernizer-service` user)
     - `ui.content/target/ui.content-0.1.0-SNAPSHOT.zip`
     - `dispatcher/target/dispatcher-0.1.0-SNAPSHOT.zip`

5. **Open the dashboard**:
   ```
   http://localhost:4502/content/aem-eds-modernizer/home.html
   ```

## What you should see

On a successful install, you have **three URLs** that should all
show the dashboard:

1. **The seeded page (recommended)**: `/content/aem-eds-modernizer/home.html`
2. **The shortcut Sling Servlet**: `/aem-eds-modernizer`
3. **The API directly**: `/bin/aem-eds-modernizer/api/projects`

### URL 1 and 2

URLs 1 and 2 are both served by the `ModernizerHomeServlet` Sling
Servlet (registered in the `core` bundle). They write the full
dashboard SPA to the response, including the `<base href="...">` tag
that points all `fetch()` calls at the API.

### URL 3

URL 3 (`/bin/aem-eds-modernizer/api/projects`) returns a JSON list
of projects. It's served by the `DashboardApi` Sling Servlet. This
endpoint is used by the SPA's `viewProjects()` function.

## If the home page is empty

See [TROUBLESHOOTING_HOME_PAGE.md](TROUBLESHOOTING_HOME_PAGE.md) for
a step-by-step diagnostic guide. The most common cause is that the
`core` bundle isn't active in the AEM OSGi container; the
`ModernizerHomeServlet` won't be registered without it.

- **Header**: `AEM → EDS Modernizer` brand, project pill, job pill,
  `MOCK MODE` indicator.
- **Sidebar**: a nav with sections for Project, Connect, Dry Run,
  Inventory, Run, Phase 2, etc.
- **Main area**: starts on the `#/projects` view with a "Create demo
  project" button. After clicking, the project pill shows the UUID,
  and the dashboard navigates to `#/connections` (which posts to
  `/bin/aem-eds-modernizer/api/projects/{id}/test-connections` and
  renders the connection cards).

The Mock mode is on by default — every connector returns success,
and the AI provider is the deterministic in-memory `MockAiProvider`,
so the full state machine runs in seconds without any external API
keys.

## Disabling mock mode

To switch to real mode (real AEM Author, real GitHub, real AI):

1. **Update the OSGi config** at
   `apps/aem-eds-modernizer/configs/com.adobe.aem.modernizer.osgi.ModernizerBundleActivator~<id>.config`
   in `ui.config` to set:
   ```json
   {
     "mockMode": false
   }
   ```
2. **Configure the secret references** (per the secret model — see
   `docs/security/SECRETS.md`):
   - AEM IMS client id/secret
   - GitHub App private key + installation id (or PAT)
   - Figma PAT
   - AI provider API keys (Anthropic/OpenAI/Gemini)
3. **Reinstall `ui.config`**.

## Troubleshooting

### "Page not found" on /content/aem-eds-modernizer/home.html

- Verify `ui.content` package is installed:
  - Package Manager → `aem-eds-modernizer.ui.content`
  - It should show the `home` page node under `/content/aem-eds-modernizer/`.
- Verify the home page's `jcr:content` has
  `sling:resourceType = aem-eds-modernizer/components/page/home`.

### Dashboard renders but API calls return 404

- The `core` bundle may not be active. Check
  <http://localhost:4502/system/console/bundles> → look for
  `com.adobe.aem.modernizer.core` (Active).

### Dashboard renders but agents fail

- Check the Sling log
  (`/logs/error.log` for local, Cloud Manager → Logs for cloud):
  ```
  grep "ModernizerBundleActivator" error.log
  ```
- Verify the `modernizer-service` user exists in the user admin
  (<http://localhost:4502/useradmin>).

## Architecture overview

```
┌─ AEM Cloud Author ─────────────────────────────┐
│                                                │
│  /content/aem-eds-modernizer/home             │
│      ↓ (HTL template)                          │
│  /apps/aem-eds-modernizer/components/page/home │
│      ↓ (Sling Model: HomePageModel)            │
│  /bin/aem-eds-modernizer/api/* (Sling Servlet) │
│      ↓ (ApiRouter)                             │
│  Orchestrator → Agents → AiGateway → MockAI    │
│      ↓                                         │
│  JcrStore → /var/aem-eds-modernizer/projects/{yyyy}/{MM} │
│                                                │
└────────────────────────────────────────────────┘
```

The HTL template renders the SPA inline; the SPA's `<base>` tag points
all relative `fetch()` calls at the Sling Servlet. ACLs on
`/content/aem-eds-modernizer` and `/var/aem-eds-modernizer` allow
`content-authors` group members to read; `modernizer-service` (the
bundle's run-as user) has `jcr:all` on `/var/aem-eds-modernizer`
for project state persisted by `JcrStore`.
