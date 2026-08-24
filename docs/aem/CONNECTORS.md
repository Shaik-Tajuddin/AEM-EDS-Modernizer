# AEM Connectors

The `AemClient` interface and its real and mock
implementations.

## Interface

```java
public interface AemClient {
    List<PageRef> listPages(String contentRoot);
    AemPage getPage(String path);
    List<ComponentUsage> getComponentUsages();
    AssetMetadata getAssetMetadata(String assetPath);
    List<MsmRelation> getMsmInfo(String path);
    TemplateInfo getTemplateInfo(String templatePath);
    List<ContentFragmentRef> listContentFragments(String contentRoot);
    // ... 24 methods total
}
```

The interface is intentionally narrow: the modernizer only
needs the operations documented in the AEM API. Anything
else is out of scope.

## Implementations

| Class | Where it runs | How it talks to AEM |
|---|---|---|
| `RealAemClient` | AEM Cloud, standalone (real mode) | IMS-authenticated HTTPS calls to AEM Author / Publish |
| `MockAemClient` | Standalone (mock mode), unit tests | In-memory seeded fixture |

## URL guard

Every URL the `RealAemClient` makes a request to is
validated by `UrlGuard.assertAllowed(url, policy)`. See
[../security/SSRF.md](../security/SSRF.md).

## Mock fixture

`MockAemClient` is seeded with a deterministic 54-page WKND
fixture (3 templates, 15 components, 2 assets). The seed
can be overridden via the constructor for tests.

## Related

- [IMS_AUTH.md](IMS_AUTH.md) — the IMS auth flow.
- [SSRF.md](../security/SSRF.md) — the URL guard.
