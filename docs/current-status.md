# Current development status

Last updated: 2026-07-24

## Outcome

Phase 1 source and CI acceptance are complete. GitHub Actions run
`30084269566`, job `89452672191`, passed the complete configured pipeline:

- Java 21 `mvn verify`.
- Unit, architecture, security, migration, RLS, and PostgreSQL integration
  tests.
- Checkstyle, PMD, JaCoCo, and SpotBugs.
- OCI image build.
- CycloneDX SBOM generation and artifact upload.
- Trivy image scan with zero vulnerabilities and zero secrets.

The accepted local foundation commit chain ends at `284673d`:

```text
284673d Remediate scanned image vulnerabilities
c17a249 Avoid Testcontainers during image build
79f0732 Remove obsolete SpotBugs suppression
d336f73 Resolve SpotBugs findings
86a06cd Fix audit timestamp JDBC binding
67f9dfa Fix Phase 1 CI test compilation
c4e792a Establish validated Phase 1 platform foundation
```

The remote repository may use different commit IDs if individual patches were
committed on another workstation. The successful CI run, file content, and
tests are authoritative; do not rewrite history merely to match these local
IDs.

## Closed validation issues

- Corrected tenant-role composite ownership and tenant-scoped inbox identity.
- Serialized concurrent idempotency-key use.
- Added stale outbox-lease recovery and dead-letter behavior.
- Made audit timestamp storage JDBC-compatible while preserving deterministic
  microsecond hash evidence.
- Added executable audit-chain verification.
- Required a non-empty Supabase JWT subject.
- Made API records defensively immutable and correlation IDs explicitly safe.
- Resolved static-analysis findings without broad suppressions.
- Kept Testcontainers in the CI verification stage rather than inside the
  Docker build.
- Added SBOM generation and a non-bypassed HIGH/CRITICAL Trivy gate.
- Upgraded runtime packages and PostgreSQL JDBC to remove scan findings.

## What is not done

- No Supabase or Render environment has been provisioned or changed.
- Staging deployment and acceptance tests have not run.
- Production database-role separation, rate limits, alerts, backups, restore
  drill, log drain, and rollback rehearsal remain open.
- Phase 2 implementation has not begun.

## Next action

Prepare a staging-validation proposal covering exact external resources,
configuration, secrets ownership, database roles, deployment steps, acceptance
evidence, rollback, cost, and deletion/cleanup behavior. Wait for explicit user
approval before making any external change.

After staging acceptance passes, request approval for the Phase 2 design. Do
not begin Phase 2 code in the same approval step.
