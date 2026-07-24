# Local development

## Setup

```bash
cp .env.example .env
docker compose up -d postgres
set -a
source .env
set +a
mvn spring-boot:run
```

Use a non-production Supabase project. Never place a service-role key in this
application; JWT validation needs only issuer and JWKS configuration.

## Tests

```bash
mvn verify
```

The integration suite starts PostgreSQL 17 with Testcontainers, applies Flyway
migrations, uses a non-superuser runtime role, verifies RLS separation, and
proves audit rows cannot be deleted.

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
