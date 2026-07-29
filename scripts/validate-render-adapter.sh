#!/usr/bin/env sh
set -eu

adapter_file="${1:-deploy/render/render.staging.yaml.example}"
root_blueprint="${RENDER_ADAPTER_ROOT_BLUEPRINT:-render.yaml}"
image_placeholder="REPLACE_AFTER_EXTERNAL_APPROVAL/IMAGE@sha256:REPLACE_WITH_64_HEX_DIGEST"
expected_image="${RENDER_ADAPTER_EXPECTED_IMAGE:-$image_placeholder}"
expected_api_name="${RENDER_ADAPTER_EXPECTED_API_SERVICE_NAME:-mychandha-staging-api}"
expected_dispatcher_name="${RENDER_ADAPTER_EXPECTED_DISPATCHER_SERVICE_NAME:-mychandha-staging-dispatcher}"

fail() {
  echo "Render adapter validation failed: $1" >&2
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

service_block() {
  service_type="$1"
  file="$2"
  awk -v requested="$service_type" '
    /^  - type: / {
      active = ($3 == requested)
    }
    active {
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
  || fail "missing non-live adapter example: $adapter_file"
test -s docs/deployment-contract.md \
  || fail "missing canonical deployment contract"
test -s docs/render-deployment-runbook.md \
  || fail "missing Render deployment runbook"
test -s docs/evidence-package.md \
  || fail "missing standard evidence package"
test -s docs/change-control.md \
  || fail "missing change-control rule"

test "$(count_lines '^previews:$' "$adapter_file")" -eq 1 \
  || fail "previews must be declared exactly once"
test "$(count_lines '^  generation: off$' "$adapter_file")" -eq 1 \
  || fail "preview generation must be off"
test "$(count_lines '^services:$' "$adapter_file")" -eq 1 \
  || fail "services must be declared exactly once"
test "$(count_lines '^  - type: web$' "$adapter_file")" -eq 1 \
  || fail "exactly one web service is required"
test "$(count_lines '^  - type: worker$' "$adapter_file")" -eq 1 \
  || fail "exactly one worker service is required"
test "$(count_lines '^    runtime: image$' "$adapter_file")" -eq 2 \
  || fail "both services must use runtime: image"
test "$(count_lines '^    plan: starter$' "$adapter_file")" -eq 2 \
  || fail "both services must use the Starter plan"
test "$(count_lines '^    region: singapore$' "$adapter_file")" -eq 2 \
  || fail "both services must use the Singapore region"
test "$(count_lines '^    numInstances: 1$' "$adapter_file")" -eq 2 \
  || fail "both services must use exactly one instance"
test "$(count_lines '^    maxShutdownDelaySeconds: 30$' "$adapter_file")" -eq 2 \
  || fail "both services must use the accepted shutdown delay"
if [ "$expected_image" != "$image_placeholder" ]; then
  image_digest="${expected_image##*@sha256:}"
  test "$image_digest" != "$expected_image" \
    || fail "the approved image must use a sha256 digest"
  test "${#image_digest}" -eq 64 \
    || fail "the approved image digest must contain 64 hexadecimal characters"
  case "$image_digest" in
    *[!0-9a-f]*)
      fail "the approved image digest must be lowercase hexadecimal"
      ;;
  esac
fi

test "$(count_fixed_lines "      url: ${expected_image}" "$adapter_file")" -eq 2 \
  || fail "both services must use the same expected immutable image"
test "$(count_fixed_lines "    name: ${expected_api_name}" \
  "$adapter_file")" -eq 1 \
  || fail "API service name does not match the expected value"
test "$(count_fixed_lines "    name: ${expected_dispatcher_name}" \
  "$adapter_file")" -eq 1 \
  || fail "dispatcher service name does not match the expected value"
test "$(count_lines '^    healthCheckPath: /actuator/health/readiness$' \
  "$adapter_file")" -eq 1 \
  || fail "API readiness path is missing or invalid"
test "$(count_lines '^        value: production,api$' "$adapter_file")" -eq 1 \
  || fail "API profile mapping is missing or duplicated"
test "$(count_lines '^        value: production,dispatcher$' "$adapter_file")" -eq 1 \
  || fail "dispatcher profile mapping is missing or duplicated"

if grep -Eq \
  '^[[:space:]]*(repo|branch|scaling|envVarGroups|fromDatabase|registryCredential|fromRegistryCreds|ipAllowList|dockerfilePath|dockerContext|dockerCommand|buildCommand|startCommand|autoDeploy|autoDeployTrigger|preDeployCommand|initialDeployHook|databases|domains|disk):|^[[:space:]]*runtime: docker$|^[[:space:]]*- type: (pserv|cron|keyvalue|redis)$' \
  "$adapter_file"; then
  fail "source deployment or an unapproved provider resource is present"
fi

if grep -Eq '^[[:space:]]*- key: MIGRATION_DATABASE_' "$adapter_file"; then
  fail "migration credentials must not appear in a long-lived service"
fi

api_block="$(service_block web "$adapter_file")"
dispatcher_block="$(service_block worker "$adapter_file")"

test "$(printf '%s\n' "$api_block" \
  | count_lines '^[[:space:]]*- key:' /dev/stdin)" -eq 10 \
  || fail "API environment allowlist must contain exactly ten keys"
test "$(printf '%s\n' "$dispatcher_block" \
  | count_lines '^[[:space:]]*- key:' /dev/stdin)" -eq 5 \
  || fail "dispatcher environment allowlist must contain exactly five keys"

printf '%s\n' "$api_block" \
  | grep -Eq 'DISPATCHER_DATABASE_|MIGRATION_DATABASE_' \
  && fail "API service contains another process credential class"
printf '%s\n' "$dispatcher_block" \
  | grep -Eq 'API_DATABASE_|MIGRATION_DATABASE_|SUPABASE_' \
  && fail "dispatcher service contains another process configuration class"

for key in \
  API_DATABASE_URL \
  API_DATABASE_USERNAME \
  API_DATABASE_PASSWORD \
  SUPABASE_JWT_ISSUER \
  SUPABASE_JWKS_URI \
  DISPATCHER_DATABASE_URL \
  DISPATCHER_DATABASE_USERNAME \
  DISPATCHER_DATABASE_PASSWORD
do
  require_unsupplied_secret "$key" "$adapter_file"
done

test "$(count_lines '^      - key: SUPABASE_JWT_AUDIENCE$' "$adapter_file")" -eq 1 \
  || fail "Supabase audience key must occur exactly once"
test "$(count_lines '^        value: authenticated$' "$adapter_file")" -eq 1 \
  || fail "Supabase audience must use the accepted value"
test "$(count_lines '^      - key: RATE_LIMIT_ENABLED$' "$adapter_file")" -eq 1 \
  || fail "rate limiting must be explicitly enabled"
test "$(count_lines '^        value: \"true\"$' "$adapter_file")" -eq 1 \
  || fail "the accepted rate-limit enabled setting must be true"
test "$(count_lines '^      - key: RATE_LIMIT_CLIENT_ADDRESS_STRATEGY$' \
  "$adapter_file")" -eq 1 \
  || fail "the client-address strategy must occur exactly once"
test "$(count_lines '^        value: render-edge-first-hop$' \
  "$adapter_file")" -eq 1 \
  || fail "the accepted Render client-address strategy is missing"
if grep -Eq 'RATE_LIMIT_TRUST_FORWARDED|RATE_LIMIT_TRUSTED_PROXY_CIDRS' \
  "$adapter_file"; then
  fail "legacy or guessed forwarded-address configuration is prohibited"
fi
test "$(count_lines '^      - key: (API|DISPATCHER)_DATABASE_SSL_ROOT_CERTIFICATE$' \
  "$adapter_file")" -eq 2 \
  || fail "both runtime CA path inputs are required"
test "$(count_lines '^        value: /etc/secrets/supabase-ca.crt$' \
  "$adapter_file")" -eq 2 \
  || fail "both runtime CA paths must use the accepted Render secret file"

if grep -Eq \
  '(^|[[:space:]])(password|token|secret|credential):[[:space:]]*[^#[:space:]]' \
  "$adapter_file"; then
  fail "a literal credential-like value is present"
fi

echo "Render adapter contract validation passed: $adapter_file"
