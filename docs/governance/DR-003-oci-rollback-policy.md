# DR-003 — OCI rollback policy

Status: Accepted for Phase 1 Gate D
Prepared: 2026-07-26
Accepted: 2026-07-26
Governing controls: `CC-001`, `MCDC-001`, `EP-001`

## Decision

An application rollback may use only a retained, accepted, schema-compatible
OCI manifest digest. Tags are discovery aliases and never deployment or
rollback identity.

The active digest and one compatible prior digest must remain pullable for the
approved staging and rollback evidence period. Render must reference the exact
digest used by the API, dispatcher, and migration release.

## Compatibility and recovery

- Verify schema compatibility before selecting the prior digest.
- Never reverse a Flyway migration automatically.
- Use a reviewed forward fix for database defects unless a separately approved
  restore is required.
- A registry pull failure, missing prior digest, digest mismatch, or
  incompatible schema blocks rollback.
- After rollback, reverify readiness, authentication, cross-tenant denial,
  audit continuity, dispatcher health/backlog, and log safety.

## Evidence

The `EP-001` package records the current and rollback digest, registry
availability, compatibility decision, provider deployment event, timestamps,
verification results, and explicit acceptance.
