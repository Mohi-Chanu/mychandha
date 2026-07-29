#!/usr/bin/env sh
set -eu

temporary_directory="$(mktemp -d)"

cleanup() {
  rm -f "$temporary_directory"/*
  rmdir "$temporary_directory"
}
trap cleanup EXIT

printf '%s\n' 'synthetic-ca' > "$temporary_directory/supabase-ca.crt"

if env -i PATH="$PATH" sh scripts/run-staging-bootstrap.sh >/dev/null 2>&1; then
  echo "Bootstrap must reject a missing credential environment." >&2
  exit 1
fi

if env -i \
  PATH="$PATH" \
  BOOTSTRAP_DATABASE_HOST=db.example.invalid \
  BOOTSTRAP_DATABASE_NAME=mychandha \
  BOOTSTRAP_DATABASE_USERNAME=bootstrap \
  BOOTSTRAP_DATABASE_PASSWORD=synthetic-bootstrap-password-0001 \
  BOOTSTRAP_DATABASE_PORT=invalid \
  BOOTSTRAP_DATABASE_SSL_ROOT_CERTIFICATE="$temporary_directory/supabase-ca.crt" \
  MYCHANDHA_STAGING_API_PASSWORD=synthetic-api-password-0000000001 \
  MYCHANDHA_STAGING_DISPATCHER_PASSWORD=synthetic-dispatcher-password-01 \
  MYCHANDHA_STAGING_MIGRATION_PASSWORD=synthetic-migration-password-0001 \
  sh scripts/run-staging-bootstrap.sh >/dev/null 2>&1; then
  echo "Bootstrap must reject a non-numeric database port." >&2
  exit 1
fi

if env -i \
  PATH="$PATH" \
  MIGRATION_DATABASE_URL='jdbc:postgresql://db.example.invalid/mychandha?SSLMODE=require' \
  MIGRATION_DATABASE_USERNAME=migration \
  MIGRATION_DATABASE_PASSWORD=synthetic-migration-password-0001 \
  MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE="$temporary_directory/supabase-ca.crt" \
  sh scripts/run-staging-migration.sh >/dev/null 2>&1; then
  echo "Migration must reject a case-varied inline TLS policy." >&2
  exit 1
fi

printf '%s\n' \
  '#!/usr/bin/env sh' \
  'set -eu' \
  'test "$PGSSLMODE" = "verify-full"' \
  'test -r "$PGSSLROOTCERT"' \
  'test "$1" = "--no-password"' \
  'test "$2" = "--no-psqlrc"' \
  'test "$3" = "--file=/app/ops/bootstrap-staging-database.sql"' \
  > "$temporary_directory/psql"
chmod 0700 "$temporary_directory/psql"

env -i \
  PATH="$temporary_directory:$PATH" \
  BOOTSTRAP_DATABASE_HOST=db.example.invalid \
  BOOTSTRAP_DATABASE_NAME=mychandha \
  BOOTSTRAP_DATABASE_USERNAME=bootstrap \
  BOOTSTRAP_DATABASE_PASSWORD=synthetic-bootstrap-password-0001 \
  BOOTSTRAP_DATABASE_SSL_ROOT_CERTIFICATE="$temporary_directory/supabase-ca.crt" \
  MYCHANDHA_STAGING_API_PASSWORD=synthetic-api-password-0000000001 \
  MYCHANDHA_STAGING_DISPATCHER_PASSWORD=synthetic-dispatcher-password-01 \
  MYCHANDHA_STAGING_MIGRATION_PASSWORD=synthetic-migration-password-0001 \
  sh scripts/run-staging-bootstrap.sh >/dev/null

if env -i \
  PATH="$PATH" \
  MIGRATION_DATABASE_URL='jdbc:postgresql://db.example.invalid/mychandha?sslmode=require' \
  MIGRATION_DATABASE_USERNAME=migration \
  MIGRATION_DATABASE_PASSWORD=synthetic-migration-password-0001 \
  MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE="$temporary_directory/supabase-ca.crt" \
  sh scripts/run-staging-migration.sh >/dev/null 2>&1; then
  echo "Migration must reject an inline TLS policy." >&2
  exit 1
fi

if env -i \
  PATH="$PATH" \
  MIGRATION_DATABASE_URL=jdbc:postgresql://db.example.invalid/mychandha \
  MIGRATION_DATABASE_USERNAME=migration \
  MIGRATION_DATABASE_PASSWORD=synthetic-migration-password-0001 \
  MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE="$temporary_directory/missing.crt" \
  sh scripts/run-staging-migration.sh >/dev/null 2>&1; then
  echo "Migration must reject an unreadable root certificate." >&2
  exit 1
fi

printf '%s\n' \
  '#!/usr/bin/env sh' \
  'set -eu' \
  'test "$SPRING_PROFILES_ACTIVE" = "production,migration"' \
  'test "$1" = "-XX:MaxRAMPercentage=75"' \
  'test "$2" = "-Djava.security.egd=file:/dev/urandom"' \
  'test "$3" = "-jar"' \
  'test "$4" = "/app/app.jar"' \
  > "$temporary_directory/java"
chmod 0700 "$temporary_directory/java"

env -i \
  PATH="$temporary_directory:$PATH" \
  MIGRATION_DATABASE_URL='jdbc:postgresql://db.example.invalid/mychandha' \
  MIGRATION_DATABASE_USERNAME=migration \
  MIGRATION_DATABASE_PASSWORD=synthetic-migration-password-0001 \
  MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE="$temporary_directory/supabase-ca.crt" \
  sh scripts/run-staging-migration.sh

if grep -Eq -- '--password([=[:space:]])' \
  scripts/run-staging-bootstrap.sh scripts/run-staging-migration.sh; then
  echo "A staging job wrapper exposes a password as a command argument." >&2
  exit 1
fi

echo "Staging bootstrap and migration job contract tests passed."
