# Architecture Decision Records

Architecture Decision Records preserve significant technical decisions,
context, alternatives, and consequences.

## Status values

- `Proposed`: under review; not authoritative.
- `Accepted`: approved and authoritative for new work.
- `Superseded`: replaced by a later ADR; retained for history.
- `Rejected`: considered but not adopted.

Accepted ADRs are append-only decision history. Do not silently rewrite a
decision after implementation begins. Add a superseding ADR that records the
reason, compatibility impact, migration impact, and affected tests.

## Index

| ADR | Decision | Status |
|---|---|---|
| [ADR-001](ADR-001-runtime-profiles.md) | Runtime profiles | Accepted |
| [ADR-002](ADR-002-database-roles.md) | Database roles | Accepted |
| [ADR-003](ADR-003-dispatcher-model.md) | Durable dispatcher model and terminology | Accepted |
| [ADR-004](ADR-004-immutable-release.md) | Immutable release identity | Accepted |
| [ADR-005](ADR-005-flyway-strategy.md) | Flyway execution strategy | Accepted |

Provider-side resources and credentials are not approved by these ADRs.
