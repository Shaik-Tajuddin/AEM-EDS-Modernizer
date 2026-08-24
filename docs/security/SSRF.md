# SSRF Protection

The SSRF (Server-Side Request Forgery) protection policy.
Every URL the modernizer makes a request to is validated by
the `UrlGuard` before the request is sent.

## Threat model

A malicious or careless operator could pass a URL that:

- Points to `http://169.254.169.254/...` (the AWS metadata
  service) to exfiltrate IAM credentials.
- Points to `http://localhost:...` to attack a sidecar
  service.
- Points to `http://192.168.0.0/16` to attack the internal
  network.
- Points to `http://internal-service.cluster.local/...` to
  attack a Kubernetes-internal service.

The `UrlGuard` blocks all of these.

## Policies

| Policy | Allows | Use |
|---|---|---|
| `PRODUCTION` (default) | Public IPs only | AEM Cloud |
| `STRICT` | Public IPs in the allowlist | Regulated environments |
| `DEVELOPMENT` | Public IPs + loopback + RFC1918 | Standalone runtime |
| `UNRESTRICTED` | All IPs | Tests only |

The policy is set in the OSGi config
(`AemEdsModernizerService.urlGuardPolicy`).

## Blocked ranges (in `PRODUCTION` mode)

- `127.0.0.0/8` — loopback
- `10.0.0.0/8` — RFC1918
- `172.16.0.0/12` — RFC1918
- `192.168.0.0/16` — RFC1918
- `169.254.0.0/16` — link-local (includes the AWS metadata
  service at `169.254.169.254`)
- `224.0.0.0/4` — multicast
- `0.0.0.0/8` — "this network"
- `::1/128` — IPv6 loopback
- `fc00::/7` — IPv6 ULA
- `fe80::/10` — IPv6 link-local

## How the check works

```java
UrlGuard.assertAllowed(url, policy);
```

1. Parse the URL.
2. Resolve the hostname to an IP (DNS lookup).
3. Check the IP against the blocked ranges.
4. If `STRICT`, check the IP against the allowlist.
5. If the IP is allowed, return; otherwise throw a
   `ConnectorException` with a clear reason.

## DNS rebinding

The DNS lookup is performed at check time. A malicious DNS
server could return a public IP for the check and a private
IP for the actual request (DNS rebinding). The `UrlGuard`
mitigates this by:

1. Caching the resolved IP for 5 minutes.
2. Re-checking the IP at request time.
3. Failing if the IP changes between the check and the
   request.

## IPv6 coverage

The MVP covers the IPv6 ranges listed above. A follow-up
adds the IPv6 equivalent of the IPv4 RFC1918 ranges.

## See also

- [../adr/0009-ssrf-protection-on-every-url.md](../adr/0009-ssrf-protection-on-every-url.md) —
  the decision record.
- [../aem/CONNECTORS.md](../aem/CONNECTORS.md) — the
  connectors that use the guard.
