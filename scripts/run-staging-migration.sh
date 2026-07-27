#!/usr/bin/env sh
set -eu

: "${MIGRATION_DATABASE_URL:?MIGRATION_DATABASE_URL is required}"
: "${MIGRATION_DATABASE_USERNAME:?MIGRATION_DATABASE_USERNAME is required}"
: "${MIGRATION_DATABASE_PASSWORD:?MIGRATION_DATABASE_PASSWORD is required}"

case "$MIGRATION_DATABASE_URL" in
  jdbc:postgresql://*'?sslmode=verify-full'* | \
  jdbc:postgresql://*'&sslmode=verify-full'*)
    ;;
  *)
    echo "migration_job_result=failed reason=tls_verification_required" >&2
    exit 64
    ;;
esac

export SPRING_PROFILES_ACTIVE="production,migration"
exec java \
  -XX:MaxRAMPercentage=75 \
  -Djava.security.egd=file:/dev/urandom \
  -jar /app/app.jar
