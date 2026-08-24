# IMS Authentication

The AEM Author and Publish connectors authenticate via the
**IMS OAuth Server-to-Server** flow. This document describes
the flow, the required credentials, and how the modernizer
stores the credentials.

## Why IMS, not basic auth?

AEM Cloud Service does not support basic auth. The only
programmatic access is via IMS, and the only programmatic
flow is **Server-to-Server** (also called "Machine-to-Machine"
or "M2M").

## The flow

```
AemClient
  │ needs an IMS access token
  ▼
IMS Auth (POST https://ims-na1.adobelogin.com/ims/token/v3)
  │ grant_type=client_credentials
  │ client_id={CLIENT_ID}
  │ client_secret={CLIENT_SECRET}
  │ scope=openid,AdobeID,read_organizations,additional_info.projectedProductContext
  ▼
IMS access token (1 hour TTL)
  │
  ▼
AEM API call
  │ Authorization: Bearer {token}
  ▼
AEM response
```

## Credentials

You need an **IMS Service Account** (also called an "M2M
Service Account" or "Server-to-Server credential") created in
the Adobe Developer Console:

1. Go to [developer.adobe.com](https://developer.adobe.com).
2. Create a new project (or use an existing one).
3. Add the **AEM as a Cloud Service** API.
4. Create a **Service Account (OAuth Server-to-Server)**.
5. Note the `client_id` and `client_secret`.

The service account must be granted access to the AEM
program in the Cloud Manager UI
(Program → Author → Permissions → Add Service Account).

## Where the credentials live

The credentials are **never** stored in source, JCR, or
browser. They live in the AEM Secret Service and are
referenced by a path:

```yaml
# In the OSGi config:
aemAuthReference: "aem:/path/to/secret"
```

The `AemSecretProvider` (planned for Phase 3) reads the
secret from the AEM Secret Service. For the MVP, the
credentials are read from env vars:

```yaml
# In the env:
AEM_IMS_CLIENT_ID=...
AEM_IMS_CLIENT_SECRET=...
```

And the OSGi config references the env var:

```yaml
aemAuthReference: "env:AEM_IMS_CLIENT_ID+env:AEM_IMS_CLIENT_SECRET"
```

## Token caching

The `AemClient` caches the IMS token in memory until 5
minutes before its expiry. Concurrent calls share the same
token; the token is refreshed when needed.

## Failure modes

- **401 from IMS:** the client credentials are wrong; the
  modernizer records a `CRITICAL` issue with the IMS error
  and the migration is blocked.
- **403 from AEM:** the service account is not granted
  access to the program; the modernizer records a
  `CRITICAL` issue.
- **Token expired mid-request:** the client refreshes the
  token and retries once; if the retry also fails, the
  error propagates.

## Related

- [SECRETS.md](../security/SECRETS.md) — full secret model.
- [OSGI_CONFIG.md](OSGI_CONFIG.md) — where the auth
  reference is configured.
