#!/usr/bin/env sh
set -eu

workflow="${1:-.github/workflows/staging-deploy.yml}"

fail() {
  echo "Gate D workflow validation failed: $1" >&2
  exit 1
}

count() {
  pattern="$1"
  grep -Ec "$pattern" "$workflow" || true
}

job_block() {
  requested="$1"
  awk -v requested="$requested" '
    /^  [a-z][a-z0-9-]*:$/ {
      current = substr($0, 3, length($0) - 3)
    }
    current == requested {
      print
    }
  ' "$workflow"
}

test -s "$workflow" || fail "workflow file is missing"
test "$(count '^  workflow_dispatch:$')" -eq 1 \
  || fail "workflow must be manually dispatched exactly once"
test "$(count '^  (push|pull_request|schedule|repository_dispatch):')" -eq 0 \
  || fail "automatic or indirect execution triggers are prohibited"

input_block="$(awk '
  /^    inputs:$/ { capture = 1 }
  /^permissions:$/ { capture = 0 }
  capture { print }
' "$workflow")"
test "$(printf '%s\n' "$input_block" | grep -Ec '^      [a-z][a-z0-9_]*:$')" -eq 10 \
  || fail "the exact ten non-secret dispatch inputs are required"
for input in \
  operation \
  verified_run_id \
  commit_sha \
  image_ref \
  api_service_id \
  dispatcher_service_id \
  api_rollback_deploy_id \
  dispatcher_rollback_deploy_id \
  cleanup_service_id \
  confirmation
do
  test "$(printf '%s\n' "$input_block" | grep -Ec "^      ${input}:$")" -eq 1 \
    || fail "dispatch input ${input} is missing or duplicated"
done
if printf '%s\n' "$input_block" | grep -Eqi \
  'secrets\.|password|token|credential|database_(url|username|password)'; then
  fail "a credential or secret reference appears in workflow inputs"
fi

for operation in bootstrap migrate deploy acceptance rollback cleanup; do
  test "$(count "^          - ${operation}$")" -eq 1 \
    || fail "operation ${operation} is missing or duplicated"
  block="$(job_block "$operation")"
  test -n "$block" || fail "job ${operation} is missing"
  test "$(printf '%s\n' "$block" | grep -Ec '^    environment: staging-deploy$')" -eq 1 \
    || fail "job ${operation} is not protected by staging-deploy"
  test "$(printf '%s\n' "$block" | grep -Ec \
    "^    if: inputs.operation == '${operation}'$")" -eq 1 \
    || fail "job ${operation} does not have an exact operation guard"
  test "$(printf '%s\n' "$block" | grep -Ec '^    needs: preflight$')" -eq 1 \
    || fail "job ${operation} does not depend on preflight"
done

preflight="$(job_block preflight)"
test -n "$preflight" || fail "preflight job is missing"
if printf '%s\n' "$preflight" | grep -Eq \
  '^[[:space:]]*environment:|secrets\.'; then
  fail "preflight must not receive protected environment secrets"
fi
for required in \
  'GATE-D-${OPERATION}-${COMMIT_SHA}' \
  'actions/download-artifact@3e5f45b2cfb9172054b4087a40e8e0b5a5461e7c' \
  'scripts/verify-release-evidence.sh' \
  'expected_ref="ghcr.io/${GITHUB_REPOSITORY,,}@${verified_digest}"' \
  'test "$IMAGE_REF" = "$expected_ref"'
do
  printf '%s\n' "$preflight" | grep -Fq "$required" \
    || fail "preflight invariant is missing: $required"
done

bootstrap="$(job_block bootstrap)"
migrate="$(job_block migrate)"
deploy="$(job_block deploy)"
acceptance="$(job_block acceptance)"
rollback="$(job_block rollback)"
cleanup="$(job_block cleanup)"

printf '%s\n' "$bootstrap" | grep -q 'BOOTSTRAP_DATABASE_PASSWORD:.*secrets\.' \
  || fail "bootstrap credential mapping is missing"
printf '%s\n' "$bootstrap" | grep -q 'MYCHANDHA_STAGING_MIGRATION_PASSWORD:.*secrets\.' \
  || fail "bootstrap role-secret mapping is missing"
printf '%s\n' "$bootstrap" \
  | grep -q 'SUPABASE_DATABASE_CA_CERTIFICATE:.*secrets\.' \
  || fail "bootstrap CA material mapping is missing"
if printf '%s\n' "$bootstrap" | grep -Eq \
  'MIGRATION_DATABASE_(URL|USERNAME|PASSWORD):|API_DATABASE_|DISPATCHER_DATABASE_'; then
  fail "bootstrap job mixes a runtime or migration connection credential"
fi

printf '%s\n' "$migrate" | grep -q 'MIGRATION_DATABASE_PASSWORD:.*secrets\.' \
  || fail "migration credential mapping is missing"
printf '%s\n' "$migrate" \
  | grep -q 'SUPABASE_DATABASE_CA_CERTIFICATE:.*secrets\.' \
  || fail "migration CA material mapping is missing"
if printf '%s\n' "$migrate" | grep -Eq \
  'BOOTSTRAP_DATABASE_|MYCHANDHA_STAGING_.*_PASSWORD|API_DATABASE_|DISPATCHER_DATABASE_'; then
  fail "migration job mixes another credential class"
fi

for block_name in deploy rollback cleanup; do
  eval "block=\$$block_name"
  if printf '%s\n' "$block" | grep -Eq \
    'DATABASE_|STAGING_.*_TOKEN|MYCHANDHA_STAGING_.*_PASSWORD'; then
    fail "${block_name} job receives an unrelated credential class"
  fi
done
for block_name in deploy rollback cleanup acceptance; do
  eval "block=\$$block_name"
  if printf '%s\n' "$block" | grep -q \
    'SUPABASE_DATABASE_CA_CERTIFICATE'; then
    fail "${block_name} job receives CA material"
  fi
done
if printf '%s\n' "$acceptance" | grep -Eq \
  'RENDER_API_KEY|DATABASE_|MYCHANDHA_STAGING_.*_PASSWORD'; then
  fail "acceptance job receives a provider or database credential"
fi

if grep -h 'uses:' "$workflow" \
  | grep -Ev 'uses: [^[:space:]#]+@[0-9a-f]{40}([[:space:]]*#.*)?$'; then
  fail "an action is not pinned to a full commit SHA"
fi

echo "Gate D protected workflow contract validation passed: $workflow"
