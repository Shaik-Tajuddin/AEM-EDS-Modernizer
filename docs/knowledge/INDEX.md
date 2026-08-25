# AEM EDS Knowledge Base — Index

This directory contains the complete AEM Edge Delivery Services knowledge base for Cursor IDE.

## Directory Structure

### `reference-blocks/` — 4 Complete Reference Implementations
| Block | Files | Description |
|-------|-------|-------------|
| `text-callout/` | JS, CSS, JSON, HTML, README | **Canonical reference** — use as baseline for all new blocks |
| `simple-cta/` | JS, CSS, JSON, HTML, README | Simple call-to-action button |
| `hero/` | JS, CSS, JSON | Hero banner block |
| `product-hero/` | JS, CSS, JSON, README | Product-specific hero variant |

### `utilities/` — Shared Helper Libraries
| File | Description |
|------|-------------|
| `block-helpers.js` | Production utility functions (extractors, responsive, grouping, toggle, env) |
| `HELPERS_GUIDE.md` | API reference for all helper functions |
| `USAGE_EXAMPLES.md` | Real-world usage examples |

### `boilerplate/` — Adobe xwalk Boilerplate Core
| File | Description |
|------|-------------|
| `scripts.js` | Page orchestrator (loadEager/Lazy/Delayed, decorateMain) |
| `aem.js` | AEM framework (RUM, block loading, decorators) |
| `component-definition.json` | Component definitions for Universal Editor |
| `component-models.json` | Component models for Universal Editor |
| `package.json` | Build scripts and dependencies |

### `documentation/` — 15 Comprehensive Guides
| File | Topic |
|------|-------|
| `00-HOLISTIC_VISION.md` | Project overview, 19-block design system vision |
| `01-FUNDAMENTALS.md` | Core EDS concepts, sections, blocks, decoration |
| `02-JSON_CONFIGURATION.md` | Complete JSON model/definition/filter patterns |
| `03-BLOCK_JAVASCRIPT_PATTERN.md` | Standard JS implementation pattern |
| `05-CSS_STYLING_APPROACH.md` | Basic CSS conventions (current) |
| `06-BLOCK_DESIGN_PHILOSOPHY.md` | Modularity, authorability, styleability |
| `07-FOUNDATIONAL_BLOCKS.md` | 19 planned blocks with categories |
| `08-BLOCK_SPECIFICATIONS.md` | Detailed specs for key blocks |
| `09-ANALYTICS_PATTERN.md` | Analytics system (reference only — NOT used) |
| `10-PROJECT_STRUCTURE.md` | Repository organization and file conventions |
| `11-IMPROVEMENTS_TO_REFERENCE.md` | Enhancements over xwalk boilerplate |
| `12-DEVELOPMENT_PATTERNS.md` | Code standards and best practices |
| `13-RESPONSIVE_DESIGN_STRATEGY.md` | Mobile-first responsive approach |
| `14-FUTURE_ROADMAP.md` | Phased enhancement plan |
| `16-BLOCK_DEVELOPMENT_TEMPLATE.md` | Copy-paste template for new blocks |

### `analysis/` — Deep Analysis & Learnings
| File | Topic |
|------|-------|
| `DEEP_DIVE_ANALYSIS.md` | Comprehensive project analysis report |
| `LEARNINGS.md` | Cumulative corrections through 5 phases |
| `SESSION_SUMMARY.md` | Key patterns and architectural decisions |
| `CHANGELOG.md` | Architectural evolution changelog |
| `MIGRATION_GUIDE.md` | SCSS → CSS migration, file renaming |
| `BLOCK_CREATION_STANDARDS.md` | Block Quad requirements |
| `aem_element_grouping_analysis.md` | Underscore naming / element grouping |
| `AEM_EDS_Tabs_and_JSONMerge_Complete_Guide.md` | Tabs implementation & merge-json-cli |
