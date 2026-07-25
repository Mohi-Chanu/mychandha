# MyChandha

Phase 1 establishes the production platform foundation for MyChandha. It
contains no festival, contribution, payment, or finance product workflows yet.

## Delivered in Phase 1

- Java 21 / Spring Boot 3 core API.
- Pluggable identity boundary with Supabase JWT validation as the first adapter.
- Organization-scoped request context and membership/RBAC authorization.
- PostgreSQL schemas, tenant Row-Level Security, immutable audit records, and
  durable outbox/inbox tables.
- Transaction helpers for tenant-scoped operations and event publication.
- Correlation IDs, structured logs, Actuator health, Prometheus metrics, and
  redacted error responses.
- OCI image, local PostgreSQL Compose profile, and Render deployment blueprint.
- Unit, architecture, security, migration, RLS, and durable-delivery tests.
- Read-only CI verification, full-history secret scanning, retained OCI
  evidence, and a separately protected immutable-promotion workflow under
  `.github/workflows/`.

See [Phase 1 scope](docs/phase-1-review.md), [architecture](docs/architecture.md),
[validation status](docs/phase-1-validation.md), and
[local development](docs/local-development.md). Phase 1 closure is controlled
by the [Platform Foundation Readiness package](docs/phase-1-platform-foundation-readiness.md),
[repository-change proposal](docs/phase-1-repository-change-proposal.md), and
[Phase 1 exit report](docs/phase-1-exit-report.md).

Engineering standards are recorded in the [ADR index](docs/adr/README.md),
[module boundaries](docs/module-boundaries.md),
[observability standards](docs/observability-standards.md),
[API contract](docs/api-contract.md), and
[developer-experience guidelines](docs/developer-experience.md).

For continued Codex Desktop work, begin with [repository instructions](AGENTS.md),
[current status](docs/current-status.md), [product decisions](docs/product-decisions.md),
and the [delivery roadmap](docs/roadmap.md).

## Prerequisites

- Java 21
- Maven 3.9+
- Docker/Podman for PostgreSQL and Testcontainers

## Run locally

```bash
cp .env.example .env
docker compose up -d postgres
set -a && source .env && set +a
mvn spring-boot:run
```

The Compose database initializes the stable roles required by Flyway V2.
Existing pre-V2 local volumes must be recreated before running the new
migration.

Verify:

```bash
mvn verify
curl http://localhost:8080/actuator/health
```

Authenticated endpoints expect a Supabase access token:

```bash
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/platform/me
```

Tenant-bound endpoints additionally require `X-Organization-Id`. The value is
never trusted by itself; active membership is verified before a tenant context
is established.
