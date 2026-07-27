# Phase 1 staging resource proposal

Status: Decisions accepted 2026-07-26; no implementation or external action approved
Gate: Phase 1 Gate D — staging resource and execution readiness
Prepared: 2026-07-26
Change control: `CC-001`
Deployment contract: `MCDC-001` version `1`
Evidence checklist: `EP-001`

## Decision outcome

The proposed non-production resource envelope, permanent decision records,
operational budget, ownership mapping, retention boundary, and cleanup date
were accepted on 2026-07-26. This acceptance authorizes preparation of the
bounded Gate D repository proposal only. It authorizes neither repository
implementation nor any GitHub, Supabase, Render, registry, migration,
deployment, acceptance, spending, or cleanup action.

## Decision register

These permanent records hold the reusable decisions. This proposal references
them and records only Gate D application, evidence, and exceptions.

| ID | Decision | Gate D status |
|---|---|---|
| `DR-001` | [Migration environment isolation](governance/DR-001-migration-environment-isolation.md) | Accepted; exact mechanism remains an implementation prerequisite |
| `DR-002` | [Application rate limiting](governance/DR-002-application-rate-limiting.md) | Accepted; implementation remains a staging prerequisite |
| `DR-003` | [OCI rollback policy](governance/DR-003-oci-rollback-policy.md) | Accepted; staging evidence remains pending |
| `R1-OPB-001` | [Release 1 approved operational budget](governance/R1-OPB-001-release-1-operational-budget.md) | Accepted; spending and provisioning remain separately gated |

The full cross-phase index is
[`docs/governance/README.md`](governance/README.md).

The proposal deliberately uses only the infrastructure needed by the Phase 1
application:

- one Supabase project for both Supabase Auth and its included PostgreSQL
  database;
- the existing `core-api` and durable `outbox-dispatcher` processes on Render;
- GHCR for the already implemented immutable-image promotion path; and
- a protected, short-lived bootstrap/migration mechanism whose bounded design
  is now proposed in `docs/phase-1-gate-d-repository-proposal.md`.

No Render PostgreSQL database is added because the Supabase project already
includes dedicated PostgreSQL. No Redis, Render Key Value, Kafka, Render
Workflow, generic worker framework, frontend, CDN, DNS, object storage,
custom domain, or third-party log/metrics platform is proposed.

## Gate D non-goals

Gate D does not:

- provision, modify, migrate, deploy, publish, or delete an external resource;
- approve a cost, account owner, provider agreement, or production workload;
- create credentials, test users, customer data, DNS, or public traffic;
- replace Supabase Auth, PostgreSQL, Render, or GHCR;
- add Phase 2 product behavior;
- claim production RPO/RTO, scale, compliance certification, or readiness; or
- merge proposal, repository, external-resource, execution, evidence, and
  closure approvals.

Only synthetic non-production data and operator-owned test identities are
permitted during the later staging execution.

## Proposed account and ownership boundary

The identities below are proposed because they already control the repository.
They require explicit confirmation before any provider resource is created.

| Responsibility | Proposed identity |
|---|---|
| Accountable owner and cost owner | `Mohi-Chanu` |
| Release, deployment, database, security, incident, restore, and cleanup owner | `Mohi-Chanu` |
| Authorized backup operator and reviewer | `hazwaTech` |
| Evidence recorder | Codex-assisted sanitized repository record, reviewed by `Mohi-Chanu` |

No shared human account is proposed. Provider MFA must be enabled for every
interactive operator. Machine credentials must be scoped to one purpose and
must not be reused as human credentials.

## Exact proposed resource inventory

Provider-generated opaque IDs cannot exist before creation. The desired names
below are exact; each generated project, service, environment, package, and
credential ID must be captured in `EP-001` immediately after creation. A name
collision or unavailable plan/region is a material change and stops execution.

| Provider | Desired name or reference | Type and plan | Region | Lifecycle |
|---|---|---|---|---|
| GitHub | `Mohi-Chanu/mychandha` environment `staging-release` | Protected Actions environment | Global | Retain while staging exists |
| GitHub | `Mohi-Chanu/mychandha` environment `staging-deploy` | Protected Actions environment | Global | Retain while staging exists |
| GHCR | `ghcr.io/mohi-chanu/mychandha` | Public OCI package, digest deployment only | Global | Retain current and one compatible prior digest |
| Supabase | organization `mychandha`; project `mychandha-staging` | Pro, one Micro compute project, spend cap enabled | Singapore | Delete at cleanup gate |
| Supabase | `mychandha-staging` IPv4 add-on | Dedicated database IPv4 | Singapore | Delete with project |
| Render | workspace `mychandha` | Hobby workspace | Global control plane | Delete at cleanup gate |
| Render | project `mychandha`; environment `staging` | Protected logical environment where supported | Singapore services | Delete at cleanup gate |
| Render | `mychandha-staging-api` | Image-backed web service, Starter, one instance | Singapore | Suspend when not testing; delete at cleanup |
| Render | `mychandha-staging-dispatcher` | Image-backed background worker, Starter, one instance | Singapore | Suspend with API; delete at cleanup |

A protected bootstrap/migration runner is required, but its Render resource
shape was intentionally left outside this resource decision. Render one-off
jobs copy all environment variables from a base service. Using the API or
dispatcher as that base would violate `DR-001` and `MCDC-001` credential
isolation. The bounded repository proposal now specifies separate,
short-lived bootstrap and migration bases; they require their own approval
before they can amend this execution inventory.

### Why Singapore

Render supports Singapore and cannot move an existing service to another
region. Supabase provides Singapore project placement and likewise binds a
project to its selected region. Co-locating the application and database in
Singapore is the smallest provider-neutral choice for the current India/APAC
operator context and avoids an unnecessary cross-region database path.

If the Supabase creation screen does not offer Singapore for the selected
organization and compute, stop. Do not silently substitute Mumbai or another
region because Render has no matching Mumbai application region.

## Release 1 governed operational budget

The estimate is in USD, excludes tax, and must be reconfirmed in both provider
checkout screens immediately before approval to provision.

| Item | Estimated recurring cost |
|---|---:|
| Supabase Pro with one included Micro project | `$25/month` |
| Supabase dedicated IPv4 add-on | `$4/month` |
| Render Hobby workspace | `$0/month` |
| Render Starter API | approximately `$7/month` |
| Render Starter dispatcher | approximately `$7/month` |
| GHCR public container package | `$0` under the current public-package policy |
| Baseline estimate | approximately `$43/month`, plus tax and overage |
| Governed ceiling | `R1-OPB-001` |

Starter services are billed proportionally while running. Suspending both
Render services outside an approved acceptance window can reduce compute cost,
but suspension must not be represented as uptime evidence. Supabase Pro and
the IPv4 add-on remain billed until removed.

The Supabase spend cap stays enabled. No autoscaling, read replica, PITR
add-on, dedicated Render outbound IP set, paid Render workspace, paid log
drain, or additional database is approved. Any expected charge over the
`R1-OPB-001` ceiling is a hard stop and requires an amended, explicitly
approved budget record.

### Capacity, support, and residency

- Each Render Starter instance is proposed at 512 MB RAM and 0.5 CPU. The API
  and dispatcher remain at exactly one instance each.
- Supabase Micro provides 1 GB RAM, 60 direct connections, and 200 pooler
  connections. Application pool totals must remain below 60 with operational
  headroom.
- Staging must stop and request a plan amendment after an out-of-memory restart,
  repeated CPU saturation, database connection use at or above 80% for five
  minutes, disk use at or above 80%, or sustained readiness failure. Gate D
  does not authorize autoscaling.
- Supabase Pro provides email support. Render Hobby uses the provider's
  self-service/community support boundary. No uptime SLA is claimed.
- Application, database, and provider logs remain in Singapore where the
  provider places regional workload data, subject to each provider's control
  plane and subprocessors. Only synthetic data is permitted; this gate makes
  no legal data-residency or regulated-data claim.
- Exit consists of a sanitized schema/evidence export if approved, credential
  revocation, resource deletion, and verification that only approved GHCR
  digests and repository evidence remain.

## Artifact and registry policy

- The release candidate must come from a successful accepted `main` CI run.
- The protected `staging-release` workflow must promote the exact OCI archive
  without rebuilding.
- The deployment identity is
  `ghcr.io/mohi-chanu/mychandha@sha256:<manifest-digest>`.
- The GHCR package is proposed as public because the source repository is
  public and public pull access removes a long-lived registry credential from
  Render. A request to keep the image private is a material refinement and
  must add a Render registry credential scoped only to `read:packages`.
- The current digest and one schema-compatible prior digest must remain
  pullable until Gate D closes and its `DR-003` rollback evidence is accepted.
- Mutable tags may aid discovery but never identify a deployment.

The currently verified evidence-merge candidate is commit
`6cf89fa62464c9e2f16ca1df29a47748edebf6eb`, CI run `30207094828`, with OCI
manifest digest
`sha256:bedff6884128fba53d1111563048af927443f20d6e43e53d1f3bd83f7e599400`.
Its Actions artifact expires on 2026-08-09. If it expires before a separately
approved promotion, rerun `main` CI and accept the new evidence; never deploy
an expired or unverified archive.

## Database and network contract

The Supabase project supplies both Auth and PostgreSQL. Render PostgreSQL is
therefore not required.

Proposed connectivity:

- enable the Supabase dedicated IPv4 add-on;
- use direct PostgreSQL connections on port `5432` with certificate validation
  for the API, dispatcher, bootstrap, migration, and restore paths;
- restrict Supabase database and pooler access to the current documented
  Render Singapore outbound CIDRs before any runtime credential is used;
- prohibit `0.0.0.0/0`, `::/0`, developer-laptop migrations, transaction-mode
  pooling, and unverified connection-string fallback; and
- record the exact applied CIDRs and provider revision in sanitized evidence.

Render's ordinary outbound CIDRs are shared rather than dedicated. A dedicated
three-IP Render set is currently unnecessary and would add a Pro workspace
plus a substantial monthly charge. If Supabase cannot safely maintain the
shared Singapore CIDR allowlist, or the ranges change during execution, stop
and amend this proposal; do not open the database globally.

### Database identities

| Login identity | Group role | Secret destination | Prohibited use |
|---|---|---|---|
| Provider bootstrap owner | None; administrative prerequisite only | Protected bootstrap runner only | Application, dispatcher, or routine Flyway traffic |
| `mychandha_staging_migration` | Object/migration authority | Protected migration runner only | Long-lived services |
| `mychandha_staging_api` | `mychandha_api` | Render API service only | Migration and dispatcher execution |
| `mychandha_staging_dispatcher` | `mychandha_dispatcher` | Render dispatcher only | API and migration execution |

All login roles must be `NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION
NOBYPASSRLS`, except that the provider bootstrap owner retains only the
provider-defined administration required to create and bind those roles.

The existing credential-free bootstrap SQL and V1/V2 Flyway migrations remain
the schema authority. Password values must never appear in SQL files, workflow
inputs, commands, logs, or evidence.

## Identity and test-user policy

- Supabase Auth is the only identity provider in this gate.
- Issuer, JWKS URI, and audience are non-secret environment configuration.
- Signing keys must be asymmetric and rotation capability must be verified.
- Public sign-up and anonymous sign-in remain disabled.
- Use only manually approved, operator-owned synthetic test identities.
- Custom SMTP and Resend are not required for this bounded acceptance because
  it will not send mail to external users. If email delivery becomes an
  acceptance requirement, stop and propose it separately.
- No Supabase service-role key is supplied to the application.

## Configuration and secret placement

| Store | Allowed secret classes |
|---|---|
| GitHub `staging-release` | Only credentials required to publish the verified image, if GitHub's workflow token is insufficient |
| GitHub `staging-deploy` | Render deployment API credential and sanitized provider identifiers; no runtime database password |
| Render API service | API database credential only |
| Render dispatcher service | Dispatcher database credential only |
| Protected migration mechanism | Migration database credential only |
| Protected bootstrap mechanism | Provider bootstrap credential and one-time role-secret material only |

Secrets are generated at provisioning, rotated at least every 90 days, and
rotated immediately after suspected exposure, operator removal, or boundary
change. Bootstrap material is removed immediately after role creation. Every
remaining machine credential is revoked during cleanup. Environment groups
must not be used for database credentials because they would blur process
allowlists.

## Observability, alerts, and retention

The minimal staging plan uses native provider capabilities and sanitized
evidence; it does not add a monitoring vendor.

- Render Hobby logs: seven-day provider retention.
- Supabase Pro API/database/Auth logs: seven-day provider retention.
- GitHub verification artifacts: existing 14-day workflow retention.
- Sanitized checksums, event IDs, findings, and acceptance summaries:
  repository evidence retained with project history.
- API availability: Render readiness check plus deploy/service-failure
  notification to the accountable owner and backup operator.
- Dispatcher availability: Render process state plus bounded outbox
  depth/age/retry/dead-letter evidence.
- Database: Supabase capacity, connection, backup, and provider alert review.
- Alert tests must cover API readiness failure, dispatcher exit, backlog age
  over five minutes, repeated authentication/authorization denial without user
  enumeration, database saturation signal, and restore failure.

Raw provider logs, tokens, connection URLs, contact data, and copied personal
data must not enter the evidence package. Provider logs must be reviewed and
their sanitized evidence captured within 24 hours because the native retention
window is short.

### Rate-limit prerequisite

Render supplies DDoS protection, but its documented boundary leaves targeted
Layer 7 API abuse to the application. The current repository has no accepted
endpoint-class rate limiter, while `docs/security.md` requires rate limiting by
IP, subject, organization, and endpoint class.

`DR-002` makes the decision criteria explicit:

- provider edge or DDoS protection alone is not sufficient;
- a documented application-layer strategy must be approved, implemented, and
  verified before public staging traffic; and
- implementation technology may evolve later, but the application-layer
  security requirement cannot be deferred.

This is a hard stop for staging traffic. The bounded repository proposal
recommends a single-instance application rate limiter for the one-instance
staging service without adding Redis. It must be approved, implemented, and
verified before execution.

The first option is recommended for Gate D staging only. It must parse trusted
proxy headers safely, bound memory/cardinality, return RFC 9457 `429` responses,
avoid user enumeration, expose bounded metrics, and make no production-scale
claim. Scaling above one API instance would require a new distributed design
and approval.

## Backup, restore, rollback, and cleanup

- Supabase Pro daily backups with seven-day retention are the proposed staging
  baseline.
- No PITR add-on is proposed. Staging must record that daily backups cannot
  prove the production five-minute RPO.
- A separately approved in-place staging restore drill must use only synthetic
  data, record start/end time and integrity/audit-chain results, and reapply
  network and role checks before routing.
- Application rollback follows `DR-003`: use only the retained compatible
  prior OCI digest, never reverse Flyway, and use a forward fix for an
  incompatible schema.
- The recovery rehearsal targets a 60-minute RTO; a miss is recorded as a
  blocker or an explicitly owned staging limitation, never silently passed.

Cleanup occurs at the earliest of:

- 30 days after explicit Phase 1 closure;
- 14 days after explicit rejection or abandonment of staging; or
- 2026-09-30 unless an extension is separately approved.

The cleanup owner must suspend and delete Render services/environment/project,
delete the Supabase staging project after approved evidence/export handling,
revoke all provider and database credentials, remove GitHub environment
secrets, and retain only the approved current/prior GHCR digests and sanitized
repository evidence.

## Required repository proposal before execution

No external action should begin until a bounded Gate D repository proposal
resolves:

1. **Bootstrap/migration execution (`DR-001`):** exact short-lived Render mechanism,
   strict bootstrap-versus-migration secret separation, same accepted image
   digest for Flyway, failure behavior, network path, and sanitized evidence.
2. **Application rate limiting (`DR-002`):** the staging-only, one-instance control
   described above, or an approved provider alternative.
3. **Acceptance evidence collection:** safe JWT/tenancy, dispatcher backlog,
   alert, restore, rollback, and forward-fix probes without broad database
   access or sensitive logs.
4. **Adapter materialization:** exact Render configuration, generated-ID
   inventory template, protected workflow boundaries, validation, and
   tamper-rejection tests.

That proposal must preserve the current provider-neutral boundaries. It must
not add Redis, a generic queue/worker, Render Workflow, a second database, or a
third-party observability service unless a newly discovered blocking
capability proves one necessary and the proposal is amended.

## Proposed approval and execution sequence

1. Review and refine this proposal, including owner/operator mapping, public
   GHCR visibility, Singapore region, plans, `R1-OPB-001`, and cleanup date —
   completed 2026-07-26.
2. Explicitly approve or reject Gate D proposal decisions — accepted
   2026-07-26.
3. Review and separately approve the bounded Gate D repository proposal.
4. Implement and locally verify only that approved repository scope.
5. Separately approve commit, push, pull request, and merge.
6. Accept green CI evidence.
7. Reconfirm exact provider prices and identifiers; separately approve GitHub
   and external-resource creation.
8. Create resources without deploying application traffic; record generated
   IDs and security configuration.
9. Separately approve release publication, bootstrap, migration, deployment,
   acceptance, restore, rollback, and cleanup execution.
10. Produce and explicitly accept the Gate D `EP-001` package.
11. Review the Phase 1 exit report and explicitly close or reject Phase 1.

## Hard-stop conditions

### Infrastructure

Stop for:

- a provider, region, plan, recurring estimate, or owner change;
- a forecast over the governed `R1-OPB-001` budget;
- inability to use Singapore for both application and database; or
- an unavailable required provider capability.

### Security

Stop for:

- global database network access or unverifiable TLS;
- shared API, dispatcher, migration, or bootstrap secrets;
- unresolved `DR-002` application-layer rate limiting;
- unsupported role creation, RLS, or security-definer behavior; or
- unsafe logs, evidence containing secrets/personal data, or missing alert
  delivery.

### Deployment

Stop for:

- a mutable, rebuilt, mismatched, expired, or unavailable OCI digest;
- inability to retain the `DR-003` compatible rollback image;
- profile, credential, ingress, or artifact divergence from `MCDC-001`; or
- an unapproved Flyway or rollout sequence.

### Validation

Stop for:

- failed bootstrap, migration, readiness, or cross-tenant denial;
- failed audit-chain, idempotency, inbox, or durable-delivery evidence;
- failed restore, rollback, forward-fix, alert, or log-safety evidence; or
- incomplete or integrity-failing `EP-001` evidence.

### Governance

Stop for:

- an unapproved material change under `CC-001`;
- pressure to add an undeclared dependency without amended approval;
- a missing accountable owner, approval, retention, or cleanup record; or
- an attempt to treat Gate D acceptance as Phase 1 closure or Phase 2
  approval.

## Gate D exit criteria

Gate D passes only when:

- all `DR-001`, `DR-002`, and `DR-003` blockers and evidence requirements are
  resolved and explicitly accepted;
- every provider assumption is revalidated against the live selected plan and
  recorded in `EP-001`;
- `R1-OPB-001` and the exact provider cost inventory are explicitly approved;
- TLS, network, secret, identity, authorization, RLS, audit, and log-safety
  constraints pass;
- the exact immutable OCI deployment and compatible rollback evidence are
  accepted;
- migration, readiness, tenancy, durable-delivery, alert, restore,
  rollback, forward-fix, retention, and cleanup evidence pass;
- every applicable `EP-001` item is passed or has an explicitly accepted
  not-applicable reason; and
- `CC-001` remains satisfied with no unapproved material deviation.

Passing Gate D does not close Phase 1. The Phase 1 exit report and explicit
Phase 1 closure remain the next independent gate.

## Assumption register

Every assumption is time-sensitive. `Provider documentation` proves the
proposal basis; `live verification` is still required before provisioning or
execution.

| ID | Assumption | Proposal basis | Required verification |
|---|---|---|---|
| `AS-001` | Render supports prebuilt `linux/amd64` OCI images by digest | Provider documentation | Recheck selected service creation and digest pull |
| `AS-002` | Render supports separate web and background-worker environments | Provider documentation | Validate exact plan, profiles, ingress, and secret allowlists |
| `AS-003` | Render one-off jobs inherit all base-service environment variables | Provider documentation | Recheck before selecting the `DR-001` mechanism |
| `AS-004` | Supabase supports required PostgreSQL roles, RLS, security-definer routines, and Flyway migrations | Provider and PostgreSQL documentation | Execute bootstrap/migration and negative-privilege probes |
| `AS-005` | Singapore is available for both Render services and the Supabase project | Provider documentation | Confirm in both creation flows; stop on mismatch |
| `AS-006` | Supabase Pro provides the proposed backup and seven-day retention capabilities | Provider documentation | Confirm selected project plan and complete restore drill |
| `AS-007` | Render Singapore outbound CIDRs can be safely allowlisted by Supabase | Provider documentation | Record current CIDRs and prove restricted TLS connectivity |
| `AS-008` | Current public GHCR policy and Render pull behavior support credential-free immutable pulls | Provider documentation | Verify package visibility, exact digest pull, restart, and rollback |
| `AS-009` | Current provider prices fit `R1-OPB-001` | Provider pricing | Reconfirm checkout totals before external-resource approval |

## Provider evidence used for this proposal

Provider capabilities and prices are time-sensitive and must be rechecked
before external approval:

- [Render regions](https://render.com/docs/regions)
- [Render prebuilt images](https://render.com/docs/deploying-an-image)
- [Render one-off jobs](https://render.com/docs/one-off-jobs)
- [Render outbound IP addresses](https://render.com/docs/outbound-ip-addresses)
- [Render DDoS boundary](https://render.com/docs/ddos-protection)
- [Render logging retention](https://render.com/docs/logging)
- [Render cost model](https://render.com/articles/how-much-does-cloud-application-hosting-cost-for-small-businesses)
- [Supabase pricing](https://supabase.com/pricing)
- [Supabase database connections](https://supabase.com/docs/guides/database/connecting-to-postgres)
- [Supabase IPv4 pricing](https://supabase.com/docs/guides/platform/manage-your-usage/ipv4)
- [Supabase network restrictions](https://supabase.com/docs/guides/platform/network-restrictions)
- [Supabase Postgres roles](https://supabase.com/docs/guides/database/postgres/roles)

## CC-001 compliance

- Current lifecycle step: decision acceptance complete; bounded repository
  proposal prepared separately.
- Granted approval: Gate D proposal decisions, `DR-001`, `DR-002`, `DR-003`,
  and `R1-OPB-001` were accepted on 2026-07-26. This authorizes preparation of
  the bounded repository implementation proposal.
- Not granted: repository implementation, commit, push, pull request, merge,
  GitHub configuration, package publication, provider changes, spending,
  credential creation, migration, deployment, acceptance, cleanup, Phase 1
  closure, or Phase 2.
- Repository boundary: this proposal and alignment of the verified Gate C
  closure record only.
- External boundary: read-only provider documentation and GitHub evidence
  inspection; no external system was changed.
- Material deviations: none. `DR-001`, `DR-002`, `DR-003`, the categorized
  hard stops, assumption register, exit criteria, and `R1-OPB-001` make the
  existing migration, security, rollback, validation, and cost boundaries
  reusable without weakening them.
- Evidence-package location: Gate D package not started; it will use `EP-001`.
- Next approval required: review and explicit approval or refinement of
  `docs/phase-1-gate-d-repository-proposal.md`.
