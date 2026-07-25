# Phase 1 acceptance validation

Validation opened on 2026-07-23 after explicit approval. Phase 2 remains
unstarted.

## Local evidence

| Gate | Result |
|---|---|
| Required foundation files and invariants | Passed |
| Java 21 source/test compilation | 48 main and 15 test source files passed |
| YAML and XML parsing | Passed |
| GitHub Actions workflow lint | Passed except the linter's obsolete runner-label catalog; `ubuntu-24.04` is a supported GitHub-hosted runner |
| Archive integrity | Passed before validation changes |
| Whitespace and placeholder-secret review | Passed |

## Defects closed during validation

- Tenant ownership is now enforced by composite foreign keys for membership,
  role, and permission assignments.
- Inbox event identity now includes the organization, preventing one tenant's
  provider event ID from colliding with another tenant.
- Concurrent requests using the same idempotency key are serialized within the
  transaction.
- Stale outbox processing leases are reclaimed after worker termination.
- Audit timestamps use PostgreSQL-compatible precision, the exact canonical
  evidence is retained, and the hash chain has an executable verifier.
- Render database credentials preserve literal `+` characters during URI
  decoding.
- Supabase JWTs must contain a non-empty stable subject.
- CI now generates a CycloneDX SBOM and fails on unresolved high or critical
  OCI-image vulnerabilities.

## Executable acceptance gates

The repository contains tests for:

- PostgreSQL RLS tenant isolation;
- cross-organization role-assignment rejection;
- tenant-scoped inbox deduplication;
- audit immutability and hash-chain recomputation;
- concurrent idempotency;
- stale outbox-claim recovery;
- identity mapping and database-URL normalization;
- architecture boundaries and request-context safety.

These gates require Java 21, Maven, and a Docker-compatible Testcontainers
runtime. The accepted Gate A evidence is GitHub Actions run `30160139310`, job
`89684030456`, on commit
`2e42b4ad82cb77a9e62bfdd389f25e1ef1fa4f37` on 2026-07-25. PR `#1` then
merged, and post-merge `main` run `30161138292`, job `89686534126`, passed on
commit `605b4fff9a026c0dbb804c97686e054b4b0370cb`.

The run passed all 52 tests with zero failures, errors, or skips. It also
passed static analysis, the OCI image build, CycloneDX SBOM generation,
artifact upload, and the configured HIGH/CRITICAL Trivy vulnerability gate
with zero findings.

The Gate A Trivy summary reported the secrets column as `-`, defined by the
report as not scanned. Gate B implements the required blocking full-history
Gitleaks control; successful Gate B CI evidence remains pending.

## Gate A local implementation evidence

On 2026-07-25, using Temurin Java 21.0.11 and Maven 3.9.11:

- production and test compilation passed;
- 37 non-Docker tests passed with zero failures, errors, or skips;
- Checkstyle passed with zero violations;
- PMD passed;
- SpotBugs passed with zero findings; and
- `scripts/validate-foundation.sh` passed.

The workstation did not have Docker. GitHub Actions run `30160139310` executed
all 15 PostgreSQL/Testcontainers tests successfully as part of the complete
Java 21 `mvn verify`.

The first draft-PR run, `30159737559`, executed the Docker-backed suite and
failed before image construction with one assertion failure and one application
startup error. The evidence confirmed that the V2 API role denies audit-table
mutation before the append-only trigger is reached, and that the custom startup
health contributor reused its health-group name. The correction tests the API
privilege and owner-level trigger as separate controls and uses the distinct
`startupState` contributor identifier. The corrected run `30160139310`
completed successfully.

## Gate B local implementation evidence

Gate B repository implementation was approved on 2026-07-25. The local change
adds:

- immutable action, service, scanner, Dockerfile frontend, builder, and runtime
  references;
- a blocking, redacted Gitleaks `8.30.1` scan over full Git history;
- an OCI archive exported once by Buildx for scanning, retention, and later
  promotion;
- CycloneDX and HIGH/CRITICAL Trivy `0.72.0` scans against that archive;
- a checksummed JSON evidence manifest and a fail-closed verifier;
- 14-day retained verification and OCI artifacts; and
- a protected manual release workflow that validates a successful `main` CI
  run and promotes the exact digest with ORAS, without rebuilding.

Local validation on 2026-07-25 recorded:

- Actionlint `1.7.12` passed both workflow files;
- POSIX shell syntax checks passed both repository scripts;
- checksum-verified Gitleaks `8.30.1` scanned all 11 existing commits with
  redaction enabled and found no leaks;
- the release-evidence verifier accepted a valid fixture and rejected a
  checksum-tampered fixture;
- `scripts/validate-foundation.sh` passed;
- Java `21.0.12` and Maven `3.9.11` compiled the project and passed all 37
  non-Docker tests, Checkstyle, PMD, JaCoCo report generation, and SpotBugs
  with zero findings; and
- the unfiltered `mvn verify` reached the Testcontainers suites and failed only
  because this workstation has no Docker-compatible runtime.

Docker-backed PostgreSQL tests, OCI construction, Gitleaks/Trivy execution,
artifact upload/download, and promotion-path evidence require the later
approved GitHub CI/PR step. The release workflow has not run and no registry
package or environment exists.

## External staging gates

Staging acceptance is not complete until all of the following evidence is
recorded:

1. The Render Blueprint validates and a staging deployment becomes ready.
2. Supabase JWT issuer, audience, signature, expiry, and subject checks pass.
3. Same-tenant access succeeds and cross-tenant probes are denied.
4. Audit verification, idempotent replay, outbox retry/recovery, health, and
   rollback checks pass in staging.
5. The immutable CI-built image digest, SBOM, vulnerability scan, and explicit
   secret scan are retained as deployment evidence.

No Supabase or Render resources were provisioned during the local validation
pass.
