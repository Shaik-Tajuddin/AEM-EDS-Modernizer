# GitHub Integration

The AEM → EDS Modernizer talks to GitHub for two purposes:

1. **Target** — push the generated files as commits to a
   branch and open a PR.
2. **Source** (optional) — read the existing EDS repo to
   understand the current state.

This section collects the GitHub-specific documentation: the
auth model, the branch policy, the PR template, and the
operational runbook for reviewing and merging the PR.

## Documents in this section

- [AUTH.md](AUTH.md) — GitHub App vs PAT, and which to use.
- [BRANCH_POLICY.md](BRANCH_POLICY.md) — the branch naming
  convention and the protection rules the modernizer
  expects.
- [PR_TEMPLATE.md](PR_TEMPLATE.md) — the PR body template the
  `PublishingAgent` produces.
- [OPERATIONS.md](OPERATIONS.md) — the operator's runbook for
  reviewing, testing, and merging the modernizer's PR.

## How GitHub fits in the architecture

The modernizer's GitHub integration is the bridge between
the migration engine and the EDS pipeline. The flow:

1. The `CodeGenerationAgent` and `ContentMigrationAgent`
   produce `GeneratedFileRecord`s.
2. The `PublishingAgent` (or the Phase 2
   `AdvancedRolloutAgent`) pushes a commit and opens a PR.
3. The EDS pipeline (sidekick) picks up the PR and deploys
   to the preview URL.
4. The validation agents run against the preview URL.
5. If the validation passes, the operator merges the PR.
6. The EDS pipeline deploys the merged branch to the
   production URL.

## See also

- [../eds/](../eds/) — the EDS pipeline.
- [../security/SECRETS.md](../security/SECRETS.md) — the
  GitHub token secret model.
