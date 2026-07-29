# Render deployment adapter runbook

Status: Repository adapter implemented; external configuration and execution
not approved
Canonical contract: `MCDC-001` version `1`
Evidence checklist: `EP-001`
Change control: `CC-001`

## Purpose and boundary

This runbook maps the provider-neutral contract in
`docs/deployment-contract.md` to Render. It is a repository-owned deployment
adapter, not authorization to create, modify, or deploy a Render resource.

The non-live mapping is:

`deploy/render/render.staging.yaml.example`

Privileged, temporary bootstrap/migration bases are kept in the separate
non-live mapping:

`deploy/render/render.staging-jobs.yaml.example`

The root `render.yaml` was intentionally removed so Render cannot
auto-discover and sync unresolved or unapproved infrastructure. Do not rename,
copy, materialize, or apply the example until the exact external-resource
proposal and staging-execution proposal are explicitly approved under
`CC-001`.

The example contains no real service, database, registry, region, plan,
credential, domain, owner, or production identifier.

## Adapter topology

| Canonical process | Render mapping | Profiles | Artifact | Ingress |
|---|---|---|---|---|
| `core-api` | Image-backed web service | `production,api` | Accepted immutable OCI digest | HTTPS API |
| `outbox-dispatcher` | Image-backed background worker | `production,dispatcher` | Same accepted immutable OCI digest | None |
| `schema-migration` | Protected one-off job from an isolated temporary base | `production,migration` | Same accepted immutable OCI digest | None |

The Render `worker` service type hosts only the already implemented durable
PostgreSQL-outbox dispatcher. It does not add Redis, Render Key Value, Render
Workflows, Kafka, a generic queue, or a generic worker framework.

## Provider conformance

`supported` means the repository mapping has a Render mechanism.
`open` means the external-resource proposal must choose and verify the
mechanism before deployment. No row represents a provisioned or tested live
resource.

| Canonical capability | Render mechanism | Configuration/evidence | Status | Plan or cost dependency |
|---|---|---|---|---|
| Prebuilt immutable OCI image | Image-backed service with digest reference | Non-live example; Gate B promotion evidence | Supported structurally | Registry access/storage remains open |
| Request-serving API | `type: web`, `runtime: image` | API service block | Supported structurally | Region, plan, scaling, and cost open |
| Continuous dispatcher | `type: worker`, `runtime: image` | Dispatcher service block | Supported structurally | Worker plan and cost open |
| API readiness | Render `healthCheckPath` | `/actuator/health/readiness` | Supported structurally | Live behavior requires staging |
| No dispatcher ingress | Background-worker service type | Dispatcher service block | Supported structurally | Live verification requires staging |
| Separate API/dispatcher secrets | Service-specific unsupplied environment entries | Adapter validator | Supported structurally | Secret store and owners open |
| Migration-only credential | One-off job from `mychandha-staging-migration-base` with a disjoint allowlist | Job adapter validator and later execution evidence | Supported structurally | Network and live provider evidence remain open |
| Database role bootstrap | One-off job from `mychandha-staging-bootstrap-base` using the packaged bootstrap authority | Job adapter validator and later execution evidence | Supported structurally | Statement logging and live provider evidence remain open |
| Render client address | Edge-only ingress plus first `X-Forwarded-For` hop | Strategy validator, adversarial tests, and later spoof probe | Supported structurally; live proof blocking | No extra plan |
| TLS and restricted database path | Code-owned `verify-full`, per-process CA path, Render secret file, and provider network configuration | Runtime/job validators, OCI group check, CA checksum, and later TLS evidence | Supported structurally; live proof blocking | Plan/network capability open |
| Dispatcher backlog evidence | Provider process state plus bounded acceptance checks under `PF-EX-002` | Existing safe backlog/metric boundary and two-person evidence review | Supported structurally; live proof blocking | No monitoring provider for Gate D |
| Log routing and retention | Render/provider log controls or approved drain | External proposal and test event | Open and blocking | Plan, destination, and retention open |
| Backups and restore | Selected PostgreSQL plan capabilities | External proposal and restore drill | Open and blocking | Database plan/cost open |
| Digest rollback | Retained prior registry digest and Render image deployment | `EP-001` artifact and rehearsal records | Supported conditionally | Registry retention and schema compatibility required |
| Preview prevention | `previews.generation: off` and non-live file path | Adapter validator | Supported structurally | None |

An open blocking row prevents materialization or deployment. The
external-resource proposal must convert every staging-required row to
`supported` with an exact mechanism, owner, cost, and evidence source.

## Materialization procedure

These are review steps, not currently authorized execution steps.

1. Obtain approval for the external-resource proposal, including service names,
   region, plans, scaling, cost, registry access, owners, secret store, network
   paths, logs, metrics, alerts, backups, retention, and cleanup.
2. Resolve every open blocking conformance row.
3. Copy the non-live example to the explicitly approved Blueprint path or
   reproduce its exact mapping through the approved Render API/Dashboard
   process.
4. Replace both service-name placeholders with approved identifiers.
5. Replace both image placeholders with the **same**
   `<registry>/<image>@sha256:<64-hex-digest>`.
6. Add a registry credential reference only when the approved registry is
   private. Never put the credential value in the Blueprint.
7. Supply environment values through the approved secret/configuration store.
8. Re-run the adapter validator against the materialized file, supplying the
   approved non-secret expectations:

   ```text
   RENDER_ADAPTER_EXPECTED_IMAGE=<registry/image@sha256:digest>
   RENDER_ADAPTER_EXPECTED_API_SERVICE_NAME=<approved-api-name>
   RENDER_ADAPTER_EXPECTED_DISPATCHER_SERVICE_NAME=<approved-dispatcher-name>
   sh scripts/validate-render-adapter.sh <approved-blueprint-path>
   ```

   Do not supply credentials to the validator.
9. Record the redacted conformance result in the `EP-001` package.
10. Obtain separate staging-execution approval before applying the
    configuration.

Do not configure source repository builds, a mutable tag as deployment
identity, source auto-deploy, preview resources, a database, a cron service,
Key Value/Redis, a private service, a disk, a domain, a pre-deploy command, or
an initial deploy hook through this adapter.

## Environment and secret allowlists

### API service

Required:

- `SPRING_PROFILES_ACTIVE=production,api`
- `API_DATABASE_URL`
- `API_DATABASE_USERNAME`
- `API_DATABASE_PASSWORD`
- `API_DATABASE_SSL_ROOT_CERTIFICATE=/etc/secrets/supabase-ca.crt`
- `SUPABASE_JWT_ISSUER`
- `SUPABASE_JWKS_URI`
- `SUPABASE_JWT_AUDIENCE=authenticated`
- `RATE_LIMIT_ENABLED=true`
- `RATE_LIMIT_CLIENT_ADDRESS_STRATEGY=render-edge-first-hop`

The URL, username, password, issuer, and JWKS URI remain unsupplied in the
repository example. The API service must not receive dispatcher or migration
database credentials.

### Dispatcher service

Required:

- `SPRING_PROFILES_ACTIVE=production,dispatcher`
- `DISPATCHER_DATABASE_URL`
- `DISPATCHER_DATABASE_USERNAME`
- `DISPATCHER_DATABASE_PASSWORD`
- `DISPATCHER_DATABASE_SSL_ROOT_CERTIFICATE=/etc/secrets/supabase-ca.crt`

Approved bounded outbox tuning may be added later only when the
external-resource proposal records the value and reason. The dispatcher must
not receive API, Supabase, or migration configuration and must have no inbound
route.

### Migration execution

Required:

- `SPRING_PROFILES_ACTIVE=production,migration`
- `MIGRATION_DATABASE_URL`
- `MIGRATION_DATABASE_USERNAME`
- `MIGRATION_DATABASE_PASSWORD`
- `MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE=/etc/secrets/supabase-ca.crt`

The migration values must be available only to the approved short-lived
runner. They must not be attached to either long-lived Render service.

Common non-secret JVM or pool tuning can be added only after the selected plans
are approved. Environment groups must not be used if they create collisions or
blur the process allowlists.

## Protected bootstrap and migration execution

Do not use a Render one-off job based on the API or dispatcher. Render one-off
jobs inherit all environment variables from their base service, so Gate D uses
separate temporary bootstrap and migration bases with disjoint allowlists.

Do not use an API or dispatcher `preDeployCommand` requiring the migration
credential. Do not run migration from a developer laptop or keep a migration
service running.

The repository-defined protected release runner:

- must pull the same accepted digest as the runtime services;
- receives only the migration credential for migration;
- reaches PostgreSQL through approved TLS and network controls;
- runs the `production,migration` profile and exits;
- fails closed on bootstrap, Flyway, or startup failure; and
- produces sanitized `EP-001` execution evidence.

The bootstrap base receives only the bootstrap connection credential and the
three one-time environment role passwords. It sets
`BOOTSTRAP_DATABASE_SSL_ROOT_CERTIFICATE=/etc/secrets/supabase-ca.crt`. The
migration base receives only the migration connection credential and the
matching migration CA path. Both use the safe idle command as their ordinary
process, are created only for a separately approved operation, and must be
deleted no later than one hour after the job reaches a terminal state.

Upload the approved Supabase CA to each service as the Render runtime secret
file `supabase-ca.crt`. The non-root OCI user is a member of Render's documented
group `1000`. Bootstrap exports `PGSSLMODE=verify-full` and `PGSSLROOTCERT`;
Java/Flyway profiles set pgJDBC `verify-full` and `sslrootcert` as code-owned
properties. The protected temporary-job adapter uploads the file without
logging it, waits for the resulting deployment, records only its SHA-256
checksum, and removes its owner-only temporary request file.

Do not put `sslmode`, `sslrootcert`, or `sslfactory` in the migration URL. Do
not embed the CA in the OCI image or evidence. A missing, relative, unreadable,
or wrong CA is a hard failure.

If a GitHub-hosted runner cannot reach the selected database without weakening
network policy, it is not acceptable. A private or provider-local alternative
must be proposed and approved.

## Database prerequisites

Before Flyway:

1. identify the approved database owner/bootstrap identity;
2. execute the credential-free statements in
   `scripts/bootstrap-database-roles.sql` through the approved mechanism;
3. create or bind separate environment login roles;
4. verify stable non-login role grants;
5. verify TLS and network policy; and
6. record sanitized ownership and grant evidence.

The migration role applies V1/V2 and owns the approved objects. The API and
dispatcher roles must not own schemas, bypass RLS, or apply migrations.

## Immutable artifact verification

Before any materialization or deployment:

1. identify an accepted successful `main` CI run;
2. verify its evidence manifest and checksums;
3. verify the full source commit, OCI manifest digest, archive digest, SBOM
   digest, vulnerability-report digest, and secret-scan evidence digest;
4. run the separately approved no-rebuild promotion workflow;
5. verify the registry digest equals the accepted OCI manifest digest;
6. verify both Render services and the migration runner reference that exact
   digest;
7. verify `linux/amd64`; and
8. verify the current and prior compatible digests remain pullable for the
   approved recovery period.

A commit tag is an alias only. A mutable tag by itself is never deployable
identity.

## Controlled rollout

After external-resource and execution approval:

1. confirm approvals, owners, cost, cleanup, and evidence locations;
2. verify the artifact and provider conformance;
3. verify resource, secret, TLS, and network inventories;
4. execute role bootstrap;
5. run the migration profile;
6. verify schema version, ownership, grants, and negative privileges;
7. deploy the dispatcher and API from the same digest;
8. wait for dispatcher process state and API readiness;
9. run JWT, tenant, audit, idempotency, inbox, outbox, health, metrics, and
   log-safety acceptance, including forwarded-address spoof resistance;
10. run the accepted native notification and bounded alert/check matrix with
    two-person evidence review;
11. accept routing only after all blocking checks pass; and
12. finalize the staging `EP-001` evidence package.

Stop immediately for a missing approval, unresolved blocking capability,
mutable/mismatched digest, credential overlap, failed migration, failed
negative privilege check, failed readiness, unsafe log, or incompatible
rollback.

## Health and acceptance

### API

- Render health check: `/actuator/health/readiness`
- Diagnostic liveness: `/actuator/health/liveness`
- Smoke test: valid JWT to `/api/v1/platform/me`
- Negative smoke test: cross-tenant request denied
- Error smoke test: RFC 9457 response with a safe correlation ID

### Dispatcher

- Render process state must remain healthy.
- No inbound public endpoint exists.
- Acceptance must record safe pending depth, oldest age, retries, stale
  processing, and dead-letter state through the existing bounded boundary.
- The external proposal must define collection and alerting without granting
  general application-table access.

The dispatcher does not expose an HTTP health endpoint. If the selected Render
and monitoring capabilities cannot provide the required evidence, stop and
propose the smallest separate repository change.

### Staging ownership and alert exception

`PF-EX-001` makes `Mohi-Chanu` the sole Render workspace member and
`hazwaTech` the protected GitHub operation/evidence reviewer. One initiates and
the other reviews; account or API-key sharing is prohibited. The owner must be
available for every execution window.

`PF-EX-002` routes documented Render deploy, image-pull, one-off-job, and API
health notifications to `Mohi-Chanu`. Dispatcher recovery, outbox age/dead
letters, authentication/authorization anomalies, Supabase capacity, backup,
and restore are bounded pre/during/post checks reviewed by both identities.
Capture sanitized evidence within 24 hours. Missing evidence fails Gate D.

Both exceptions apply only to synthetic Phase 1 staging and expire at cleanup
or 2026-09-30. They do not authorize external configuration and must never be
used as production readiness evidence.

## Rollback and forward fix

- Never reverse a Flyway migration automatically.
- Record schema compatibility before choosing a prior image.
- Use only a retained, accepted registry digest.
- After an application rollback, recheck API readiness, authentication,
  cross-tenant denial, audit continuity, dispatcher backlog, and log safety.
- Correct a database defect through a reviewed forward migration unless a
  separately approved restore is required.
- A restore is a recovery action, not an application rollback shortcut.

## Evidence and cleanup

Use `docs/evidence-package.md`. Gate C repository evidence records structural
mapping and validator results only. Provider, deployment, database, migration,
acceptance, recovery, and cost items remain pending until separately approved
execution.

For every later-created resource, record:

- accountable and deletion owners;
- resource identifier and environment;
- plan and cost;
- retention or deletion condition;
- secret revocation;
- database and backup disposition;
- registry artifact retention; and
- cleanup completion evidence.

## Repository validation

From the repository root:

```text
sh scripts/validate-render-adapter.sh
sh scripts/test-validate-render-adapter.sh
sh scripts/validate-render-job-adapter.sh
sh scripts/test-validate-render-job-adapter.sh
sh scripts/validate-staging-workflow.sh
sh scripts/test-validate-staging-workflow.sh
sh scripts/test-staging-job-contracts.sh
sh scripts/test-validate-staging-evidence.sh
sh scripts/validate-foundation.sh
mvn verify
```

These commands make no provider API call and do not authorize materialization,
publication, provisioning, migration, or deployment.

## CC-001 state

- Repository implementation: approved, merged through PR `#4`, and CI verified.
- Evidence record: accepted, merged through PR `#5`, and `main` CI verified.
- Gate D staging-resource decisions: accepted 2026-07-26.
- Gate D repository implementation: approved 2026-07-27; local validation in
  progress.
- Image publication and GitHub environment changes: not approved.
- External resources and staging execution: not approved.
- Next approval after local validation: commit, push, and draft pull request
  for CI evidence.
