#!/usr/bin/env sh
set -eu

validator="scripts/validate-staging-evidence.sh"
fixtures="scripts/fixtures/staging-evidence"

sh "$validator" "$fixtures/valid.json" >/dev/null
for fixture in "$fixtures"/invalid-*.json; do
  if sh "$validator" "$fixture" >/dev/null 2>&1; then
    echo "Expected staging evidence fixture to be rejected: $fixture" >&2
    exit 1
  fi
done

echo "Staging evidence positive and tamper-rejection fixtures passed."
