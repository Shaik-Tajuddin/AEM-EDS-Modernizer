# ConnectionAgent

> Tests reachability and authentication for every external system:
> AEM Author, AEM Publish, GitHub, Figma, EDS.

- **Stage:** `CONNECTING`
- **Phase:** 1
- **Agent name:** `connection`
- **Task type:** `CONNECTION`

## Inputs

- The project's `connectionConfig` (AEM URLs, GitHub repo, Figma
  URL, EDS preview URL, auth references).

## Outputs

- A `ConnectionCard` per external system with: `reachable`,
  `authenticated`, `latencyMs`, `apiVersion`, `details`.

## AI usage

None. The agent makes synchronous HTTP calls to each system and
records the result.

## Failure modes

- **System unreachable:** the card is `reachable=false`; the
  agent records the HTTP error. The migration is allowed to
  continue (the system may be optional) but a `WARNING` issue
  is created.
- **Authentication failure:** the card is `authenticated=false`;
  the agent creates a `CRITICAL` issue; the `MIGRATE` button
  is disabled until resolved.

## Related

- [Request Flow](../architecture/REQUEST_FLOW.md) — where the
  ConnectionAgent fits in the lifecycle.
