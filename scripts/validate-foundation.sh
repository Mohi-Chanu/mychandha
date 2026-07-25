#!/usr/bin/env sh
set -eu

required_files="
pom.xml
Dockerfile
render.yaml
src/main/resources/db/migration/V1__platform_foundation.sql
src/main/resources/db/migration/V2__runtime_role_isolation.sql
src/main/resources/application-api.yml
src/main/resources/application-dispatcher.yml
src/main/resources/application-migration.yml
src/main/java/com/mychandha/platform/security/SecurityConfiguration.java
src/main/java/com/mychandha/platform/tenancy/OrganizationContextFilter.java
src/main/java/com/mychandha/platform/audit/AuditService.java
src/main/java/com/mychandha/platform/events/OutboxPublisher.java
"

for path in $required_files; do
  test -s "$path" || {
    echo "Missing required foundation file: $path" >&2
    exit 1
  }
done

grep -q "ENABLE ROW LEVEL SECURITY" src/main/resources/db/migration/V1__platform_foundation.sql
grep -q "FORCE ROW LEVEL SECURITY" src/main/resources/db/migration/V1__platform_foundation.sql
grep -q "UNIQUE (organization_id, source, external_event_id)" src/main/resources/db/migration/V1__platform_foundation.sql
grep -q "lock-timeout" src/main/resources/application.yml
grep -q "enabled: false" src/main/resources/application-api.yml
grep -q "enabled: false" src/main/resources/application-dispatcher.yml
grep -q "enabled: true" src/main/resources/application-migration.yml
grep -q "SECURITY DEFINER" src/main/resources/db/migration/V2__runtime_role_isolation.sql
grep -q "REVOKE ALL ON FUNCTION platform.claim_outbox_events" src/main/resources/db/migration/V2__runtime_role_isolation.sql
grep -q "platform.claim_outbox_events" src/main/java/com/mychandha/platform/events/OutboxPublisher.java
if grep -q "FOR UPDATE SKIP LOCKED" src/main/java/com/mychandha/platform/events/OutboxPublisher.java; then
  echo "Direct cross-tenant outbox claim SQL remains in Java" >&2
  exit 1
fi
grep -q "trivy-action@" .github/workflows/ci.yml
grep -q "format: cyclonedx" .github/workflows/ci.yml
grep -q "Idempotency-Key" docs/architecture.md
grep -q "Supabase" docs/architecture.md
grep -q "Render" docs/operations.md

echo "Phase 1 structural validation passed."
