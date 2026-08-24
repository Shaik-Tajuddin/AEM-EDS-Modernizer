# PreviewAgent

> Deploys the generated files to the EDS preview URL so the
> validation agents can run against a live site.

- **Stage:** `PREVIEWING`
- **Phase:** 1
- **Agent name:** `preview`
- **Task type:** `PREVIEW`

## Inputs

- Every `GeneratedFileRecord` from the building + migrating
  stages.
- The project's `edsPreviewUrl`.

## Outputs

- A deployed EDS preview site.
- A `JobEventRecord` with the preview URL and the deploy
  timestamp.

## AI usage

None. Pure deployment.

## Failure modes

- **EDS preview unreachable:** the agent records a `CRITICAL`
  issue and the migration is blocked.
- **Generated file fails to deploy** (e.g. invalid CSS): the
  agent records a `HIGH` issue with the file path and
  continues.
