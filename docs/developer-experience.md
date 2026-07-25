# Developer Experience Guidelines

Status: Approved guideline
Last updated: 2026-07-25

## Goal

Keep local development predictable without weakening staging or production
security. Convenience behavior must be explicit, local-only, and covered by
configuration tests.

## Prerequisites

- Java 21.
- Maven 3.9 or later.
- Docker or a compatible Testcontainers runtime.
- Git Bash or another POSIX shell for the structural validator on Windows.

Use `.env.example` as a name/format reference. Never copy production
credentials or customer data into local files.

## Local runtime

The `local` profile composes API and dispatcher behavior in one process for
developer convenience while using only local credentials. Deployed runtimes
must use exactly one of `api`, `dispatcher`, or `migration`.

Guardrails:

- `local` combined mode must fail when `production` is active;
- placeholder Supabase configuration is not suitable for authenticated manual
  testing;
- local Flyway runs only against a local/test database;
- no developer-laptop command may target production; and
- local logs follow the same redaction rules as deployed logs.

## Common commands

```text
docker compose up -d postgres
mvn spring-boot:run
mvn verify
sh scripts/validate-foundation.sh
docker build -t mychandha:local .
```

Prefer Maven goals and small repository scripts over requiring an additional
Make dependency. Add a helper only when it removes a repeated, error-prone
sequence and works in CI.

## Test data

- Use migrations only for stable reference data required by the application.
- Use test builders or controlled fixtures for organizations, identities,
  memberships, roles, permissions, audit, inbox, and outbox scenarios.
- Generate synthetic identifiers and masked contact hints.
- Never import production/customer data, tokens, service-role keys, or copied
  provider payloads.
- Fixtures must establish tenant context and exercise non-owner roles rather
  than bypassing RLS.

Phase 1 intentionally has no public organization-creation endpoint. Manual
local fixtures remain test-only until Phase 2 onboarding is approved.

## Troubleshooting sequence

1. Confirm Java and Maven versions.
2. Confirm Docker is running and Testcontainers can start PostgreSQL.
3. Confirm the active Spring profile and ensure it is local/test.
4. Confirm the database URL points to a disposable local database.
5. Run `mvn verify` and inspect the first failing test/report.
6. Run the structural validator as an additional check.
7. Inspect safe health/log output without printing environment secrets.

Document recurring failures in `docs/local-development.md` rather than relying
on personal shell history.

## Contributor completion checklist

- Relevant tests pass.
- Architecture and tenant boundaries are preserved.
- No credentials or personal data enter source, fixtures, logs, or reports.
- Migrations are forward-only.
- Documentation and current status are updated.
- Image/SBOM/scan gates run when dependencies or the image change.
- External changes receive their own explicit approval.
