# Local development

The contributor guardrails, local combined-mode boundary, fixture policy,
helper-command policy, and troubleshooting sequence are in
`docs/developer-experience.md`.

## Setup

```bash
cp .env.example .env
docker compose up -d postgres
set -a
source .env
set +a
mvn spring-boot:run
```

`compose.yaml` initializes the stable `mychandha_api` and
`mychandha_dispatcher` group roles required by V2. The local application uses
the database owner only inside the disposable local environment so Flyway can
run and the combined API/dispatcher mode can exercise both privilege paths.
Delete an old local volume before first using V2 if it was initialized before
the role bootstrap existed.

Use a non-production Supabase project. Never place a service-role key in this
application; JWT validation needs only issuer and JWKS configuration.

## Tests

```bash
mvn verify
```

The integration suite starts PostgreSQL 17 with Testcontainers, bootstraps
distinct API and dispatcher roles, applies Flyway V1/V2 with the owner,
verifies runtime-profile separation, RLS and routine boundaries, and proves
audit rows cannot be deleted.

When Maven or Docker is not available, the limited structural check is:

```bash
sh scripts/validate-foundation.sh
```

That check does not replace `mvn verify`.

## Bootstrap fixture

Phase 1 intentionally has no public organization-creation API. Create test
organization, user, membership, role, and permission rows through a migration
or controlled test fixture with a tenant context. Phase 2 will add the audited
onboarding use cases.
