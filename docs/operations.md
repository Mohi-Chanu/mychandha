# Operations

## Deployment

`Dockerfile` produces a non-root OCI image. `render.yaml` is the initial Render
blueprint and supplies:

- one web service;
- one managed PostgreSQL database;
- readiness checks;
- production profile;
- database and Supabase configuration through environment variables.

Render's PostgreSQL URI is normalized to JDBC at startup. Other platforms can
supply a JDBC URL directly, so this compatibility adapter does not leak into
domain code.

The current blueprint is a foundation artifact, not an accepted staging or
production topology. It uses one application process and one database
credential for API traffic, Flyway, and the durable dispatcher, and its
`production`-only profile no longer satisfies the Gate A runtime guard. Do not
deploy it. Gate C must align it to the repository's API, dispatcher, and
migration profiles. The target controlled migration sequence, immutable
artifact deployment, acceptance evidence, and approval boundaries are in
`docs/phase-1-platform-foundation-readiness.md`.

## Health and metrics

| Signal | Endpoint/metric | Alert intent |
|---|---|---|
| Liveness | `/actuator/health/liveness` | Process cannot make progress |
| Readiness | `/actuator/health/readiness` | Instance should leave routing |
| Startup | Runtime-specific startup signal | Initialization exceeded its budget or failed permanently |
| Durable delivery | `durableDelivery` health component | Oldest pending event exceeds 5 minutes |
| Delivery success | `mychandha.outbox.published` | Throughput and success |
| Delivery failure | `mychandha.outbox.failed`, `.retried`, `.dead.lettered` | Retry/dead-letter pressure |
| Backlog | `mychandha.outbox.pending`, `.oldest.age`, `.stale.processing` | Queue age and recovery |
| JVM/HTTP/DB | `/actuator/prometheus` | Saturation, latency and errors |

Every HTTP request returns `X-Correlation-Id`. A caller value is retained only
when it matches the safe format and length. Logs carry correlation ID and, in
worker scope, organization ID; raw tokens and contact data are never logged.

The complete health, structured-log, bounded-metric, tracing-compatibility, and
privacy requirements are in `docs/observability-standards.md`.

## Backup and recovery

Initial production requirements:

1. Render PostgreSQL backups and point-in-time recovery enabled on the selected
   production plan.
2. Daily restore verification in a non-production environment.
3. Quarterly recovery exercise against the target RPO of 5 minutes and RTO of
   60 minutes.
4. Supabase signing-key and tenant/session recovery procedures documented.
5. Audit export and retention configured only after legal retention approval.

## Deployment checklist

1. `mvn verify` passes with Java 21 and a Docker-capable test runtime.
2. OCI image build passes, runs as the non-root `mychandha` user, has no
   unresolved high/critical findings, and produces a CycloneDX SBOM.
3. Flyway migration reviewed; no destructive migration is present.
4. Supabase issuer, JWKS URI, and audience point to the production project.
5. Database credentials are secrets and TLS is required.
6. Readiness is green and the outbox age is zero.
7. A valid JWT can call `/api/v1/platform/me`.
8. A cross-tenant access probe receives a denial.
9. Rollback image and database forward-fix plan are recorded.

## Incident priorities

- P0: suspected cross-tenant access, token verification bypass, audit loss, or
  acknowledged financial data loss.
- P1: authentication outage, durable event backlog over 15 minutes, or database
  unavailability.
- P2: isolated request errors, delayed non-critical workers, or degraded
  observability.
