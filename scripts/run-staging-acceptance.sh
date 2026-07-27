#!/usr/bin/env sh
set -eu

raw_directory="target/staging-evidence/raw"
sanitized_directory="target/staging-evidence/sanitized"
started_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
checks_file="$raw_directory/checks.tsv"

umask 077
mkdir -p "$raw_directory" "$sanitized_directory"
: > "$checks_file"

require_value() {
  name="$1"
  eval "value=\${$name:-}"
  test -n "$value" || {
    echo "acceptance_result=failed reason=missing_${name}" >&2
    exit 64
  }
}

for name in \
  EVIDENCE_COMMIT_SHA \
  EVIDENCE_IMAGE_REF \
  STAGING_API_BASE_URL \
  STAGING_VALID_TOKEN \
  STAGING_INVALID_ISSUER_TOKEN \
  STAGING_INVALID_AUDIENCE_TOKEN \
  STAGING_EXPIRED_TOKEN \
  STAGING_INVALID_SIGNATURE_TOKEN \
  STAGING_MISSING_SUBJECT_TOKEN \
  STAGING_METRICS_TOKEN \
  STAGING_ORGANIZATION_ID \
  STAGING_INACTIVE_ORGANIZATION_ID \
  STAGING_CROSS_TENANT_ORGANIZATION_ID
do
  require_value "$name"
done

case "$STAGING_API_BASE_URL" in
  https://*) ;;
  *)
    echo "acceptance_result=failed reason=https_required" >&2
    exit 64
    ;;
esac

request() {
  check_id="$1"
  path="$2"
  expected_status="$3"
  token="$4"
  organization_id="$5"
  response="$raw_directory/${check_id}.json"
  headers="$raw_directory/${check_id}.headers"
  config="$raw_directory/${check_id}.curl"
  {
    printf 'silent\n'
    printf 'show-error\n'
    printf 'url = "%s%s"\n' "$STAGING_API_BASE_URL" "$path"
    printf 'request = "GET"\n'
    printf 'header = "Accept: application/json"\n'
    if [ -n "$token" ]; then
      printf 'header = "Authorization: Bearer %s"\n' "$token"
    fi
    if [ -n "$organization_id" ]; then
      printf 'header = "X-Organization-Id: %s"\n' "$organization_id"
    fi
    printf 'dump-header = "%s"\n' "$headers"
    printf 'output = "%s"\n' "$response"
    printf 'write-out = "%%{http_code}"\n'
  } > "$config"
  status="$(curl --config "$config")"
  rm -f "$config"
  if [ "$status" = "$expected_status" ]; then
    printf '%s\tpassed\t%s\n' "$check_id" "$status" >> "$checks_file"
  else
    printf '%s\tfailed\t%s\n' "$check_id" "$status" >> "$checks_file"
    return 1
  fi
}

request readiness /actuator/health/readiness 200 "" ""
request unauthenticated /api/v1/platform/me 401 "" ""
request valid_identity /api/v1/platform/me 200 "$STAGING_VALID_TOKEN" ""
request invalid_issuer /api/v1/platform/me 401 \
  "$STAGING_INVALID_ISSUER_TOKEN" ""
request invalid_audience /api/v1/platform/me 401 \
  "$STAGING_INVALID_AUDIENCE_TOKEN" ""
request expired_token /api/v1/platform/me 401 "$STAGING_EXPIRED_TOKEN" ""
request invalid_signature /api/v1/platform/me 401 \
  "$STAGING_INVALID_SIGNATURE_TOKEN" ""
request missing_subject /api/v1/platform/me 401 \
  "$STAGING_MISSING_SUBJECT_TOKEN" ""
request protected_metrics /actuator/prometheus 200 "$STAGING_METRICS_TOKEN" ""
request same_tenant \
  "/api/v1/organizations/${STAGING_ORGANIZATION_ID}/platform/access" \
  200 "$STAGING_VALID_TOKEN" "$STAGING_ORGANIZATION_ID"
request missing_tenant \
  "/api/v1/organizations/${STAGING_ORGANIZATION_ID}/platform/access" \
  400 "$STAGING_VALID_TOKEN" ""
request malformed_tenant \
  "/api/v1/organizations/${STAGING_ORGANIZATION_ID}/platform/access" \
  400 "$STAGING_VALID_TOKEN" "not-a-uuid"
request inactive_tenant \
  "/api/v1/organizations/${STAGING_INACTIVE_ORGANIZATION_ID}/platform/access" \
  403 "$STAGING_VALID_TOKEN" "$STAGING_INACTIVE_ORGANIZATION_ID"
request cross_tenant \
  "/api/v1/organizations/${STAGING_CROSS_TENANT_ORGANIZATION_ID}/platform/access" \
  403 "$STAGING_VALID_TOKEN" "$STAGING_CROSS_TENANT_ORGANIZATION_ID"

exercise_rate_limit() {
  check_id="$1"
  path="$2"
  token="$3"
  organization_id="$4"
  maximum_requests="$5"
  rate_limited=0
  request_number=1
  while [ "$request_number" -le "$maximum_requests" ]; do
    response="$raw_directory/${check_id}-${request_number}.json"
    headers="$raw_directory/${check_id}-${request_number}.headers"
    config="$raw_directory/${check_id}-${request_number}.curl"
    {
      printf 'silent\n'
      printf 'show-error\n'
      printf 'url = "%s%s"\n' "$STAGING_API_BASE_URL" "$path"
      printf 'request = "GET"\n'
      printf 'header = "Accept: application/json"\n'
      if [ -n "$token" ]; then
        printf 'header = "Authorization: Bearer %s"\n' "$token"
      fi
      if [ -n "$organization_id" ]; then
        printf 'header = "X-Organization-Id: %s"\n' "$organization_id"
      fi
      printf 'dump-header = "%s"\n' "$headers"
      printf 'output = "%s"\n' "$response"
      printf 'write-out = "%%{http_code}"\n'
    } > "$config"
    status="$(curl --config "$config")"
    rm -f "$config"
    if [ "$status" = "429" ]; then
      retry_after="$(awk '
        BEGIN { IGNORECASE = 1 }
        /^Retry-After:[[:space:]]*[1-9][0-9]*/ {
          gsub("\r", "", $2)
          print $2
          exit
        }
      ' "$headers")"
      test -n "$retry_after"
      grep -q '"code":"RATE_LIMIT_EXCEEDED"' "$response"
      printf '%s\n' "$retry_after" > "$raw_directory/${check_id}.retry"
      rate_limited=1
      break
    fi
    test "$status" = "200"
    request_number=$((request_number + 1))
  done
  test "$rate_limited" -eq 1
  printf '%s\tpassed\t429\n' "$check_id" >> "$checks_file"
}

exercise_rate_limit subject_rate_limit /api/v1/platform/me \
  "$STAGING_VALID_TOKEN" "" 65
exercise_rate_limit metrics_rate_limit /actuator/prometheus \
  "$STAGING_METRICS_TOKEN" "" 35
exercise_rate_limit client_rate_limit /actuator/health/liveness "" "" 130

retry_after="$(cat "$raw_directory/client_rate_limit.retry")"
test "$retry_after" -le 65
sleep "$((retry_after + 1))"
request client_rate_limit_refill /actuator/health/liveness 200 "" ""

failed="$(awk -F '\t' '$2 != "passed" { count++ } END { print count + 0 }' "$checks_file")"
test "$failed" -eq 0
finished_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
checks_json="$(awk -F '\t' '
  BEGIN { print "[" }
  {
    if (NR > 1) {
      print ","
    }
    printf "{\"id\":\"%s\",\"status\":\"%s\",\"httpStatus\":%s}", $1, $2, $3
  }
  END { print "]" }
' "$checks_file")"

evidence="$sanitized_directory/acceptance.json"
jq -n \
  --arg schema "EP-001-GATE-D-1" \
  --arg commit "$EVIDENCE_COMMIT_SHA" \
  --arg image "$EVIDENCE_IMAGE_REF" \
  --arg started "$started_at" \
  --arg finished "$finished_at" \
  --argjson checks "$checks_json" \
  '{
    schema:$schema,
    operation:"acceptance",
    status:"passed",
    commitSha:$commit,
    imageRef:$image,
    startedAt:$started,
    finishedAt:$finished,
    checks:$checks,
    temporaryBaseDeleted:"not_applicable"
  }' > "$evidence"
sh scripts/validate-staging-evidence.sh "$evidence"

echo "acceptance_result=passed"
