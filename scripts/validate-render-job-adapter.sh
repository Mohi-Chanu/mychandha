#!/usr/bin/env sh
set -eu

adapter_file="${1:-deploy/render/render.staging-jobs.yaml.example}"
root_blueprint="${RENDER_ADAPTER_ROOT_BLUEPRINT:-render.yaml}"
image_placeholder="REPLACE_AFTER_EXTERNAL_APPROVAL/IMAGE@sha256:REPLACE_WITH_64_HEX_DIGEST"
expected_image="${RENDER_ADAPTER_EXPECTED_IMAGE:-$image_placeholder}"
expected_bootstrap_name="${RENDER_ADAPTER_EXPECTED_BOOTSTRAP_NAME:-mychandha-staging-bootstrap-base}"
expected_migration_name="${RENDER_ADAPTER_EXPECTED_MIGRATION_NAME:-mychandha-staging-migration-base}"

fail() {
  echo "Render job adapter validation failed: $1" >&2
  exit 1
}

count_lines() {
  pattern="$1"
  file="$2"
  grep -Ec "$pattern" "$file" || true
}

count_fixed_lines() {
  line="$1"
  file="$2"
  grep -Fxc "$line" "$file" || true
}

worker_block() {
  requested="$1"
  file="$2"
  awk -v requested="$requested" '
    /^  - type: worker$/ {
      current++
    }
    current == requested {
      print
    }
  ' "$file"
}

require_unsupplied_secret() {
  secret_key="$1"
  file="$2"
  awk -v key="$secret_key" '
    $0 ~ "^[[:space:]]*- key: " key "$" {
      found++
      if (getline <= 0 || $0 !~ "^[[:space:]]+sync: false$") {
        invalid = 1
      }
    }
    END {
      if (found != 1 || invalid) {
        exit 1
      }
    }
  ' "$file" || fail "$secret_key must occur once with sync: false"
}

test ! -e "$root_blueprint" \
  || fail "a live root render.yaml is prohibited"
test -s "$adapter_file" \
  || fail "missing non-live privileged job adapter: $adapter_file"
grep -q "delete it no" "$adapter_file" \
  || fail "the one-hour cleanup requirement is missing"
grep -q "one hour" "$adapter_file" \
  || fail "the cleanup deadline is missing"

test "$(count_lines '^previews:$' "$adapter_file")" -eq 1 \
  || fail "previews must be declared exactly once"
test "$(count_lines '^  generation: off$' "$adapter_file")" -eq 1 \
  || fail "preview generation must be off"
test "$(count_lines '^services:$' "$adapter_file")" -eq 1 \
  || fail "services must be declared exactly once"
test "$(count_lines '^  - type: worker$' "$adapter_file")" -eq 2 \
  || fail "exactly two privileged worker bases are required"
test "$(count_lines '^    runtime: image$' "$adapter_file")" -eq 2 \
  || fail "both job bases must use runtime: image"
test "$(count_lines '^    plan: starter$' "$adapter_file")" -eq 2 \
  || fail "both job bases must use Starter"
test "$(count_lines '^    region: singapore$' "$adapter_file")" -eq 2 \
  || fail "both job bases must use Singapore"
test "$(count_lines '^    numInstances: 1$' "$adapter_file")" -eq 2 \
  || fail "both job bases must use one instance"
test "$(count_lines '^    maxShutdownDelaySeconds: 30$' "$adapter_file")" -eq 2 \
  || fail "both job bases must use the accepted shutdown delay"
test "$(count_lines '^    dockerCommand: /app/ops/run-idle.sh$' "$adapter_file")" -eq 2 \
  || fail "both job bases must use the safe idle command"
test "$(count_fixed_lines "    name: ${expected_bootstrap_name}" "$adapter_file")" -eq 1 \
  || fail "bootstrap base name does not match"
test "$(count_fixed_lines "    name: ${expected_migration_name}" "$adapter_file")" -eq 1 \
  || fail "migration base name does not match"

if [ "$expected_image" != "$image_placeholder" ]; then
  digest="${expected_image##*@sha256:}"
  test "$digest" != "$expected_image" && test "${#digest}" -eq 64 \
    || fail "the approved image must contain a full sha256 digest"
  case "$digest" in
    *[!0-9a-f]*) fail "the digest must be lowercase hexadecimal" ;;
  esac
fi
test "$(count_fixed_lines "      url: ${expected_image}" "$adapter_file")" -eq 2 \
  || fail "both job bases must use the same immutable image"

if grep -Eq \
  '^[[:space:]]*(repo|branch|scaling|envVarGroups|fromDatabase|registryCredential|fromRegistryCreds|ipAllowList|dockerfilePath|dockerContext|buildCommand|startCommand|autoDeploy|autoDeployTrigger|preDeployCommand|initialDeployHook|databases|domains|disk):|^[[:space:]]*runtime: docker$|^[[:space:]]*- type: (web|pserv|cron|keyvalue|redis)$' \
  "$adapter_file"; then
  fail "an unapproved resource or source deployment is present"
fi
if grep -Eq '^[[:space:]]*- key: (API|DISPATCHER)_DATABASE_' "$adapter_file"; then
  fail "long-running runtime credentials are prohibited"
fi

bootstrap_block="$(worker_block 1 "$adapter_file")"
migration_block="$(worker_block 2 "$adapter_file")"
test "$(printf '%s\n' "$bootstrap_block" \
  | count_lines '^[[:space:]]*- key:' /dev/stdin)" -eq 9 \
  || fail "bootstrap allowlist must contain exactly nine keys"
test "$(printf '%s\n' "$migration_block" \
  | count_lines '^[[:space:]]*- key:' /dev/stdin)" -eq 4 \
  || fail "migration allowlist must contain exactly four keys"
printf '%s\n' "$bootstrap_block" | grep -q 'MIGRATION_DATABASE_' \
  && fail "bootstrap base contains migration connection credentials"
printf '%s\n' "$migration_block" \
  | grep -Eq 'BOOTSTRAP_DATABASE_|MYCHANDHA_STAGING_.*_PASSWORD' \
  && fail "migration base contains bootstrap or role-secret material"

for key in \
  BOOTSTRAP_DATABASE_HOST \
  BOOTSTRAP_DATABASE_NAME \
  BOOTSTRAP_DATABASE_USERNAME \
  BOOTSTRAP_DATABASE_PASSWORD \
  MYCHANDHA_STAGING_API_PASSWORD \
  MYCHANDHA_STAGING_DISPATCHER_PASSWORD \
  MYCHANDHA_STAGING_MIGRATION_PASSWORD \
  MIGRATION_DATABASE_URL \
  MIGRATION_DATABASE_USERNAME \
  MIGRATION_DATABASE_PASSWORD
do
  require_unsupplied_secret "$key" "$adapter_file"
done

test "$(count_lines '^        value: production,migration$' "$adapter_file")" -eq 1 \
  || fail "migration profile mapping is missing"
test "$(count_lines '^        value: verify-full$' "$adapter_file")" -eq 1 \
  || fail "bootstrap TLS verification is missing"
if grep -Eq \
  '(^|[[:space:]])(password|token|secret|credential):[[:space:]]*[^#[:space:]]' \
  "$adapter_file"; then
  fail "a literal credential-like value is present"
fi

echo "Render privileged job adapter validation passed: $adapter_file"
