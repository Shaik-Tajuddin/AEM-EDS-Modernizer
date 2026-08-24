# PR Template

The PR body template the `PublishingAgent` produces.

## The template

```markdown
## Modernize {projectName} to EDS

This PR migrates **{pageCount}** pages from AEM to EDS, producing
**{blockCount}** distinct blocks.

**Job ID:** `{jobId}`
**Project ID:** `{projectId}`
**Branch:** `modernizer/{projectId}/{jobId}`

---

### Summary

| Metric | Value |
|---|---|
| Pages migrated | {pageCount} |
| Components analysed | {componentCount} |
| Distinct EDS blocks | {blockCount} |
| AI requests | {aiRequestCount} |
| AI cost | ${aiCostUsd} |
| Validations passed | {validationPassed} |
| Validations failed | {validationFailed} |
| Repairs applied | {repairCount} |
| Rollout stages | {rolloutStageCount} |

### Dashboard

The full migration report is at:
`{dashboardUrl}`

### Checklist

- [ ] Preview URL reviewed
- [ ] Visual diff reviewed (`#/diff`)
- [ ] Validations reviewed (`#/validation`)
- [ ] Issues reviewed (`#/issues`)
- [ ] Cost vs estimate reviewed

### Test plan

- [ ] Open the preview URL and click through the migrated pages
- [ ] Run axe-core on the preview URL
- [ ] Verify the URL redirects (`#/redirects`)
- [ ] Verify the dependency graph (`#/dependencies`)
- [ ] Verify the rollout stages (`#/rollout`)

### Related

- Closes #{issueNumber} (if any)
- Supersedes #{previousPrNumber} (if any)
```

## How the template is filled

The `PublishingAgent` reads:

- `projectName` from `ProjectRecord.name`.
- `pageCount` from `SiteInventory.pages`.
- `componentCount` from `SiteInventory.components`.
- `blockCount` from the distinct `targetBlock` values in
  `ComponentMappingRecord`s.
- `aiRequestCount`, `aiCostUsd` from `AIDecision`s.
- `validationPassed`, `validationFailed` from
  `ValidationResultRecord`s.
- `repairCount` from `RepairAttemptRecord`s.
- `rolloutStageCount` from `RolloutStageRecord`s (Phase 2).
- `dashboardUrl` from the project's dashboard URL.

## Customisation

The template is defined in
`com.adobe.aem.modernizer.github.PrTemplate`. Operators can
override it by:

1. Creating a `PrTemplate.properties` file in
   `ui.apps/content/jcr_root/apps/aem-eds-modernizer/configs/`.
2. Adding a property `body.template` with the customised
   template.

## Related

- [BRANCH_POLICY.md](BRANCH_POLICY.md) — the branch naming
  convention.
- [OPERATIONS.md](OPERATIONS.md) — the operator's runbook.
