# Architecture Decision Records

This directory contains the ADRs (architecture decision records) for
the AEM → EDS Modernizer. Each ADR captures a single architectural
decision: the context, the choice, the consequences, and the
alternatives considered.

## Format

Each ADR follows the [MADR](https://adr.github.io/madr/) template:

- **Status** — proposed / accepted / superseded / deprecated
- **Context** — what problem we were solving
- **Decision** — what we chose
- **Consequences** — the positive and negative outcomes
- **Alternatives considered** — what else we looked at

## Index

| # | Title | Status |
|---|---|---|
| [0001](0001-phase2-advanced-features.md) | Phase 2: Advanced features (Master §33) | Accepted |
| [0002](0002-control-plane-inside-aem.md) | Control plane inside AEM (Master §4) | Accepted |
| [0003](0003-ai-gateway-with-routing-policy.md) | AI gateway with routing policy (Master §60) | Accepted |
| [0004](0004-osgi-bundle-with-no-third-party-imports.md) | OSGi bundle with no third-party imports | Accepted |
| [0005](0005-dry-run-is-mandatory.md) | Dry Run is mandatory (Master §0A) | Accepted |
| [0006](0006-marker-based-eligibility.md) | Marker-based eligibility (Master §33) | Accepted |
| [0007](0007-capability-registry-gate.md) | Capability registry gate on AI dispatch | Accepted |
| [0008](0008-secrets-as-references-only.md) | Secrets as references only (no raw keys) | Accepted |
| [0009](0009-ssrf-protection-on-every-url.md) | SSRF protection on every URL | Accepted |
| [0010](0010-assets-are-metadata-only.md) | Assets are metadata-only, never binaries (Master §20) | Accepted |
| [0011](0011-virtual-diff-not-real-git.md) | Virtual diff, not real Git commits (during dry run) | Accepted |
| [0012](0012-checkpoints-for-resumability.md) | Checkpoints for resumability (Master §40) | Accepted |
| [0013](0013-events-as-source-of-truth.md) | Events as the dashboard's source of truth | Accepted |
| [0014](0014-redactor-on-every-log-and-response.md) | Redactor on every log and response | Accepted |
| [0015](0015-phase-1-and-phase-2-coexist.md) | Phase 1 and Phase 2 agents coexist in the same map | Accepted |

## When to write a new ADR

Write a new ADR when:

- You are about to make a decision that is hard to reverse.
- Two reasonable people would disagree on the right answer.
- The decision will affect how the system is built or operated for
  a long time.
- The decision is visible to operators or users (not just an
  internal implementation detail).

Don't write an ADR for:

- A small implementation detail (e.g. "use SLF4J instead of
  java.util.logging" — that's a trivial library choice).
- A bug fix (those go in commit messages and the changelog).
- A pure refactor with no behaviour change.

## How to number

The next available number is `0016`. The first three digits are
zero-padded for sort order; MADR convention.
