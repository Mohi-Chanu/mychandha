# Phase 1 acceptance validation

Validation opened on 2026-07-23 after explicit approval. Phase 2 remains
unstarted.

## Local evidence

| Gate | Result |
|---|---|
| Required foundation files and invariants | Passed |
| Java source parsing | 47 files passed |
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
runtime. They passed in GitHub Actions run `30084269566`, job `89452672191`, on
2026-07-24.

The same run also passed the OCI image build, CycloneDX SBOM generation,
artifact upload, and Trivy scanning with zero vulnerabilities and zero secrets.

## External staging gates

Staging acceptance is not complete until all of the following evidence is
recorded:

1. The Render Blueprint validates and a staging deployment becomes ready.
2. Supabase JWT issuer, audience, signature, expiry, and subject checks pass.
3. Same-tenant access succeeds and cross-tenant probes are denied.
4. Audit verification, idempotent replay, outbox retry/recovery, health, and
   rollback checks pass in staging.

No Supabase or Render resources were provisioned during the local validation
pass.
