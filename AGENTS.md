# Codex repository instructions

These instructions apply to the entire repository.

## Product and delivery posture

- MyChandha is an enterprise general-availability product, not a prototype,
  proof of concept, or reduced MVP.
- Treat the product requirements and recorded decisions as the baseline. Do not
  silently narrow scope, replace providers, or reinterpret financial,
  compliance, tenancy, or security rules.
- Work incrementally. Before starting a new phase or major module, present the
  proposed scope, design, migrations, security impact, tests, and deployment
  impact, then wait for explicit approval.
- Apply `CC-001` in `docs/change-control.md` to every future gate and major
  change. Proposal, implementation, GitHub, external-resource, execution,
  evidence-acceptance, and closure approvals remain separate unless an
  approval explicitly combines named actions.
- Do not provision or modify GitHub, Supabase, Render, payment, messaging,
  database, DNS, or other external resources without explicit approval.
- Never commit credentials, tokens, service-role keys, production identifiers,
  or copied customer data.

## Current phase boundary

- Phase 1 foundation is implemented and its GitHub Actions CI gates are green.
- Staging deployment and acceptance testing are not complete.
- Phase 2 is not started. Its approved boundary is organization onboarding,
  verification, memberships, event editions, localized content, publication
  snapshots, and QR links.
- Read `docs/current-status.md` before proposing or making changes.
- Read `docs/product-decisions.md` before changing product behavior or provider
  integration.

## Architecture constraints

- Use Java 21, Spring Boot 3.x, Maven, PostgreSQL, Flyway, and OCI containers.
- Preserve the modular-monolith and hexagonal boundaries. Introduce a networked
  service, Kafka, or another infrastructure dependency only with evidence and
  approval.
- Keep identity provider logic behind `IdentityProvider`; Supabase Auth is the
  first adapter, not a domain dependency.
- Every organization-scoped table and query must enforce tenant isolation.
  Keep `organization_id`, PostgreSQL RLS, transaction-local tenant binding, and
  application authorization aligned.
- A tenant header selects a requested scope; it never grants access.
- Preserve append-only, reproducible, hash-linked audit evidence.
- Write domain changes and outbox records atomically. Consumers must be
  idempotent and inbox-protected.
- Use minor currency units plus ISO currency codes for money. MyChandha
  orchestrates payments and does not custody organizer funds.
- Keep external configuration in environment variables or secret stores.
- Treat `docs/deployment-contract.md` as the provider-neutral deployment
  authority. Render is an adapter to that contract and must not become an
  application or domain dependency.

## Engineering workflow

1. Inspect the relevant documentation, code, migrations, tests, and current Git
   status before editing.
2. Preserve unrelated user changes. Do not rewrite history or use destructive
   Git commands.
3. Make the smallest cohesive change that satisfies the approved scope.
4. Add or update tests for behavior, security boundaries, edge cases, and
   regressions.
5. Run `mvn verify` with Java 21 and Docker available. Testcontainers requires a
   Docker-compatible runtime.
6. Run `sh scripts/validate-foundation.sh` as an additional structural check.
   It does not replace `mvn verify`.
7. For image or dependency changes, build the OCI image, generate the CycloneDX
   SBOM, and keep the Trivy HIGH/CRITICAL threshold unchanged.
8. Update `docs/current-status.md` and any affected decision or operations
   document in the same change.
9. Build gate evidence using `EP-001` in `docs/evidence-package.md`.
10. Report what changed, verification evidence, residual risks, and the next
    approval gate.

## Quality and security rules

- Keep versioned REST endpoints under `/api/v1` and use RFC 9457 Problem
  Details with stable error codes and correlation IDs.
- Validate all untrusted input. Never log or return secrets, tokens, raw
  personal data, stack traces, or unsafe correlation IDs.
- Enforce authorization server-side with membership, permission, and tenant
  checks. UI state and identity claims such as email or phone are not
  authorization evidence.
- Use idempotency keys for sensitive commands and optimistic concurrency for
  mutable resources where appropriate.
- Migrations are forward-only and use expand/migrate/contract for production
  evolution.
- Do not suppress static-analysis or vulnerability findings broadly. Fix real
  findings; narrowly document framework false positives.
- Keep filters final, response/record collections immutable, and
  Spring-managed resources closed according to their actual lifecycle.

## Documentation authority

When documents disagree, use this order and surface the conflict:

1. Approved product requirements and `docs/product-decisions.md`
2. `docs/architecture.md`, `docs/security.md`, and
   `docs/deployment-contract.md`
3. `docs/current-status.md` and phase documents
4. Implementation and tests

Do not resolve a material conflict by assumption.
