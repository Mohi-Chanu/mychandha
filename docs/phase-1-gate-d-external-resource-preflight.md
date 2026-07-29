# Phase 1 Gate D external-resource preflight

Status: Preflight complete; bounded remediation implemented and locally validated
Verified: 2026-07-27
Gate: Phase 1 Gate D — external-resource readiness
Change control: `CC-001`
Evidence package: `EP-001`
Deployment contract: `MCDC-001` version `1`
Decisions: `DR-001`, `DR-002`, `DR-003`, `R1-OPB-001`

## Outcome

The accepted Singapore staging envelope remains technically available and its
baseline recurring estimate remains approximately USD 43 per month, excluding
tax and variable overage. GitHub Container Registry, Render, and Supabase still
provide the core capabilities assumed by the accepted proposal.

External-resource approval is not yet recommended. Read-only verification
found four material gaps:

1. Render's documented client-address contract does not match the accepted
   terminal-hop resolver, and no authoritative stable ingress-proxy CIDR set
   was found for `RATE_LIMIT_TRUSTED_PROXY_CIDRS`.
2. Supabase requires an explicit CA trust path for `verify-full`, while the
   accepted image and Render adapter do not yet materialize that CA for all
   Java and `psql` processes.
3. Render Hobby permits one workspace member, which does not satisfy the
   accepted interactive owner plus backup-operator model without a separately
   approved role refinement. Render Pro would raise the baseline forecast
   above `R1-OPB-001`.
4. The selected native plans do not document alert delivery for every accepted
   application, dispatcher, database, and restore condition.

These findings are preflight evidence, not permission to change the repository,
providers, budget, ownership, or monitoring boundary.

## Scope and non-goals

This package:

- revalidates public provider documentation and read-only GitHub evidence;
- fixes the desired external inventory for later approval;
- binds the proposed deployment to the accepted CI-built OCI candidate;
- reconciles provider prices with `R1-OPB-001`;
- defines the required secret, network, backup, retention, alert, and cleanup
  controls; and
- identifies facts that still require account-specific confirmation.

It does not:

- create or configure a GitHub environment or package;
- execute a GitHub workflow or publish an image;
- create a Supabase organization, project, add-on, user, key, role, or backup;
- create a Render workspace, project, environment, service, job, credential,
  secret, notification, or network rule;
- spend money, migrate a database, deploy, run staging acceptance, restore,
  roll back, or clean up;
- add Redis, Kafka, Render Key Value, Render Workflows, another database, or a
  monitoring vendor; or
- authorize a repository change, external-resource change, execution,
  acceptance, Phase 1 closure, or Phase 2.

## Verification method and limitations

Provider claims and prices were checked against current official documentation
on 2026-07-27. GitHub repository metadata, PR `#7`, workflow jobs, and artifact
records were checked through the read-only GitHub integration.

The GitHub integration confirmed that `Mohi-Chanu/mychandha` is public, is
owned by `Mohi-Chanu`, and uses `main` as its default branch. It did not have
permission to independently return collaborator permissions. The repository
owner's access was visible through repository metadata; `hazwaTech` access
remains a user attestation that must be reconfirmed before environment
reviewers are configured.

The available integration did not expose current environment or account-package
inventories. Repository evidence says that neither protected environment nor
the GHCR release package has been created, but their provider-side absence and
the availability of every desired name must be reconfirmed in the applicable
read-only account screen immediately before external-resource approval.

Public documentation proves that a region or plan is offered, not that a
particular account can select it at checkout. Generated IDs, exact Render
outbound CIDRs, Supabase project reference, project CA certificate, and service
URLs cannot exist before resource creation. Those values stay pending and must
be captured without secrets in the later `EP-001` package.

## Repository and CI baseline

| Item | Verified state |
|---|---|
| Repository | Public `Mohi-Chanu/mychandha`; default branch `main` |
| Gate D implementation | PR `#6`; merge commit `cc75d0c3c59dc9d11ef748bff2f3633770854ffd` |
| Accepted Gate D CI | Run `30245195541`; job `89910592727`; success |
| Evidence record | PR `#7`; merge commit `afbb2109c6eb442baf01331296cdf3e0be294503` |
| Evidence-record CI | Run `30249016269`; job `89922507241`; success |
| External state | No external action is authorized or recorded |

The evidence-record run proves the merged documentation state. It does not
replace the separately accepted deployment candidate.

## Accepted OCI source binding

The only currently accepted publication source is:

| Field | Accepted value |
|---|---|
| Source repository | `Mohi-Chanu/mychandha` |
| Source commit | `cc75d0c3c59dc9d11ef748bff2f3633770854ffd` |
| Source CI run | `30245195541` |
| Source CI job | `89910592727` |
| OCI manifest digest | `sha256:a19c285d61c62927093bad4adc898a66122adb37978d3894f6f53c54d0e206b0` |
| OCI artifact | ID `8644793537`, `mychandha-oci-cc75d0c3c59dc9d11ef748bff2f3633770854ffd` |
| OCI artifact archive digest | `sha256:0c329ca4668315f47be6e85ef834ab71106529ad7a4b49c8e51c802a80f7b995` |
| Verification artifact | ID `8644791692`, `verification-reports` |
| Verification artifact digest | `sha256:cf9a0ea66b05c68c2234d31cb9cfae6050e81233dd4dc73d8097aee42e2dc5f5` |
| Artifact expiry | 2026-08-10 07:14Z |
| Proposed registry identity | `ghcr.io/mohi-chanu/mychandha@sha256:a19c285d61c62927093bad4adc898a66122adb37978d3894f6f53c54d0e206b0` |

The OCI manifest digest identifies the image. The GitHub artifact digests
identify the downloadable archives and must not be substituted for the OCI
digest.

Publication, if later approved, must use the protected no-rebuild promotion
workflow against this exact retained archive. If the artifacts expire first,
`main` CI must be rerun and the resulting candidate must receive explicit
evidence acceptance. A rebuild, mutable tag, later documentation-only image,
or manually uploaded image is not an equivalent source.

## Exact desired resource inventory

Provider-generated IDs remain `Pending`. Names, plans, regions, ownership, and
lifecycle are fixed for review:

| Provider | Exact desired name/reference | Plan/type | Region | Lifecycle |
|---|---|---|---|---|
| GitHub | `Mohi-Chanu/mychandha` environment `staging-release` | Protected Actions environment | Global | Retain while staging exists |
| GitHub | `Mohi-Chanu/mychandha` environment `staging-deploy` | Protected Actions environment | Global | Retain while staging exists |
| GHCR | `ghcr.io/mohi-chanu/mychandha` | Public OCI package; digest-only deployment | Global | Retain current and one compatible prior digest |
| Supabase | organization `mychandha` | Pro billing organization; spend cap enabled | Control plane | Delete after project cleanup where supported |
| Supabase | project `mychandha-staging` | One included Micro compute project | Singapore, `ap-southeast-1` | Delete at cleanup |
| Supabase | `mychandha-staging` IPv4 add-on | Dedicated database ingress IPv4 | Singapore | Delete with project |
| Render | workspace `mychandha` | Hobby, subject to `PF-HS-003` | Global control plane | Delete at cleanup |
| Render | project `mychandha` | One project | Singapore services | Delete at cleanup |
| Render | environment `staging` | Protected logical environment where the selected plan supports it | Singapore services | Delete at cleanup |
| Render | `mychandha-staging-api` | Image-backed web service; Starter; one instance | Singapore | Suspend outside approved windows; delete at cleanup |
| Render | `mychandha-staging-dispatcher` | Image-backed background worker; Starter; one instance | Singapore | Suspend with API; delete at cleanup |
| Render | `mychandha-staging-bootstrap-base` | Temporary image-backed background worker; Starter; one instance | Singapore | Create only for bootstrap; delete within one hour of terminal job state |
| Render | `mychandha-staging-migration-base` | Temporary image-backed background worker; Starter; one instance | Singapore | Create only for migration; delete within one hour of terminal job state |

The two temporary bases are not generic workers. They exist only to satisfy
`DR-001` secret isolation because a Render one-off job inherits all
environment variables from its base service. Their inclusion here does not
authorize creation.

Name availability is account-specific. A collision, unavailable plan, region
substitution, or generated resource outside the listed owner is a material
change and stops execution.

## Environment capability matrix

| Capability | Required state | Read-only result | Gate state |
|---|---|---|---|
| Public repository | Required for the accepted GitHub protection/pricing assumptions | `Mohi-Chanu/mychandha` is public | Verified |
| Protected GitHub environments | Required reviewers and environment secrets | Available for public repositories; exact repository configuration absent/unverified | Capability verified; configuration pending |
| Public GHCR OCI package | Anonymous digest pulls and no registry pull secret | GHCR supports OCI and anonymous public pulls; public package usage is free | Capability verified; package absent/unverified |
| Singapore application region | Both long-running and temporary services | Render lists Singapore for services | Verified offering; checkout pending |
| Prebuilt image by digest | Exact no-rebuild OCI deployment | Render accepts public registry images by digest and requires `linux/amd64` | Verified |
| API service | Image-backed web service, Starter, one instance | Starter is 512 MB and 0.5 CPU | Verified offering |
| Durable dispatcher | Image-backed background worker, Starter, one instance | Background workers support Starter at 512 MB and 0.5 CPU | Verified offering |
| Isolated one-off execution | Same artifact, separate secret allowlists | Jobs inherit base artifact and all base environment variables and are billed per second | Verified; separate bases remain required |
| Service outbound allowlist source | Current Singapore CIDRs | Render exposes region-specific shared outbound CIDRs per created service | Capability verified; exact CIDRs pending |
| Ingress client-address trust | Stable spoof-resistant provider contract | Current public material identifies the real client as the first forwarded hop; no stable ingress-proxy CIDRs were found | Blocked by `PF-HS-001` |
| Render secrets | Per-service variables and CA file | Runtime secret files are available at `/etc/secrets`; Docker user access has an extra group requirement | Capability verified; adapter blocked by `PF-HS-002` |
| Render native log retention | Seven days | Hobby retains application/deploy/job logs for seven days; HTTP request logs require Pro | Verified with limitation |
| Render native notifications | Deploy, image-pull, job, and service-health failures | Email/Slack notifications support those events | Partially verified; blocked by `PF-HS-003`/`PF-HS-004` |
| Singapore database region | Exact Singapore project placement | Supabase lists Singapore general and `ap-southeast-1` specific regions | Verified offering; checkout pending |
| PostgreSQL direct connection | Port 5432 for long-lived backends and migrations | Supported; Render is documented as IPv4-only for this path | Verified |
| Dedicated database IPv4 | Required for Render direct connectivity | Pro add-on is available and currently USD 4/month | Verified offering |
| Database network restrictions | No global CIDRs | Supabase can restrict direct and pooled database routes by IPv4/IPv6 CIDR | Verified |
| TLS hostname and CA verification | `verify-full` for every database process | Supabase provides a project CA; clients must install/reference it | Capability verified; adapter blocked by `PF-HS-002` |
| Supabase Auth asymmetric signing | JWKS validation without a shared signing secret in the app | Current signing-key system supports asymmetric keys and public JWKS discovery | Verified |
| Supabase backups | Daily, seven days | Pro provides seven daily backups; restore is in place and causes downtime | Verified |
| Supabase logs | Seven days | Pro includes seven-day log retention | Verified |
| Cost control | Spend cap plus governed total | Pro spend cap exists but excludes compute, IPv4, and other opted-in items | Verified with limitation |

## Owner and operator mapping

| Responsibility | Approved identity | Preflight state |
|---|---|---|
| Accountable and cost owner | `Mohi-Chanu` | Retained |
| Release/deployment/database/security/incident/restore/cleanup owner | `Mohi-Chanu` | Retained |
| Backup operator and reviewer | `hazwaTech` | User-attested GitHub access; provider access requires resolution of `PF-HS-003` |
| Evidence recorder | Codex-assisted sanitized repository record | Retained; owner review required |

Required controls:

- every interactive GitHub, Render, and Supabase operator uses an individual
  account with MFA;
- no human account or provider token is shared;
- `Mohi-Chanu` remains billing owner and the only authority to accept cost or
  provider changes;
- protected GitHub environments require an approved reviewer and prevent
  self-review where the selected GitHub plan supports it;
- machine credentials are purpose-scoped and are never used as human
  credentials; and
- removal of either operator triggers immediate credential review and
  rotation.

Render Hobby's one-member limit prevents independent verification that both
approved operators can act in the Render control plane. Moving to Render Pro
would add USD 25/month and take the baseline to approximately USD 68/month,
which exceeds `R1-OPB-001`. Retaining Hobby while limiting `hazwaTech` to a
GitHub reviewer/operator is a material owner/operator refinement and requires
explicit approval.

## R1-OPB-001 reconciliation

Current public prices:

| Item | Current estimate | Budget treatment |
|---|---:|---|
| Supabase Pro | USD 25/month | Includes USD 10 compute credit |
| Supabase Micro compute | USD 10/month | Offset by the included USD 10 credit |
| Supabase dedicated IPv4 | USD 4/month | Not covered by Supabase spend cap |
| Render Hobby workspace | USD 0/month | One member; 5 GB outbound bandwidth included |
| Render Starter API | USD 7/month | Billed proportionally while running |
| Render Starter dispatcher | USD 7/month | Billed proportionally while running |
| Public GHCR package | USD 0 | Public package usage is free under current policy |
| GitHub-hosted Actions | USD 0 baseline | Standard runners are free for public repositories |
| Baseline recurring forecast | **USD 43/month** | Excludes tax and variable overage |
| `R1-OPB-001` ceiling | **USD 50/month** | Excludes tax |
| Baseline headroom | **USD 7/month** | Must cover variable usage |

The temporary bootstrap base, bootstrap job, migration base, and migration job
are each billed at the Starter per-second rate. Four full Starter instance-hours
would add approximately USD 0.04 using a 730-hour month. This is a planning
allowance, not approval to run them. Repeated runs, delayed base deletion,
bandwidth overage, storage growth, or any other metered charge consumes the
remaining headroom.

The Supabase spend cap covers selected usage categories but does not cover
compute or IPv4. The Render Hobby plan includes 5 GB of outbound bandwidth;
additional bandwidth is currently USD 0.15/GB. Cost and usage must therefore
be reviewed manually even when Supabase's spend cap is enabled.

The baseline still fits `R1-OPB-001`. Render Pro, a Supabase PITR add-on, a log
drain, dedicated Render outbound IPs, an additional service left running, or
another paid dependency does not fit the accepted baseline without a material
budget proposal.

## Secret and configuration controls

### GitHub `staging-release`

- Use the workflow-scoped `GITHUB_TOKEN` with `packages: write`; do not create a
  personal package token unless a documented limitation requires a separately
  approved credential.
- Store no database, Supabase, or Render runtime credential.
- Permit only the exact accepted run, commit, and OCI digest.

### GitHub `staging-deploy`

- Store only the Render API credential, approved non-secret provider IDs, the
  operation-specific bootstrap/migration material required by the accepted
  workflow, and synthetic acceptance credentials.
- Do not store the long-running API or dispatcher database connection
  credentials. Their one-time generated passwords may exist during bootstrap,
  but the resulting runtime connection secrets belong only on their matching
  Render service.
- Make bootstrap, migration, deploy, acceptance, rollback, and cleanup separate
  approved workflow runs.
- Never place tokens, JWTs, passwords, connection URLs, or raw provider output
  in dispatch inputs or evidence.

### Render

- API service: API database URL, username, and password only; Supabase issuer
  and JWKS URL are non-secret; no service-role key.
- Dispatcher service: dispatcher database URL, username, and password only.
- Bootstrap base: provider bootstrap connection credential and the three
  one-time role passwords only.
- Migration base: migration URL, username, and password only.
- No environment group may contain database credentials.
- The Supabase CA is non-secret integrity-sensitive configuration. If a Render
  secret file is selected, capture its filename, SHA-256 checksum, project
  source, rotation condition, and process allowlist without recording the
  certificate contents in evidence.

### Supabase

- Use asymmetric Auth signing keys and record the public issuer, audience, and
  JWKS URI.
- Disable public sign-up and anonymous sign-in.
- Use only operator-owned synthetic test identities.
- Do not provide a Supabase `service_role` or secret API key to MyChandha.
- Generate disjoint bootstrap, migration, API, and dispatcher database
  credentials; remove bootstrap material immediately after role creation.

Every machine credential rotates at least every 90 days and immediately after
suspected exposure, operator removal, or a trust-boundary change. Cleanup
revokes all remaining machine credentials.

## Network and TLS controls

Required topology:

1. Public clients reach only the Render API HTTPS endpoint.
2. Render terminates public TLS and redirects HTTP to HTTPS.
3. The dispatcher and temporary bases have no public ingress.
4. API, dispatcher, bootstrap, and migration processes use the Supabase direct
   hostname on port `5432`, never transaction pooling.
5. The Supabase dedicated IPv4 add-on supplies the Render-compatible direct
   route.
6. Supabase database network restrictions contain only the current outbound
   CIDRs reported for the approved Singapore Render services/bases. Empty,
   `0.0.0.0/0`, and `::/0` rules are prohibited.
7. GitHub-hosted runners do not connect directly to PostgreSQL.
8. Every database path uses `verify-full`, the exact Supabase hostname, and the
   project CA.

After any Render service recreation, region change, Supabase IPv4 toggle, or
restore, re-resolve the database address, re-read the Render outbound CIDRs,
reapply the allowlist, and repeat TLS plus negative network probes.

The exact outbound CIDRs remain pending until the services exist. This is an
expected generated value, not permission to use a broad rule. The client
address and CA materialization gaps are separately blocking because their
current adapter contracts are incomplete.

## Backup, restore, and recovery controls

- Supabase Pro daily backups with seven-day retention are the staging baseline.
- No PITR add-on is approved. The staging recovery point may therefore be up
  to one day old and does not prove the production five-minute RPO.
- Restore uses only synthetic data, is separately approved, and records
  provider event ID, start/end time, downtime, schema version, row counts,
  tenant/RLS probes, and audit-chain integrity.
- Supabase daily backups do not retain passwords for custom roles. After a
  restore, rotate the bootstrap, migration, API, and dispatcher login
  passwords as applicable, rebind service secrets, and re-run negative
  privilege checks before routing.
- A restore must revalidate the database network restrictions and CA/TLS path.
- The staging recovery rehearsal targets 60 minutes. A miss remains a blocker
  or an explicitly accepted staging limitation.
- Rollback follows `DR-003`: retain the current and one compatible prior OCI
  digest, never reverse Flyway, and use a forward fix when the prior
  application is schema-incompatible.
- Deleting the Supabase project permanently deletes its data and provider
  backups. Any approved sanitized export/evidence handling must finish first.

## Logging, alerts, and retention controls

| Evidence/control | Required retention or route | Current capability |
|---|---|---|
| Render application/deploy/job logs | Seven days; sanitized review within 24 hours | Hobby supports seven days |
| Render HTTP request logs | Not required if application correlation evidence is sufficient | Pro-only; unavailable on accepted Hobby plan |
| Supabase API/database/Auth logs | Seven days; sanitized review within 24 hours | Pro supports seven days |
| GitHub release/evidence artifacts | Fourteen days | Workflow and current artifacts conform |
| GHCR deployment images | Current and one compatible prior digest | Pending publication |
| Sanitized evidence summaries | Repository history | `EP-001`; no raw secrets or personal data |

Render native notifications can cover deploy failure, image-pull failure,
one-off job failure, and an unhealthy service. They do not establish the full
accepted alert set by themselves. Before execution, an approved route and test
must cover:

- API readiness failure;
- dispatcher exit;
- outbox backlog age above five minutes and retry/dead-letter pressure;
- repeated authentication/authorization denial without user enumeration;
- database connection, CPU, memory, disk, or I/O saturation;
- backup absence and restore failure; and
- delivery to both the accountable owner and the approved backup operator.

Supabase exposes database health reports, while its official guidance points
to Prometheus/Grafana for configurable resource alerts. No third-party
monitoring service is approved. The next proposal must therefore define which
conditions require continuous alert delivery and which are bounded Gate D
operator acceptance checks. It must not silently weaken the accepted alert
evidence or add a monitoring vendor.

## Cleanup controls

Temporary job bases:

- create only for their separately approved operation;
- delete no later than one hour after a success, failure, cancellation, or
  timeout; and
- record base service ID, job ID, terminal state, deletion time, and sanitized
  cost in `EP-001`.

The staging envelope cleanup date is the earliest of:

- 30 days after explicit Phase 1 closure;
- 14 days after explicit staging rejection or abandonment; or
- 2026-09-30 unless an extension is separately approved.

The cleanup owner must:

1. stop traffic and suspend/delete the Render services, temporary bases,
   environment, and project;
2. complete approved sanitized evidence/export handling, then delete the
   Supabase staging project and IPv4 add-on;
3. revoke Render, Supabase, database, and any package credentials;
4. remove GitHub environment secrets and protected environments;
5. remove unapproved GHCR versions while retaining only the accepted current
   and compatible prior digest for the approved period;
6. verify provider billing no longer forecasts the deleted resources; and
7. record immutable provider event IDs and sanitized completion evidence.

## Hard stops

The bounded remediation proposal requested by this preflight is prepared
in `phase-1-gate-d-provider-conformance-remediation-proposal.md`. Proposal
decisions and staging-only exceptions were accepted on 2026-07-29. Preparation
and decision acceptance do not resolve the hard stops: each remains open until
its repository implementation, CI evidence, and applicable account/provider
and live evidence are explicitly accepted under `CC-001`.

### `PF-HS-001` — Render client-address trust contract

The accepted resolver consumes the terminal `X-Forwarded-For` hop only when
the socket peer is inside configured trusted proxy CIDRs. Render's current
public guidance tells applications to read `X-Forwarded-For`, and Render's
published response identifies the real client as the first hop. No
authoritative stable Render ingress-proxy CIDRs were found.

Do not set `RATE_LIMIT_TRUSTED_PROXY_CIDRS` to a guessed range, a service
outbound CIDR, or a global network. Obtain an authoritative Render contract
and prepare a bounded repository proposal for any resolver/configuration
change. Staging traffic is blocked until spoof resistance is verified.

### `PF-HS-002` — Supabase CA materialization

Supabase requires its CA certificate for `verify-full`. The current Render
adapter contains no CA file contract or `sslrootcert` path, the bootstrap
wrapper does not set `PGSSLROOTCERT`, and the image user is not in Render's
documented group for Docker secret-file access.

Define a provider-neutral CA-file input in `MCDC-001` terms and a Render
adapter mapping for API, dispatcher, bootstrap, and migration. Preserve
per-process configuration and checksum the non-secret CA material. Do not
fall back to `sslmode=require` or a non-validating socket factory.

### `PF-HS-003` — Render operator model versus budget

Render Hobby permits one workspace member. The accepted model names
`Mohi-Chanu` as owner and `hazwaTech` as backup operator/reviewer. Either:

- refine the backup operator to GitHub-mediated operations only while
  `Mohi-Chanu` remains the sole Render member; or
- select a multi-member Render plan and amend `R1-OPB-001`.

Render Pro currently adds USD 25/month, raising the recurring baseline to
approximately USD 68/month. It cannot be selected under the current budget.
The role or budget decision requires explicit material-change approval.

### `PF-HS-004` — complete alert delivery

Native Render notifications cover service/deploy/job/image events, but current
public documentation does not establish delivery for every application,
dispatcher, database, backup, and restore condition in the accepted proposal.
Supabase exposes reports, but configurable alerts are documented through an
external metrics stack.

Define the minimum continuous alert set, the bounded acceptance-only checks,
both operator destinations, and a test method. Do not add a monitoring
provider without a separate dependency, cost, security, and retention
proposal.

## Required account-specific confirmations

These are mandatory immediately before any external-resource approval:

- GitHub account plan and current repository environment inventory;
- `hazwaTech` read/write permission sufficient for the proposed reviewer and
  operator duties;
- absence and availability of `staging-release` and `staging-deploy`;
- absence and namespace availability of `ghcr.io/mohi-chanu/mychandha`;
- Render account plan, billing currency/tax presentation, workspace name,
  service names, Singapore selection, Starter price, and notification
  destinations;
- Supabase organization/project name availability, Singapore
  `ap-southeast-1`, Pro/Micro checkout, USD 25 plan total, USD 4 IPv4 add-on,
  spend-cap state, and operator memberships; and
- forecast total at or below USD 50/month excluding tax.

Capture only sanitized screenshots or textual confirmations. Do not capture
tokens, payment details, connection strings, email addresses, project secrets,
or customer data.

## Standard evidence package checklist

The later external-resource and execution package must add to
`docs/phase-1-gate-d-evidence.md`:

- [ ] explicit approval reference for the exact external inventory;
- [ ] account owner, operators, MFA, and reviewer mapping;
- [ ] generated GitHub, GHCR, Render, and Supabase IDs;
- [ ] checkout prices, forecast, and `R1-OPB-001` result;
- [ ] accepted commit/run/artifact/OCI binding and no-rebuild publication;
- [ ] GHCR public visibility, digest pull, retention, and rollback pull;
- [ ] per-process secret inventory without values;
- [ ] Supabase CA checksum and every `verify-full` path;
- [ ] exact Render outbound CIDRs and Supabase allowlist;
- [ ] negative network and credential-isolation probes;
- [ ] Auth issuer/audience/JWKS/signing-key configuration;
- [ ] Render and Supabase log routes, access, and retention;
- [ ] alert routes and test events for the accepted set;
- [ ] backup presence, restore, password rotation, and integrity evidence;
- [ ] temporary base/job creation, termination, deletion, and cost;
- [ ] API/dispatcher deployment event IDs and exact image digest;
- [ ] staging acceptance, rollback, and forward-fix results;
- [ ] final cost, cleanup due date, and cleanup owner; and
- [ ] integrity-checked sanitized evidence manifest plus explicit acceptance.

## Source register

Official sources revalidated on 2026-07-27, with the four hard-stop sources
revalidated again on 2026-07-29:

- [GitHub deployment environments](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments)
- [GitHub Actions artifact retention](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository)
- [GitHub Packages and public-package pricing](https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages)
- [GHCR permissions and anonymous public pulls](https://docs.github.com/en/packages/learn-github-packages/about-permissions-for-github-packages)
- [Render regions](https://render.com/docs/regions)
- [Render prebuilt images](https://render.com/docs/deploying-an-image)
- [Render instance types](https://render.com/docs/compute-plans)
- [Render current workspace plans](https://render.com/docs/new-workspace-plans)
- [Render one-off jobs](https://render.com/docs/one-off-jobs)
- [Render outbound IP addresses](https://render.com/docs/outbound-ip-addresses)
- [Render environment variables and secret files](https://render.com/docs/configure-environment-variables)
- [Render Docker secret-file access](https://render.com/docs/docker-secrets)
- [Render logs and retention](https://render.com/docs/logging)
- [Render notifications](https://render.com/docs/notifications)
- [Render managed TLS](https://render.com/docs/tls)
- [Render DDoS boundary](https://render.com/docs/ddos-protection)
- [Render client-address guidance](https://render.com/articles/how-render-handles-ddos-attacks)
- [Supabase pricing](https://supabase.com/pricing)
- [Supabase regions](https://supabase.com/docs/guides/platform/regions)
- [Supabase database connections](https://supabase.com/docs/guides/database/connecting-to-postgres)
- [Supabase dedicated IPv4](https://supabase.com/docs/guides/platform/ipv4-address)
- [Supabase IPv4 pricing](https://supabase.com/docs/guides/platform/manage-your-usage/ipv4)
- [Supabase network restrictions](https://supabase.com/docs/guides/platform/network-restrictions)
- [Supabase SSL enforcement](https://supabase.com/docs/guides/platform/ssl-enforcement)
- [Supabase backups](https://supabase.com/docs/guides/platform/backups)
- [Supabase cost controls](https://supabase.com/docs/guides/platform/cost-control)
- [Supabase JWT signing keys](https://supabase.com/docs/guides/auth/signing-keys)
- [Supabase Auth configuration](https://supabase.com/docs/guides/auth/general-configuration)
- [Supabase access control](https://supabase.com/docs/guides/platform/access-control)
- [pgJDBC SSL configuration](https://jdbc.postgresql.org/documentation/ssl/)

## CC-001 compliance and exact next gate

- Current lifecycle step: preflight complete; bounded remediation decisions
  and repository implementation approved; local implementation and validation
  complete subject to the accepted workstation OCI-build TLS limitation.
- Granted: local preflight and bounded remediation-proposal preparation plus
  read-only provider verification; `PF-R-001` through `PF-R-004` and
  `PF-EX-001`/`PF-EX-002` accepted; bounded repository implementation.
- Not granted: commit, push, pull request, merge,
  GitHub configuration, package publication, workflow execution, provider
  change, spending, migration, deployment, acceptance, restore, rollback,
  cleanup, Phase 1 closure, or Phase 2.
- Material deviations discovered: `PF-HS-001` through `PF-HS-004`.
- Evidence location: this document and the pending external sections of
  `docs/phase-1-gate-d-evidence.md`.
- External boundary: only public documentation and read-only GitHub metadata
  were inspected.

The exact next gate is explicit authorization for a named branch, commit,
push, and draft pull request.
External-resource approval should be requested only after the repository
changes are merged, authoritative CI evidence is explicitly accepted, and the
remaining account/provider and live-evidence portions of the four hard stops
are ready for separately approved execution.
