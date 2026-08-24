# Branch Policy

The branch naming convention and the protection rules the
modernizer expects.

## Branch naming

The modernizer uses the pattern:

```
modernizer/{projectId}/{jobId}
```

For example:

```
modernizer/217b186e-406a-4162-bea1-49279a3b7cdf/5347d0d1-2475-4122-93f3-120bbcf2f7ed
```

The branch is created from the configured default branch
(typically `main`). The `PublishingAgent` pushes all
`GeneratedFileRecord`s as a single commit (or a small
number of logical commits) to this branch.

## Branch protection

The modernizer expects the following branch protection
rules on `main`:

| Rule | Why |
|---|---|
| Require pull request reviews before merging | The modernizer is read-only; the operator must approve |
| Require status checks to pass before merging | CI must pass |
| Require linear history | Rebase or squash, no merge commits |
| Do not allow force pushes | The modernizer's commits must not be rewritten |
| Do not allow deletions | The branch is a record of the migration |

The modernizer does **not** enforce these rules; the
operator's GitHub configuration does. If the rules are not
in place, the modernizer records a `MEDIUM` issue at the
start of the migration.

## Default branch

The modernizer reads the default branch from the GitHub
API (`GET /repos/{owner}/{repo}`). The branch can be
overridden in the OSGi config (`githubDefaultBranch`).

## Multi-migration branches

If the operator runs multiple migrations against the same
repo, each migration gets its own branch (because each
migration has its own `jobId`). The branches coexist
without conflict.

If the operator wants to compare two migrations, the
dashboard's `#/diff` view shows the virtual diff for each
job, side-by-side.

## Cleanup

Branches are not automatically deleted. The operator can
delete a branch after merging the PR (via the GitHub UI)
or after deciding not to merge (via `git push origin
--delete modernizer/{projectId}/{jobId}`).

The modernizer has a "delete old branches" tool in the
dashboard's `#/github` view (planned for Phase 3) that
deletes all `modernizer/*` branches older than 30 days.

## Related

- [AUTH.md](AUTH.md) — the auth model.
- [PR_TEMPLATE.md](PR_TEMPLATE.md) — the PR body template.
