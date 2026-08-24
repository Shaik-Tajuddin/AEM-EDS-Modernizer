# EDS Integration

The AEM → EDS Modernizer generates content for
[Edge Delivery Services (EDS)](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/edge-delivery/overview.html),
Adobe's web publishing platform. This section collects the
EDS-specific documentation: the repository conventions, the
block generation rules, the preview URL pattern, and the
section model schema.

## Documents in this section

- [REPO_CONVENTIONS.md](REPO_CONVENTIONS.md) — the EDS repo
  layout the modernizer produces (`fstab.yaml`, block folder
  structure, content paths).
- [BLOCK_GENERATION.md](BLOCK_GENERATION.md) — the rules
  the `BlockGenerationAgent` follows to produce EDS blocks.
- [SECTION_MODEL.md](SECTION_MODEL.md) — the `.md` section
  model schema the `ContentMigrationAgent` produces.
- [PREVIEW.md](PREVIEW.md) — the EDS preview URL pattern and
  the `PreviewAgent` deploy flow.
- [FRANKLIN_PIPELINE.md](FRANKLIN_PIPELINE.md) — how the
  generated files flow through the EDS pipeline (sidekick →
  preview → publish).

## How EDS fits in the architecture

The modernizer generates the EDS repo in a Git branch (or as
a virtual diff during the Dry Run). The branch is then
deployed to EDS via the GitHub integration. The
`PreviewAgent` deploys to the EDS preview URL so the
validation agents can run against a live site.

```
Modernizer
  │ generates files
  ▼
Git branch (modernizer/{projectId}/{jobId})
  │
  ▼ (operator clicks MIGRATE)
EDS GitHub integration
  │ triggers the sidekick
  ▼
EDS preview URL
  │
  ▼
Validation agents
  │ browser + axe-core
  ▼
Validation results
  │
  ▼ (operator approves)
EDS production URL
```

## See also

- [../architecture/STATE_MACHINE.md](../architecture/STATE_MACHINE.md) —
  the state machine that drives the EDS flow.
- [../github/](../github/) — how the modernizer talks to
  GitHub.
