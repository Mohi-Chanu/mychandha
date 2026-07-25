# ADR-005: Flyway Execution Strategy

Status: Accepted
Date: 2026-07-25

## Context

Flyway currently runs automatically during normal application startup. That
requires runtime credentials capable of migration and makes deployment order
implicit.

Production evolution must remain forward-only and use
expand/migrate/contract. A developer laptop must never become the production
migration path.

## Decision

Flyway runs only in the explicit `migration` profile with the migration role.
The API and dispatcher profiles have Flyway disabled and fail configuration
validation if migration is enabled.

Deployment order is:

1. verify the accepted artifact and migration;
2. run the one-off migration profile;
3. verify schema version and grants;
4. start API and dispatcher profiles; and
5. run readiness and acceptance checks.

Migrations are forward-only. Database correction uses a reviewed later
migration rather than automatic reversal.

## Consequences

- Migration failure stops deployment before application routing.
- The migration role requires tightly controlled availability and audit.
- Runtime roles can be non-owner roles.
- Compatibility with the prior accepted image must be evaluated for each
  migration.
- Local development may run migrations through an explicitly documented local
  command, never against production.

## Alternatives rejected

- Flyway on every API/dispatcher startup: rejected because it over-privileges
  runtime credentials and creates migration races.
- Developer-laptop production migration: rejected because it is not
  reproducible or centrally auditable.
- Down migrations: rejected in favor of forward fixes and compatible rollout
  design.
