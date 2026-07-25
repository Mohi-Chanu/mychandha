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
runtime. The latest successful evidence is GitHub Actions run `30085167487`,
job `89455532377`, on commit `b12ff66fc7e4868eebc6b6de9de11951aafb8261`
on 2026-07-24.

The same run also passed the OCI image build, CycloneDX SBOM generation,
artifact upload, and the configured HIGH/CRITICAL Trivy vulnerability gate
with zero findings.

The Trivy summary reported the secrets column as `-`, defined by the report as
not scanned. An explicit blocking secret-scanning control remains required
before staging deployment.

## Gate A local implementation evidence

On 2026-07-25, using Temurin Java 21.0.11 and Maven 3.9.11:

- production and test compilation passed;
- 36 non-Docker tests passed with zero failures, errors, or skips;
- Checkstyle passed with zero violations;
- PMD passed;
- SpotBugs passed with zero findings; and
- `scripts/validate-foundation.sh` passed.

The workstation did not have Docker. The 15 PostgreSQL/Testcontainers tests
compiled but were not executed. Gate A therefore still requires a complete
Docker-backed `mvn verify` and green CI evidence before acceptance.

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
