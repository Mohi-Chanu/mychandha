#!/usr/bin/env sh
set -eu

validator="scripts/validate-render-adapter.sh"
example="deploy/render/render.staging.yaml.example"
fixture_directory="scripts/fixtures/render-adapter"
fixture_digest="sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

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

if RENDER_ADAPTER_ROOT_BLUEPRINT="$fixture_directory/simulated-root-render.yaml" \
  sh "$validator" "$example" >/dev/null 2>&1; then
  echo "Expected a live root Blueprint to be rejected" >&2
  exit 1
fi

echo "Render adapter positive and tamper-rejection fixtures passed."
