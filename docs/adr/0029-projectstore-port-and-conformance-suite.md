# ADR-0029 — One `ProjectStore` port, one conformance test suite

- **Status:** Accepted
- **Date:** 2026-08-30
- **Scope:** `core/.../persistence/Store.java`, `JcrStore`, `JsonFileStore`, `InMemoryStore`, test suite

## Context

The `Store` interface currently has three implementations:
`JcrStore` (AEM Cloud), `JsonFileStore` (local dev), and
`InMemoryStore` (standalone fallback). They are intended to be
semantically identical, but there is no conformance test that proves
it. Behaviour that works standalone fails in AEM and vice versa, and
only in production.

Additionally, `JsonFileStore` has a crash-safety bug: a plain JSON
file rewrite is not atomic. A kill during write leaves a truncated
file and the project is gone. For 25k-page inventories, a single
JSON document is also the wrong shape — it must be read and written
in its entirety on every mutation.

## Decision

### Rename to `ProjectStore`

Rename the `Store` interface to `ProjectStore` to signal that this is
a **port** (in the ports-and-adapters sense, per ADR-0020). The port
defines the contract; the adapters (`JcrProjectStore`,
`JsonFileProjectStore`, `InMemoryProjectStore`) implement it.

### One abstract conformance test

Write **one abstract conformance test class** that every adapter must
pass:

```java
public abstract class ProjectStoreConformanceTest {
    protected abstract ProjectStore createStore();
    protected abstract void tearDown();

    @Test void saveAndLoadProject() { ... }
    @Test void upsertIsIdempotent() { ... }
    @Test void missingKeyReturnsEmpty() { ... }
    @Test void concurrentWritesAreSerializable() { ... }
    @Test void largePayloadRoundTrip() { ... }
    @Test void eventOrderingPreserved() { ... }
}

class JcrProjectStoreConformanceTest extends ProjectStoreConformanceTest { ... }
class JsonFileProjectStoreConformanceTest extends ProjectStoreConformanceTest { ... }
class InMemoryProjectStoreConformanceTest extends ProjectStoreConformanceTest { ... }
```

Concurrency, idempotent upsert, ordering, missing-key behaviour, and
large-payload handling all get identical assertions. Any divergence
between adapters is a test failure, not a production incident.

### Fix `JsonFileStore` crash safety

Replace the plain file rewrite with an atomic move:

```java
Path tmp = Files.createTempFile(dir, "store", ".json.tmp");
Files.writeString(tmp, json);
Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING);
```

Without this, a kill during write leaves a truncated file and the
project is gone.

### Consider one file per project

For 25k-page inventories, a single JSON document is the wrong shape.
Consider one file per project (keyed by `projectId`) so that
mutations to one project don't require rewriting the entire store.

Alternatively, for the standalone runner, consider SQLite — it gives
atomic writes, concurrent reads, and a single-file deployment without
the overhead of a full database.

## Consequences

### Positive

- Semantic divergence between adapters is caught in CI, not production.
- `JsonFileStore` is crash-safe.
- The port name (`ProjectStore`) communicates intent clearly.
- Adding a new adapter (e.g. `DynamoProjectStore` for a serverless
  future) requires only passing the conformance suite.

### Negative

- The conformance suite is additional test code to maintain.
- The rename from `Store` to `ProjectStore` is a breaking change for
  any downstream code that references the old name.
- One file per project changes the directory layout and requires a
  migration for existing `JsonFileStore` users.

## Related

- [ADR-0020](0020-ports-and-adapters-archunit-enforced.md) —
  ports and adapters enforced by ArchUnit.
- [ADR-0026](0026-project-state-under-var-not-conf.md) —
  the `/var` move (conformance tests verify the new path).
- [ADR-0028](0028-batched-checkpoint-persistence.md) —
  batched writes (conformance tests verify batch behaviour).
