# ADR-002: Database Roles

Status: Accepted
Date: 2026-07-25

## Context

Tenant isolation depends on aligned application authorization, transaction-local
tenant binding, and PostgreSQL RLS. Runtime ownership or `BYPASSRLS` would
weaken that boundary. The durable dispatcher nevertheless needs controlled
cross-organization access to outbox work.

## Decision

Use distinct migration, API, and dispatcher roles:

- the migration role owns schemas and versioned database objects;
- the API role performs only required tenant-bound DML and cannot migrate,
  own schemas, or bypass RLS; and
- the dispatcher role invokes narrowly scoped security-definer routines and
  cannot directly access tenant business tables or bypass RLS.

Security-definer routines must use schema-qualified objects, a fixed safe
`search_path`, validated limits and transitions, explicit claim ownership, and
`PUBLIC` execution revocation.

Environment login credentials are created outside Flyway. Versioned routines
and grants to stable group roles are maintained through forward-only migrations
where supported by the selected PostgreSQL provider.

## Consequences

- The runtime cannot alter schema objects.
- Dispatcher access can be audited and tested at the routine boundary.
- Role bootstrap and grants become deployment prerequisites.
- Tests must connect as the real non-owner roles and prove negative access.
- Provider capabilities must be verified before staging provisioning.

## Alternatives rejected

- API schema ownership: rejected because owners can bypass intended controls.
- Dispatcher `BYPASSRLS`: rejected because the privilege is broader than the
  required outbox operations.
- Direct dispatcher table ownership: rejected in favor of a narrower callable
  interface.
