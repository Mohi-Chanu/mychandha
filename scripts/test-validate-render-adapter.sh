#!/usr/bin/env sh
set -eu

validator="scripts/validate-render-adapter.sh"
example="deploy/render/render.staging.yaml.example"
fixture_directory="scripts/fixtures/render-adapter"
fixture_digest="sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
temporary_directory="$(mktemp -d)"
trap 'rm -rf "$temporary_directory"' EXIT

sh "$validator" "$example" >/dev/null

RENDER_ADAPTER_EXPECTED_IMAGE="registry.example.invalid/mychandha@${fixture_digest}" \
RENDER_ADAPTER_EXPECTED_API_SERVICE_NAME="approved-api-fixture" \
RENDER_ADAPTER_EXPECTED_DISPATCHER_SERVICE_NAME="approved-dispatcher-fixture" \
  sh "$validator" "$fixture_directory/valid-materialized.yaml" >/dev/null

for fixture in "$fixture_directory"/invalid-*.yaml; do
  if sh "$validator" "$fixture" >/dev/null 2>&1; then
    echo "Expected Render adapter fixture to be rejected: $fixture" >&2
    exit 1
  fi
done

sed 's#value: render-edge-first-hop#value: trusted-proxy-cidr#' \
  "$fixture_directory/valid-materialized.yaml" \
  > "$temporary_directory/invalid-render-strategy.yaml"
if RENDER_ADAPTER_EXPECTED_IMAGE="registry.example.invalid/mychandha@${fixture_digest}" \
  RENDER_ADAPTER_EXPECTED_API_SERVICE_NAME="approved-api-fixture" \
  RENDER_ADAPTER_EXPECTED_DISPATCHER_SERVICE_NAME="approved-dispatcher-fixture" \
  sh "$validator" "$temporary_directory/invalid-render-strategy.yaml" \
    >/dev/null 2>&1; then
  echo "Expected an unapproved Render client-address strategy to be rejected" >&2
  exit 1
fi

sed 's#/etc/secrets/supabase-ca.crt#/tmp/unapproved-ca.crt#g' \
  "$fixture_directory/valid-materialized.yaml" \
  > "$temporary_directory/invalid-ca-path.yaml"
if RENDER_ADAPTER_EXPECTED_IMAGE="registry.example.invalid/mychandha@${fixture_digest}" \
  RENDER_ADAPTER_EXPECTED_API_SERVICE_NAME="approved-api-fixture" \
  RENDER_ADAPTER_EXPECTED_DISPATCHER_SERVICE_NAME="approved-dispatcher-fixture" \
  sh "$validator" "$temporary_directory/invalid-ca-path.yaml" \
    >/dev/null 2>&1; then
  echo "Expected an unexpected runtime CA path to be rejected" >&2
  exit 1
fi

if RENDER_ADAPTER_ROOT_BLUEPRINT="$fixture_directory/simulated-root-render.yaml" \
  sh "$validator" "$example" >/dev/null 2>&1; then
  echo "Expected a live root Blueprint to be rejected" >&2
  exit 1
fi

echo "Render adapter positive and tamper-rejection fixtures passed."
