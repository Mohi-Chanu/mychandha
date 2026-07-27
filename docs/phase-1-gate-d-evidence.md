# Phase 1 Gate D evidence package

Status: Repository implementation locally accepted with documented OCI limitation; external execution not started
Gate: Phase 1 Gate D — staging execution and acceptance
Evidence package: `EP-001`
Deployment contract: `MCDC-001` version `1`
Change control: `CC-001`
Decisions: `DR-001`, `DR-002`, `DR-003`, `R1-OPB-001`
Last updated: 2026-07-27

## Package identity

| Item | State | Evidence |
|---|---|---|
| Gate and evidence version | Passed | Gate D; `EP-001-GATE-D-1` |
| Repository | Passed | `Mohi-Chanu/mychandha` |
| Repository implementation branch | Pending | Commit/push approval has not been granted |
| Full implementation commit | Pending | No Gate D implementation commit exists yet |
| Pull request and merge commit | Pending | No Gate D implementation PR exists yet |
| CI run, job, attempt, and timestamps | Pending | Requires a separately approved push and PR |
| External environment and resources | Pending | No staging resource has been provisioned or changed |
| Evidence retention and deletion owner | Pending | Must be approved with the protected environment and provider resources |

## Scope and approval

| Item | State | Evidence |
|---|---|---|
| Staging-resource decisions | Passed | `docs/phase-1-staging-resource-proposal.md`; accepted 2026-07-26 |
| Repository implementation proposal | Passed | `docs/phase-1-gate-d-repository-proposal.md`; approved 2026-07-27 |
| Repository implementation | Passed locally | Local working tree only; no commit created |
| Non-goals preserved | Passed | No Redis, Kafka, Key Value, Workflow, extra database, or Phase 2 behavior |
| External action authorization | Pending | Not granted |
| Unapproved actions not performed | Passed | No commit, push, publication, protected environment, provider mutation, migration, or deployment |
| Deviations | Passed | None from the approved repository scope |

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
| Secret scanning | Passed | Pinned Gitleaks `8.30.1`: 18 commits and source-only current tree, no leaks |
| Whitespace and Markdown-link validation | Passed | `git diff --check`; all local Markdown targets resolve |
| OCI build, SBOM, and Trivy | Accepted local limitation | Avast HTTPS interception presents an untrusted generated CA inside Linux containers; `apk` cannot securely fetch Alpine indexes. The user accepted this local limitation on 2026-07-27; authoritative CI evidence remains required |

## Artifact and supply chain

Every release item in this section remains `Pending` until a separately
approved commit/push/PR produces an accepted post-merge `main` artifact. A
local `linux/amd64` build was attempted, but it stopped before package
installation because Docker Desktop's Linux network path received an
Avast-generated TLS certificate that the pinned image correctly rejected. No
certificate check or vulnerability gate was weakened. The user accepted this
local-machine limitation on 2026-07-27. This acceptance does not mark the OCI,
SBOM, or Trivy evidence as passed; those remain mandatory in the later
authoritative CI evidence package.

- [ ] Exact `linux/amd64` OCI manifest digest
- [ ] Source commit-to-artifact binding
- [ ] OCI archive checksum
- [ ] CycloneDX SBOM and checksum
- [ ] Gitleaks full-history result and evidence checksum
- [ ] Trivy HIGH/CRITICAL result and report checksum
- [ ] Evidence-manifest checksum and verification
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
- [ ] Audit-chain recomputation
- [ ] Idempotent replay and mismatch behavior
- [ ] Inbox duplicate and payload-substitution behavior
- [ ] Outbox retry, stale-claim recovery, and dead-letter behavior
- [ ] Safe logs, bounded metric labels, and no credential/personal-data exposure

## Operations and recovery

Every item remains `Pending` until separately approved provider execution.

- [ ] Liveness, readiness, startup, durable-delivery, and dispatcher signals
- [ ] Protected metrics and alert test
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

- [ ] All blocking `EP-001` items passed
- [ ] Every not-applicable item has an accepted reason
- [ ] Findings are resolved or explicitly dispositioned
- [ ] Residual-risk owner and review date are recorded
- [ ] Evidence integrity is verified
- [ ] Gate D evidence is explicitly accepted
- [ ] Phase 1 closure is explicitly approved

Green repository CI, successful deployment, or this evidence template alone
does not accept Gate D. The next approval after local implementation
validation is the separate commit/push/draft-PR approval.
