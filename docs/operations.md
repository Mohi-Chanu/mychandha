# Operations

## Deployment

`Dockerfile` produces a non-root OCI image. Gate C removes the unsafe,
auto-discovered root `render.yaml` and replaces it with the deliberately
non-live `deploy/render/render.staging.yaml.example`.

The example maps:

- one image-backed API web service using `production,api`;
- one image-backed durable-dispatcher service using
  `production,dispatcher`;
- separate unsupplied database credential classes;
- the same immutable OCI digest placeholder for both services; and
- API readiness at `/actuator/health/readiness`.

It defines no database or other external resource. Do not rename, sync,
materialize, or deploy it until the exact provider resources and staging
execution are separately approved. The controlled migration boundary and
provider conformance requirements are in
`docs/render-deployment-runbook.md`.

Render PostgreSQL URIs are normalized to JDBC at startup. Other platforms can
supply JDBC URLs directly, so this compatibility behavior does not leak into
domain code.

The provider-neutral deployment authority is `MCDC-001` in
`docs/deployment-contract.md`. Render configuration is an adapter mapping and
must record its capability conformance and `EP-001` evidence without changing
the canonical process, secret, health, rollout, or rollback requirements.

## Immutable CI and release evidence

The Gate B CI workflow builds one `linux/amd64` OCI archive, extracts its OCI
layout for Trivy inspection, and retains the original archive with a
checksummed evidence manifest for 14 days. The manifest binds the commit and
workflow run to the OCI manifest digest, archive checksum, CycloneDX SBOM
checksum, HIGH/CRITICAL Trivy report checksum, and sanitized full-history
Gitleaks evidence checksum. Ordinary CI has read-only repository permissions.

The separate `Promote verified OCI image` workflow is manual and bound to the
`staging-release` environment. It accepts a successful `main` CI run and full
commit SHA, verifies the retained bundle, reruns Gitleaks and Trivy, and copies
the exact OCI archive to GHCR with ORAS without rebuilding. The resulting
deployment identity is the registry reference plus digest, never the commit tag
alone.

The workflow is repository configuration only until separately approved
GitHub package and protected-environment changes exist. Do not execute it or
publish an image without that approval. Retained GitHub Actions artifacts are
readable by users with repository Actions access and expire after 14 days;
authorized repository operators may delete them earlier under incident or
cleanup procedures.

Gate B's accepted post-merge evidence is `main` run `30166358486`, job
`89699959544`, on commit
`e34239f34056ea1b6bf5769e5e7920a8ceedf053`. Its retained OCI manifest digest
is `sha256:befc26d564687ce34ee826f7c77bf418b43d83e861b9ec9edfa6cba3057633ba`.
The associated OCI and verification artifacts expire on 2026-08-08 unless
deleted earlier by an authorized operator. Acceptance of this CI evidence does
not authorize promotion or deployment.

Gate C's latest verified post-merge evidence is `main` run `30204430920`, job
`89799925991`, on commit
`818c9b2d1d991bed67c51b6f3a9978998ab8c7b2`. Its retained OCI manifest digest
is `sha256:48a4f9b0f44703344bb9dcdc524c59f7fc6c355e4e3b5ae7ba018f87ea28cd11`.
The associated OCI and verification artifacts expire on 2026-08-09 unless
deleted earlier by an authorized operator. The evidence is recorded in
`docs/phase-1-gate-c-evidence.md` and was explicitly accepted on 2026-07-26.
Evidence-record merge remains pending. Verification and acceptance do not
authorize promotion or deployment.

Future gate and deployment evidence must follow the reusable checklist in
`docs/evidence-package.md`. Approval progression follows `CC-001` in
`docs/change-control.md`.

## Health and metrics

| Signal | Endpoint/metric | Alert intent |
|---|---|---|
| Liveness | `/actuator/health/liveness` | Process cannot make progress |
| Readiness | `/actuator/health/readiness` | Instance should leave routing |
| Startup | Runtime-specific startup signal | Initialization exceeded its budget or failed permanently |
| Durable delivery | `durableDelivery` health component | Oldest pending event exceeds 5 minutes |
| Delivery success | `mychandha.outbox.published` | Throughput and success |
| Delivery failure | `mychandha.outbox.failed`, `.retried`, `.dead.lettered` | Retry/dead-letter pressure |
| Backlog | `mychandha.outbox.pending`, `.oldest.age`, `.stale.processing` | Queue age and recovery |
| JVM/HTTP/DB | `/actuator/prometheus` | Saturation, latency and errors |

Every HTTP request returns `X-Correlation-Id`. A caller value is retained only
when it matches the safe format and length. Logs carry correlation ID and, in
worker scope, organization ID; raw tokens and contact data are never logged.

The complete health, structured-log, bounded-metric, tracing-compatibility, and
privacy requirements are in `docs/observability-standards.md`.

## Backup and recovery

Initial production requirements:

1. Render PostgreSQL backups and point-in-time recovery enabled on the selected
   production plan.
2. Daily restore verification in a non-production environment.
3. Quarterly recovery exercise against the target RPO of 5 minutes and RTO of
   60 minutes.
4. Supabase signing-key and tenant/session recovery procedures documented.
5. Audit export and retention configured only after legal retention approval.

## Deployment checklist

1. `mvn verify` passes with Java 21 and a Docker-capable test runtime.
2. The full-history Gitleaks gate passes and records sanitized evidence.
3. The retained OCI image build passes, runs as the non-root `mychandha` user,
   has no unresolved high/critical findings, and produces a CycloneDX SBOM.
4. The evidence manifest verifies the commit, CI run, OCI digest, and all
   retained checksums.
5. Flyway migration reviewed; no destructive migration is present.
6. Supabase issuer, JWKS URI, and audience point to the production project.
7. Database credentials are secrets and TLS is required.
8. Readiness is green and the outbox age is zero.
9. A valid JWT can call `/api/v1/platform/me`.
10. A cross-tenant access probe receives a denial.
11. Rollback image and database forward-fix plan are recorded.

## Incident priorities

- P0: suspected cross-tenant access, token verification bypass, audit loss, or
  acknowledged financial data loss.
- P1: authentication outage, durable event backlog over 15 minutes, or database
  unavailability.
- P2: isolated request errors, delayed non-critical workers, or degraded
  observability.
