# EDS Preview

The `PreviewAgent` deploys the generated files to the EDS
preview URL so the validation agents can run against a live
site.

## Preview URL pattern

The EDS preview URL follows the pattern:

```
https://{branch}--{repo}--{org}.hlx.page/{path}
```

For example:

```
https://main--wknd-eds--company.hlx.page/us/en/about
```

The modernizer derives the preview URL from the GitHub
configuration:

- `repo` = the GitHub repo name (e.g. `wknd-eds`).
- `org` = the GitHub org (e.g. `company`).
- `branch` = the modernizer branch
  (`modernizer/{projectId}/{jobId}`).

The `PreviewAgent` triggers a deploy by:

1. Pushing the generated files to the branch.
2. Waiting for the EDS pipeline to pick up the change
   (typically 30-60 seconds).
3. Polling the preview URL until it returns 200.

## Failure modes

- **EDS pipeline is down:** the agent records a `CRITICAL`
  issue and the migration is blocked.
- **Preview URL returns 5xx after 5 minutes:** the agent
  records a `CRITICAL` issue with the last 5xx response
  body.

## How validation uses the preview URL

The `ValidationAgent` and `VisualValidationAgent` (and the
Phase 2 `AdvancedVisualValidationAgent`) construct the
preview URL for each page and pass it to the
`BrowserClient`:

```java
String url = "https://" + branch + "--" + repo + "--" + org + ".hlx.page" + edsPath;
browser.screenshot(url, "desktop", true);
```

The mock mode returns a deterministic 16×16 PNG; the real
mode uses Playwright to take a real screenshot.

## Related

- [FRANKLIN_PIPELINE.md](FRANKLIN_PIPELINE.md) — the EDS
  pipeline that processes the preview.
- [../agents/PreviewAgent.md](../agents/PreviewAgent.md) —
  the agent that deploys the preview.
