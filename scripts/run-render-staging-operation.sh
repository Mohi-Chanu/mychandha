#!/usr/bin/env sh
set -eu

api_base="https://api.render.com/v1"
evidence_directory="target/staging-evidence/sanitized"
temporary_directory="$(mktemp -d)"
base_service_id=""
base_deleted="not_applicable"
ca_certificate_sha256=""
ca_secret_deploy_id=""
started_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

umask 077
mkdir -p "$evidence_directory"

cleanup_files() {
  rm -f "$temporary_directory"/*
  rmdir "$temporary_directory"
}

valid_render_id() {
  value="$1"
  case "$value" in
    [a-z][a-z][a-z]-[a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9][a-z0-9])
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

require_render_id() {
  name="$1"
  value="$2"
  valid_render_id "$value" || {
    echo "staging_operation_result=failed reason=invalid_${name}" >&2
    exit 64
  }
}

api_call() {
  method="$1"
  path="$2"
  body_file="$3"
  response_file="$4"
  config_file="$temporary_directory/curl-config"
  {
    printf 'silent\n'
    printf 'show-error\n'
    printf 'request = "%s"\n' "$method"
    printf 'url = "%s%s"\n' "$api_base" "$path"
    printf 'header = "Accept: application/json"\n'
    printf 'header = "Content-Type: application/json"\n'
    printf 'header = "Authorization: Bearer %s"\n' "$RENDER_API_KEY"
    printf 'output = "%s"\n' "$response_file"
    printf 'write-out = "%%{http_code}"\n'
    if [ -n "$body_file" ]; then
      printf 'data-binary = "@%s"\n' "$body_file"
    fi
  } > "$config_file"
  status="$(curl --config "$config_file")"
  rm -f "$config_file"
  case "$status" in
    200 | 201 | 204)
      ;;
    *)
      echo "staging_operation_result=failed reason=render_api_status_${status}" >&2
      return 1
      ;;
  esac
}

delete_base() {
  if [ -z "$base_service_id" ]; then
    return 0
  fi
  response="$temporary_directory/delete-base.json"
  if api_call DELETE "/services/${base_service_id}" "" "$response"; then
    base_deleted="passed"
    base_service_id=""
    return 0
  fi
  base_deleted="failed"
  return 1
}

on_exit() {
  result=$?
  trap - EXIT HUP INT TERM
  if [ -n "$base_service_id" ]; then
    delete_base || result=1
  fi
  cleanup_files
  exit "$result"
}
trap on_exit EXIT
trap 'exit 130' HUP INT TERM

wait_for_deploy() {
  service_id="$1"
  deploy_id="${2:-}"
  attempts=0
  while [ "$attempts" -lt 180 ]; do
    response="$temporary_directory/deploy-status.json"
    if [ -n "$deploy_id" ]; then
      api_call GET "/services/${service_id}/deploys/${deploy_id}" "" "$response"
      status="$(jq -r '.status // empty' "$response")"
    else
      api_call GET "/services/${service_id}/deploys?limit=1" "" "$response"
      status="$(jq -r '.[0].deploy.status // empty' "$response")"
      deploy_id="$(jq -r '.[0].deploy.id // empty' "$response")"
    fi
    case "$status" in
      live)
        printf '%s' "$deploy_id"
        return 0
        ;;
      build_failed | update_failed | pre_deploy_failed | canceled | deactivated)
        echo "staging_operation_result=failed reason=deploy_${status}" >&2
        return 1
        ;;
    esac
    attempts=$((attempts + 1))
    sleep 5
  done
  echo "staging_operation_result=failed reason=deploy_timeout" >&2
  return 1
}

wait_for_new_deploy() {
  service_id="$1"
  prior_deploy_id="$2"
  attempts=0
  while [ "$attempts" -lt 60 ]; do
    response="$temporary_directory/latest-deploy.json"
    api_call GET "/services/${service_id}/deploys?limit=1" "" "$response"
    deploy_id="$(jq -r '.[0].deploy.id // empty' "$response")"
    if [ -n "$deploy_id" ] && [ "$deploy_id" != "$prior_deploy_id" ]; then
      ca_secret_deploy_id="$(wait_for_deploy "$service_id" "$deploy_id")"
      return
    fi
    attempts=$((attempts + 1))
    sleep 5
  done
  echo "staging_operation_result=failed reason=secret_file_deploy_timeout" >&2
  return 1
}

install_ca_secret_file() {
  service_id="$1"
  prior_deploy_id="$2"
  case "$SUPABASE_DATABASE_CA_CERTIFICATE" in
    *"-----BEGIN CERTIFICATE-----"*"-----END CERTIFICATE-----"*)
      ;;
    *)
      echo "staging_operation_result=failed reason=invalid_ca_certificate" >&2
      return 1
      ;;
  esac
  ca_certificate_sha256="$(
    printf '%s' "$SUPABASE_DATABASE_CA_CERTIFICATE" \
      > "$temporary_directory/supabase-ca.crt"
    sha256sum "$temporary_directory/supabase-ca.crt" | awk '{print $1}'
  )"
  ca_material="$temporary_directory/supabase-ca.crt"
  request="$temporary_directory/ca-secret-file.json"
  response="$temporary_directory/ca-secret-file-response.json"
  jq -n \
    --rawfile content "$ca_material" \
    '{content:$content}' > "$request"
  api_call PUT \
    "/services/${service_id}/secret-files/supabase-ca.crt" \
    "$request" \
    "$response"
  rm -f "$request" "$response" "$ca_material"
  wait_for_new_deploy "$service_id" "$prior_deploy_id"
}

wait_for_job() {
  service_id="$1"
  job_id="$2"
  attempts=0
  while [ "$attempts" -lt 180 ]; do
    response="$temporary_directory/job-status.json"
    api_call GET "/services/${service_id}/jobs/${job_id}" "" "$response"
    status="$(jq -r '.status // empty' "$response")"
    case "$status" in
      succeeded)
        return 0
        ;;
      failed | canceled)
        echo "staging_operation_result=failed reason=job_${status}" >&2
        return 1
        ;;
    esac
    attempts=$((attempts + 1))
    sleep 5
  done
  echo "staging_operation_result=failed reason=job_timeout" >&2
  return 1
}

create_base_and_run_job() {
  mode="$1"
  case "$mode" in
    bootstrap)
      name="mychandha-staging-bootstrap-base"
      command="/app/ops/run-bootstrap.sh"
      env_vars="$(jq -n \
        --arg host "$BOOTSTRAP_DATABASE_HOST" \
        --arg name "$BOOTSTRAP_DATABASE_NAME" \
        --arg username "$BOOTSTRAP_DATABASE_USERNAME" \
        --arg password "$BOOTSTRAP_DATABASE_PASSWORD" \
        --arg apiPassword "$MYCHANDHA_STAGING_API_PASSWORD" \
        --arg dispatcherPassword "$MYCHANDHA_STAGING_DISPATCHER_PASSWORD" \
        --arg migrationPassword "$MYCHANDHA_STAGING_MIGRATION_PASSWORD" \
        '[
          {key:"BOOTSTRAP_DATABASE_HOST",value:$host},
          {key:"BOOTSTRAP_DATABASE_PORT",value:"5432"},
          {key:"BOOTSTRAP_DATABASE_NAME",value:$name},
          {key:"BOOTSTRAP_DATABASE_USERNAME",value:$username},
          {key:"BOOTSTRAP_DATABASE_PASSWORD",value:$password},
          {key:"BOOTSTRAP_DATABASE_SSL_ROOT_CERTIFICATE",value:"/etc/secrets/supabase-ca.crt"},
          {key:"MYCHANDHA_STAGING_API_PASSWORD",value:$apiPassword},
          {key:"MYCHANDHA_STAGING_DISPATCHER_PASSWORD",value:$dispatcherPassword},
          {key:"MYCHANDHA_STAGING_MIGRATION_PASSWORD",value:$migrationPassword}
        ]')"
      ;;
    migrate)
      name="mychandha-staging-migration-base"
      command="/app/ops/run-migration.sh"
      env_vars="$(jq -n \
        --arg url "$MIGRATION_DATABASE_URL" \
        --arg username "$MIGRATION_DATABASE_USERNAME" \
        --arg password "$MIGRATION_DATABASE_PASSWORD" \
        '[
          {key:"SPRING_PROFILES_ACTIVE",value:"production,migration"},
          {key:"MIGRATION_DATABASE_URL",value:$url},
          {key:"MIGRATION_DATABASE_USERNAME",value:$username},
          {key:"MIGRATION_DATABASE_PASSWORD",value:$password},
          {key:"MIGRATION_DATABASE_SSL_ROOT_CERTIFICATE",value:"/etc/secrets/supabase-ca.crt"}
        ]')"
      ;;
  esac

  request="$temporary_directory/create-service.json"
  response="$temporary_directory/create-service-response.json"
  jq -n \
    --arg name "$name" \
    --arg owner "$RENDER_OWNER_ID" \
    --arg environment "$RENDER_ENVIRONMENT_ID" \
    --arg image "$EVIDENCE_IMAGE_REF" \
    --argjson envVars "$env_vars" \
    '{
      type:"background_worker",
      name:$name,
      ownerId:$owner,
      autoDeploy:"no",
      image:{ownerId:$owner,imagePath:$image},
      environmentId:$environment,
      envVars:$envVars,
      serviceDetails:{
        runtime:"image",
        numInstances:1,
        plan:"starter",
        region:"singapore",
        maxShutdownDelaySeconds:30,
        envSpecificDetails:{dockerCommand:"/app/ops/run-idle.sh"}
      }
    }' > "$request"
  api_call POST "/services" "$request" "$response"
  base_service_id="$(jq -r '.id // empty' "$response")"
  require_render_id base_service_id "$base_service_id"
  base_deploy_id="$(wait_for_deploy "$base_service_id")"
  install_ca_secret_file "$base_service_id" "$base_deploy_id"
  base_deploy_id="$ca_secret_deploy_id"

  job_request="$temporary_directory/create-job.json"
  job_response="$temporary_directory/create-job-response.json"
  jq -n --arg command "$command" \
    '{startCommand:$command,planId:"plan-srv-006"}' > "$job_request"
  api_call POST "/services/${base_service_id}/jobs" "$job_request" "$job_response"
  job_id="$(jq -r '.id // empty' "$job_response")"
  require_render_id job_id "$job_id"
  wait_for_job "$base_service_id" "$job_id"
  delete_base

  write_evidence "$mode" "$base_deploy_id" "$job_id" "" ""
}

deploy_exact_image() {
  service_id="$1"
  require_render_id service_id "$service_id"
  request="$temporary_directory/deploy-request.json"
  response="$temporary_directory/deploy-response.json"
  jq -n --arg image "$EVIDENCE_IMAGE_REF" '{imageUrl:$image}' > "$request"
  api_call POST "/services/${service_id}/deploys" "$request" "$response"
  deploy_id="$(jq -r '.id // empty' "$response")"
  require_render_id deploy_id "$deploy_id"
  wait_for_deploy "$service_id" "$deploy_id" >/dev/null
  printf '%s' "$deploy_id"
}

rollback_deploy() {
  service_id="$1"
  prior_deploy_id="$2"
  require_render_id service_id "$service_id"
  require_render_id rollback_deploy_id "$prior_deploy_id"
  request="$temporary_directory/rollback-request.json"
  response="$temporary_directory/rollback-response.json"
  jq -n --arg deploy "$prior_deploy_id" '{deployId:$deploy}' > "$request"
  api_call POST "/services/${service_id}/rollback" "$request" "$response"
  rollback_id="$(jq -r '.id // empty' "$response")"
  require_render_id rollback_result_id "$rollback_id"
  wait_for_deploy "$service_id" "$rollback_id" >/dev/null
  printf '%s' "$rollback_id"
}

write_evidence() {
  operation="$1"
  event_one="$2"
  event_two="$3"
  service_one="$4"
  service_two="$5"
  finished_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  evidence="$evidence_directory/${operation}.json"
  jq -n \
    --arg schema "EP-001-GATE-D-1" \
    --arg operation "$operation" \
    --arg commit "$EVIDENCE_COMMIT_SHA" \
    --arg image "$EVIDENCE_IMAGE_REF" \
    --arg started "$started_at" \
    --arg finished "$finished_at" \
    --arg eventOne "$event_one" \
    --arg eventTwo "$event_two" \
    --arg serviceOne "$service_one" \
    --arg serviceTwo "$service_two" \
    --arg baseDeleted "$base_deleted" \
    --arg caCertificateSha256 "$ca_certificate_sha256" \
    '{
      schema:$schema,
      operation:$operation,
      status:"passed",
      commitSha:$commit,
      imageRef:$image,
      startedAt:$started,
      finishedAt:$finished,
      eventIds:[$eventOne,$eventTwo] | map(select(length > 0)),
      serviceIds:[$serviceOne,$serviceTwo] | map(select(length > 0)),
      temporaryBaseDeleted:$baseDeleted
    }
    + if $caCertificateSha256 == "" then {}
      else {caCertificateSha256:$caCertificateSha256}
      end' > "$evidence"
  sh scripts/validate-staging-evidence.sh "$evidence"
}

: "${RENDER_API_KEY:?RENDER_API_KEY is required}"
: "${STAGING_OPERATION:?STAGING_OPERATION is required}"
: "${EVIDENCE_COMMIT_SHA:?EVIDENCE_COMMIT_SHA is required}"
: "${EVIDENCE_IMAGE_REF:?EVIDENCE_IMAGE_REF is required}"

case "$EVIDENCE_COMMIT_SHA" in
  *[!0-9a-f]*)
    echo "staging_operation_result=failed reason=invalid_commit_sha" >&2
    exit 64
    ;;
esac
test "${#EVIDENCE_COMMIT_SHA}" -eq 40 || {
  echo "staging_operation_result=failed reason=invalid_commit_sha" >&2
  exit 64
}
image_digest="${EVIDENCE_IMAGE_REF##*@sha256:}"
test "$image_digest" != "$EVIDENCE_IMAGE_REF" \
  && test "${#image_digest}" -eq 64 || {
  echo "staging_operation_result=failed reason=invalid_image_ref" >&2
  exit 64
}
case "$image_digest" in
  *[!0-9a-f]*)
    echo "staging_operation_result=failed reason=invalid_image_ref" >&2
    exit 64
    ;;
esac

case "$STAGING_OPERATION" in
  bootstrap)
    : "${RENDER_OWNER_ID:?RENDER_OWNER_ID is required}"
    : "${RENDER_ENVIRONMENT_ID:?RENDER_ENVIRONMENT_ID is required}"
    : "${SUPABASE_DATABASE_CA_CERTIFICATE:?SUPABASE_DATABASE_CA_CERTIFICATE is required}"
    create_base_and_run_job bootstrap
    ;;
  migrate)
    : "${RENDER_OWNER_ID:?RENDER_OWNER_ID is required}"
    : "${RENDER_ENVIRONMENT_ID:?RENDER_ENVIRONMENT_ID is required}"
    : "${SUPABASE_DATABASE_CA_CERTIFICATE:?SUPABASE_DATABASE_CA_CERTIFICATE is required}"
    create_base_and_run_job migrate
    ;;
  deploy)
    : "${API_SERVICE_ID:?API_SERVICE_ID is required}"
    : "${DISPATCHER_SERVICE_ID:?DISPATCHER_SERVICE_ID is required}"
    api_deploy="$(deploy_exact_image "$API_SERVICE_ID")"
    dispatcher_deploy="$(deploy_exact_image "$DISPATCHER_SERVICE_ID")"
    write_evidence deploy "$api_deploy" "$dispatcher_deploy" \
      "$API_SERVICE_ID" "$DISPATCHER_SERVICE_ID"
    ;;
  rollback)
    : "${API_SERVICE_ID:?API_SERVICE_ID is required}"
    : "${DISPATCHER_SERVICE_ID:?DISPATCHER_SERVICE_ID is required}"
    : "${API_ROLLBACK_DEPLOY_ID:?API_ROLLBACK_DEPLOY_ID is required}"
    : "${DISPATCHER_ROLLBACK_DEPLOY_ID:?DISPATCHER_ROLLBACK_DEPLOY_ID is required}"
    api_rollback="$(rollback_deploy "$API_SERVICE_ID" "$API_ROLLBACK_DEPLOY_ID")"
    dispatcher_rollback="$(rollback_deploy \
      "$DISPATCHER_SERVICE_ID" "$DISPATCHER_ROLLBACK_DEPLOY_ID")"
    write_evidence rollback "$api_rollback" "$dispatcher_rollback" \
      "$API_SERVICE_ID" "$DISPATCHER_SERVICE_ID"
    ;;
  cleanup)
    : "${CLEANUP_SERVICE_ID:?CLEANUP_SERVICE_ID is required}"
    require_render_id cleanup_service_id "$CLEANUP_SERVICE_ID"
    response="$temporary_directory/cleanup-service.json"
    api_call GET "/services/${CLEANUP_SERVICE_ID}" "" "$response"
    service_name="$(jq -r '.name // empty' "$response")"
    case "$service_name" in
      mychandha-staging-bootstrap-base | mychandha-staging-migration-base)
        ;;
      *)
        echo "staging_operation_result=failed reason=cleanup_target_not_temporary" >&2
        exit 64
        ;;
    esac
    api_call DELETE "/services/${CLEANUP_SERVICE_ID}" "" \
      "$temporary_directory/cleanup-response.json"
    base_deleted="passed"
    write_evidence cleanup "$CLEANUP_SERVICE_ID" "" "" ""
    ;;
  *)
    echo "staging_operation_result=failed reason=unsupported_operation" >&2
    exit 64
    ;;
esac

echo "staging_operation_result=passed operation=${STAGING_OPERATION}"
