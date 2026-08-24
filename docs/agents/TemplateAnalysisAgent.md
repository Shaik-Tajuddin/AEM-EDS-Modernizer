# TemplateAnalysisAgent

> Analyses AEM templates (cq:template) to extract structure
> rules: allowed components, container hierarchy, page-level
> properties.

- **Stage:** `ANALYZING`
- **Phase:** 1
- **Agent name:** `template-analysis`
- **Task type:** `TEMPLATE_ANALYSIS`

## Inputs

- The `AemPageRecord.template` for every eligible page.
- The AEM template definitions (via `AemClient.getTemplateInfo`).

## Outputs

- `TemplateAnalysisEvent`s (per-template) with:
  - `templatePath`
  - `allowedComponents` (resource types)
  - `containerHierarchy` (parent / child relationships)
  - `pageProperties` (jcr:title, description, etc.)

## AI usage

None in the MVP. The agent uses AEM's template definitions
directly. A follow-up could call AI to suggest
component-to-block mappings at the template level.

## Failure modes

- **Template not found:** the agent records a `HIGH` issue and
  continues with an empty template analysis.
- **Template has no allowed components:** the agent records a
  `MEDIUM` issue and the page is flagged as "low-info" for
  downstream agents.

## Performance

- Pure database queries; no AI cost.
- For 1000 pages with 3 unique templates: < 1 second.
