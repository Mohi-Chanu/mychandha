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
- OCI image, local PostgreSQL Compose profile, and a validated non-live Render
  adapter example that remains blocked from materialization or deployment.
- Unit, architecture, security, migration, RLS, and durable-delivery tests.
- Read-only CI verification, full-history secret scanning, retained OCI
  evidence, and a separately protected immutable-promotion workflow under
  `.github/workflows/`.

See [Phase 1 scope](docs/phase-1-review.md), [architecture](docs/architecture.md),
[validation status](docs/phase-1-validation.md), and
[local development](docs/local-development.md). Phase 1 closure is controlled
by the [Platform Foundation Readiness package](docs/phase-1-platform-foundation-readiness.md),
[repository-change proposal](docs/phase-1-repository-change-proposal.md), and
[Phase 1 exit report](docs/phase-1-exit-report.md). The prepared
[Gate C proposal](docs/phase-1-gate-c-proposal.md) has been implemented and
merged through PR `#4`; pull-request and post-merge CI passed. The
[Gate C evidence package](docs/phase-1-gate-c-evidence.md) was explicitly
accepted on 2026-07-26, merged through PR `#5`, and verified by successful
post-merge `main` CI. The
[staging resource proposal](docs/phase-1-staging-resource-proposal.md) and its
Gate D decisions were accepted on 2026-07-26. The bounded
[Gate D repository proposal](docs/phase-1-gate-d-repository-proposal.md) is
implemented and merged through PR `#6` as
`cc75d0c3c59dc9d11ef748bff2f3633770854ffd`. Post-merge `main` CI run
`30245195541` passed, and its repository evidence was explicitly accepted on
2026-07-27. The retained OCI manifest digest is
`sha256:a19c285d61c62927093bad4adc898a66122adb37978d3894f6f53c54d0e206b0`;
CycloneDX generation, full-history Gitleaks, and Trivy with zero
HIGH/CRITICAL vulnerabilities and zero secrets passed. Full results and the
still-pending external execution checklist are in
[Gate D evidence](docs/phase-1-gate-d-evidence.md). This acceptance does not
authorize registry publication, provider changes, migration, deployment, or
staging execution. Evidence PR `#7` merged as
`afbb2109c6eb442baf01331296cdf3e0be294503`, and resulting `main` CI run
`30249016269`, job `89922507241`, passed. The
[Gate D external-resource preflight](docs/phase-1-gate-d-external-resource-preflight.md)
records the current provider capabilities, exact proposed inventory, cost and
control reconciliation, and four hard stops that must be resolved before
external-resource approval. The bounded
[Gate D provider-conformance remediation proposal](docs/phase-1-gate-d-provider-conformance-remediation-proposal.md)
now defines the recommended resolution and evidence path for those four hard
stops. Its decisions and expiring staging-only exceptions were accepted on
2026-07-29. Implementation of only its bounded repository scope was then
approved and merged through PR `#8` as
`87d0b7bc0891f29482ad9c46856e9a3e4b7a22ad`. PR CI run `30440420020`, job
`90538148786`, and post-merge `main` run `30441738217`, job `90542418893`,
passed and were explicitly accepted. The current remediation-containing OCI
digest is
`sha256:7b97937aed5a44b4ad7a177ebeadf8f631740b7051949c7d2e56f17cfbdf0829`.
The repository and CI portions of the four hard stops are complete; provider
materialization and live evidence remain pending. No publication, provider
change, spending, migration, deployment, or staging execution is authorized.

Engineering standards are recorded in the [ADR index](docs/adr/README.md),
[module boundaries](docs/module-boundaries.md),
[observability standards](docs/observability-standards.md),
[API contract](docs/api-contract.md), and
[developer-experience guidelines](docs/developer-experience.md).
Cross-gate governance is defined by [CC-001](docs/change-control.md), the
[canonical deployment contract](docs/deployment-contract.md), and the
[standard evidence package](docs/evidence-package.md). Stable ADR, decision,
control, and operational-budget identifiers are collected in the
[governance decision index](docs/governance/README.md).

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
