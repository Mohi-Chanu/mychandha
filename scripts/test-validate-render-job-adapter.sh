#!/usr/bin/env sh
set -eu

validator="scripts/validate-render-job-adapter.sh"
example="deploy/render/render.staging-jobs.yaml.example"
fixtures="scripts/fixtures/render-job-adapter"
digest="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"

sh "$validator" "$example" >/dev/null

RENDER_ADAPTER_EXPECTED_IMAGE="registry.example.invalid/mychandha@${digest}" \
  sh "$validator" "$fixtures/valid-materialized.yaml" >/dev/null

for fixture in "$fixtures"/invalid-*.yaml; do
  if sh "$validator" "$fixture" >/dev/null 2>&1; then
    echo "Expected Render job fixture to be rejected: $fixture" >&2
    exit 1
  fi
done

echo "Render privileged job positive and tamper-rejection fixtures passed."
