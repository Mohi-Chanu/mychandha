# Phase 1 Gate D provider-conformance remediation proposal

Status: Implemented and locally validated; authoritative CI not started
Prepared: 2026-07-29
Approved: 2026-07-29
Scope: `PF-HS-001` through `PF-HS-004` only
Change control: `CC-001`
Deployment contract: `MCDC-001`
Evidence checklist: `EP-001`

## Decision outcome

The user explicitly accepted `PF-R-001` through `PF-R-004` and
`PF-EX-001`/`PF-EX-002` on 2026-07-29. This accepts the design, staging-only
exception boundaries, compensating controls, owners, and expiry documented
here. It does not approve repository implementation.

Repository implementation of only the bounded scope below was explicitly
approved on 2026-07-29. That approval does not authorize a commit, push, pull
request, merge, workflow execution, package or image publication, GitHub
configuration, provider configuration, resource creation, spending, migration,
deployment, acceptance run, restore, rollback, cleanup, Phase 1 closure, or
Phase 2 work.

## Outcome and recommendation

The current Render and Supabase selections can remain within the accepted
minimal staging envelope if all four remediations are accepted and later
implemented and verified:

| Resolution | Accepted decision | Remaining hard-stop evidence |
|---|---|---|
| `PF-R-001` | Replace the Render mapping of the generic trusted-CIDR resolver with an explicit Render edge adapter that accepts only the first valid `X-Forwarded-For` hop | Repository implementation, CI, and live spoof-resistance evidence |
| `PF-R-002` | Add a provider-neutral, per-process database CA-file contract and map it to `/etc/secrets/supabase-ca.crt` on Render | Repository implementation, CI, provider materialization, and TLS evidence |
| `PF-R-003` | Keep Render Hobby and the USD 43 baseline; make `Mohi-Chanu` the sole Render control-plane member and constrain `hazwaTech` to GitHub-mediated operation/review | Account/price revalidation, two-person GitHub control evidence, and owner availability |
| `PF-R-004` | Use Render-native failure notifications for the owner and require two-person review of bounded application/database/recovery checks in GitHub evidence | Provider materialization, route tests, bounded checks, and two-person evidence review |

No Redis, Render Key Value, Render Workflows, Kafka, generic worker tier,
third-party monitoring platform, log drain, new database, or new application
service is required by these remediations.

## Scope

The proposed repository implementation would be limited to:

1. client-address resolution and production validation for Render ingress;
2. Supabase CA-file configuration for API, dispatcher, bootstrap, and migration;
3. the Render staging ownership/operator contract and budget validation;
4. the minimum Gate D alert and bounded-check contract;
5. automated positive and tamper-rejection tests for those controls; and
6. affected security, deployment, operations, evidence, status, and roadmap
   documentation.

## Non-goals

This proposal does not:

- provision or modify GitHub, GHCR, Render, Supabase, PostgreSQL, DNS, email,
  Slack, or another external system;
- select a Render Pro workspace or amend `R1-OPB-001`;
- add a monitoring or notification provider;
- add distributed rate-limit storage or claim multi-instance correctness;
- change rate-limit thresholds or the accepted `DR-002` application-layer
  requirement;
- weaken `verify-full`, hostname verification, CA verification, secret
  separation, tenant isolation, RLS, or application authorization;
- change SQL migrations, domain behavior, REST APIs, database roles, or the
  API/dispatcher/migration process split;
- publish or deploy the previously accepted OCI candidate;
- execute acceptance, restore, rollback, cleanup, or other workflows; or
- start Phase 2.

## Verified baseline

The preflight in
`docs/phase-1-gate-d-external-resource-preflight.md` established:

- Render web-service traffic reaches the application through Render's load
  balancer; the application port is not directly reachable from the public
  internet.
- Render instructs applications to use `X-Forwarded-For` for the real client
  address, and Render's published first-hop clarification conflicts with the
  repository's terminal-hop behavior.
- Supabase requires its project CA for PostgreSQL `verify-full`.
- Render makes runtime secret files available at `/etc/secrets/<filename>` and
  documents group `1000` access for a non-root Docker user.
- Render Hobby permits one workspace member. The current Render Pro plan is
  USD 25/month flat and would raise the current approximately USD 43 baseline
  to approximately USD 68, above `R1-OPB-001`.
- Render native notifications cover deploy/build failure, image-pull failure,
  failed one-off jobs, and an unhealthy running service. Supabase Reports
  provide database health views, while custom metric alerts require an
  additional monitoring stack.

Provider facts and prices remain time-sensitive assumptions. They must be
revalidated immediately before any later external-resource approval.

## `PF-R-001` — Render client-address adapter

### Problem

`TrustedProxyClientAddressResolver` currently trusts a forwarded header only
when the socket peer matches a configured CIDR and returns the terminal header
hop. That is a safe generic proxy pattern, but it cannot be materialized from
an authoritative Render ingress CIDR list and its terminal-hop rule does not
match Render's documented first-hop behavior.

Guessing a CIDR, trusting a Render outbound range, trusting all addresses, or
using a Cloudflare-specific header would weaken the control and is rejected.

### Recommended design

Introduce an explicit client-address strategy at the HTTP security adapter
boundary:

| Strategy | Intended environment | Trust rule |
|---|---|---|
| `direct` | Local/test or a provider with no trusted forwarding | Ignore forwarding and use the canonical socket peer |
| `trusted-proxy-cidr` | A deployment with authoritative ingress CIDRs | Preserve the existing CIDR boundary and select the accepted hop for that provider contract |
| `render-edge-first-hop` | Render web service only | Require exactly one bounded `X-Forwarded-For` field and select its first canonical IP address |

`render-edge-first-hop` is justified by two provider invariants that must both
remain true:

1. the public application port is reachable only through Render's load
   balancer; and
2. Render places the real client address in the first `X-Forwarded-For` hop.

The strategy is an application/security adapter concern, not a domain
dependency and not a generic license to trust forwarded headers. The
production API must fail startup unless one approved strategy is selected.
The Render adapter validator must require `render-edge-first-hop`, reject a
simultaneous CIDR strategy, and reject legacy terminal-hop configuration.

The resolver must retain the current single-header, maximum-length,
maximum-hop, IP canonicalization, and safe fallback protections. In Render
mode, a missing, duplicate, empty, overlong, over-depth, or malformed header
must not be accepted as a client assertion. It must fall back to the canonical
socket peer, increment only a bounded anomaly counter, and make the rate-limit
health contributor report degraded after a bounded repeated-failure threshold.
This fail-safe can collapse malformed traffic into the proxy bucket, but it
cannot let an attacker select independent buckets. Raw header or address
values must not be logged or returned.

The servlet container remains configured not to interpret forwarded headers.
Application code owns this one bounded decision so authentication,
authorization, logs, and other framework behavior do not begin trusting proxy
metadata.

### Required tests and evidence

Repository tests must prove:

- first-hop selection for the Render strategy;
- a client-supplied later hop cannot change the selected address;
- duplicate, malformed, blank, overlong, and over-depth fields are rejected;
- IPv4, IPv6, and IPv4-mapped IPv6 canonicalization;
- direct and CIDR strategies remain isolated;
- production startup rejects missing, conflicting, or unapproved strategies;
- the Render adapter rejects the legacy CIDR/terminal mapping; and
- no raw address is logged, returned, or used as an unbounded metric label.

Live staging acceptance must send forged `X-Forwarded-For` values from one
actual client and prove they cannot create independent client-address rate
limit buckets. It must also prove distinct actual clients are not collapsed
when a safe second test source is available. Sanitized evidence records only
outcomes and correlation IDs, not raw client addresses.

### Stop rule

`PF-HS-001` remains open if Render changes either ingress invariant, the live
spoof-resistance test cannot distinguish actual and supplied addresses, or the
implementation falls back to guessed network ranges.

## `PF-R-002` — Supabase CA materialization

### Provider-neutral contract

Add an integrity-sensitive database trust-material input to `MCDC-001`.
Every PostgreSQL process using `verify-full` must receive:

- an absolute readable CA certificate path;
- a provider-recorded SHA-256 checksum for the expected CA material;
- the exact database hostname covered by the server certificate; and
- a client configuration that performs both CA and hostname verification.

The CA certificate is public trust material, not a database credential, but it
must still be protected against unauthorized replacement. Certificate
contents, connection URLs, usernames, and passwords must not enter logs or the
evidence package.

### Render mapping

The Render adapter maps the contract to the exact runtime file:

`/etc/secrets/supabase-ca.crt`

The same verified CA file may be present in all four processes. Database
credentials remain per-process and must not be shared through an environment
group.

| Process | Proposed path input | Client mapping |
|---|---|---|
| API | `API_DATABASE_SSL_ROOT_CERTIFICATE` | pgJDBC `sslmode=verify-full` plus `sslrootcert` |
| Dispatcher | `DISPATCHER_DATABASE_SSL_ROOT_CERTIFICATE` | pgJDBC `sslmode=verify-full` plus `sslrootcert` |
| Migration | `MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE` | pgJDBC/Flyway `sslmode=verify-full` plus `sslrootcert` |
| Bootstrap | `BOOTSTRAP_DATABASE_SSL_ROOT_CERTIFICATE` | `PGSSLMODE=verify-full` and `PGSSLROOTCERT` |

The production profile configuration must set `verify-full` as code-owned
policy rather than a user-selectable weaker default. Each runtime must fail
before database use when its file is missing, unreadable, not a regular file,
or outside the approved absolute-path contract.

The OCI image remains non-root. Its `mychandha` user must be added to the
documented Render secret-file group `1000` without making that user root,
changing file ownership broadly, or embedding the CA in the image.

For temporary bootstrap and migration bases, the protected workflow may pass
the approved CA contents as a masked integrity-sensitive value to the Render
adapter. The adapter must use Render's secret-file API, never echo the
contents, use an owner-only temporary file with cleanup on every exit, wait for
the resulting service configuration to be active, and delete the temporary
base after the job. Long-lived API and dispatcher secret files are materialized
only during a later separately approved external-resource step.

### Required tests and evidence

Repository validation must prove:

- all four process allowlists require their own CA path input;
- `verify-full` cannot be replaced by `require`, `prefer`, a non-validating
  socket factory, or an omitted root certificate;
- bootstrap exports `PGSSLROOTCERT`;
- migration rejects a missing root-certificate contract;
- the non-root OCI user has group `1000` membership;
- missing, unreadable, unexpected-path, and tampered configuration fixtures
  fail closed;
- temporary Render request/evidence fixtures contain no certificate contents
  or credentials; and
- CA checksum evidence is sanitized and integrity-verified.

Later provider evidence must capture the Supabase CA SHA-256 checksum, Render
secret-file name, file readability for each process, successful verified TLS
connections, a wrong-CA negative test, and absence of weaker client modes.

### Stop rule

`PF-HS-002` remains open if any database path lacks the exact CA, uses a
weaker SSL mode, embeds trust material in the OCI image, mixes credentials, or
cannot produce both positive and negative TLS evidence.

## `PF-R-003` — Render ownership and budget

### Options considered

| Option | Cost effect | Result |
|---|---:|---|
| Render Hobby with one Render member and GitHub-mediated backup duties | No change; baseline remains approximately USD 43/month | Recommended for bounded Phase 1 staging |
| Render Pro with both people as workspace members | Approximately USD 68/month baseline | Rejected under current `R1-OPB-001`; requires a budget amendment |
| Shared Render account or shared API key | No plan increase | Prohibited because it destroys identity, audit, and revocation boundaries |

### Recommended staging role mapping

| Identity | Approved responsibility | Explicit limitation |
|---|---|---|
| `Mohi-Chanu` | Sole Render workspace member and accountable owner; billing, dashboard-only configuration, provider recovery, notification ownership, and cleanup | Must not share the account or personal API key |
| `hazwaTech` | GitHub protected-environment reviewer, repository/evidence reviewer, and initiator or reviewer for approved GitHub-mediated operations | No direct Render dashboard membership or control-plane recovery on Hobby |

For every GitHub-mediated staging operation, one identity initiates and the
other reviews/approves; self-approval is prohibited. Render credentials stay
inside the protected GitHub environment and are never provided to
`hazwaTech` as a reusable personal credential.

This leaves a single-person Render control-plane recovery risk. The proposal
recommends a time-bounded Gate D staging exception only because:

- the environment contains synthetic data only;
- no production traffic or production acceptance is allowed;
- the owner must be available for every approved execution window;
- the backup can review evidence and block GitHub-mediated changes; and
- the exception expires with the staging envelope, no later than 2026-09-30.

Production readiness, an unattended staging environment, owner unavailability,
or a requirement for independent dashboard recovery requires a multi-member
plan and a separately approved `R1-OPB-001` amendment.

### Stop rule

The decision component of `PF-HS-003` is accepted. The hard stop remains open
for account/price revalidation, protected-operation evidence, and owner
availability. It reopens at the decision level if checkout exceeds the budget,
Render changes the one-member rule, or a second direct control-plane operator
becomes required.

## `PF-R-004` — minimum alert and bounded-check contract

### Recommended Gate D split

Gate D staging uses provider-native continuous notifications where they exist
and bounded operator checks where the selected plans do not provide a complete
alert engine.

| Condition | Gate D mechanism | Delivery/review | Test evidence |
|---|---|---|---|
| API deploy/build failure | Render native notification | `Mohi-Chanu`; result also recorded for two-person GitHub evidence review | Controlled failed or canceled non-live test where safe |
| OCI image-pull failure | Render native notification | `Mohi-Chanu`; two-person evidence review | Invalid test reference only in an isolated approved test |
| API becomes unhealthy | Render health check and native notification | `Mohi-Chanu`; two-person evidence review | Controlled readiness failure and recovery |
| Bootstrap or migration one-off job fails | Render native notification | `Mohi-Chanu`; two-person evidence review | Safe failing job fixture without credentials in output |
| Dispatcher process exits | Bounded Render event/log and process-recovery check; no undocumented continuous notification is claimed | Both identities review sanitized `EP-001` evidence | Controlled dispatcher termination and recovery |
| Outbox oldest age over five minutes | Bounded acceptance query/metric check | Both identities review sanitized `EP-001` evidence | Seeded synthetic backlog, detection, recovery, and dead-letter checks |
| Dead-letter growth | Bounded acceptance query/metric check | Both identities review sanitized `EP-001` evidence | Synthetic failed delivery and bounded recovery |
| Repeated authentication/authorization denial | Bounded safe metric/log review | Both identities review sanitized `EP-001` evidence | Synthetic denial burst without subjects, tokens, or enumeration |
| Database CPU, memory, disk, or connections | Supabase Reports review before, during, and after the acceptance window | `Mohi-Chanu` records; `hazwaTech` reviews sanitized evidence | Timestamped screenshots/text with project identifiers and sensitive values removed |
| Backup presence or restore failure | Supabase backup/restore execution result and bounded operator check | `Mohi-Chanu` records; `hazwaTech` reviews | Separately approved restore drill and integrity result |

Provider logs must be reviewed and sanitized evidence captured within 24 hours.
The acceptance window must record a pre-execution baseline, each induced event,
route receipt time, recovery time, and a post-execution review. A missing
native notification or missing two-person review is a failed Gate D result.

### Staging-only limitation

Render Hobby cannot provide a second direct workspace member, and no external
monitoring or shared notification destination is approved. Therefore:

- continuous Render notifications terminate at the accountable owner;
- the backup operator's destination is the protected GitHub operation/evidence
  record, not a second real-time page; and
- custom application, database, backup, and restore signals are bounded Gate D
  checks, not continuous production alerts.

This is a proposed `CC-001` exception, not a production monitoring claim. It
uses the same synthetic-data, owner-availability, and 2026-09-30 expiry as
`PF-R-003`. If independent real-time delivery to both operators is required,
the work must stop for a separate notification/monitoring dependency,
security, cost, retention, and ownership proposal.

### Stop rule

The limited staging contract for `PF-HS-004` is accepted. The hard stop remains
open until later evidence proves every listed native route and bounded check.
It reopens at the decision level on owner unavailability or any attempt to
apply this exception to production. A missing route, incomplete two-person
review, or unsafe evidence fails the execution gate.

## Accepted `CC-001` exceptions

The user accepted these exact exceptions on 2026-07-29. They remain dormant
until a separately approved staging environment and execution exist and cannot
authorize either.

| ID | Rule varied and reason | Boundary and expiry | Compensating controls, owner, and removal evidence |
|---|---|---|---|
| `PF-EX-001` | The accepted backup operator cannot independently access the Render control plane because Hobby permits one member and Pro exceeds `R1-OPB-001` | Synthetic Phase 1 staging only; never production; expires at cleanup or 2026-09-30, whichever comes first | `Mohi-Chanu` remains available and accountable; no account/key sharing; all GitHub-mediated operations use two-person initiation/review; `EP-001` records role checks, owner coverage, incidents, and final resource/credential removal |
| `PF-EX-002` | The selected native plans do not continuously deliver every application, dispatcher, database, backup, and restore signal to two operators; adding a provider is outside the accepted envelope | Bounded Gate D acceptance windows with synthetic data only; never production; expires at cleanup or 2026-09-30, whichever comes first | Render-native owner alerts where documented; pre/during/post bounded checks; `hazwaTech` reviews sanitized evidence; missing checks fail the gate; `Mohi-Chanu` owns response; closure records route removal and whether a later monitoring proposal is required |

Neither exception changes tenancy, authorization, RLS, TLS, credential
separation, backup integrity, or evidence-redaction requirements. A security
incident, owner absence, customer data, public production traffic, expired
date, or need for unattended operation terminates the exception immediately.

## Exact prospective repository impact

The user authorized implementation of only the repository changes in this
table on 2026-07-29. The implementation remains local until a later separate
GitHub approval.

| Area | Prospective change |
|---|---|
| Client-address source | Refactor the rate-limit client-address resolver behind explicit `direct`, trusted-CIDR, and Render-edge strategies; retain bounded parsing and HMAC-derived keys |
| Rate-limit configuration | Replace the Render example's guessed-CIDR requirement with an explicit Render strategy and fail-closed production validation |
| TLS configuration | Add per-profile pgJDBC `verify-full` and root-certificate path inputs; add startup validation |
| OCI runtime | Preserve the non-root user and add only the documented secret-file group membership |
| Bootstrap/migration | Require CA path inputs, export `PGSSLROOTCERT`, validate pgJDBC root-certificate configuration, and upload/delete temporary Render secret files without logging contents |
| Render adapters | Update non-live API/dispatcher/job examples, allowlists, validators, fixtures, and sanitized evidence contracts |
| Protected workflow | Add the protected CA-material input only to bootstrap/migration jobs and keep unrelated jobs free of database/CA material |
| Acceptance | Add spoof-resistance and alert/bounded-check evidence probes without raw IP, identity, token, certificate, or provider payload output |
| Documentation | Align `MCDC-001`, security, observability, operations, Render runbook, Gate D evidence, status, roadmap, exit report, and the preflight |

No SQL migration, Maven dependency, external SDK, provider SDK, REST API, or
domain/application module change is proposed.

## Security and privacy impact

- Render-specific trust remains at the HTTP adapter boundary.
- A tenant header still selects requested scope and never grants access.
- Rate-limit client identity remains an HMAC-derived in-memory key; raw
  addresses are not retained or exposed.
- Database credentials remain split across API, dispatcher, migration, and
  bootstrap processes.
- CA material is integrity-sensitive but does not become an application secret
  or a shared credential bundle.
- `verify-full`, exact host verification, non-root execution, RLS, membership
  authorization, append-only audit evidence, and log/evidence redaction remain
  mandatory.
- Synthetic identities and data only are permitted in staging.

## Migration, compatibility, and rollback impact

There is no database migration and no API compatibility change.

Repository rollback before external execution is a normal revert to the last
accepted code and documentation. After implementation, however, the accepted
OCI digest
`sha256:a19c285d61c62927093bad4adc898a66122adb37978d3894f6f53c54d0e206b0`
must not be deployed because it lacks these remediations. A new post-merge
`main` CI build must produce one immutable `linux/amd64` digest, SBOM, Gitleaks
and Trivy evidence, and receive explicit acceptance before publication or
deployment.

The old artifact remains historical evidence; it is not rewritten or silently
promoted. Any later runtime rollback must use a retained compatible digest
that includes the accepted provider-conformance controls.

## Validation plan for a later implementation

The implementation gate must run:

1. Java 21 `mvn verify` with Docker/Testcontainers;
2. resolver and production-startup positive and adversarial tests;
3. profile-specific TLS configuration and wrong/missing-CA tests;
4. bootstrap/migration wrapper positive and tamper-rejection tests;
5. Render runtime/job adapter and workflow allowlist tests;
6. secret/evidence sanitization tests;
7. `sh scripts/validate-foundation.sh`;
8. POSIX shell syntax, Actionlint, Markdown-link, and `git diff --check`
   validation;
9. full-history Gitleaks;
10. a `linux/amd64` OCI build because the Dockerfile changes;
11. CycloneDX SBOM generation; and
12. Trivy with the existing zero HIGH/CRITICAL and secret thresholds.

Live spoof, TLS, alert, restore, and recovery evidence remains a later
external/execution gate and cannot be substituted with repository tests.

## Evidence package

The later `EP-001` package must add:

- approved `PF-R-001` through `PF-R-004` decisions and the accepted or rejected
  `PF-EX-001`/`PF-EX-002` dispositions and expiry;
- exact source commit, main CI run/job, new OCI digest, SBOM, Gitleaks, and
  Trivy results;
- client-address strategy configuration and spoof-resistance results;
- CA filename, expected checksum, per-process readable-file result, positive
  `verify-full` connections, and wrong-CA rejection;
- checkout plan and final `R1-OPB-001` calculation;
- Render owner and GitHub-mediated backup mapping;
- each native notification event, destination, receipt timestamp, and recovery;
- each bounded check, result, reviewer, and 24-hour log-review completion;
- deviations, residual risks, and explicit acceptance or rejection; and
- integrity-checked sanitized evidence with no IPs, tokens, connection
  strings, certificate contents, email addresses, or personal data.

## Exit criteria

This remediation gate passes only when:

- all four decisions and proposed `PF-EX-001`/`PF-EX-002` dispositions are
  explicitly accepted;
- the exact repository implementation is separately approved and completed;
- local and authoritative post-merge CI evidence passes and is accepted;
- a new OCI digest containing the remediations is accepted;
- account/provider assumptions, price, owner availability, and exception
  expiry are revalidated;
- CA material is correctly installed and all four TLS paths pass;
- live client-address spoof resistance passes;
- every native notification and bounded check has accepted evidence;
- `R1-OPB-001` remains satisfied;
- no unapproved dependency or external change is introduced; and
- `CC-001` and `EP-001` remain satisfied.

Passing this remediation gate does not provision resources or authorize
staging execution. Exact external-resource approval and each execution remain
later gates.

## Source register

Official provider sources revalidated on 2026-07-29:

- [Render web services and ingress](https://render.com/docs/web-services)
- [Render client-address guidance](https://render.com/articles/how-render-handles-ddos-attacks)
- [Render first-hop clarification](https://feedback.render.com/features/p/send-the-correct-x-forwarded-for)
- [Render environment variables and secret files](https://render.com/docs/configure-environment-variables)
- [Render Docker secret-file access](https://render.com/docs/docker-secrets)
- [Render secret-file API](https://api-docs.render.com/reference/add-or-update-secret-file)
- [Render current workspace plans](https://render.com/docs/new-workspace-plans)
- [Render workspace members and roles](https://render.com/docs/team-members)
- [Render notifications](https://render.com/docs/notifications)
- [Render health checks](https://render.com/docs/health-checks)
- [Supabase PostgreSQL SSL enforcement](https://supabase.com/docs/guides/platform/ssl-enforcement)
- [Supabase PostgreSQL client example](https://supabase.com/docs/guides/database/psql)
- [Supabase Reports](https://supabase.com/docs/guides/telemetry/reports)
- [Supabase Metrics API](https://supabase.com/docs/guides/telemetry/metrics)
- [pgJDBC connection properties](https://jdbc.postgresql.org/documentation/use/)

## CC-001 compliance

- Current lifecycle step: bounded repository implementation complete and
  locally validated; GitHub publication approval pending.
- Approvals granted: preparation of this bounded proposal and explicit
  acceptance of `PF-R-001` through `PF-R-004` and
  `PF-EX-001`/`PF-EX-002`; implementation of only the exact repository scope
  in this proposal.
- Approvals not granted: commit, push, pull request, merge, workflow
  execution, package/image publication,
  GitHub/provider configuration, provisioning, spending, migration,
  deployment, acceptance execution or evidence acceptance, restore, rollback,
  cleanup, hard-stop or Gate D closure, or Phase 2.
- Repository boundary: this proposal and status-document alignment only.
- External boundary: official read-only provider documentation verification;
  no account or resource change.
- Material deviations: none from the approved proposal-preparation scope.
- Evidence package: `docs/phase-1-gate-d-evidence.md`; remediation evidence not
  started.
- Local validation: Java 21 offline Maven verification passed 80 tests with
  PostgreSQL 17/Testcontainers, Checkstyle, PMD, JaCoCo, and SpotBugs;
  structural, adapter, workflow, shell, evidence, Actionlint, Gitleaks,
  Markdown-link, and whitespace checks passed. The accepted workstation
  Avast/Docker TLS limitation still blocks `apk`, local OCI completion, SBOM,
  and Trivy; no TLS control was weakened. The pinned runtime image separately
  proved non-root `mychandha` membership in group `1000`.
- Next approval required: explicit authorization to create
  `codex/phase-1-gate-d-provider-conformance`, commit the bounded changes, push
  it, open a draft pull request, and allow the automatic pull-request CI run.
  Manual release/staging workflow execution, publication, and external changes
  remain separate.
