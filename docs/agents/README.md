# Agents

Every agent in the AEM → EDS Modernizer has its own doc in this
directory. The docs are organised by agent family: connection,
discovery, analysis, design, planning, building, migration,
authoring, preview, validation, repair, publishing, verification.

## Conventions

Every agent doc follows the same structure:

- **Stage** — the `MigrationState` in which the agent runs
- **Inputs** — what the agent reads (other records, connectors)
- **Outputs** — what the agent writes (records, generated files)
- **AI usage** — which `ChatRequest` types the agent issues
- **Failure modes** — how the agent fails and how the
  orchestrator handles it
- **Phase** — Phase 1 (basic) or Phase 2 (advanced)

## Agent index

### Connection

- [ConnectionAgent](ConnectionAgent.md) — Phase 1

### Discovery

- [DiscoveryAgent](DiscoveryAgent.md) — Phase 1

### Analysis

- [ComponentIntelligenceAgent](ComponentIntelligenceAgent.md) — Phase 1
- [ComponentMappingAgent](ComponentMappingAgent.md) — Phase 1
- [TemplateAnalysisAgent](TemplateAnalysisAgent.md) — Phase 1
- [ContentAnalysisAgent](ContentAnalysisAgent.md) — Phase 1
- [AssetAnalysisAgent](AssetAnalysisAgent.md) — Phase 1
- [ContentFragmentAnalysisAgent](ContentFragmentAnalysisAgent.md) — Phase 1
- [MsmAnalysisAgent](MsmAnalysisAgent.md) — Phase 1

### Design

- [FigmaAnalysisAgent](FigmaAnalysisAgent.md) — Phase 1
- [AdvancedFigmaIntelligenceAgent](AdvancedFigmaIntelligenceAgent.md) — Phase 2

### Planning

- [MigrationPlannerAgent](MigrationPlannerAgent.md) — Phase 1

### Building

- [BlockGenerationAgent](BlockGenerationAgent.md) — Phase 1
- [CodeGenerationAgent](CodeGenerationAgent.md) — Phase 1

### Migration

- [ContentMigrationAgent](ContentMigrationAgent.md) — Phase 1

### Authoring

- [AuthoringAgent](AuthoringAgent.md) — Phase 1
- [AuthoringStrategyRegistry](AuthoringStrategyRegistry.md) — Phase 2

### Preview

- [PreviewAgent](PreviewAgent.md) — Phase 1

### Validation

- [ValidationAgent](ValidationAgent.md) — Phase 1
- [VisualValidationAgent](VisualValidationAgent.md) — Phase 1
- [AdvancedVisualValidationAgent](AdvancedVisualValidationAgent.md) — Phase 2

### Repair

- [SelfRepairAgent](SelfRepairAgent.md) — Phase 1
- [AdvancedRepairAgent](AdvancedRepairAgent.md) — Phase 2

### Publishing

- [PublishingAgent](PublishingAgent.md) — Phase 1
- [AdvancedRolloutAgent](AdvancedRolloutAgent.md) — Phase 2

### Verification

- [VerificationAgent](VerificationAgent.md) — Phase 1
