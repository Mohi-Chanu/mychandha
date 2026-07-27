#!/usr/bin/env sh
set -eu

evidence_file="${1:?evidence JSON path is required}"

fail() {
  echo "Staging evidence validation failed: $1" >&2
  exit 1
}

test -s "$evidence_file" || fail "evidence file is missing"
jq -e . "$evidence_file" >/dev/null || fail "evidence is not valid JSON"
jq -e '
  .schema == "EP-001-GATE-D-1"
  and (.operation | IN("bootstrap","migrate","deploy","acceptance","rollback","cleanup"))
  and (.status | IN("passed","failed","pending"))
  and (.commitSha | test("^[0-9a-f]{40}$"))
  and (.imageRef | test("@sha256:[0-9a-f]{64}$"))
  and (.startedAt | type == "string")
  and (.finishedAt | type == "string")
  and (.temporaryBaseDeleted | IN("passed","failed","not_applicable"))
' "$evidence_file" >/dev/null || fail "required evidence fields are invalid"

if grep -Eqi \
  '(authorization["'\'']?[[:space:]]*:[[:space:]]*["'\'']?bearer|postgres(ql)?://[^[:space:]"]+:[^@[:space:]"]+@|["'\'']?(password|token|secret|credential)["'\'']?[[:space:]]*[:=]|service_role|eyJ[a-zA-Z0-9_-]{20,}\.)' \
  "$evidence_file"; then
  fail "credential-like material is present"
fi

allowed_keys='["schema","operation","status","commitSha","imageRef","startedAt","finishedAt","eventIds","serviceIds","checks","temporaryBaseDeleted"]'
jq -e --argjson allowed "$allowed_keys" \
  '([keys_unsorted[] | select(. as $key | $allowed | index($key) | not)] | length) == 0' \
  "$evidence_file" >/dev/null || fail "an undeclared top-level field is present"

echo "Sanitized Gate D staging evidence validation passed: $evidence_file"
