#!/usr/bin/env sh
set -eu

evidence_dir=${1:-}
expected_commit=${2:-}
expected_run_id=${3:-}

if [ -z "$evidence_dir" ] || [ -z "$expected_commit" ] || [ -z "$expected_run_id" ]; then
  echo "Usage: $0 <evidence-directory> <expected-commit-sha> <expected-run-id>" >&2
  exit 2
fi

case "$expected_commit" in
  *[!0-9a-f]*)
    echo "Expected commit must be a full 40-character lowercase SHA." >&2
    exit 2
    ;;
esac

test "${#expected_commit}" -eq 40 || {
  echo "Expected commit must be a full 40-character lowercase SHA." >&2
  exit 2
}

case "$expected_run_id" in
  *[!0-9]*)
    echo "Expected workflow run ID must be numeric." >&2
    exit 2
    ;;
esac

archive="$evidence_dir/mychandha.oci.tar"
metadata="$evidence_dir/image-metadata.json"
evidence="$evidence_dir/release-evidence.json"
sbom="$evidence_dir/mychandha.cdx.json"
trivy_report="$evidence_dir/trivy-vulnerability-report.json"
gitleaks_evidence="$evidence_dir/gitleaks-evidence.txt"

for required_file in \
  "$archive" \
  "$metadata" \
  "$evidence" \
  "$sbom" \
  "$trivy_report" \
  "$gitleaks_evidence"
do
  test -s "$required_file" || {
    echo "Missing release evidence file: $required_file" >&2
    exit 1
  }
done

recorded_commit=$(jq -er '.commit' "$evidence")
recorded_schema_version=$(jq -er '.schema_version' "$evidence")
recorded_run_id=$(jq -er '.workflow_run_id' "$evidence")
recorded_image_digest=$(jq -er '.image_digest' "$evidence")
metadata_image_digest=$(jq -er '."containerimage.digest"' "$metadata")
recorded_archive_sha=$(jq -er '.oci_archive_sha256' "$evidence")
recorded_sbom_sha=$(jq -er '.sbom_sha256' "$evidence")
recorded_trivy_sha=$(jq -er '.trivy_report_sha256' "$evidence")
recorded_gitleaks_sha=$(jq -er '.gitleaks_evidence_sha256' "$evidence")

test "$recorded_commit" = "$expected_commit" || {
  echo "Release evidence commit does not match the requested commit." >&2
  exit 1
}

test "$recorded_schema_version" = "1" || {
  echo "Unsupported release evidence schema version." >&2
  exit 1
}

test "$recorded_run_id" = "$expected_run_id" || {
  echo "Release evidence workflow run does not match the requested run." >&2
  exit 1
}

test "$recorded_image_digest" = "$metadata_image_digest" || {
  echo "Image digest differs between release evidence and Buildx metadata." >&2
  exit 1
}

case "$recorded_image_digest" in
  sha256:*) recorded_image_hash=${recorded_image_digest#sha256:} ;;
  *) recorded_image_hash= ;;
esac

case "$recorded_image_hash" in
  *[!0-9a-f]* | '')
    echo "Release evidence contains an invalid OCI image digest." >&2
    exit 1
    ;;
esac

test "${#recorded_image_hash}" -eq 64 || {
  echo "Release evidence contains an invalid OCI image digest." >&2
  exit 1
}

verify_sha256() {
  expected=$1
  path=$2
  actual=$(sha256sum "$path" | awk '{print $1}')
  test "$actual" = "$expected" || {
    echo "SHA-256 mismatch for $path" >&2
    exit 1
  }
}

verify_sha256 "$recorded_archive_sha" "$archive"
verify_sha256 "$recorded_sbom_sha" "$sbom"
verify_sha256 "$recorded_trivy_sha" "$trivy_report"
verify_sha256 "$recorded_gitleaks_sha" "$gitleaks_evidence"

grep -qx "result=passed" "$gitleaks_evidence" || {
  echo "Secret-scan evidence does not record a passing result." >&2
  exit 1
}

echo "$recorded_image_digest"
