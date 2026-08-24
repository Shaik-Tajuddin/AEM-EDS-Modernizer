# ADR 0009 — SSRF Protection on Every URL

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Security

## Context

The platform accepts URL inputs from operators (AEM Author URL,
Publish URL, Figma URL, EDS preview URL, GitHub repo URL, etc.).
Every URL is fetched, redirected, or proxied by the platform.

A malicious or careless operator could:

- Point the connector at `http://169.254.169.254/...` (the AWS
  metadata service) to exfiltrate IAM credentials.
- Point the connector at `http://localhost:...` to attack a
  sidecar service.
- Point the connector at `http://192.168.0.0/16` to attack the
  internal network.

Per Master §59, every URL must pass an SSRF check before the
connector is allowed to make a request.

## Decision

The `UrlGuard` class wraps every HTTP connector call:

```java
UrlGuard.assertAllowed(url, policy);
```

`policy` is one of:

- `PRODUCTION` (default) — refuses loopback, RFC1918, link-local,
  multicast, and the AWS metadata service. Used in AEM Cloud.
- `STRICT` — also refuses public IPs unless they are in the
  operator's allowlist. Used in regulated environments.
- `DEVELOPMENT` — allows loopback and RFC1918 for local
  development. Used in the standalone runtime.
- `UNRESTRICTED` — allows everything. Used in tests only.

The check happens at the *outer* layer of the connector, so a
malicious URL cannot reach the inner OkHttp call. The check is
synchronous and fast (a single DNS resolution + IP range check).

If the check fails, the connector throws a
`ConnectorException` with a clear reason: `"URL {url} blocked by
SSRF guard (policy={policy}, reason={reason})"`.

## Consequences

### Positive

- **No SSRF.** A misconfigured AEM URL cannot exfiltrate
  credentials or attack the internal network.
- **Configurable per environment.** The standalone runtime uses
  `DEVELOPMENT`; production uses `PRODUCTION`. The choice is
  explicit, not implicit.
- **Fails loud.** A blocked URL is an error, not a silent
  retry.

### Negative

- **DNS resolution cost.** Every URL check performs a DNS lookup.
  The cost is small but non-zero; high-throughput connectors
  cache the result.
- **Operators must understand the policy.** A URL that works in
  `DEVELOPMENT` may be blocked in `PRODUCTION`. The error
  message is clear; the documentation is in
  [../security/SSRF.md](../security/SSRF.md).
- **IPv6 is not fully covered** in the MVP. A follow-up adds
  the IPv6 equivalent of the IPv4 ranges.

## Alternatives considered

- **Network-level isolation** (run the connectors in a sandboxed
  container with no network access to the internal range):
  rejected because it complicates the AEM Cloud deployment.
- **No SSRF check** (rely on operator good faith): rejected
  per Master §59.

## Related

- [../security/SSRF.md](../security/SSRF.md) — full SSRF policy
  and runbook.
- [ADR 0008](0008-secrets-as-references-only.md) — the secret
  model that pairs with SSRF protection.
