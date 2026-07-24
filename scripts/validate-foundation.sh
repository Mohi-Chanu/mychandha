#!/usr/bin/env sh
set -eu

required_files="
pom.xml
Dockerfile
render.yaml
src/main/resources/db/migration/V1__platform_foundation.sql
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
grep -q "trivy-action@" .github/workflows/ci.yml
grep -q "format: cyclonedx" .github/workflows/ci.yml
grep -q "Idempotency-Key" docs/architecture.md
grep -q "Supabase" docs/architecture.md
grep -q "Render" docs/operations.md

echo "Phase 1 structural validation passed."
