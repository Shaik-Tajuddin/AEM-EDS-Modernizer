# Franklin Pipeline

The generated files flow through the EDS (formerly Franklin)
pipeline:

```
Modernizer
  │ generates files
  ▼
Git branch (modernizer/{projectId}/{jobId})
  │
  ▼ (commit pushed)
GitHub
  │ webhook
  ▼
EDS pipeline (sidekick)
  │ 1. fetch content from fstab mount points
  │ 2. transform .md → section model
  │ 3. render sections with blocks
  │ 4. publish to preview / production
  ▼
EDS preview URL
  │ (preview)
  │ (production, after operator approval)
  ▼
End user
```

## Pipeline stages

1. **Fetch** — EDS reads content from the mount points
   declared in `fstab.yaml`. For the modernizer, this is
   the GitHub repo.
2. **Transform** — `.md` files are parsed into section
   models (see [SECTION_MODEL.md](SECTION_MODEL.md)).
3. **Render** — the section models are rendered into HTML
   by the block decoration functions.
4. **Publish** — the rendered HTML is deployed to the
   preview URL (and to production, after operator
   approval).

## How the modernizer triggers the pipeline

The modernizer triggers the pipeline by pushing a commit to
the GitHub branch. The EDS pipeline listens for GitHub
webhooks and starts processing automatically.

In mock mode, the modernizer simulates the pipeline by
returning a deterministic preview URL
(`https://preview.localhost/{eds-path}`). The mock browser
client returns a 16×16 PNG for any URL on this host.

## Operator workflow

1. Modernizer creates a branch with the generated files.
2. EDS pipeline deploys the branch to a preview URL.
3. Operator opens the preview URL in a browser (or in the
   dashboard's `#/preview` view).
4. Operator reviews the preview.
5. If approved, operator merges the branch into the default
   branch (or runs the `AdvancedRolloutAgent` for a staged
   rollout).
6. EDS pipeline deploys the default branch to the
   production URL.

## Related

- [PREVIEW.md](PREVIEW.md) — the preview URL pattern.
- [REPO_CONVENTIONS.md](REPO_CONVENTIONS.md) — the repo
  layout.
