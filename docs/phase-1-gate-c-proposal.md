# Phase 1 Gate C deployment-adapter proposal

Status: Implementation merged; pull-request and post-merge CI passed;
evidence acceptance and evidence-record publication pending
Prepared: 2026-07-26

## Approval boundary

The user first approved preparation and refinement of this proposal, then
explicitly approved its bounded repository implementation.

The implementation approval authorizes the adapter, runbook, validation,
fixtures, evidence, and documentation changes in the expected file inventory.
It does not authorize:

- application, domain, or migration code outside this scope;
- committing, pushing, or opening a pull request;
- creating or changing GitHub packages or protected environments;
- running the release workflow or publishing an OCI image;
- creating or changing Render, Supabase, PostgreSQL, registry, DNS, or other
  external resources;
- applying a migration or deploying an application; or
- starting Phase 2.

Gate C implementation was explicitly approved after this proposal was refined.
That approval did not authorize commit, push, pull request, merge, workflow
execution, image publication, external-resource changes, migration, or
deployment.

Commit, push, branch creation, and draft PR `#4` were later approved
separately. The user merged PR `#4` after its CI passed and directed
post-merge evidence preparation after `main` CI passed. Those later actions do
not authorize image publication, external resources, migration, deployment, or
Phase 2.

## Governing specifications

This proposal is governed by:

- the provider-neutral [Canonical Deployment Contract](deployment-contract.md)
  (`MCDC-001` version `1`);
- the [Standard Evidence Package](evidence-package.md) (`EP-001`); and
- [Change Control Rule CC-001](change-control.md).

The canonical contract, not Render configuration, is the architectural source
of truth. The Render adapter must declare and validate how it satisfies the
contract.

## Objective

Gate C removes the unsafe, obsolete deployment assumption in the current
repository adapter and records a deployable contract for the already accepted
Phase 1 runtime:

- one immutable OCI image;
- one API execution using `production,api`;
- one durable dispatcher execution using `production,dispatcher`;
- one controlled, short-lived migration execution using
  `production,migration`; and
- separate least-privilege database credentials for all three executions.

This is deployment separation for the existing API, dispatcher, and migration
profiles. The Render background-worker service type is only a host for the
existing PostgreSQL-outbox dispatcher. Gate C does not add Redis, Render Key
Value, Render Workflows, Kafka, a generic worker framework, or another
infrastructure dependency.

## Gate C Non-goals

Gate C does not:

- provision, update, suspend, resume, or delete an external resource;
- publish an image, create a registry package, configure a GitHub environment,
  or execute a release/deployment workflow;
- select real service names, resource identifiers, regions, plans, prices,
  scaling, domains, owners, credentials, retention periods, or cleanup dates;
- select or create the final migration runner or weaken network controls to
  accommodate one;
- apply database-role bootstrap or a Flyway migration;
- change Java code, SQL migrations, domain behavior, API contracts, identity
  behavior, RLS, audit, idempotency, inbox, or outbox semantics;
- add Redis/Key Value, Render Workflows, Kafka, cron, a generic worker
  framework, or an additional runtime;
- add frontend, CDN, DNS, storage, payment, messaging, or Phase 2 capability;
- claim staging or production readiness; or
- approve the external-resource, staging-execution, Phase 1 closure, or Phase
  2 gates.

## Verified baseline and current gap

Gate A implemented and CI-verified the runtime profiles, V2 database roles,
dispatcher routines, readiness signals, and database-backed durable delivery.
Gate B implemented and CI-verified the immutable OCI evidence and no-rebuild
promotion path.

The current root `render.yaml` must not be used because it:

- asks Render to build from the repository instead of consuming the accepted
  CI-built OCI digest;
- enables source-driven deployment;
- starts only `production`, which the Gate A production guard rejects;
- defines only one web process and omits the dispatcher execution;
- gives API, dispatcher, and Flyway one database credential; and
- declares a database resource before its plan, ownership, role bootstrap,
  backup, cost, and cleanup controls have been approved.

No Render or Supabase resource currently exists for this gate, no accepted
image has been published to a registry, and the Gate B release workflow has
not been run.

## Provider facts that constrain the design

The design relies on the following current Render behavior:

- Render Blueprints use `runtime: image` for a prebuilt registry image and
  `type: worker` for a continuously running background worker
  ([Blueprint specification](https://render.com/docs/blueprint-spec)).
- Image-backed web services and background workers can consume a registry
  image by digest. Render pulls the image again for deploys, restarts, and
  rescheduling, so the registry must retain accepted and rollback digests
  ([prebuilt image deployment](https://render.com/docs/deploying-an-image)).
- Image-backed services do not redeploy merely because a tag changes. A deploy
  hook can select a tag or digest, but a digest is required by MyChandha's
  release contract
  ([prebuilt image deployment](https://render.com/docs/deploying-an-image)).
- Render background workers run continuously and receive no inbound network
  traffic
  ([background workers](https://render.com/docs/background-workers)).
- A Render one-off job inherits the latest successful artifact and **all**
  environment variables of its base service
  ([one-off jobs](https://render.com/docs/one-off-jobs)).
- A pre-deploy command runs in a separate instance but remains a command of a
  paid web, private, or worker service
  ([deploy lifecycle](https://render.com/docs/deploys)).

The last two facts prevent the proposal from treating an API- or
dispatcher-based one-off job or pre-deploy command as migration-secret
isolation. Supplying a migration credential to either long-lived service would
make that credential part of the service configuration even if its normal
profile did not read it.

## Recommended Gate C repository change

### 1. Establish the provider-neutral deployment contract

Use `docs/deployment-contract.md` as the normative `MCDC-001` specification.
The Gate C Render runbook must include a conformance table mapping every
required canonical capability to:

- the Render mechanism;
- configuration location;
- evidence source;
- `supported`, `unsupported`, or `open` status; and
- plan/cost dependency.

An unsupported or open blocking capability prevents deployment. The
environment capability matrix in the canonical contract defines the
differences between local development, CI, staging, and production; provider
configuration cannot silently reinterpret them.

### 2. Retire the auto-discovered live Blueprint

Delete the root `render.yaml`. A root Blueprint containing unresolved resource
names, plans, registry references, or secrets is either unsafe to sync or
pretends that later external decisions are already approved.

Add a deliberately non-auto-discovered example at:

`deploy/render/render.staging.yaml.example`

The example will be valid YAML but will contain conspicuous
`REPLACE_AFTER_EXTERNAL_APPROVAL` values. It is a reviewable provider mapping,
not a file to sync to Render. The later external-resource proposal must either:

1. approve concrete values and a materialized Blueprint at an explicitly
   approved path; or
2. approve equivalent Render Dashboard/API configuration and retain an
   exported, redacted configuration record as evidence.

The example must:

- set Blueprint preview generation to `off`;
- define one `web` service with `runtime: image`;
- define one `worker` service with `runtime: image`;
- use the same digest-shaped image placeholder for both services;
- define no `databases`, Key Value, Redis, cron, disk, domain, or unrelated
  service;
- contain no source `repo`, `branch`, `runtime: docker`, `dockerfilePath`,
  build command, or source auto-deploy setting;
- omit `preDeployCommand` and `initialDeployHook`;
- configure API readiness at `/actuator/health/readiness`; and
- contain no literal secret or real external resource identifier.

Render documents that `autoDeployTrigger` applies only to Git-backed services.
It should be omitted from the image-backed example instead of being presented
as an image-promotion control.

### 3. Add a Render deployment runbook

Add `docs/render-deployment-runbook.md` with:

- the exact profile/process mapping;
- the environment-variable allowlist for each process;
- immutable digest verification and retention rules;
- database-role bootstrap prerequisites;
- migration, rollout, smoke-test, rollback, forward-fix, and cleanup order;
- evidence that must be captured without copying secrets; and
- hard stops for missing approval, mutable images, credential overlap,
  unverified migration connectivity, failed readiness, or incompatible
  rollback.

The runbook remains non-executable until the external-resource and staging
execution approvals are granted.

### 4. Add structural deployment-contract validation

Add `scripts/validate-render-adapter.sh` and invoke it from
`scripts/validate-foundation.sh`.

The validator must fail if:

- a live root `render.yaml` remains;
- the example is missing or malformed;
- an API or dispatcher profile is missing or combined;
- the two services do not use the same digest placeholder;
- the API lacks the readiness path;
- a source build/deploy field or an unapproved resource type appears;
- migration credentials appear in either long-lived service;
- API credentials appear in the dispatcher service or vice versa;
- a mutable image tag is accepted as the deployment identity; or
- the example contains a credential value instead of an explicit unsupplied
  secret marker.

The validator is a repository guard, not a provider API call.

### 5. Standardize the Gate C evidence package

Gate C implementation and CI evidence must use `EP-001` from
`docs/evidence-package.md`. The package must contain the applicable identity,
scope, approval, source, build, artifact, supply-chain, security, deployment
contract, and acceptance records.

Provider deployment, migration, database, recovery, and staging-acceptance
items remain pending—not falsely marked passed—until their separately approved
execution. Every not-applicable item requires a reason.

### 6. Align repository documentation

Update the current status, roadmap, operations guide, readiness package,
repository-change proposal, product decision history, and Phase 1 exit report
to:

- identify the reviewed Gate C scope and approval state;
- state that the old root Blueprint is unsafe and must be retired;
- link the Render runbook and non-live example after implementation;
- retain external resources and staging execution as later gates; and
- keep Phase 2 blocked until staging acceptance and explicit Phase 1 closure.

No Java source, database migration, domain behavior, API contract, Dockerfile,
or CI image-build logic is expected to change in Gate C.

## Runtime and secret contract

| Execution | Render mapping | Active profiles | Allowed sensitive configuration | Explicitly prohibited |
|---|---|---|---|---|
| API | Image-backed web service | `production,api` | `API_DATABASE_URL`, `API_DATABASE_USERNAME`, `API_DATABASE_PASSWORD`; approved Supabase validation configuration | Dispatcher or migration database credentials; Flyway; scheduler |
| Dispatcher | Image-backed background worker | `production,dispatcher` | `DISPATCHER_DATABASE_URL`, `DISPATCHER_DATABASE_USERNAME`, `DISPATCHER_DATABASE_PASSWORD`; bounded outbox tuning | API, Supabase, or migration credentials; Flyway; HTTP ingress |
| Migration | Short-lived controlled release execution; mechanism approved later | `production,migration` | `MIGRATION_DATABASE_URL`, `MIGRATION_DATABASE_USERNAME`, `MIGRATION_DATABASE_PASSWORD` only | API traffic, dispatcher scheduling, long-lived availability |

Common non-secret JVM resource settings may be shared. Environment groups must
not be used when they blur the allowlists or create variable collisions.
Secrets are supplied only through an approved secret store after external
approval; they never appear in the example, logs, command line, evidence
bundle, or documentation.

## Immutable-image contract

The API, dispatcher, and migration execution must run:

`<approved-registry>/<approved-image>@sha256:<64-hex-digest>`

The registry digest must match a successful, accepted `main` CI evidence
bundle and the no-rebuild promotion record. A commit tag may remain a
human-readable alias, but it is never sufficient deployment identity.

Before deployment, the operator must verify:

- accepted commit and GitHub Actions run;
- OCI manifest digest;
- OCI archive, SBOM, vulnerability-report, and secret-scan evidence checksums;
- successful no-rebuild registry promotion;
- registry pull availability for the selected digest;
- `linux/amd64` compatibility; and
- availability of both current and prior compatible digests for recovery.

Gate C does not publish the image or configure registry credentials.

## Migration execution contract

The preferred control boundary is an approval-protected release runner that:

1. pulls the same accepted digest used by the runtime services;
2. receives only the migration database credential;
3. uses verified TLS and an approved network path;
4. starts `production,migration`;
5. records the image digest, Flyway result, schema version, timestamps,
   initiator, approver, and sanitized logs;
6. fails closed on bootstrap, Flyway, or startup failure; and
7. exits before the API and dispatcher rollout proceeds.

The exact runner is intentionally an external-resource decision. A
GitHub-hosted runner is acceptable only if the selected database can be reached
without weakening network controls. Otherwise, the external proposal must
select an approved private or provider-local execution mechanism.

The following are rejected:

- migration from a developer laptop;
- enabling Flyway in the API or dispatcher;
- attaching the migration credential to the API or dispatcher;
- using an API- or dispatcher-based Render one-off job;
- adding a service `preDeployCommand` that requires the migration credential;
- running an always-on migration service; and
- automatically reversing V2.

Before Flyway runs, an approved database owner/bootstrap identity must create
the environment login roles and grant the stable non-login roles described by
`scripts/bootstrap-database-roles.sql`. The bootstrap mechanism, identity,
network path, and evidence are mandatory items in the external-resource
proposal.

## Health and observability contract

### API

- Render routes only when `/actuator/health/readiness` succeeds.
- `/actuator/health/liveness` remains diagnostic evidence.
- Readiness includes application startup and database connectivity.
- Identity, `/api/v1/platform/me`, tenant-denial, safe-error, and correlation
  smoke tests run before routing is accepted.

### Dispatcher

- Render service/process state proves that the long-running process remains
  active.
- The dispatcher receives no inbound public traffic.
- Staging acceptance must capture safe backlog evidence from the existing
  `platform.outbox_backlog` boundary and existing bounded metrics.
- Alert routing, log retention, metrics collection, and the method used to
  observe backlog without granting broader database access are selected and
  priced in the external-resource proposal.

Gate C must not claim that the non-web dispatcher exposes an HTTP health
endpoint. If the selected monitoring plan cannot observe the required backlog
and failure signals without expanding privileges, staging cannot begin until a
separate repository change is proposed and approved.

## Deployment order and failure behavior

The later, separately approved staging execution must use this order:

1. confirm all approvals, owners, cost, cleanup, and evidence locations;
2. verify the accepted CI bundle and promoted image digest;
3. verify the database, TLS, secret, and role inventory;
4. execute the approved credential-free role bootstrap as the approved owner;
5. run the migration profile with the migration-only credential;
6. verify Flyway version, ownership, grants, and API/dispatcher negative
   permissions;
7. deploy the dispatcher and API from the same immutable digest;
8. wait for API readiness and dispatcher process health;
9. execute JWT, tenant, audit, idempotency, inbox, outbox, observability, and
   log-safety smoke tests;
10. enable or accept routing only after all blocking checks pass; and
11. record immutable evidence and begin the monitored acceptance window.

A failure before migration leaves the current environment unchanged. A
migration failure stops the release and requires diagnosis or a reviewed
forward fix. An API or dispatcher failure after migration retains the database
state and permits only a schema-compatible prior image or a reviewed forward
application fix.

## Rollback, recovery, and cleanup

- Never roll Flyway backward automatically.
- A prior image can be selected only after its compatibility with the current
  schema is documented and verified.
- Retain current and prior accepted registry digests for the approved recovery
  period because Render pulls images again during rollback and rescheduling.
- After rollback, recheck readiness, authentication, cross-tenant denial,
  audit continuity, dispatcher backlog, and log safety.
- A database restore is a separately authorized recovery action, not an
  application rollback shortcut.
- Every later-created resource needs a deletion owner, retention condition,
  secret-revocation procedure, backup disposition, and cleanup evidence.

Gate C creates no external resource and therefore creates no current provider
cost.

## Validation plan for Gate C implementation

Required local evidence:

```text
mvn verify
sh scripts/validate-foundation.sh
sh scripts/validate-render-adapter.sh
```

The Gate C pull request CI must also retain the existing Gate B gates:

- Java 21 and all unit, architecture, security, migration, RLS,
  runtime-profile, and PostgreSQL/Testcontainers tests;
- Checkstyle, PMD, JaCoCo, and SpotBugs;
- full-history blocking Gitleaks;
- one `linux/amd64` OCI build;
- CycloneDX SBOM generation;
- Trivy HIGH/CRITICAL scanning; and
- release-evidence verification.

The resulting Gate C evidence index must follow `EP-001`, identify
`MCDC-001` version `1`, and record that provider-side deployment items were not
executed.

Focused review fixtures must prove:

- the valid example passes;
- a mutable tag is rejected;
- source-build configuration is rejected;
- API/dispatcher profile mixing is rejected;
- database-credential cross-contamination is rejected;
- a migration credential in a long-lived service is rejected;
- an unapproved database, Redis/Key Value, cron, or extra worker resource is
  rejected; and
- an invalid or missing readiness path is rejected.

No live Render API validation is part of Gate C. Provider-side validation
belongs to the later external-resource and staging-execution approvals.

## Expected implementation file inventory

| File | Proposed change |
|---|---|
| `render.yaml` | Delete the obsolete, auto-discovered live Blueprint |
| `deploy/render/render.staging.yaml.example` | Add the non-live, image-backed API/dispatcher mapping |
| `docs/render-deployment-runbook.md` | Add provider mapping, release, migration, evidence, rollback, and hard-stop procedures |
| `docs/deployment-contract.md` | Retain the normative provider-neutral `MCDC-001` specification and environment capability matrix |
| `docs/evidence-package.md` | Retain the reusable `EP-001` evidence checklist |
| `docs/change-control.md` | Retain the reusable `CC-001` lifecycle rule |
| `scripts/validate-render-adapter.sh` | Add repository-only deployment-contract validation |
| `scripts/test-validate-render-adapter.sh` and fixtures | Add positive and tamper-rejection coverage |
| `scripts/validate-foundation.sh` | Require the new example/runbook/validator and reject a live root Blueprint |
| `docs/phase-1-gate-c-evidence.md` | Record local `EP-001` implementation evidence and later CI state |
| Architecture, README, status, roadmap, decisions, readiness, operations, and exit documents | Align the adapter abstraction, Gate C state, and next approval boundary |

If implementation reveals a necessary Java, SQL, CI-release, or provider API
change, stop and present that change separately. It is not implicitly approved
by this proposal.

## Risks and unresolved external decisions

| Risk or decision | Gate C disposition |
|---|---|
| Migration credentials leak into a long-lived service | Prohibited by contract and validator |
| Render one-off job inherits API/dispatcher secrets | Do not use those services as the migration base |
| Safe migration network path is not yet selected | Blocking item for the external-resource proposal |
| Registry/package and protected release environment do not exist | Separate GitHub/external approval |
| Resource names, region, plans, cost, scaling, alerts, logs, backups, retention, and owners are unknown | Separate external-resource proposal |
| Dispatcher has no inbound health endpoint | Use process state plus approved backlog evidence; propose a separate code change if the selected monitoring plan is insufficient |
| Accepted Gate B Actions artifact expires | Re-run current `main` CI or separately approve promotion; never deploy expired/unverified material |
| Root Blueprint could provision unapproved resources | Delete it and keep only a non-auto-discovered example |
| Schema rollback is unsafe | Forward-only migration and compatibility-gated image rollback |

## Exact next gate and recommendation

Gate C implementation is merged and its pull-request and post-merge `main` CI
passed. The next gate is **explicit Gate C evidence acceptance and
evidence-record publication approval**.

Recommended action:

1. review `docs/phase-1-gate-c-evidence.md`;
2. explicitly accept or reject the Gate C CI evidence;
3. approve or reject committing and publishing the evidence-only branch
   `codex/phase-1-gate-c-evidence`;
4. after that record merges and `main` CI is green, record Gate C as accepted;
5. then prepare the exact external-resource proposal, including the migration
   runner/network decision, provider inventory, plans, costs, owners, secrets,
   backup/log/alert controls, retention, and cleanup; and
6. only after that separate approval, provision and execute staging
   acceptance.

Phase 2 remains blocked until staging acceptance passes, the Phase 1 exit
report is complete, and Phase 1 is explicitly closed.

## CC-001 compliance

- Current lifecycle step: CI and evidence acceptance.
- Granted approval: Gate C proposal/refinement, bounded implementation,
  implementation commit/push/draft PR, user-performed merge, and preparation
  of the post-merge evidence record.
- Not granted: evidence-record commit/push/pull request, explicit Gate C
  evidence acceptance, image publication, external-resource changes,
  migration, deployment, staging execution, Phase 1 closure, or Phase 2.
- Repository boundary: the approved Gate C adapter, runbook, validator,
  fixtures, evidence record, and aligned documentation.
- External boundary: no external system may be changed.
- Material deviations: none; the refinements make existing cloud-neutral,
  evidence, and approval requirements explicit.
- Evidence status: local, pull-request, and post-merge `main` evidence is in
  `docs/phase-1-gate-c-evidence.md`; explicit acceptance and evidence-record
  publication are pending.
- Next approval required: explicit Gate C evidence acceptance and approval to
  commit/push/open a draft PR for the evidence-only branch.
