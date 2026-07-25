# Phase 1 security model

## Trust boundaries

| Boundary | Control |
|---|---|
| Internet to API | Render edge TLS, bearer authentication, rate limits at edge, safe headers |
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
- Edge rate-limit policy by IP, subject, organization, and endpoint class.
- Supabase project hardening: asymmetric signing key, rotation rehearsal,
  session lifetime, MFA/step-up policy, breached-password protection, and audit
  export.
- Central secret manager and rotation runbook.
- SAST, dependency, container, IaC, and secret scanning in the deployment
  environment.
- Independent penetration test before GA.
