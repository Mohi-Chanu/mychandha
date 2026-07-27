# Phase 1 Gate D repository implementation proposal

Status: Repository implementation approved 2026-07-27; external action not approved
Gate: Phase 1 Gate D — repository prerequisites
Prepared: 2026-07-26
Accepted inputs: `DR-001`, `DR-002`, `DR-003`, `R1-OPB-001`
Change control: `CC-001`
Deployment contract: `MCDC-001` version `1`
Evidence checklist: `EP-001`

## Decision requested

The bounded repository implementation described in this document was approved
on 2026-07-27.

That approval authorizes only local source, test, workflow, adapter, script,
and documentation changes followed by local validation. It did not authorize
a commit, push, pull request, merge, protected GitHub environment, package
publication, provider resource, spending, credential creation, bootstrap,
migration, deployment, acceptance test, restore, rollback, cleanup, Phase 1
closure, or Phase 2 work.

## Outcome and recommendation

Implement four cohesive repository prerequisites:

1. a protected bootstrap and Flyway execution path that satisfies `DR-001`;
2. a bounded in-process rate limiter for the accepted one-instance staging API
   that satisfies `DR-002`;
3. non-live Render materialization and protected execution contracts; and
4. sanitized staging acceptance and `EP-001` evidence automation.

No Redis, Kafka, Render Key Value, Render Workflow, generic worker framework,
additional database, or third-party observability platform is required.

## Scope

The repository change will:

- add two secret-isolated, short-lived Render job-base definitions;
- package the existing database bootstrap authority and a pinned PostgreSQL
  client in the same OCI image used by API, dispatcher, and migration;
- add safe bootstrap and migration launch commands;
- add a protected, manually dispatched Gate D workflow contract;
- implement API rate limiting by client address, authenticated subject,
  authorized organization, and endpoint class;
- emit RFC 9457 `429` responses with a stable error code and `Retry-After`;
- keep rate-limit state and telemetry bounded;
- extend Render adapter validation and tamper fixtures;
- add non-secret acceptance probes and a sanitized evidence manifest; and
- update affected security, operations, validation, status, and decision
  records.

No Flyway product-schema migration is proposed.

## Non-goals

This repository gate will not:

- provision or modify GitHub, GHCR, Supabase, Render, PostgreSQL, DNS, or
  another external system;
- publish, deploy, migrate, restore, roll back, or route public traffic;
- add production-scale distributed rate-limit correctness;
- add autoscaling or a second API instance;
- add a new durable worker, queue, cache service, database, log platform, or
  monitoring vendor;
- add Phase 2 behavior or synthetic product features solely for staging tests;
- place a secret, token, password, provider-generated ID, or customer record in
  the repository or evidence package; or
- treat green repository CI as Gate D acceptance or Phase 1 closure.

## Design 1 — protected database execution (`DR-001`)

### Render mechanism

Render one-off jobs use the latest successful artifact and all environment
variables from a base service. The API and dispatcher therefore remain
prohibited as bootstrap or migration bases.

The adapter will define two non-live, image-backed background-worker bases:

| Desired base | Allowed secret class | One-off command | Lifetime |
|---|---|---|---|
| `mychandha-staging-bootstrap-base` | Provider bootstrap credential and one-time API, dispatcher, and migration role-secret material | `/app/ops/run-bootstrap.sh` | Create for an approved bootstrap window; delete no later than one hour after a terminal job result |
| `mychandha-staging-migration-base` | Migration database credential only | Java with `production,migration` | Create for an approved migration window; delete no later than one hour after a terminal job result |

Each base is a Singapore, Starter, one-instance background worker using the
same accepted `linux/amd64` OCI manifest digest as the runtime release. It has
no ingress, persistent disk, environment group, API secret, dispatcher secret,
or unrelated credential. Its ordinary process is a signal-aware idle command;
the approved one-off job overrides that command. The implementation will not
assume that a suspended base can launch a job because that behavior has not
been accepted as a provider capability.

The bases are materialized only after a separate external-resource approval.
They are temporary additions to the accepted resource inventory. Their
prorated compute and job charges must remain within `R1-OPB-001`; a forecast
over the ceiling is a hard stop.

### OCI and bootstrap content

The runtime stage of `Dockerfile` will:

- install the exact Alpine PostgreSQL client package available from the pinned
  runtime-image repository;
- copy only the approved bootstrap SQL and operational wrappers under
  `/app/ops`;
- keep the existing non-root runtime user and Java entry point; and
- remain subject to the existing OCI, CycloneDX, Gitleaks, and Trivy gates.

The bootstrap path will add a staging wrapper and SQL entry point that:

- use `psql` with `ON_ERROR_STOP=1`;
- read every password through environment variables inside the process rather
  than a workflow input, command-line argument, SQL file, or generated
  evidence file;
- disable command echo and never print connection URLs or role passwords;
- create or rotate only the exact environment login roles;
- enforce `NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS`;
- invoke the existing credential-free
  `scripts/bootstrap-database-roles.sql` authority to create and bind group
  roles;
- verify group membership and negative privilege probes; and
- return only a sanitized pass/fail summary and exit code.

The implementation must test the selected provider's statement-logging
behavior before a real password is supplied. If role-password DDL would be
recorded in provider logs or evidence, bootstrap stops and the mechanism must
be amended under `CC-001`.

### Migration path

The migration one-off job uses:

- `SPRING_PROFILES_ACTIVE=production,migration`;
- `MIGRATION_DATABASE_URL`, `MIGRATION_DATABASE_USERNAME`, and
  `MIGRATION_DATABASE_PASSWORD` only;
- direct TLS-verified PostgreSQL connectivity;
- the existing Flyway locations and `MigrationExitRunner`; and
- the exact accepted OCI digest.

A migration failure exits non-zero, blocks API/dispatcher rollout, leaves the
runtime services unchanged, and records only sanitized job identifiers,
timestamps, Flyway version/result, and hashes. It never triggers a Flyway
clean, reverse migration, or automatic retry.

### Workflow boundary

A new `.github/workflows/staging-deploy.yml` will be manual and protected by
the `staging-deploy` environment. It will:

- accept only a full commit SHA, accepted CI run ID, exact OCI manifest digest,
  approved operation name, non-secret provider resource IDs, and an exact
  confirmation phrase;
- reject mutable tags, shortened SHAs, an unaccepted CI run, an image mismatch,
  an unknown operation, or a missing approval boundary;
- obtain credentials only from protected environment secrets, never dispatch
  inputs;
- expose separately selectable `bootstrap`, `migrate`, `deploy`,
  `acceptance`, `rollback`, and `cleanup` operations so each run can receive
  its own approval;
- create and delete the two temporary job bases only during their matching
  approved operations;
- use the existing immutable release evidence checks; and
- upload only sanitized, checksum-bound evidence.

Adding the workflow to the repository does not create the protected
environment or authorize any dispatch.

## Design 2 — application rate limiting (`DR-002`)

### Technology and boundary

The implementation will add
`com.bucket4j:bucket4j_jdk17-caffeine:8.19.0`. Bucket4j provides the
concurrency-safe token buckets and the Caffeine integration provides bounded,
expiring in-process storage. The version will be pinned in `pom.xml` and
captured by the CycloneDX SBOM.

This is an API-adapter control, not a domain dependency. A provider-neutral
rate-limit policy and endpoint classifier remain independent from the Render
client-address adapter. Redis or another networked store is not introduced.

### Initial staging policy

The one-instance staging policy uses one-minute token buckets:

| Scope | Capacity and refill | Key material |
|---|---:|---|
| Client address | 120 requests per minute per endpoint class | Canonical address plus endpoint class |
| Authenticated subject | 60 requests per minute per endpoint class | One-way keyed digest of subject plus endpoint class |
| Authorized organization | 300 requests per minute per endpoint class | One-way keyed digest of organization ID plus endpoint class |
| Protected metrics | 30 requests per minute per authenticated subject | One-way keyed subject digest plus `metrics` |
| Process safety bucket | 2,000 requests per minute | One process-wide key |

Endpoint classes are the bounded enum `health`, `metrics`, `api-read`,
`api-command`, and `other`. Raw paths, subjects, organization IDs, tokens, and
client-supplied labels never become metric labels.

The policy is externalized under validated `mychandha.rate-limit` properties.
The `production,api` profile refuses to start if the limiter is disabled, a
limit is non-positive, the cache is unbounded, or the client-address policy is
missing. Local and test profiles may use deterministic overrides.

### Filter order

The security chain will use three final filters:

1. `ClientAddressRateLimitFilter`, after `CorrelationIdFilter` and before
   bearer authentication, limits all requests and invalid-token attempts;
2. `SubjectRateLimitFilter`, after `BearerTokenAuthenticationFilter` and before
   `OrganizationContextFilter`, limits authenticated subjects; and
3. `OrganizationRateLimitFilter`, after `OrganizationContextFilter`, limits
   only an organization scope that membership authorization has already
   established.

The organization header remains a requested scope only and never grants
access. Cross-tenant or invalid requests are still bounded by the address and
subject filters before membership denial.

The client-address adapter will not accept an arbitrary left-most forwarded
address. It will canonicalize the servlet remote address and the
Render-overwritten terminal forwarded hop through a strict, configured
boundary. Direct, malformed, multi-header, IPv4-mapped IPv6, and spoofed-header
cases are tested. Live staging must prove Render's overwrite behavior before
traffic is accepted; failure is a `DR-002` hard stop.

### State, response, and telemetry

- A shared Caffeine-backed registry is capped at 10,000 entries and expires
  inactive entries after ten minutes.
- All cache keys use a process-local keyed digest; raw identifiers are not
  retained or logged.
- Rejection is fail-closed for the affected scope and returns HTTP `429`,
  `Retry-After`, RFC 9457 Problem Details, stable code
  `RATE_LIMIT_EXCEEDED`, and the existing safe correlation ID.
- Authentication and authorization details are not revealed by the rejection.
- Metrics use only `scope`, `endpoint_class`, and `outcome` from fixed enums.
- A cache-capacity or policy-configuration error fails readiness rather than
  silently disabling the control.

Scaling the API above one instance invalidates this design and requires a new
distributed proposal under `CC-001`.

## Design 3 — adapter materialization

The repository will continue to contain examples, never a live root
`render.yaml`.

`deploy/render/render.staging.yaml.example` will gain the accepted exact API
and dispatcher names, `singapore`, `starter`, one instance, 30-second graceful
shutdown, the accepted profiles, exact-digest placeholders, and the rate-limit
configuration allowlist.

`deploy/render/render.staging-jobs.yaml.example` will define only the two
temporary job bases and their disjoint secret allowlists. It will remain
separate so a normal API/dispatcher materialization cannot silently create
privileged bases.

The adapter validator will reject:

- a live root `render.yaml`;
- a mutable image or different digests across API, dispatcher, bootstrap, and
  migration;
- a non-Singapore region, non-Starter plan, autoscaling, or instance count
  other than one;
- a source build, persistent disk, public migration/bootstrap ingress, or
  unapproved service/database/cache type;
- an environment group or any cross-class credential;
- a missing readiness path or production rate-limit setting;
- a job base without the exact safe idle command and cleanup deadline; and
- any literal secret or unresolved materialization value.

Positive and tamper fixtures will prove each rule locally and in CI.

## Design 4 — acceptance and evidence

### Automated probes

A non-secret `scripts/run-staging-acceptance.sh` entry point will accept
credentials only through its protected process environment and write temporary
raw output under ignored `target/staging-evidence/raw`. It will emit a separate
sanitized manifest containing checksums, timestamps, fixed check IDs, pass/fail
states, and provider event IDs.

The automated checks will cover:

- liveness, readiness, protected metrics, TLS, and immutable digest identity;
- valid JWT plus invalid issuer, audience, expiry, signature, and subject;
- same-tenant access plus missing, malformed, inactive, and cross-tenant
  denial;
- client-address, subject, organization, metrics, burst, refill, and
  `Retry-After` rate-limit behavior;
- API and dispatcher database negative privileges;
- RLS, audit-chain recomputation, idempotent replay, inbox deduplication,
  outbox retry, stale-claim recovery, and dead-letter behavior;
- safe logs and bounded metric labels; and
- post-deploy and post-rollback readiness/security smoke tests.

The probes use only synthetic identifiers and the minimum role required by
each check. A provider bootstrap owner is never used by the acceptance suite.

### Operator evidence

Provider capabilities that cannot be safely simulated remain explicit operator
checks: price confirmation, MFA and owner review, network allowlist, alert
delivery, backup presence, in-place restore, registry retention, deployment
event, rollback event, and cleanup.

`docs/phase-1-gate-d-evidence.md` will instantiate `EP-001` with every item
initially `Pending`. Repository CI can pass the evidence schema and sanitization
tests, but no staging item becomes `Passed` until the later execution produces
accepted evidence.

Raw provider logs, screenshots containing secrets, database dumps, JWTs,
passwords, connection URLs, email addresses, and copied personal data are
excluded from repository evidence.

## Exact repository impact

The approved implementation would change or add:

| Area | Planned files |
|---|---|
| Dependencies and image | `pom.xml`, `Dockerfile` |
| Rate-limit source | new classes under `src/main/java/com/mychandha/platform/security/ratelimit/`, plus `SecurityConfiguration.java` |
| Configuration | `application.yml`, `application-api.yml`, `application-production.yml` |
| Database execution | `scripts/bootstrap-staging-database.sql`, `scripts/run-staging-bootstrap.sh`, `scripts/run-staging-migration.sh`; reuse `scripts/bootstrap-database-roles.sql` |
| Deployment workflow | `.github/workflows/staging-deploy.yml` |
| Render adapter | `deploy/render/render.staging.yaml.example`, new `deploy/render/render.staging-jobs.yaml.example` |
| Validation | `scripts/validate-render-adapter.sh`, `scripts/test-validate-render-adapter.sh`, `scripts/validate-foundation.sh`, new staging validation/acceptance scripts and tamper fixtures |
| Tests | new unit, MVC/security, concurrency, configuration, architecture, and script-fixture tests under `src/test/` and `scripts/fixtures/` |
| Evidence and operations | new `docs/phase-1-gate-d-evidence.md`; updates to security, API, operations, deployment-runbook, validation, status, roadmap, exit, and governance documents |

Implementation may use a different filename within one listed area when a
testable naming constraint requires it. Adding a new provider, service class,
database, cache, runtime topology, product schema migration, or secret class
is a material deviation and requires renewed approval.

## Security impact

Positive impact:

- API, dispatcher, migration, and bootstrap credentials remain disjoint.
- Targeted Layer 7 abuse receives an application-layer control.
- Privileged processes have no public ingress and exist only during approved
  windows.
- Rate-limit state, logs, metrics, and evidence exclude raw identity and tenant
  values.
- Immutable digest and rollback rules remain enforced.

Residual risks that remain staging hard stops:

- Render forwarded-address behavior must be proven live.
- Provider SQL statement logging must not capture role passwords.
- Shared Render outbound CIDRs must remain safely allowlistable.
- In-process limits do not coordinate across multiple API instances.
- A restore and rollback cannot be proven by repository tests alone.
- Provider price, plan, region, and retention assumptions can change.

## Migration and rollback impact

No product-schema migration is added by this repository gate. The existing
forward-only V1/V2 Flyway history remains authoritative.

Before external execution, repository changes can be reverted normally. After
staging execution:

- application rollback follows `DR-003` and uses a retained compatible digest;
- Flyway is never reversed automatically;
- database defects use a reviewed forward fix or separately approved restore;
- temporary privileged bases are deleted and their secrets removed; and
- a failed bootstrap or migration blocks runtime rollout.

## Validation plan

Local validation after implementation:

1. Java 21 `mvn verify`, including deterministic-clock, concurrency,
   filter-order, spoofed-header, cache-bound, expiry, RFC 9457, profile, and
   PostgreSQL integration tests.
2. `sh scripts/validate-foundation.sh`.
3. Positive and tamper-rejection tests for runtime and temporary-job adapters.
4. Shell syntax and fixture tests for bootstrap, migration, workflow input,
   sanitization, and acceptance scripts without provider calls.
5. OCI `linux/amd64` build after the dependency and PostgreSQL-client changes.
6. CycloneDX SBOM generation and hash verification.
7. Full-history Gitleaks.
8. Trivy image and filesystem scanning with the existing blocking
   HIGH/CRITICAL threshold and secret checks.
9. `git diff --check` and local Markdown-link validation.

Local validation will use Docker/Testcontainers and synthetic values only. It
will not call GitHub, GHCR, Supabase, or Render.

CI evidence after a separately approved commit/push/PR must repeat the existing
Java 21, PostgreSQL, static-analysis, OCI, SBOM, Gitleaks, and Trivy gates.

## External execution gates after repository acceptance

Repository acceptance would still leave these independent approvals:

1. commit, push, and pull request;
2. green CI evidence acceptance and merge;
3. GitHub protected environments and GHCR publication;
4. exact non-production resources and current checkout totals;
5. bootstrap;
6. migration;
7. API/dispatcher deployment and routing;
8. staging acceptance and alert tests;
9. restore;
10. rollback/forward-fix;
11. cleanup;
12. Gate D `EP-001` acceptance; and
13. Phase 1 closure.

## Provider assumptions used

The design relies on current official documentation:

- Render one-off jobs inherit their base service's latest successful artifact
  and all configured environment variables:
  <https://render.com/docs/one-off-jobs>.
- Render supports image-backed background workers, `dockerCommand`, Singapore,
  Starter, and explicit instance counts:
  <https://render.com/docs/blueprint-spec>.
- Bucket4j `8.19.0` documents the current release and its Caffeine integration:
  <https://bucket4j.com/> and
  <https://central.sonatype.com/artifact/com.bucket4j/bucket4j_jdk17-caffeine/8.19.0>.

These assumptions must be revalidated during implementation dependency review
and again before external-resource execution.

## CC-001 compliance

- Current lifecycle step: approved local repository implementation.
- Granted approval: Gate D proposal decisions were accepted on 2026-07-26,
  including `DR-001`, `DR-002`, `DR-003`, and `R1-OPB-001`.
- Granted approval: local repository implementation was approved on
  2026-07-27.
- Not granted: commit, push, pull request, merge,
  GitHub/provider changes, spending, package publication, credentials,
  bootstrap, migration, deployment, acceptance, restore, rollback, cleanup,
  Phase 1 closure, or Phase 2.
- Repository boundary: the files and behavior explicitly listed above.
- External boundary: official documentation was inspected read-only; no
  external state was changed.
- Material dependencies: Bucket4j/Caffeine and the Alpine PostgreSQL client are
  implemented locally and must pass SBOM/license/vulnerability review.
- Evidence package: `docs/phase-1-gate-d-evidence.md` records all execution
  evidence as initially pending.
- Next approval required after local validation: commit, push, and draft pull
  request for CI evidence.
