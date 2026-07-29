#!/usr/bin/env sh
set -eu

required_files="
pom.xml
Dockerfile
deploy/render/render.staging.yaml.example
deploy/render/render.staging-jobs.yaml.example
.github/workflows/ci.yml
.github/workflows/release.yml
.github/workflows/staging-deploy.yml
scripts/verify-release-evidence.sh
scripts/validate-render-adapter.sh
scripts/test-validate-render-adapter.sh
scripts/validate-render-job-adapter.sh
scripts/test-validate-render-job-adapter.sh
scripts/validate-staging-workflow.sh
scripts/test-validate-staging-workflow.sh
scripts/test-staging-job-contracts.sh
scripts/validate-staging-evidence.sh
scripts/test-validate-staging-evidence.sh
scripts/run-render-staging-operation.sh
scripts/run-staging-acceptance.sh
scripts/run-staging-bootstrap.sh
scripts/run-staging-migration.sh
docs/change-control.md
docs/deployment-contract.md
docs/evidence-package.md
docs/render-deployment-runbook.md
docs/phase-1-gate-c-evidence.md
docs/phase-1-gate-d-evidence.md
src/main/resources/db/migration/V1__platform_foundation.sql
src/main/resources/db/migration/V2__runtime_role_isolation.sql
src/main/resources/application-api.yml
src/main/resources/application-dispatcher.yml
src/main/resources/application-migration.yml
src/main/java/com/mychandha/platform/security/SecurityConfiguration.java
src/main/java/com/mychandha/platform/security/ratelimit/ClientAddressRateLimitFilter.java
src/main/java/com/mychandha/platform/security/ratelimit/SubjectRateLimitFilter.java
src/main/java/com/mychandha/platform/security/ratelimit/OrganizationRateLimitFilter.java
src/main/java/com/mychandha/platform/security/ratelimit/ProductionRateLimitValidator.java
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
grep -q "forward-headers-strategy: none" src/main/resources/application.yml
grep -q "cache-maximum-size:.*10000" src/main/resources/application.yml
grep -q "client-address-strategy:.*direct" src/main/resources/application.yml
grep -q "enabled: false" src/main/resources/application-api.yml
grep -q "enabled: false" src/main/resources/application-dispatcher.yml
grep -q "enabled: true" src/main/resources/application-migration.yml
grep -q "SECURITY DEFINER" src/main/resources/db/migration/V2__runtime_role_isolation.sql
grep -q "REVOKE ALL ON FUNCTION platform.claim_outbox_events" src/main/resources/db/migration/V2__runtime_role_isolation.sql
grep -q "platform.claim_outbox_events" src/main/java/com/mychandha/platform/events/OutboxPublisher.java
grep -q "bucket4j_jdk17-caffeine" pom.xml
grep -q "postgresql17-client" Dockerfile
grep -q "addgroup -S -g 1000 rendersecrets" Dockerfile
grep -q "run-staging-bootstrap.sh" Dockerfile
grep -q "run-staging-migration.sh" Dockerfile
grep -q -- '--rawfile content' scripts/run-render-staging-operation.sh
if grep -q -- '--arg content' scripts/run-render-staging-operation.sh; then
  echo "CA certificate contents must not be passed on a process command line" >&2
  exit 1
fi
grep -q "secret-files/supabase-ca.crt" \
  scripts/run-render-staging-operation.sh
grep -q "sslmode: verify-full" src/main/resources/application-api.yml
grep -q "sslrootcert:.*API_DATABASE_SSL_ROOT_CERTIFICATE" \
  src/main/resources/application-api.yml
grep -q "sslrootcert:.*DISPATCHER_DATABASE_SSL_ROOT_CERTIFICATE" \
  src/main/resources/application-dispatcher.yml
grep -q "sslrootcert:.*MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE" \
  src/main/resources/application-migration.yml
grep -q "addFilterAfter(clientAddressRateLimitFilter, CorrelationIdFilter.class)" \
  src/main/java/com/mychandha/platform/security/SecurityConfiguration.java
grep -q "addFilterAfter(subjectRateLimitFilter, BearerTokenAuthenticationFilter.class)" \
  src/main/java/com/mychandha/platform/security/SecurityConfiguration.java
grep -q "addFilterAfter(organizationRateLimitFilter, OrganizationContextFilter.class)" \
  src/main/java/com/mychandha/platform/security/SecurityConfiguration.java
if grep -q "FOR UPDATE SKIP LOCKED" src/main/java/com/mychandha/platform/events/OutboxPublisher.java; then
  echo "Direct cross-tenant outbox claim SQL remains in Java" >&2
  exit 1
fi
grep -q "actions/checkout@[0-9a-f]\\{40\\}" .github/workflows/ci.yml
grep -q "actions/setup-java@[0-9a-f]\\{40\\}" .github/workflows/ci.yml
grep -q "actions/upload-artifact@[0-9a-f]\\{40\\}" .github/workflows/ci.yml
grep -q "docker/setup-buildx-action@[0-9a-f]\\{40\\}" .github/workflows/ci.yml
grep -q "postgres:17-alpine@sha256:" .github/workflows/ci.yml
grep -q "zricethezav/gitleaks:v8.30.1@sha256:" .github/workflows/ci.yml
grep -q "aquasec/trivy:0.72.0@sha256:" .github/workflows/ci.yml
grep -q "type=oci" .github/workflows/ci.yml
grep -q "tar -xf.*OCI_ARCHIVE" .github/workflows/ci.yml
grep -q "image --input /workspace/target/release/mychandha.oci" .github/workflows/ci.yml
grep -q "format cyclonedx" .github/workflows/ci.yml
grep -q "sh scripts/verify-release-evidence.sh" .github/workflows/ci.yml
grep -q "permissions:" .github/workflows/release.yml
grep -q "packages: write" .github/workflows/release.yml
grep -q "environment: staging-release" .github/workflows/release.yml
grep -q "oras cp" .github/workflows/release.yml
grep -q "tar -xf target/release/mychandha.oci.tar" .github/workflows/release.yml
grep -q "image --input /workspace/target/release/mychandha.oci" .github/workflows/release.yml
grep -q "sh scripts/verify-release-evidence.sh" .github/workflows/release.yml
grep -q "oras-project/setup-oras@[0-9a-f]\\{40\\}" .github/workflows/release.yml
grep -q "actions/download-artifact@[0-9a-f]\\{40\\}" .github/workflows/release.yml
grep -q "actions/upload-artifact@[0-9a-f]\\{40\\}" .github/workflows/release.yml
grep -q "FROM maven:3.9.11-eclipse-temurin-21@sha256:" Dockerfile
grep -q "FROM eclipse-temurin:21-jre-alpine@sha256:" Dockerfile
if grep -h "uses:" .github/workflows/*.yml \
  | grep -Ev "uses: [^[:space:]#]+@[0-9a-f]{40}([[:space:]]*#.*)?$"; then
  echo "A GitHub Action is not pinned to a full commit SHA" >&2
  exit 1
fi
if grep "^FROM " Dockerfile \
  | grep -Ev "@sha256:[0-9a-f]{64}([[:space:]]+AS[[:space:]]+[[:alnum:]_-]+)?$"; then
  echo "A Dockerfile build/runtime image is not pinned to a full digest" >&2
  exit 1
fi
grep -q "Idempotency-Key" docs/architecture.md
grep -q "Supabase" docs/architecture.md
grep -q "Render" docs/operations.md
grep -q "MCDC-001" docs/deployment-contract.md
grep -q "EP-001" docs/evidence-package.md
grep -q "CC-001" docs/change-control.md
sh scripts/validate-render-adapter.sh
sh scripts/test-validate-render-adapter.sh
sh scripts/validate-render-job-adapter.sh
sh scripts/test-validate-render-job-adapter.sh
sh scripts/validate-staging-workflow.sh
sh scripts/test-validate-staging-workflow.sh
sh scripts/test-staging-job-contracts.sh
sh scripts/test-validate-staging-evidence.sh

echo "Phase 1 structural validation passed."
