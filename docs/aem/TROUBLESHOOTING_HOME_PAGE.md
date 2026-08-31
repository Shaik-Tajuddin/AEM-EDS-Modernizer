# Troubleshooting the Home Page

If `/content/aem-eds-modernizer/home.html` (or `/aem-eds-modernizer`)
renders empty or shows an AEM error page, follow this guide.

## Quick test: try the shortcut URL first

Before anything else, try the **shortcut URL** that does NOT require
the seeded page to be installed:

```
http://localhost:4502/content/aem-eds-modernizer/home.html
```

(or its Cloud Manager equivalent: `https://author-pXXXX-eYYYY.adobeaemcloud.com/aem-eds-modernizer`)

This URL is served by the `ModernizerHomeServlet` Sling Servlet
registered at the path `/aem-eds-modernizer` — it's a plain Servlet
path, no AEM page required. If this works, the bundle is correctly
installed and the dashboard is functional; only the seeded
`/content/aem-eds-modernizer/home` page has a problem.

## Step 1: check the bundle is active

Open the OSGi console:

- Local: <http://localhost:4502/system/console/bundles>
- Cloud: `https://author-pXXXX-eYYYY.adobeaemcloud.com/system/console/bundles`
  (Cloud Manager → environment → developer console)

Search for `com.adobe.aem.modernizer.core`. It should be **Active**.
If it's not:

| State | Likely cause | Fix |
|---|---|---|
| Installed | Missing dependencies | Check the error message: probably a Sling/JCR/Servlet API version mismatch. Open the bundle's "Resolve" tab. |
| Resolved | Bundle could not start | Look at the bundle's "Errors" tab; usually a NoClassDefFoundError. |
| Active | The OSGi component isn't being activated | See the modernizer logs (next step). |

## Step 2: check the modernizer logs

In Cloud Manager, look for the modernizer-specific log lines:

- AEM Cloud: Cloud Manager → Logs → filter on `ModernizerBundleActivator`
- Local: `/crx-quickstart/logs/error.log` filtered on `modernizer`

You should see lines like:

```
INFO  ModernizerBundleActivator activating (mockMode=true)
INFO  Registered AI provider: mock
INFO  ModernizerBundleActivator activated with 26 agents
```

If you see `WARN ModernizerHomeServlet: ... SlingSafeMethodsServlet` or
`ERROR ApiRouter: API error for /api/projects: ...`, the issue is in
the wiring.

## Step 3: check the seeded page

Open CRXDE Lite (Local: <http://localhost:4502/crx/de/index.jsp>;
Cloud Manager → CRXDE Lite) and navigate to:
`/content/aem-eds-modernizer/home`

The `jcr:content` node should have:
- `jcr:primaryType = cq:PageContent`
- `sling:resourceType = aem-eds-modernizer/components/page/home`
- `cq:template = /apps/aem-eds-modernizer/templates/home`

If those properties are wrong, reinstall the `ui.content` package.

## Step 4: test the API directly

Test the JSON API without involving the page:

```
curl -u admin:admin http://localhost:4502/bin/aem-eds-modernizer/api/health
```

Expected response:
```json
{"status":"ok","time":1700000000000}
```

If this fails with 404, the `DashboardApi` Sling Servlet at
`/bin/aem-eds-modernizer/*` is not registered. The most likely cause
is the `core` bundle not being active (Step 1).

If this works, the page-rendering issue is isolated to the HTL/Sling
Models pipeline — and the `ModernizerHomeServlet` should bypass it.

## What I changed to fix the "empty home page" problem

1. **Added `ModernizerHomeServlet`** — a Sling Servlet that writes the
   full SPA HTML to the response at:
   - `/content/aem-eds-modernizer/home.html` (via `sling:resourceType`
     binding to `aem-eds-modernizer/components/page/home`)
   - `/aem-eds-modernizer` and `/aem-eds-modernizer/` (plain Servlet
     paths, work even without the seeded page)
2. **Made the home HTL script `home.html` a no-op** — it's never
   executed because the servlet matches first.
3. **Added `StaticDashboardUse`** — a Sling Use object as a
   fallback that the home component can use if needed.
4. **The seeded page** still has `sling:resourceType =
   aem-eds-modernizer/components/page/home`, so the servlet is
   matched by resource type when the URL is
   `/content/aem-eds-modernizer/home.html`.

## Why the original approach failed

The original setup was a `cq:Component` + HTL script. For AEM's
page-rendering pipeline to wrap the output in a full HTML document,
the page component needs `sling:resourceSuperType = wcm/foundation/components/page`
(or one of the WCM Core Components equivalents). Without that, AEM
either renders an empty body (which modern AEM rejects as a scripting
error) or fails to resolve the Sling Model that the HTL used.

The `ModernizerHomeServlet` bypasses this entirely by writing the
full SPA document directly to the response.

## If you still see an error

Look in the AEM error log (`/logs/error.log` for local, Cloud Manager
→ Logs for cloud) for the *exact* error message. Common ones:

| Error | Cause | Fix |
|---|---|---|
| `javax.servlet.ServletException: Cannot serve request to /content/aem-eds-modernizer/home` | Page component not found | Reinstall `ui.apps` (contains the component + HTL). |
| `java.lang.ClassNotFoundException: com.adobe.aem.modernizer.dashboard.servlets.ModernizerHomeServlet` | The `core` bundle is not active | See Step 1. |
| `org.apache.sling.api.SlingException: No script found for /aem-eds-modernizer` | The Sling Servlet isn't registered | The `core` bundle is not active. See Step 1. |
| `java.lang.IllegalStateException: Could not activate component` | Repo Init failed | Check the `Repo Init` log: usually a service user creation error. |
| `OakName0001: Prefix 'eds' not present in namespace registry` | `JcrStore` failed because the `eds` namespace is not registered | Ensure `ui.config` is deployed (it contains the `register namespace (eds)` Repo Init line). Repo Init runs at startup; if `core` is deployed before `ui.config`, namespace registration may not have run yet. Check `/system/console/status-repoinit` for errors. |
| Blank page with `404` status | Wrong URL | Use the full URL: `/content/aem-eds-modernizer/home.html` (with `.html`). |
