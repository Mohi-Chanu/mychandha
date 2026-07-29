#!/usr/bin/env sh
set -eu

umask 077

: "${BOOTSTRAP_DATABASE_HOST:?BOOTSTRAP_DATABASE_HOST is required}"
: "${BOOTSTRAP_DATABASE_NAME:?BOOTSTRAP_DATABASE_NAME is required}"
: "${BOOTSTRAP_DATABASE_USERNAME:?BOOTSTRAP_DATABASE_USERNAME is required}"
: "${BOOTSTRAP_DATABASE_PASSWORD:?BOOTSTRAP_DATABASE_PASSWORD is required}"
: "${BOOTSTRAP_DATABASE_SSL_ROOT_CERTIFICATE:?BOOTSTRAP_DATABASE_SSL_ROOT_CERTIFICATE is required}"
: "${MYCHANDHA_STAGING_API_PASSWORD:?MYCHANDHA_STAGING_API_PASSWORD is required}"
: "${MYCHANDHA_STAGING_DISPATCHER_PASSWORD:?MYCHANDHA_STAGING_DISPATCHER_PASSWORD is required}"
: "${MYCHANDHA_STAGING_MIGRATION_PASSWORD:?MYCHANDHA_STAGING_MIGRATION_PASSWORD is required}"

case "${BOOTSTRAP_DATABASE_PORT:-5432}" in
  '' | *[!0-9]*)
    echo "bootstrap_result=failed reason=invalid_database_port" >&2
    exit 64
    ;;
esac

case "$BOOTSTRAP_DATABASE_SSL_ROOT_CERTIFICATE" in
  /*)
    ;;
  *)
    echo "bootstrap_result=failed reason=invalid_ssl_root_certificate_path" >&2
    exit 64
    ;;
esac
test -f "$BOOTSTRAP_DATABASE_SSL_ROOT_CERTIFICATE" \
  && test -r "$BOOTSTRAP_DATABASE_SSL_ROOT_CERTIFICATE" || {
  echo "bootstrap_result=failed reason=unreadable_ssl_root_certificate" >&2
  exit 64
}

export PGHOST="$BOOTSTRAP_DATABASE_HOST"
export PGPORT="${BOOTSTRAP_DATABASE_PORT:-5432}"
export PGDATABASE="$BOOTSTRAP_DATABASE_NAME"
export PGUSER="$BOOTSTRAP_DATABASE_USERNAME"
export PGPASSWORD="$BOOTSTRAP_DATABASE_PASSWORD"
export PGSSLMODE="verify-full"
export PGSSLROOTCERT="$BOOTSTRAP_DATABASE_SSL_ROOT_CERTIFICATE"
export PGCONNECT_TIMEOUT="${BOOTSTRAP_DATABASE_CONNECT_TIMEOUT:-10}"

if psql \
  --no-password \
  --no-psqlrc \
  --file=/app/ops/bootstrap-staging-database.sql; then
  echo "bootstrap_job_result=passed"
else
  result=$?
  echo "bootstrap_job_result=failed" >&2
  exit "$result"
fi
