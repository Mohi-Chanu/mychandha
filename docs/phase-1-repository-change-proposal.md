# Phase 1 Repository Change Proposal

Status: Gate A and Gate B accepted; Gate C not approved
Readiness design approved: 2026-07-25
Gate A implementation approval: Granted 2026-07-25
Gate B implementation approval: Granted 2026-07-25
Gate B evidence acceptance: Granted 2026-07-26
Gate C implementation approval: Not granted
External-resource approval: Not granted
Phase 2: Out of scope

## Purpose

This proposal defines the approved Phase 1 Platform Foundation Readiness
repository changes needed before staging. Gate A and Gate B are accepted. Gate
C and all external actions remain open.

The proposal covers only:

- migration, API, and durable-dispatcher execution isolation;
- least-privilege database access for the existing outbox dispatcher;
- explicit secret scanning and immutable build inputs;
- an immutable OCI release path;
- tests and documentation needed to prove those controls.

It does not introduce Redis, Kafka, a generic worker platform, frontend/CDN/
storage services, product workflows, feature-flag infrastructure, or another
application provider.

## Approved design resolutions

| Decision | Resolution used by this proposal |
|---|---|
| Runtime isolation | Use API, dispatcher, and migration profiles from one OCI artifact |
| Dispatcher database access | Use narrowly scoped security-definer routines; no schema ownership or `BYPASSRLS` for the dispatcher |
| Artifact provenance | Publish an immutable OCI digest and deploy that digest rather than rebuilding |
| Artifact registry | Recommend GitHub Container Registry because the repository and CI already use GitHub; creation/use remains an external approval |
| Secret scanning | Use one blocking Gitleaks full-history scan, pinned immutably; avoid redundant scanners without evidence |
| External resources | Keep exact Supabase, Render, PostgreSQL, region, plan, cost, owners, and cleanup selections in the later external-resource proposal |
| Architecture records | Preserve the five approved decisions under `docs/adr/` |
| Module boundaries | Enforce capability and hexagonal dependency rules incrementally; do not create empty future modules |
| Observability | Standardize liveness/readiness/startup, structured logs, bounded metrics, and OpenTelemetry compatibility without adding a backend |
| API contract | Preserve `/api/v1` and standardize RFC 9457 error and validation responses |
| Developer experience | Keep a guarded local combined mode, synthetic fixtures, Maven helpers, and troubleshooting guidance |

GitHub native secret scanning should be enabled later when repository policy and
plan support it. It complements the CI gate but does not replace the explicit
scanner.

Release signing is not part of this repository implementation. The immutable
digest, SBOM, scan evidence, and protected release workflow are sufficient for
the staging gate. Signing remains a separately reviewed production control.

The authoritative design references are:

- `docs/adr/`;
- `docs/module-boundaries.md`;
- `docs/observability-standards.md`;
- `docs/api-contract.md`; and
- `docs/developer-experience.md`.

## Proposed change sets

Implementation should be incremental. Each change set must pass `mvn verify`,
the structural validator, and its applicable image/security gates before the
next change set begins.

### Change set 1: Runtime and database-role isolation

#### Runtime design

Use one application artifact with three explicit Spring profiles:

| Profile | Process behavior | Database credential | Flyway | Scheduling |
|---|---|---|---|---|
| `api` | Servlet API and Actuator endpoints | API role | Disabled | Disabled |
| `dispatcher` | Non-web durable outbox dispatcher | Dispatcher role | Disabled | Enabled |
| `migration` | Non-web one-off migration execution, then exit | Migration role | Enabled | Disabled |

Local development may use a documented combined profile only for developer
convenience. Staging and production must reject a combined API/dispatcher mode.

The API and dispatcher remain modules of the same modular monolith and use the
same OCI image. The dispatcher profile is only the already-implemented outbox
dispatcher; it is not a generic worker service.

#### Implemented source and configuration changes

| Path | Proposed change |
|---|---|
| `src/main/java/com/mychandha/platform/MyChandhaApplication.java` | Remove global `@EnableScheduling`; keep shared configuration-property registration |
| `src/main/java/com/mychandha/platform/runtime/DispatcherRuntimeConfiguration.java` | New dispatcher-only scheduling configuration |
| `src/main/java/com/mychandha/platform/runtime/MigrationExitRunner.java` | New migration-only runner that exits successfully only after application/Flyway startup completes |
| `src/main/java/com/mychandha/platform/security/SecurityConfiguration.java` | Load servlet security only for the API/web runtime |
| `src/main/java/com/mychandha/platform/security/CorrelationIdFilter.java` | Keep validated correlation IDs and add a separate server-generated request ID |
| `src/main/java/com/mychandha/platform/security/ApiExceptionHandler.java` | Enforce the approved RFC 9457 validation and error contract |
| `src/main/java/com/mychandha/platform/events/OutboxPublisher.java` | Replace direct cross-tenant table SQL with calls to the approved dispatcher routines |
| `src/main/java/com/mychandha/platform/observability/OutboxHealthIndicator.java` | Read safe aggregate backlog data through a dedicated routine instead of direct owner-visible SQL |
| `src/main/java/com/mychandha/platform/observability/StartupHealthIndicator.java` | New deterministic startup health state for API/dispatcher initialization |
| `src/main/java/com/mychandha/platform/observability/OutboxMetrics.java` | New bounded backlog, retry, stale-claim, delivery-duration, and dead-letter metrics |
| `src/main/resources/application.yml` | Make the shared baseline safe: Flyway and scheduling disabled unless an explicit profile enables them |
| `src/main/resources/application-api.yml` | New API runtime settings |
| `src/main/resources/application-dispatcher.yml` | New non-web dispatcher settings |
| `src/main/resources/application-migration.yml` | New non-web Flyway settings |
| `.env.example` | Document profile-specific configuration names without real values |

Profile activation must fail fast when:

- no runtime profile is selected outside local/test use;
- more than one of `api`, `dispatcher`, or `migration` is active;
- a runtime profile uses placeholder credentials; or
- Flyway is enabled in the API or dispatcher profile.

The migration process must return a non-zero exit code on migration or
configuration failure.

#### Forward-only platform migration

Add:

`src/main/resources/db/migration/V2__runtime_role_isolation.sql`

The migration should:

1. add security-definer functions for:
   - claiming available or stale outbox events;
   - marking a claimed event published;
   - rescheduling or dead-lettering a claimed event; and
   - reading aggregate backlog health without exposing payloads;
2. schema-qualify every referenced object;
3. set a fixed safe `search_path`;
4. validate worker identity, claim ownership, transition state, limits, and
   time values;
5. revoke function execution from `PUBLIC`;
6. grant only the required routine execution to stable API/dispatcher group
   roles;
7. remove direct dispatcher table privileges; and
8. preserve all existing data, event identifiers, retry state, hashes, and RLS
   policies.

No product table or product workflow is added.

Environment login principals must not be created with credentials in Flyway.
Add an idempotent, credential-free administrative bootstrap template under
`scripts/` for stable group roles and membership grants. It must accept role
names as validated parameters or use reviewed stable names, contain no
passwords, and require an approved database administrator.

#### Security properties

- Migration role owns schemas and routines but cannot serve application
  traffic.
- API and dispatcher roles are `NOSUPERUSER`, `NOCREATEDB`, `NOCREATEROLE`,
  `NOREPLICATION`, and `NOBYPASSRLS`.
- API role continues to use tenant-bound RLS for organization-scoped DML.
- API role cannot claim or transition outbox records.
- Dispatcher role cannot select tenant business tables or raw inbox/audit data.
- Dispatcher functions return only the fields required for delivery.
- Backlog health exposes counts and age, never payloads or tenant identifiers.
- Function ownership and execute grants are verified after migration.

### Change set 2: Automated role and profile evidence

#### Integration tests

Add or update Testcontainers tests to create distinct migration, API, and
dispatcher roles and run the real Flyway scripts.

Required cases:

1. migration profile applies V1 and V2 and exits successfully;
2. API and dispatcher profiles start with Flyway disabled;
3. invalid or conflicting runtime profiles fail startup;
4. API role cannot create, alter, or drop schema objects;
5. API role cannot bypass RLS or access another organization;
6. API role can enqueue a tenant-bound outbox record atomically with a domain
   write;
7. API role cannot call dispatcher claim/transition routines;
8. dispatcher role can claim, publish, retry, reclaim stale work, and
   dead-letter through the routines;
9. dispatcher role cannot directly read or modify tenant business, audit,
   inbox, or idempotency tables;
10. a dispatcher cannot transition a claim owned by another dispatcher;
11. invalid state transitions and unsafe limits are rejected;
12. backlog health returns aggregate values only;
13. audit-chain, inbox, idempotency, and existing RLS tests remain green; and
14. local combined mode cannot be activated under the production profile;
15. liveness, readiness, and startup health categories have distinct behavior;
16. structured logs contain safe request/correlation/runtime context without
    tokens, contact data, provider subjects, payloads, or unsafe identifiers;
17. outbox metrics use bounded tags and expose backlog, retry, stale recovery,
    delivery, and dead-letter signals;
18. application controller routes remain under `/api/v1`; and
19. authentication, authorization, validation, idempotency, and internal
    failures follow the approved Problem Details contract.

#### Architecture tests

Extend ArchUnit rules so:

- API controllers cannot depend on dispatcher infrastructure;
- dispatcher runtime code cannot depend on controller packages;
- migration-only code is not reachable from API controllers; and
- direct cross-tenant outbox SQL is confined to the migration routines rather
  than Java runtime code;
- controllers call application interfaces and cannot access repositories or
  Spring JDBC directly;
- domain/value-contract packages do not depend on Spring, JDBC, HTTP, Render,
  Supabase, or provider payload packages;
- outbound adapters implement ports owned by the consuming module; and
- future domain modules cannot skip the application/domain/port boundaries.

Apply these rules incrementally to the existing foundation. Do not perform a
broad package rewrite or add broad suppressions merely to make a rule pass.

#### Configuration tests

Test profile precedence, placeholder rejection, Flyway enablement, dispatcher
scheduling, non-web modes, and safe database URL normalization for each
credential class.

#### API and observability contract

Gate A must also:

- preserve `/api/v1` as the application REST major version;
- standardize validation failures and other API errors according to
  `docs/api-contract.md`;
- retain safe `X-Correlation-Id` behavior while generating a distinct request
  ID;
- implement liveness, readiness, and startup evidence for the relevant
  runtime profiles;
- add the bounded metrics required by `docs/observability-standards.md`;
- propagate OpenTelemetry-compatible W3C trace context without adding an
  exporter/backend; and
- keep external identity subjects, user/contact data, tenant-controlled
  values, and payloads out of logs, metric tags, trace baggage, and API errors.

No OpenTelemetry SDK, agent, collector, hosted backend, or new networked
observability service is added by this change set.

### Change set 3: CI and immutable release evidence

#### CI hardening

Modify `.github/workflows/ci.yml` to:

- pin every third-party action to a reviewed full commit SHA;
- pin PostgreSQL and Docker build/runtime images by digest;
- check out full history for secret scanning;
- run a pinned Gitleaks scan with a blocking exit code;
- retain the existing Java 21 Maven, Checkstyle, PMD, JaCoCo, SpotBugs,
  PostgreSQL integration, OCI build, CycloneDX, and HIGH/CRITICAL Trivy gates;
- export the CI-built image as a retained OCI artifact with its digest so the
  release workflow can promote that exact image without rebuilding;
- upload secret-scan and role-boundary test evidence without secrets; and
- keep repository permissions read-only for ordinary verification.

Keep the HIGH/CRITICAL exit threshold unchanged. Treat the current
`ignore-unfixed` behavior as an explicit policy to review; do not expand
ignored findings or weaken test, static-analysis, SBOM, or scan gates as a
shortcut. Any future scan-policy change requires evidence and approval.

#### Immutable release workflow

Add a separate manually approved release workflow rather than giving the normal
CI job package-write permission.

Proposed behavior:

1. accept an already verified commit SHA;
2. promote the retained CI image artifact without rebuilding;
3. rerun required vulnerability and secret gates;
4. publish to the approved registry using a protected staging-release
   environment;
5. record the OCI digest, commit, SBOM digest, scan artifacts, workflow run,
   and approver;
6. use immutable commit and digest references; and
7. never deploy a mutable tag by itself.

The proposal recommends GitHub Container Registry. Enabling package writes,
creating the package, configuring a protected environment, or publishing an
image changes GitHub/external state and requires separate explicit approval.

#### Dockerfile

Pin the Maven builder and Temurin runtime images by digest while retaining
human-readable version comments. Preserve the non-root runtime user and current
Java 21 requirement.

### Change set 4: Deployment adapter and runbook alignment

This change set is repository configuration only. It must not deploy.

Proposed `render.yaml`/runbook treatment:

- API service command activates `production,api`;
- durable dispatcher command activates `production,dispatcher`;
- migration execution activates `production,migration`;
- each execution receives only its own database credential;
- automatic source rebuild/deploy is disabled if it cannot consume the
  accepted immutable OCI digest;
- API readiness remains `/actuator/health/readiness`;
- dispatcher process health uses process status, safe backlog health evidence,
  and alerting defined in the later resource proposal; and
- external resource identifiers, URLs, credentials, and plan-specific values
  are never committed.

The exact Render mechanism must be confirmed in the separately approved
external-resource proposal. This document does not assume a plan feature or
provision a background service.

## Expected file inventory

The implementation proposal is expected to affect only:

- runtime/configuration classes required for the three profiles;
- outbox publisher and health access;
- one forward-only platform migration;
- credential-free role bootstrap documentation/template;
- focused unit, integration, configuration, and architecture tests;
- application profile YAML;
- `.env.example`;
- `Dockerfile`;
- `.github/workflows/ci.yml`;
- a protected/manual release workflow;
- `render.yaml` or its deployment runbook; and
- affected status, architecture, security, operations, and validation
  documents.

No Phase 2 package, endpoint, domain table, provider adapter, frontend, Redis,
Kafka, payment, or messaging file is in scope.

The documentation inventory also includes the accepted ADRs, module boundaries,
observability standards, API/error contract, and developer-experience
guidelines. Future module directories and feature-flag code are not created.

## Validation commands

Implementation is not complete until the approved environment can run:

```text
mvn verify
sh scripts/validate-foundation.sh
docker build <immutable local tag> .
explicit secret scan
CycloneDX SBOM generation
Trivy HIGH/CRITICAL image scan
```

Additional evidence must show:

- all three runtime profiles behave as specified;
- database grants match the role matrix;
- API and dispatcher negative-access probes pass;
- liveness, readiness, startup, logging, metrics, API version, and Problem
  Details contract tests pass;
- migration and rollback compatibility is documented;
- the published OCI digest matches the release evidence; and
- the working tree contains no credentials or production identifiers.

## Migration and compatibility impact

- V2 is forward-only and additive.
- Existing outbox rows and state are preserved.
- Java changes must call the new routines only after V2 is present.
- Deployment order is migration, dispatcher/API rollout, then acceptance.
- The prior image may be used only while schema compatibility is demonstrated.
- A database failure is corrected through a reviewed forward fix, not Flyway
  rollback.

No expand/migrate/contract removal occurs in this change. Direct dispatcher
table access can be revoked in V2 because the new runtime is deployed only
after the migration and the old runtime must not run concurrently unless its
compatibility is explicitly proven.

## Security impact

Expected improvement:

- removes schema ownership and migration ability from runtime roles;
- prevents the API from using cross-tenant dispatcher access;
- limits dispatcher access to reviewed state-transition routines;
- adds explicit secret-scanning evidence;
- makes build inputs and deployed artifacts immutable and traceable.

New risks to test:

- security-definer function injection or unsafe `search_path`;
- profile misconfiguration enabling migration or scheduling in the API;
- dispatcher claim/transition races through the routine boundary;
- over-broad role or function grants;
- supply-chain pin staleness and controlled update procedure; and
- accidental registry publication from an unprotected workflow.

## Deployment and cost impact

Repository implementation itself does not create cost.

The later external proposal must price and approve:

- one API execution;
- one durable dispatcher execution only if the selected Render topology
  requires a separate billed service;
- one managed PostgreSQL database;
- one non-production Supabase project;
- artifact registry storage/egress;
- backups, logs, alerts, and retention.

No cost is accepted until the exact plan and cleanup behavior are approved.

## Developer experience impact

Gate A must keep local development usable:

- the `local` profile may compose API and dispatcher behavior only with local
  credentials;
- local combined mode must fail with the production profile;
- synthetic fixture helpers must exercise tenant context and non-owner roles;
- Maven and repository scripts remain the default commands; no Make dependency
  is required;
- profile and role failures must produce actionable, secret-free startup
  messages; and
- `docs/local-development.md` and `docs/developer-experience.md` must describe
  setup, test data, commands, and troubleshooting.

## Rollback of repository changes

Before staging use, normal source rollback remains available.

After V2 is applied:

- do not reverse or delete V2;
- fix defects through V3 or later;
- retain compatibility with the last accepted application image where
  practical;
- stop the release if role-isolation tests or migration evidence fail; and
- never restore a database solely to undo application configuration.

## Approval and evidence state

Treat the implementation sequence as explicit mini-gates:

### Gate A: Profiles, roles, security, contracts, and developer experience

Change sets 1 and 2 were approved and implemented:

- runtime/profile separation;
- V2 security routines and role-grant model;
- automated profile, role, RLS, dispatcher, health, API/error, observability,
  module-boundary, and configuration evidence; and
- local development and troubleshooting updates.

Gate A completed Docker-backed Java 21 verification and its evidence was
accepted. Gate B repository implementation was separately approved and is
accepted after PR `#2` merged and post-merge `main` run `30166358486`, job
`89699959544`, passed on
`e34239f34056ea1b6bf5769e5e7920a8ceedf053`.

### Gate B: CI and immutable release

Change set 3 is merged, CI verified, and accepted. The accepted run retained the
OCI archive, SBOM, Trivy report, and Gitleaks evidence and recorded OCI
manifest digest
`sha256:befc26d564687ce34ee826f7c77bf418b43d83e861b9ec9edfa6cba3057633ba`.
Package/environment configuration, publication, and release-workflow execution
remain separate approvals.

### Gate C: Deployment adapter

Gate B evidence is accepted. Change set 4 is the next proposal and approval
gate; implementation approval has not been granted. Repository deployment
configuration must not create or change external resources.

### External resources

Only after Gate C review, approve or reject the exact Supabase, Render,
PostgreSQL, registry, plans, costs, owners, secrets, and cleanup proposal.
