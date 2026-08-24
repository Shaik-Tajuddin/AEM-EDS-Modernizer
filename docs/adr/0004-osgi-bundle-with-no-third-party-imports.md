# ADR 0004 — OSGi Bundle with No Third-Party Imports

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Build configuration

## Context

AEM Cloud projects can be built in two patterns:

1. **"uber-jar" pattern** — the bundle imports public OSGi/Sling/JCR
   APIs and the third-party dependencies (Jackson, OkHttp, SLF4J,
   etc.) are provided by the AEM runtime.
2. **"fat-jar" pattern** — the bundle embeds its third-party
   dependencies and the AEM runtime provides only OSGi/Sling/JCR.

The Master Prompt says the modernizer should follow AEM Cloud
conventions. Adobe's documentation is explicit: prefer the uber-jar
pattern unless you have a hard reason to bundle dependencies (e.g.
you need a Jackson version newer than what AEM ships with).

## Decision

The modernizer follows the **uber-jar pattern**:

- `core` compiles against `org.osgi.framework`,
  `org.osgi.service.component`, `org.apache.sling.api`,
  `org.apache.sling.servlets.annotations`, `javax.jcr`, and
  `javax.servlet` only.
- The bundle's import-package policy is
  `*; resolution:=optional` for everything except OSGi/Sling/JCR.
- The `maven-shade-plugin` produces a *separate* fat jar
  (`core-0.1.0-SNAPSHOT-standalone.jar`) used by the standalone
  runtime only.
- The OSGi bundle is published as
  `core/target/core-0.1.0-SNAPSHOT.jar` and embedded in
  `ui.apps` (the AEM Cloud deployment shape).

## Consequences

### Positive

- **No version conflicts with the AEM runtime.** Jackson, OkHttp,
  SLF4J, etc. are all provided by AEM; the modernizer uses the
  versions AEM ships with.
- **Smaller bundle.** The OSGi bundle is ~1 MB instead of 10 MB.
- **No `Bundle-RequiredExecutionEnvironment` surprises.** AEM
  Cloud runs on JDK 11; the bundle declares
  `Bundle-RequiredExecutionEnvironment: JavaSE-11`.

### Negative

- **Third-party API changes can break us.** If AEM upgrades
  Jackson from 2.16 to 2.18, any deprecated API our code uses
  must be migrated. Mitigated by the modernizer compiling against
  the AEM-public-API surface, which is stable.
- **Local development needs the AEM SDK or the standalone
  runtime.** The OSGi bundle cannot run on its own; that's
  expected and is the same constraint every AEM Cloud project
  has.

## Alternatives considered

- **Fat-jar pattern for the OSGi bundle** (rejected): would
  conflict with AEM's own Jackson / OkHttp versions and make the
  bundle hard to evolve.
- **"Use only the JDK"** (rejected): would force us to write
  HTTP/JSON by hand and lose a major productivity win.

## Related

- [../architecture/COMPONENTS.md](../architecture/COMPONENTS.md) —
  the build and packaging layout.
- [RUNTIME_TOPOLOGY.md](../architecture/RUNTIME_TOPOLOGY.md) — the
  standalone runtime uses the fat-jar variant.
