# Security

The AEM → EDS Modernizer's security posture: the secret
model, SSRF protection, the redactor, the audit log, and
the capability gate.

## Documents in this section

- [SECRETS.md](SECRETS.md) — the secret model (references
  only, no raw keys).
- [SSRF.md](SSRF.md) — the SSRF protection policy.
- [REDACTOR.md](REDACTOR.md) — the redactor that strips
  secrets from logs and responses.
- [AUDIT.md](AUDIT.md) — the audit log (every
  `JobEventRecord` includes the actor).
- [RBAC.md](RBAC.md) — the role-based access control model
  (inherits from AEM ACLs).
- [CAPABILITY_GATE.md](CAPABILITY_GATE.md) — the AI
  capability registry gate.

## How security fits in the architecture

Security is a **cross-cutting concern**: every layer of the
modernizer has security responsibilities.

```
┌─────────────────────────────────────────┐
│  ApiRouter                              │ RBAC (Sling ACLs)
├─────────────────────────────────────────┤
│  Agent                                  │ Capability gate
├─────────────────────────────────────────┤
│  AiGateway                              │ Secret model + redactor
├─────────────────────────────────────────┤
│  Connector (AEM, GitHub, Figma, EDS)    │ SSRF guard + secret model
├─────────────────────────────────────────┤
│  Logger (SLF4J)                         │ Redactor
├─────────────────────────────────────────┤
│  Store (JCR or InMemory)                │ Audit log
└─────────────────────────────────────────┘
```

## Threat model

The modernizer faces the following threats:

1. **Secret leakage.** A secret ends up in source, JCR, logs,
   or the browser. Mitigated by the secret model + redactor.
2. **SSRF.** A malicious URL is passed to a connector and
   used to attack the internal network. Mitigated by the
   URL guard.
3. **Capability misconfiguration.** A model is assigned to
   a task it can't handle, leading to a silent fallback or
   a confusing failure. Mitigated by the capability gate.
4. **Audit gap.** An action is taken without an audit
   trail. Mitigated by the event log + `actor` field.
5. **RBAC bypass.** A user accesses the dashboard without
   the right permission. Mitigated by Sling ACLs.

## See also

- [../adr/](../adr/) — the security decision records.
- [../operations/INCIDENT_RESPONSE.md](../operations/INCIDENT_RESPONSE.md) —
  the incident response runbook.
