# GitHub Operations Runbook

The operator's runbook for reviewing, testing, and merging
the modernizer's PR.

## When the PR is opened

The modernizer's `PublishingAgent` (or Phase 2's
`AdvancedRolloutAgent`) opens a PR with the body from
[PR_TEMPLATE.md](PR_TEMPLATE.md). The PR includes a
checklist of items the operator should review.

## Step 1: Read the dashboard

Open the dashboard URL in the PR body. The dashboard shows
the full migration state: dry run, real run, AI decisions,
generated files, issues, validations.

## Step 2: Review the virtual diff

Open the dashboard's `#/diff` view. The diff shows every
generated file: the section models, the blocks, the repo
scaffold.

For each file, ask:

- Is the content correct? (Sample a few pages.)
- Is the block selection correct? (Compare to the Figma
  component map.)
- Is the styling correct? (Compare to the Figma tokens.)
- Are the asset references correct? (Check a few image
  URLs.)

## Step 3: Review the preview

The PR body includes a link to the EDS preview URL. Open it
in a browser and click through the migrated pages.

For each page, ask:

- Does the page render?
- Does the page match the AEM original visually?
- Do the interactive elements work? (Buttons, forms,
  accordions.)
- Does the page pass axe-core? (Run the axe DevTools
  extension.)

## Step 4: Review the validations

Open the dashboard's `#/validation` view. Each page has a
list of validation results, broken down by kind
(`CONTENT`, `VISUAL`, `FUNCTIONAL`, `SEO`, `ACCESSIBILITY`).

For each failed validation, ask:

- Is the failure real? (The mock mode may produce false
  positives.)
- Is the failure critical? (Severity is recorded on the
  `ValidationResultRecord`.)
- Should the migration proceed? (If the failure is in a
  high-traffic page, the operator may want to fix it before
  merging.)

## Step 5: Review the issues

Open the dashboard's `#/issues` view. Each issue has a
severity and a recommended action.

For each `CRITICAL` issue, fix it before merging. For
`HIGH` and `MEDIUM` issues, decide based on context.

## Step 6: Review the cost

The PR body's summary table shows the AI cost. The
dashboard's `#/ai-activity` view shows the per-agent cost
breakdown.

If the cost is significantly higher than the pre-implementation
estimate, the operator may want to investigate (e.g. by
reviewing the `AIDecision`s in the dashboard).

## Step 7: Merge the PR

If the PR is approved:

1. Click "Merge" in the GitHub UI.
2. Choose "Squash and merge" (or "Rebase and merge",
   depending on the branch protection rules).
3. The EDS pipeline deploys the merged branch to the
   production URL.

## Rollback

If the merged PR causes a production issue:

1. Revert the merge commit in the GitHub UI.
2. The EDS pipeline deploys the revert to the production
   URL.
3. Open a new migration to fix the issue.

Reverting does not delete the modernizer branch; the
operator can re-run the migration on top of the revert.

## Related

- [BRANCH_POLICY.md](BRANCH_POLICY.md) — the branch naming
  convention.
- [PR_TEMPLATE.md](PR_TEMPLATE.md) — the PR body template.
- [../operations/RECOVERY.md](../operations/RECOVERY.md) —
  the broader recovery runbook.
