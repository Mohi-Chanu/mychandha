# DR-001 — Migration environment isolation

Status: Accepted for Phase 1 Gate D
Prepared: 2026-07-26
Accepted: 2026-07-26
Governing controls: `CC-001`, `MCDC-001`, `EP-001`

## Decision

Database bootstrap and Flyway migration execute through protected,
short-lived boundaries that are separate from the long-running API and
dispatcher environments.

The API, dispatcher, migration, and bootstrap secret classes must not be
merged. A provider job that inherits API or dispatcher configuration is not an
acceptable migration runner.

## Required outcome

- Bootstrap receives only the high-impact material required to create and bind
  environment login roles.
- Migration receives only the migration database credential.
- API and dispatcher never receive bootstrap or migration credentials.
- Flyway uses the same accepted OCI digest as the runtime release.
- TLS, approved network restrictions, failure behavior, and sanitized
  `EP-001` evidence are verified before runtime rollout.
- Bootstrap material is removed immediately after use; migration credentials
  are unavailable to long-running services.

## Current disposition

The security requirement is fixed. The bounded mechanism is proposed in
`docs/phase-1-gate-d-repository-proposal.md`: separate, short-lived bootstrap
and migration job bases because Render one-off jobs inherit the full
environment of their base service. The API and dispatcher therefore cannot be
used as job bases. Approval, implementation, and execution of the proposed
mechanism remain separately gated.

Changing the mechanism is permitted later; weakening secret isolation requires
a material `CC-001` amendment and explicit approval.
