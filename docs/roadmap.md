# Delivery roadmap and approval gates

The roadmap is incremental, but the target remains the complete enterprise GA
baseline. A phase boundary controls risk and review; it does not make later
baseline capabilities optional.

## Change-control rule for every gate

Every gate listed here, and every future gate added to this roadmap, follows
`CC-001` in `docs/change-control.md`. Each detailed proposal must include
scope, non-goals, design, migration, security, test, deployment, rollback,
evidence, and approval boundaries. Evidence uses `EP-001` in
`docs/evidence-package.md`. Deployment gates conform to `MCDC-001` in
`docs/deployment-contract.md`. Stable governance identifiers and their
canonical records are indexed in `docs/governance/README.md`.

Proposal preparation, implementation, GitHub changes, external-resource
changes, execution, evidence acceptance, closure, and the next phase remain
separate approvals unless an approval explicitly combines named actions.

## Phase 1 — Platform foundation

Implemented:

- Java 21 / Spring Boot application foundation.
- Pluggable managed identity with Supabase Auth adapter.
- PostgreSQL tenancy, RLS, membership/RBAC, and tenant transaction boundary.
- Append-only reproducible audit chain.
- Idempotency and durable outbox/inbox processing with recovery and metrics.
- Health, correlation, safe errors, structured logs, OCI image, SBOM, and CI.

Configured CI acceptance is complete. Platform-foundation readiness and staging
acceptance remain open.

## Phase 1 Platform Foundation Readiness Gate

The repository-owned design package is
`docs/phase-1-platform-foundation-readiness.md`. It covers only the existing
Phase 1 application context and infrastructure necessary to validate it.
Reference examples do not add providers or dependencies.

The gate uses separate approvals:

1. readiness design approval (completed 2026-07-25);
2. review `docs/phase-1-repository-change-proposal.md`;
3. Gate A: profiles, roles, security, contracts, tests, and developer
   experience — merged, CI verified, and accepted 2026-07-25;
4. Gate B: CI hardening and immutable release — repository implementation
   merged, post-merge CI verified, and accepted 2026-07-26;
5. Gate C: deployment-adapter configuration — merged through PR `#4`;
   pull-request and post-merge `main` CI passed; evidence explicitly accepted
   2026-07-26; evidence PR `#5` merged and resulting `main` CI passed;
6. Gate D staging-resource and execution-readiness decisions — accepted
   2026-07-26 in `docs/phase-1-staging-resource-proposal.md`;
7. bounded repository prerequisites in
   `docs/phase-1-gate-d-repository-proposal.md` — implementation approved
   2026-07-27; local implementation and validation in progress;
8. separately approve external resources and execute staging acceptance; and
9. review `docs/phase-1-exit-report.md` and explicitly close Phase 1.

Repository-change approval does not authorize external changes. External
resource approval does not authorize Phase 2.

### Staging acceptance execution stage

Requires explicit approval before creating or modifying external resources.

1. Provision or select non-production Supabase and Render resources.
2. Configure secrets, separate database roles, TLS, rate limits, alerts,
   backups, and log drain.
3. Deploy the exact CI-verified OCI image and apply Flyway migrations.
4. Verify JWT issuer, audience, signature, time, and subject handling.
5. Prove same-tenant access and cross-tenant denial with non-owner API roles.
6. Verify audit-chain recomputation, idempotent replay, inbox deduplication,
   outbox retry/stale-claim recovery, and dead-letter behavior.
7. Verify liveness, readiness, metrics protection, backup restore, rollback,
   and forward-fix procedures.
8. Record evidence and close every failure before approving Phase 2.

The accepted evidence must also prove explicit secret scanning and deployment
of the immutable CI-built image digest. The existing durable outbox dispatcher
must use approved least-privilege database access. No Redis, generic worker
platform, or unrelated provider is required.

## Phase 2 — Organization and event publishing

Not started. Approved scope boundary:

- Organization onboarding and manual verification.
- Memberships, roles, and administrative workflows.
- Event editions and explicit lifecycle transitions.
- English, Telugu, and Hindi content.
- Review, publication requirements, immutable publication snapshots, and QR
  links.

Before implementation, produce and obtain approval for the domain model,
authorization matrix, migrations/RLS policies, API contracts, audit events,
localization model, test plan, rollout plan, and rollback approach.
The Phase 2 proposal and every subordinate gate must include its `CC-001`
compliance record and use the standardized evidence package.

## Later baseline domains

These remain part of the enterprise product baseline but are outside Phase 2:

- Contributions, Razorpay Route settlement, webhook processing, manual refund
  recording, receipts, and reconciliation.
- Cash collection controls and finance reporting.
- 80G/FCRA/GST evidence and eligibility presentation.
- Email, SMS, WhatsApp, media, moderation, abuse, and notification workflows.
- Public experience, search/discovery, reporting, operations, billing, and
  remaining GA readiness.

Each major domain requires a new explicit approval after its design and
dependencies are reviewed under `CC-001`.
