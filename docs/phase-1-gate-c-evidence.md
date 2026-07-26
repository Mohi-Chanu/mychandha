# Phase 1 Gate C evidence package

Status: Local implementation evidence complete; repository publication and CI
evidence pending
Checklist: `EP-001`
Deployment contract: `MCDC-001` version `1`
Change control: `CC-001`
Prepared: 2026-07-26

## Package identity

| Field | Evidence |
|---|---|
| Gate | Phase 1 Gate C — deployment adapter |
| Repository | MyChandha |
| Branch | `codex/phase-1-gate-c-proposal` |
| Baseline commit | `bbffe16fe270a7e8d36a2db6889da5b435349d6e` |
| Implementation commit | Pending; working tree is intentionally uncommitted |
| Pull request/merge | Pending separate approval |
| Gate C CI run/job | Pending separate publication and CI |
| Environment class | Local development/verification |
| External resources | None created or changed |
| Evidence retention | Current local working tree; CI retention pending |

The user explicitly approved Gate C repository implementation according to the
refined proposal. That approval did not authorize commit, push, pull request,
merge, workflow execution, image publication, provider changes, migration, or
deployment.

## Implemented scope

- [x] Removed the unsafe auto-discovered root `render.yaml`.
- [x] Added a deliberately non-live Render staging example.
- [x] Mapped one image-backed API and one image-backed dispatcher to the same
      digest placeholder.
- [x] Enforced profile and credential-class isolation.
- [x] Kept migration execution outside the long-lived services.
- [x] Added the Render conformance and deployment runbook.
- [x] Added structural validation and positive/tamper-rejection fixtures.
- [x] Integrated adapter validation into foundation validation.
- [x] Aligned authoritative repository documentation.
- [x] Preserved the Gate C non-goals.

No Java source, SQL migration, API behavior, Dockerfile, OCI build, or external
resource was changed.

## Local source and validation evidence

| Check | Result |
|---|---|
| Java | `21.0.12` |
| Maven | `3.9.16` |
| Docker Engine | `29.6.2` |
| PostgreSQL/Testcontainers | PostgreSQL `17.10` |
| `sh scripts/validate-render-adapter.sh` | Passed |
| `sh scripts/test-validate-render-adapter.sh` | Passed |
| `sh scripts/validate-foundation.sh` | Passed |
| `mvn verify` | Passed |
| Tests | 52; zero failures, errors, or skips |
| Checkstyle | Zero violations |
| PMD | Passed |
| JaCoCo | Report generated |
| SpotBugs | Zero findings |
| Documentation local-link/whitespace checks | Passed |
| `git diff --check` | Passed |

Tamper-rejection fixtures prove rejection of:

- mutable image tags;
- source Dockerfile builds;
- API/dispatcher profile mixing;
- cross-process database credentials;
- migration credentials in a long-lived service;
- a literal secret value;
- an unapproved database resource;
- an invalid API readiness path;
- malformed adapter structure;
- missing dispatcher topology; and
- a simulated live root Blueprint.

## Adapter contract evidence

| Requirement | Evidence | Status |
|---|---|---|
| Canonical contract | `docs/deployment-contract.md` (`MCDC-001` v1) | Passed locally |
| Adapter abstraction | Architecture and canonical contract | Passed by review |
| Environment capability matrix | Canonical contract | Present |
| Render mapping | `deploy/render/render.staging.yaml.example` | Passed structurally |
| Materialized mapping validation | Reserved-name/digest positive fixture with explicit expected values | Passed locally |
| Provider conformance | `docs/render-deployment-runbook.md` | Structural rows recorded; live rows open |
| API mapping | `production,api`, API credential allowlist, readiness path | Passed structurally |
| Dispatcher mapping | `production,dispatcher`, dispatcher credential allowlist, no ingress service type | Passed structurally |
| Migration mapping | Protected short-lived runner contract only | Mechanism open and blocking for external gate |
| Immutable image | Same digest-shaped placeholder in both services | Passed structurally; registry publication pending |
| Preview prevention | Non-live filename plus `previews.generation: off` | Passed structurally |
| Root Blueprint prevention | Root file absent and validator rejects it | Passed |

## Artifact and supply-chain evidence

The Gate C change does not modify application dependencies, the Dockerfile, or
the CI/release workflow. Local `mvn verify` passed.

Gate C pull-request evidence remains pending and must include:

- full-history Gitleaks;
- Java/PostgreSQL verification and static analysis;
- one `linux/amd64` OCI build;
- CycloneDX SBOM;
- Trivy HIGH/CRITICAL result;
- release-evidence manifest verification; and
- artifact/checksum retention.

The previously accepted Gate B OCI evidence is baseline evidence only. It is
not presented as an artifact built from the uncommitted Gate C working tree.

## Database, deployment, and acceptance status

The following `EP-001` items are pending rather than passed:

- database role bootstrap against a selected environment;
- migration runner, TLS, and network-path validation;
- Flyway execution and deployed role/grant evidence;
- registry reference and no-rebuild publication;
- Render service creation or configuration;
- API readiness and dispatcher process evidence in staging;
- JWT, tenant, audit, idempotency, inbox, outbox, and log-safety staging
  acceptance;
- metrics, alerts, log retention, backups, restore, rollback, and forward-fix
  rehearsals;
- external cost, ownership, retention, and cleanup evidence; and
- Phase 1 closure.

These items are not applicable to the repository-only local execution step
because their external-resource and staging-execution approvals have not been
granted. They become required at the applicable later `CC-001` steps.

## Security and residual risks

- No secret value or external identifier was added.
- Migration credentials are structurally prohibited from both long-lived
  service blocks.
- API and dispatcher credential classes are mutually isolated.
- Mutable-image and source-build paths are rejected.
- The exact migration runner/network mechanism remains a blocking external
  decision.
- Dispatcher backlog collection, alerting, and log routing remain blocking
  external capability decisions.
- Plans, costs, owners, backups, retention, and cleanup remain unselected.
- The non-live example must never be renamed or applied with placeholders.

## Acceptance record

- [x] Approved Gate C repository scope implemented locally.
- [x] Local validation completed without weakening existing gates.
- [x] Material deviations: none.
- [x] Unapproved external actions confirmed not performed.
- [ ] Implementation commit recorded.
- [ ] Draft pull request and Gate C CI evidence recorded.
- [ ] Gate C CI evidence explicitly accepted.
- [ ] Exact external-resource proposal approved.
- [ ] Staging execution approved and completed.

## Exact next gate

Review this local implementation and evidence. If accepted, the next
independent approval is to commit the Gate C changes, push the approved branch,
and open a draft pull request for CI evidence.

That publication approval would not authorize merge, release-workflow
execution, image publication, external-resource changes, migration, or
deployment.
