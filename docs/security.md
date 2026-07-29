# Phase 1 security model

## Trust boundaries

| Boundary | Control |
|---|---|
| Internet to API | Render edge TLS and DDoS protection, application rate limits, bearer authentication, safe headers |
| Supabase to API | JWKS signature, exact issuer, required audience, token time validation |
| User to organization | Active membership, permission check, path/header consistency |
| Application to database | Transaction-local tenant binding and PostgreSQL RLS |
| Domain write to worker | Atomic outbox, versioned payload, bounded retry |
| External event to processing | Signature adapter (future provider), inbox identity and payload hash |

## Non-negotiable rules

- No authorization decision trusts email, phone, UI role labels, or tenant
  headers.
- Provider `sub` is the stable identity key.
- Contact fields persisted in Phase 1 are masked hints only.
- Authentication failures do not reveal whether an account or organization
  exists.
- Audit events cannot be updated or deleted by application SQL.
- Diagnostic logs and audit records are different data products.
- Secrets are injected as environment secrets and excluded from source control.
- Prometheus requires the dedicated `platform.metrics` scope.
- Application errors follow `docs/api-contract.md` and never expose rejected
  values, stack traces, SQL, provider payloads, or personal data.
- Telemetry follows `docs/observability-standards.md`; external identity
  subjects, tokens, contact data, and payloads never enter logs, metric tags,
  trace baggage, or error responses.

## Threats covered by automated checks

- Cross-tenant reads through incorrect organization context.
- Audit tampering by update/delete.
- Identity claim mapping regressions.
- Unsafe controller-to-JDBC coupling.
- Replayed provider event IDs with changed payload.
- Reuse of an idempotency key with different request content.

## Required before production traffic

- Deploy and verify the repository-defined migration, API, and dispatcher
  database-role separation.
- Application-layer rate-limit policy by IP, subject, organization, and
  endpoint class; provider DDoS protection alone is insufficient.
- Supabase project hardening: asymmetric signing key, rotation rehearsal,
  session lifetime, MFA/step-up policy, breached-password protection, and audit
  export.
- Central secret manager and rotation runbook.
- SAST, dependency, container, IaC, and secret scanning in the deployment
  environment.
- Independent penetration test before GA.

## Gate D application rate-limit control

The approved single-instance staging API uses bounded in-process token buckets
for client address, authenticated subject, authorized organization, protected
metrics, and process safety. Keys are process-local HMAC digests; raw
addresses, subjects, and organization IDs are not retained in bucket keys or
metric labels. Metrics use only fixed `scope`, `endpoint_class`, and `outcome`
values.

The servlet container does not interpret forwarded headers. The rate-limit
adapter supports explicit direct, trusted-proxy-CIDR, and provider-edge
strategies. The accepted Render mapping uses the first canonical
`X-Forwarded-For` hop because Render owns the public ingress path and documents
the real client in that position. It rejects conflicting CIDRs and never uses
Render outbound ranges as ingress trust.

Missing, duplicate, malformed, overlong, or over-depth Render forwarding falls
back to the canonical socket peer, increments a fixed-cardinality anomaly
metric, and degrades rate-limit readiness after three consecutive anomalies.
A valid provider header resets the consecutive anomaly state. Raw addresses
and header values never enter logs, responses, metric tags, or evidence.
Production API startup requires rate limiting and an approved non-direct
client-address boundary. Live spoof-resistance remains required before public
staging traffic.

Rejection returns RFC 9457 status `429`, stable code
`RATE_LIMIT_EXCEEDED`, the safe correlation ID, and `Retry-After`. Cache
capacity exhaustion fails readiness and rejects new keys rather than silently
disabling the control. Scaling beyond one API instance requires a new
distributed-control proposal under `CC-001`.

Every production PostgreSQL profile uses code-owned `sslmode=verify-full` and
its process-specific root-certificate path. Startup fails before datasource or
Flyway use when the path is missing, relative, unreadable, or not a regular
file. The Render mapping is `/etc/secrets/supabase-ca.crt`; database
credentials remain disjoint.
