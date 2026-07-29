#!/usr/bin/env sh
set -eu

: "${MIGRATION_DATABASE_URL:?MIGRATION_DATABASE_URL is required}"
: "${MIGRATION_DATABASE_USERNAME:?MIGRATION_DATABASE_USERNAME is required}"
: "${MIGRATION_DATABASE_PASSWORD:?MIGRATION_DATABASE_PASSWORD is required}"
: "${MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE:?MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE is required}"

normalized_database_url="$(printf '%s' "$MIGRATION_DATABASE_URL" \
  | tr '[:upper:]' '[:lower:]')"
case "$normalized_database_url" in
  *sslmode=* | *sslrootcert=* | *sslfactory=* | *sslhostnameverifier=* | *ssl=*)
    echo "migration_job_result=failed reason=tls_policy_must_be_code_owned" >&2
    exit 64
    ;;
esac

case "$MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE" in
  /*)
    ;;
  *)
    echo "migration_job_result=failed reason=invalid_ssl_root_certificate_path" >&2
    exit 64
    ;;
esac
test -f "$MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE" \
  && test -r "$MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE" || {
  echo "migration_job_result=failed reason=unreadable_ssl_root_certificate" >&2
  exit 64
}

export SPRING_PROFILES_ACTIVE="production,migration"
exec java \
  -XX:MaxRAMPercentage=75 \
  -Djava.security.egd=file:/dev/urandom \
  -jar /app/app.jar
