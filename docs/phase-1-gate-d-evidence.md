# Phase 1 Gate D evidence package

Status: Base Gate D evidence accepted; provider remediation locally validated; external execution not started
Gate: Phase 1 Gate D — staging execution and acceptance
Evidence package: `EP-001`
Deployment contract: `MCDC-001` version `1`
Change control: `CC-001`
Decisions: `DR-001`, `DR-002`, `DR-003`, `R1-OPB-001`
Last updated: 2026-07-29

## Package identity

| Item | State | Evidence |
|---|---|---|
| Gate and evidence version | Passed | Gate D; `EP-001-GATE-D-1` |
| Repository | Passed | `Mohi-Chanu/mychandha` |
| Repository implementation branch | Passed | `codex/phase-1-gate-d` |
| Baseline commit | Passed | `6cf89fa62464c9e2f16ca1df29a47748edebf6eb` |
| Full implementation commit | Passed | `7b124dd1cc19987de2322863f850da80f1645da2` |
| Pull request and merge commit | Passed | PR `#6`; merged as `cc75d0c3c59dc9d11ef748bff2f3633770854ffd` on 2026-07-27 |
| Pull-request merge-test commit | Passed | `6bcf88febe5d19ec8c6ea9c6ff9f0ef2db783724` |
| Pull-request CI | Passed | `CI` run `30244571128`, run number `23`, job `89908695051`, attempt `1`; 2026-07-27 07:00:16Z through 07:03:13Z |
| Post-merge `main` CI | Passed and accepted | `CI` run `30245195541`, run number `24`, job `89910592727`, attempt `1`; 2026-07-27 07:10:45Z through 07:14:43Z |
| External environment and resources | Pending | No staging resource has been provisioned or changed |
| Evidence retention and deletion owner | Passed for repository CI | GitHub Actions artifacts retained for 14 days through 2026-08-10; accessible to authorized repository users and deletable by an authorized repository operator |
| Evidence recorder and acceptor | Passed | Codex-assisted record; repository CI evidence explicitly accepted by `Mohi-Chanu` on 2026-07-27 |

## Scope and approval

| Item | State | Evidence |
|---|---|---|
| Staging-resource decisions | Passed | `docs/phase-1-staging-resource-proposal.md`; accepted 2026-07-26 |
| Repository implementation proposal | Passed | `docs/phase-1-gate-d-repository-proposal.md`; approved 2026-07-27 |
| Repository implementation | Passed and merged | Implementation commit `7b124dd1cc19987de2322863f850da80f1645da2`; PR `#6`; merge commit `cc75d0c3c59dc9d11ef748bff2f3633770854ffd` |
| Repository CI evidence | Passed and accepted | Post-merge `main` run `30245195541`, job `89910592727`; explicitly accepted 2026-07-27 |
| Non-goals preserved | Passed | No Redis, Kafka, Key Value, Workflow, extra database, or Phase 2 behavior |
| External action authorization | Pending | Not granted |
| Unapproved actions not performed | Passed | No package publication, protected-environment change, provider mutation, migration, deployment, or staging execution |
| Deviations | Passed | None from the approved repository scope |

## Provider-conformance remediation repository evidence

The user accepted `PF-R-001` through `PF-R-004`,
`PF-EX-001`/`PF-EX-002`, and then explicitly approved implementation of only
the bounded repository scope on 2026-07-29. The implementation remains local.
Commit, push, pull request, workflow execution, publication, provider changes,
spending, and deployment are not authorized.

| Control | Local repository implementation | Evidence state |
|---|---|---|
| `PF-R-001` | Explicit `direct`, `trusted-proxy-cidr`, and `render-edge-first-hop` strategies; bounded first-hop parsing; safe socket-peer fallback; fixed-cardinality anomaly metric; readiness degradation; Render adapter and spoof-resistance checks | Local tests and repository validation passed |
| `PF-R-002` | Per-process absolute readable CA-file inputs; code-owned `verify-full`; URL override rejection; `PGSSLROOTCERT`; Render secret-file materialization contract; non-root group `1000`; sanitized checksum evidence | Local tests and repository validation passed; live TLS pending |
| `PF-R-003` | `Mohi-Chanu` sole Render owner and `hazwaTech` GitHub-mediated reviewer boundary recorded under `PF-EX-001`; no shared account or key | Repository documentation complete; account evidence pending |
| `PF-R-004` | Native owner-notification and two-person bounded-check matrix recorded under `PF-EX-002` | Repository documentation complete; live route/check evidence pending |
| Scope guard | No dependency, SQL migration, domain behavior, REST API, extra service, monitoring provider, Redis, worker tier, or budget change | Local validation passed |

Local validation on 2026-07-29 recorded:

- Java `21.0.12`, Maven `3.9.16`, and Docker Engine `29.6.2`;
- `mvn -B -o verify`: 80 tests, zero failures/errors/skips, PostgreSQL
  `17.10` Testcontainers integration, Checkstyle zero, PMD, JaCoCo, and
  SpotBugs zero findings;
- resolver, production-startup, health degradation/recovery, TLS URL-override,
  missing-file, adapter, wrapper, workflow, and evidence tamper tests;
- `scripts/validate-foundation.sh`, POSIX shell syntax, Actionlint `1.7.12`,
  Markdown local links, `git diff --check`, pinned Gitleaks `8.30.1` full
  history and changed/new working files; and
- an ephemeral pinned-runtime check proving `mychandha` remains non-root and
  belongs to Render secret-file group `1000`.

The local `linux/amd64` OCI build again stopped at Alpine package retrieval
because the Linux container received the known Avast-generated CA and rejected
it. No TLS or scanner control was weakened. Consequently, local OCI, CycloneDX,
and Trivy results remain unavailable and must be supplied by authoritative CI.

The accepted historical OCI digest
`sha256:a19c285d61c62927093bad4adc898a66122adb37978d3894f6f53c54d0e206b0`
predates these remediations and is no longer deployment-eligible if this
implementation is accepted. It remains immutable historical evidence. A new
post-merge `main` OCI digest, SBOM, Gitleaks result, Trivy result, and explicit
evidence acceptance are required before any later publication or deployment.

## Repository source and validation

| Item | State | Evidence |
|---|---|---|
| Java 21 `mvn -o verify` | Passed | Java `21.0.12`; Maven `3.9.16`; 67 tests, zero failures/errors/skips |
| PostgreSQL/Testcontainers integration tests | Passed | Docker Engine `29.6.2`; PostgreSQL `17.10`; V1/V2 and runtime profiles passed |
| Checkstyle, PMD, JaCoCo, and SpotBugs | Passed | Zero Checkstyle violations; PMD and JaCoCo completed; zero SpotBugs findings |
| Rate-limit deterministic/concurrency/filter tests | Passed | 15 focused tests, including concurrency, refill, expiry, cache bound, proxy, response, and filters |
| Render service/job adapter validation | Passed | Positive and all tamper-rejection cases |
| Protected workflow validation | Passed | Checksum-verified Actionlint `1.7.12` plus positive/tamper contract tests |
| Bootstrap/migration wrapper validation | Passed | Synthetic missing-input, TLS, command, and credential-boundary tests |
| Evidence schema and sanitization validation | Passed | jq `1.8.1`; positive and tamper-rejection fixtures |
| Foundation structural validation | Passed | `scripts/validate-foundation.sh` |
| Secret scanning | Passed | Pinned Gitleaks `8.30.1`: local source/history checks passed; post-merge CI scanned 19 commits and found no leaks |
| Whitespace and Markdown-link validation | Passed | `git diff --check`; all local Markdown targets resolve |
| OCI build, SBOM, and Trivy | Passed in authoritative CI | Post-merge `main` run `30245195541` built the retained `linux/amd64` OCI archive, generated CycloneDX `1.7`, and recorded zero Trivy HIGH/CRITICAL vulnerabilities and zero secrets |
| Local OCI limitation | Accepted without waiver | Avast HTTPS interception presents an untrusted generated CA inside Linux containers; the user accepted this workstation limitation on 2026-07-27. CI supplied the mandatory evidence without weakening TLS or scanner controls |

## Artifact and supply chain

The local build stopped before package installation because Docker Desktop's
Linux network path received an Avast-generated TLS certificate that the pinned
image correctly rejected. No certificate or scanner control was weakened. The
accepted post-merge CI run supplied the mandatory provider-independent build
and security evidence.

### Pull-request CI evidence

PR `#6` tested branch head
`7b124dd1cc19987de2322863f850da80f1645da2` through synthetic merge commit
`6bcf88febe5d19ec8c6ea9c6ff9f0ef2db783724`.

| Field | Evidence |
|---|---|
| Workflow/run | `CI` run `30244571128`, run number `23`, attempt `1` |
| Event and time | `pull_request`; 2026-07-27 07:00:16Z through 07:03:13Z |
| Job | `verify`, job `89908695051` |
| Result | Success |
| Tests and analysis | 67 tests; zero failures/errors/skips; Checkstyle zero; PMD, JaCoCo, and SpotBugs passed |
| Secret scan | Gitleaks `8.30.1`; full Git history; 19 commits; no leaks |
| OCI platform and digest | `linux/amd64`; `sha256:7aa589746960ea13150ff7435288520599381db0893073fc00a6fc5f42946aba` |
| SBOM and Trivy | CycloneDX generated; zero HIGH/CRITICAL vulnerabilities and zero secrets |
| Evidence verification | Source/run/digest and all recorded hashes passed |

The pull-request artifacts expire on 2026-08-10:

| Artifact | Artifact ID | GitHub artifact digest |
|---|---:|---|
| `verification-reports` | `8644544262` | `sha256:36663a3e5b916275a43de14da3b0eea2e35ebcfb1a1548035060036890098b6e` |
| `mychandha-oci-6bcf88febe5d19ec8c6ea9c6ff9f0ef2db783724` | `8644545660` | `sha256:c4935847c0dbf997d1df05aae93fe9b133be108a2def675268715d5445aed922` |

### Accepted post-merge `main` CI evidence

PR `#6` merged into `main` as
`cc75d0c3c59dc9d11ef748bff2f3633770854ffd`. The user explicitly accepted the
following repository CI evidence on 2026-07-27:

| Field | Evidence |
|---|---|
| Workflow/run | `CI` run `30245195541`, run number `24`, attempt `1` |
| Event and time | `push` to `main`; 2026-07-27 07:10:45Z through 07:14:43Z |
| Initiator | `Mohi-Chanu` |
| Job | `verify`, job `89910592727` |
| Result | Success |
| Java and build | Temurin Java 21; Maven `verify`; `BUILD SUCCESS` |
| Tests | 67 tests across 21 report files; zero failures, errors, or skips |
| PostgreSQL | Pinned PostgreSQL 17 service and PostgreSQL/Testcontainers integration suites passed |
| Static analysis | Checkstyle zero violations; PMD `7.17.0`; JaCoCo generated; SpotBugs passed |
| Secret scan | Gitleaks `8.30.1`; full Git history; 19 commits; `result=passed` |
| OCI platform | `linux/amd64`; OCI layout `1.0.0` |
| OCI media types | `application/vnd.oci.image.index.v1+json`; `application/vnd.oci.image.manifest.v1+json` |
| OCI manifest digest | `sha256:a19c285d61c62927093bad4adc898a66122adb37978d3894f6f53c54d0e206b0` |
| SBOM | CycloneDX `1.7`; 152 components |
| Vulnerability and secret scan | Trivy `0.72.0`; zero HIGH/CRITICAL vulnerabilities; zero secrets |
| Evidence verification | Manifest commit/run/digest and all recorded file hashes independently matched |

The `main` digest is the only Gate D candidate eligible for a later,
separately approved no-rebuild publication. The pull-request digest must not
be used as a deployment identity.

### Retained `main` artifact index

| Artifact | Artifact ID | GitHub artifact digest | Expiry |
|---|---:|---|---|
| `verification-reports` | `8644791692` | `sha256:cf9a0ea66b05c68c2234d31cb9cfae6050e81233dd4dc73d8097aee42e2dc5f5` | 2026-08-10 07:14:28Z |
| `mychandha-oci-cc75d0c3c59dc9d11ef748bff2f3633770854ffd` | `8644793537` | `sha256:0c329ca4668315f47be6e85ef834ab71106529ad7a4b49c8e51c802a80f7b995` | 2026-08-10 07:14:30Z |

The retained OCI artifact contains:

| File | SHA-256 | Sensitivity |
|---|---|---|
| `mychandha.oci.tar` | `80238d287f564fb463742061e6130f48ac8072ee74a4d6d684ff25305fc9213b` | Internal build artifact |
| `image-metadata.json` | `b4721127ca44d861d3bb14481f311f01371327b33bb8d6cfcd969b52d6c2fd2a` | Sanitized internal metadata |
| `release-evidence.json` | `1f214fb2f15a679b8b7ac2ae6708ae00689ad864903c53bd4e38fb37c45ead5e` | Sanitized evidence |
| `mychandha.cdx.json` | `4d4ef1bd32da005da1f0f017f1b42c95c8476a11a13c6ac5607aa7464d7ac439` | Internal dependency inventory |
| `trivy-vulnerability-report.json` | `5e3397453916100784c32d52293e5ae85f47348d55405a3ce8e2695229b7af5f` | Sanitized security evidence |
| `gitleaks-evidence.txt` | `c0cae7cf926c1893cc803cb4e6907a7249589003fd249dd22d126b01db5cfff4` | Sanitized security evidence |

- [x] Exact `linux/amd64` OCI manifest digest
- [x] Source commit-to-artifact binding
- [x] OCI archive checksum
- [x] CycloneDX SBOM and checksum
- [x] Gitleaks full-history result and evidence checksum
- [x] Trivy HIGH/CRITICAL and secret results and report checksum
- [x] Evidence-manifest verification
- [ ] No-rebuild GHCR publication record
- [ ] Current and rollback digest retention

## Database and migration

Every item remains `Pending` until separately approved staging execution.

- [ ] Provider statement-logging behavior proves role passwords are not retained
- [ ] Bootstrap identity and exact login-role creation/rotation
- [ ] Stable API and dispatcher group-role membership
- [ ] Negative privilege probes, including `NOBYPASSRLS`
- [ ] Migration-only identity and disjoint secret inventory
- [ ] Exact migration image digest, start/end time, and provider job ID
- [ ] Flyway V1/V2 versions, checksums, ownership, and result
- [ ] RLS, tenant binding, and application/dispatcher negative privileges
- [ ] Forward-fix and restore decision points
- [ ] Temporary bootstrap and migration bases deleted within one hour

## Deployment

Every item remains `Pending` until separately approved publication,
provisioning, and deployment.

- [ ] `MCDC-001` environment instance and Render adapter revision
- [ ] Current provider capability and price revalidation
- [ ] `R1-OPB-001` checkout approval
- [ ] Exact API and dispatcher service identifiers
- [ ] Same immutable digest used by API, dispatcher, bootstrap, and migration
- [ ] Process/profile and credential-class mapping
- [ ] Configuration inventory without values
- [ ] TLS, network allowlist, and forwarded-address overwrite verification
- [ ] `render-edge-first-hop` materialization and forged-header bucket proof
- [ ] `/etc/secrets/supabase-ca.crt` filename and expected SHA-256 checksum
- [ ] API, dispatcher, bootstrap, and migration CA readability
- [ ] Positive `verify-full` connection and wrong-CA rejection for every path
- [ ] Render sole-owner availability and GitHub-mediated backup-role evidence
- [ ] Migration completed before runtime rollout
- [ ] API readiness and dispatcher process state
- [ ] Deployment event identifiers and routing acceptance timestamp

## Security and functional acceptance

Every item remains `Pending` until separately approved staging acceptance.
Repository tests do not substitute for live acceptance.

- [ ] JWT issuer, audience, signature, expiry, and subject rejection
- [ ] Same-tenant access
- [ ] Missing, malformed, inactive, and cross-tenant denial
- [ ] Client-address, subject, metrics, and process rate-limit behavior
- [ ] Organization rate-limit live proof or an accepted bounded test method
- [ ] `429` RFC 9457 response, stable code, correlation ID, and `Retry-After`
- [ ] Render forwarded-address overwrite and spoof-resistance proof
- [ ] Missing, duplicate, malformed, overlong, and over-depth header behavior
- [ ] Client-address anomaly metric and readiness degradation/recovery
- [ ] Audit-chain recomputation
- [ ] Idempotent replay and mismatch behavior
- [ ] Inbox duplicate and payload-substitution behavior
- [ ] Outbox retry, stale-claim recovery, and dead-letter behavior
- [ ] Safe logs, bounded metric labels, and no credential/personal-data exposure

## Operations and recovery

Every item remains `Pending` until separately approved provider execution.

- [ ] Liveness, readiness, startup, durable-delivery, and dispatcher signals
- [ ] Protected metrics and alert test
- [ ] Render native deploy, pull, health, and one-off-job notification routes
- [ ] Each native event destination, receipt timestamp, and recovery timestamp
- [ ] Dispatcher, backlog, dead-letter, denial, database, backup, and restore
      bounded-check results
- [ ] `Mohi-Chanu` evidence record and `hazwaTech` review for every bounded
      check
- [ ] Sanitized provider-log review completed within 24 hours
- [ ] `PF-EX-001`/`PF-EX-002` expiry and termination-condition revalidation
- [ ] Log route, retention, redaction, and access review
- [ ] Backup status and retention
- [ ] In-place restore drill and integrity result
- [ ] Compatible-digest rollback and post-rollback smoke tests
- [ ] Forward-fix rehearsal
- [ ] RPO/RTO result or approved limitation
- [ ] Resource cost, secret revocation, and cleanup

## Evidence manifest contract

The repository emits only sanitized JSON under
`target/staging-evidence/sanitized`. The schema binds an operation to a full
commit, exact digest, timestamps, fixed check IDs, sanitized provider event
IDs, and temporary-base cleanup state. Raw probe responses remain under the
ignored `target/staging-evidence/raw` path and are not uploaded.

`scripts/validate-staging-evidence.sh` rejects undeclared top-level fields and
credential-like material. Provider logs, screenshots containing secrets,
database dumps, JWTs, connection URLs, email addresses, and copied personal
data are excluded.

## Acceptance record

- [x] Repository implementation and applicable repository CI items passed
- [x] Repository non-goals and `CC-001` boundaries preserved
- [x] Repository CI evidence integrity independently verified
- [x] Gate D repository CI evidence explicitly accepted on 2026-07-27
- [x] External execution items remain identified as pending rather than waived
- [x] Provider-conformance implementation local validation passed, with the
      accepted workstation OCI/SBOM/Trivy limitation recorded
- [ ] Provider-conformance authoritative CI evidence passed and accepted
- [ ] New remediation-containing OCI digest explicitly accepted
- [ ] All blocking staging `EP-001` items passed
- [ ] Every external not-applicable item has an accepted reason
- [ ] External findings and residual risks are resolved or dispositioned
- [ ] Full Gate D staging evidence is explicitly accepted
- [ ] Phase 1 closure is explicitly approved

The accepted repository CI evidence closes only the Gate D repository
implementation sub-gate. It does not authorize or accept registry publication,
protected-environment configuration, provider resources, migration,
deployment, live acceptance, Gate D closure, Phase 1 closure, or Phase 2.

The provider-conformance implementation and this evidence update remain local.
Their next gate is separate authorization to create
`codex/phase-1-gate-d-provider-conformance`, commit the bounded changes, push
it, open a draft pull request, and allow its automatic CI run. Manual
release/staging workflow execution, publication, external resources, spending,
and staging execution remain separately required approvals.
