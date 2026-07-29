#!/usr/bin/env sh
set -eu

validator="scripts/validate-render-job-adapter.sh"
example="deploy/render/render.staging-jobs.yaml.example"
fixtures="scripts/fixtures/render-job-adapter"
digest="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
temporary_directory="$(mktemp -d)"
trap 'rm -rf "$temporary_directory"' EXIT

sh "$validator" "$example" >/dev/null

RENDER_ADAPTER_EXPECTED_IMAGE="registry.example.invalid/mychandha@${digest}" \
  sh "$validator" "$fixtures/valid-materialized.yaml" >/dev/null

for fixture in "$fixtures"/invalid-*.yaml; do
  if sh "$validator" "$fixture" >/dev/null 2>&1; then
    echo "Expected Render job fixture to be rejected: $fixture" >&2
    exit 1
  fi
done

sed 's#/etc/secrets/supabase-ca.crt#/tmp/unapproved-ca.crt#g' \
  "$fixtures/valid-materialized.yaml" \
  > "$temporary_directory/invalid-ca-path.yaml"
if RENDER_ADAPTER_EXPECTED_IMAGE="registry.example.invalid/mychandha@${digest}" \
  sh "$validator" "$temporary_directory/invalid-ca-path.yaml" \
    >/dev/null 2>&1; then
  echo "Expected an unexpected privileged-job CA path to be rejected" >&2
  exit 1
fi

echo "Render privileged job positive and tamper-rejection fixtures passed."
