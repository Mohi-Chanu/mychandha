#!/usr/bin/env sh
set -eu

validator="scripts/validate-staging-workflow.sh"
source_workflow=".github/workflows/staging-deploy.yml"
temporary_directory="$(mktemp -d)"

cleanup() {
  rm -f "$temporary_directory"/*
  rmdir "$temporary_directory"
}
trap cleanup EXIT

sh "$validator" "$source_workflow" >/dev/null

cp "$source_workflow" "$temporary_directory/automatic.yml"
sed 's/^  workflow_dispatch:$/  push:\\n  workflow_dispatch:/' \
  "$temporary_directory/automatic.yml" > "$temporary_directory/automatic.changed"
mv "$temporary_directory/automatic.changed" "$temporary_directory/automatic.yml"

cp "$source_workflow" "$temporary_directory/unprotected.yml"
sed '0,/^    environment: staging-deploy$/s//    # environment removed/' \
  "$temporary_directory/unprotected.yml" > "$temporary_directory/unprotected.changed"
mv "$temporary_directory/unprotected.changed" "$temporary_directory/unprotected.yml"

cp "$source_workflow" "$temporary_directory/secret-input.yml"
sed '0,/description: Successful post-merge main CI run/s//description: ${{ secrets.DATABASE_PASSWORD }}/' \
  "$temporary_directory/secret-input.yml" > "$temporary_directory/secret-input.changed"
mv "$temporary_directory/secret-input.changed" "$temporary_directory/secret-input.yml"

cp "$source_workflow" "$temporary_directory/mixed-secret.yml"
sed '0,/          RENDER_API_KEY: ${{ secrets.RENDER_API_KEY }}/{
  /          RENDER_API_KEY: ${{ secrets.RENDER_API_KEY }}/i\
          API_DATABASE_PASSWORD: ${{ secrets.API_DATABASE_PASSWORD }}
}' "$temporary_directory/mixed-secret.yml" > "$temporary_directory/mixed-secret.changed"
mv "$temporary_directory/mixed-secret.changed" "$temporary_directory/mixed-secret.yml"

for fixture in "$temporary_directory"/*.yml; do
  if sh "$validator" "$fixture" >/dev/null 2>&1; then
    echo "Expected Gate D workflow tamper case to be rejected: $fixture" >&2
    exit 1
  fi
done

echo "Gate D workflow positive and tamper-rejection tests passed."
