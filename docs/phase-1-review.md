# Phase 1 review

## Purpose

Establish a secure, testable platform foundation without prematurely
implementing organization, festival, contribution, or finance functionality.

## Delivered capability

| Area | Evidence |
|---|---|
| Repository and build | Maven Java 21 build, enforcer, JaCoCo, OCI image |
| Identity | Provider port, Supabase adapter, issuer/audience/JWKS validation |
| Tenancy | Context filter, membership gate, tenant transaction executor, RLS |
| Authorization | Organization roles, permissions, assignments, permission aspect |
| Audit | Append-only table, immutable trigger, reproducible per-organization hash chain and verifier |
| Durable events | Transactional outbox, tenant-safe inbox dedupe, retry, stale-claim recovery, dead letter, metrics |
| API safety | Stateless auth, RFC 9457 errors, correlation IDs, safe headers |
| Operations | Health probes, Prometheus, Render blueprint, recovery checklist |
| Quality | Unit, architecture, migration, RLS, and immutability tests |

## Architecture decisions

- Spring Boot 3.5 is retained because the approved baseline specified Spring
  Boot 3.x. Version 3.5.16 is the final OSS 3.5 release; upgrading to Spring
  Boot 4 is a planned maintenance decision, not an unreviewed Phase 1 change.
- Supabase is behind `IdentityProvider`, preserving the pluggable identity
  decision.
- The initial durable dispatcher is PostgreSQL-backed. Kafka remains deferred.
- The OCI image is cloud-neutral. Render-specific behavior is confined to
  `render.yaml` and database-URL normalization.
- RLS supplements, rather than replaces, application authorization.

## Edge cases addressed

- Invalid or malicious organization header.
- Authenticated user with no membership.
- Membership with no required permission.
- Organization path/header mismatch.
- Replayed external event with altered content.
- Idempotency key reused for a different request.
- Concurrent audit append ordering.
- Concurrent outbox claims.
- Worker termination with a stale `PROCESSING` outbox claim.
- Outbox retry exhaustion and dead-letter state.
- Concurrent reuse of the same idempotency key.
- Render-style percent-encoded database credentials.

## Verification status

The repository includes executable Java 21/Maven/Testcontainers verification.
Gate A local validation compiled all sources, passed 36 non-Docker tests and
all configured static-analysis gates, and passed the structural validator.
Docker is unavailable on this workstation, so the 15 PostgreSQL/Testcontainers
tests and complete `mvn verify` still require CI or another Docker-capable host.

## Remaining Phase 1 production tasks

- Provision the actual Supabase and Render environments.
- Run and accept the Docker-backed V2 migration, API-role, and dispatcher-role
  evidence, then apply those roles only in an approved environment.
- Configure edge rate limits, secret storage, alerts, backups, and log drain.
- Run `mvn verify`, image scanning, Render blueprint validation, restore drill,
  and security review in the deployment environment.

These tasks require external account changes or infrastructure credentials and
were not performed automatically.

## Phase boundary

Phase 2 design may begin only after the Phase 1 Platform Foundation Readiness
Gate closes, staging evidence passes, and the Phase 1 exit report receives
explicit approval. Phase 2 implementation requires its own later approval. Its
scope is organization onboarding, verification, memberships, event editions,
localized content, publication snapshots, and QR links.
