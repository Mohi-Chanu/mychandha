# Phase 1 Gate C evidence package

Status: Pull-request and post-merge CI evidence verified; explicit Gate C
acceptance and evidence-record publication pending
Checklist: `EP-001`
Deployment contract: `MCDC-001` version `1`
Change control: `CC-001`
Prepared: 2026-07-26

## Package identity

| Field | Evidence |
|---|---|
| Gate | Phase 1 Gate C — deployment adapter |
| Repository | `Mohi-Chanu/mychandha` |
| Implementation branch | `codex/phase-1-gate-c` |
| Evidence branch | `codex/phase-1-gate-c-evidence` (local; publication pending) |
| Baseline commit | `bbffe16fe270a7e8d36a2db6889da5b435349d6e` |
| Implementation commit | `3b3ba3462759b6713aebe0e5677ba296bb0b2ed0` |
| Pull request | `#4`, merged 2026-07-26 |
| Pull-request merge-test commit | `c04df76aabebbf1e688824ba739888454641591f` |
| `main` merge commit | `818c9b2d1d991bed67c51b6f3a9978998ab8c7b2` |
| Pull-request CI | Run `30203978892`, job `89798724750`, attempt `1` |
| Post-merge `main` CI | Run `30204430920`, job `89799925991`, attempt `1` |
| Environment class | Local verification and GitHub-hosted CI; no provider environment |
| External resources | None created or changed |
| Evidence-package recorder | Codex-assisted repository record, pending user review |
| Explicit Gate C acceptor | Pending |

The user approved the refined Gate C repository implementation, then separately
approved its commit, push, branch, and draft pull request. The user merged PR
`#4` after its CI passed and directed preparation of the next evidence step
after the post-merge `main` run was green.

Those approvals did not authorize image publication, release-workflow
execution, GitHub package or protected-environment changes, provider
provisioning, migration, deployment, staging execution, Phase 1 closure, or
Phase 2.

## Approved and implemented scope

- [x] Removed the unsafe auto-discovered root `render.yaml`.
- [x] Added a deliberately non-live Render staging example.
- [x] Mapped one image-backed API and one image-backed dispatcher to the same
      digest placeholder.
- [x] Enforced profile and credential-class isolation.
- [x] Kept migration execution outside the long-lived services.
- [x] Added the Render conformance and deployment runbook.
- [x] Added structural validation and positive/tamper-rejection fixtures.
- [x] Integrated adapter validation into foundation validation.
- [x] Added the canonical deployment contract, environment capability matrix,
      provider-adapter abstraction, evidence checklist, and change-control rule.
- [x] Aligned authoritative repository documentation.
- [x] Preserved all Gate C non-goals.

No Java source, SQL migration, API behavior, Dockerfile, dependency, CI/release
workflow, or external resource was changed by Gate C.

## Local source and validation evidence

The approved local validation completed before implementation commit
`3b3ba3462759b6713aebe0e5677ba296bb0b2ed0`.

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
| Tests | 52 across 15 suites; zero failures, errors, or skips |
| Checkstyle | Zero violations |
| PMD | Passed |
| JaCoCo | Report generated |
| SpotBugs | Zero findings |
| Shell syntax | Passed |
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

The approved validation plan assigns the adapter and tamper suite to local
evidence. Gate C did not modify CI to add a separate adapter-validation step;
the pull-request and post-merge runs retained every Gate B CI gate.

## Evidence-record validation

The evidence-only branch was revalidated locally on 2026-07-26:

- Java `21.0.12`, Maven `3.9.16`, Docker Engine `29.6.2`, and PostgreSQL
  `17.10` through Testcontainers;
- `mvn verify` passed all 52 tests with zero failures, errors, or skips;
- Checkstyle reported zero violations, PMD passed, JaCoCo generated its report,
  and SpotBugs reported zero findings;
- shell syntax, the adapter contract, all positive/tamper fixtures, and
  `scripts/validate-foundation.sh` passed;
- all local links and trailing-whitespace checks passed across 29 Markdown
  files; and
- `git diff --check` passed.

## Adapter-contract evidence

| Requirement | Evidence | Status |
|---|---|---|
| Canonical contract | `docs/deployment-contract.md` (`MCDC-001` v1) | Passed |
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

## Pull-request CI evidence

PR `#4` tested the approved implementation branch at head commit
`3b3ba3462759b6713aebe0e5677ba296bb0b2ed0`. GitHub evaluated synthetic merge
commit `c04df76aabebbf1e688824ba739888454641591f`.

| Field | Evidence |
|---|---|
| Workflow/run | `CI` run `30203978892`, run number `19`, attempt `1` |
| Event | `pull_request` |
| Job | `verify`, job `89798724750` |
| Time | 2026-07-26 13:24:37Z through 13:27:17Z |
| Result | Success |
| Tests | 52 across 15 suites; zero failures, errors, or skips |
| Static analysis | Checkstyle zero; PMD passed; JaCoCo retained; SpotBugs zero |
| Secret scan | Gitleaks `8.30.1`, full Git history, passed |
| OCI platform/digest | `linux/amd64`; `sha256:7c7b72c200e51eb91e7855d928a390749cb7b44d9204dc22d4e415f7eb6aff67` |
| SBOM | CycloneDX `1.7`, 142 components |
| Vulnerability scan | Trivy `0.72.0`, zero HIGH/CRITICAL findings |
| Evidence verification | Release-evidence manifest and recorded checksums passed |

The conditional `Enforce secret-scan result` step was skipped because it runs
only when the preceding Gitleaks step fails. The scan step succeeded and the
sanitized evidence records `result=passed`.

## Post-merge `main` CI evidence

PR `#4` merged into `main` as
`818c9b2d1d991bed67c51b6f3a9978998ab8c7b2` at
2026-07-26 13:37:48Z. The authoritative post-merge run is:

| Field | Evidence |
|---|---|
| Workflow/run | `CI` run `30204430920`, run number `20`, attempt `1` |
| Event | `push` to `main` |
| Job | `verify`, job `89799925991` |
| Time | 2026-07-26 13:37:51Z through 13:40:57Z |
| Result | Success |
| Java | Temurin Java 21 |
| Build | Maven `verify`; `BUILD SUCCESS` |
| Tests | 52 across 15 suites; zero failures, errors, or skips |
| Checkstyle | Zero violations |
| PMD | Version `7.17.0`; passed as part of `mvn verify` |
| JaCoCo | Report generated and retained |
| SpotBugs | `BugInstance size is 0`; `Error size is 0` |
| Secret scan | Gitleaks `8.30.1`, full Git history, `result=passed` |
| OCI platform | `linux/amd64` |
| OCI index media type | `application/vnd.oci.image.index.v1+json` |
| OCI manifest media type | `application/vnd.oci.image.manifest.v1+json` |
| OCI manifest digest | `sha256:48a4f9b0f44703344bb9dcdc524c59f7fc6c355e4e3b5ae7ba018f87ea28cd11` |
| SBOM | CycloneDX `1.7`, 142 components |
| Vulnerability scan | Trivy `0.72.0`, zero HIGH/CRITICAL findings |
| Evidence verification | Manifest commit/run/digest and all recorded file hashes independently matched |

The `main` digest is the only Gate C candidate eligible for a later approved
no-rebuild promotion. The pull-request digest is evidence for the PR gate, not
the deployment identity.

## Retained artifact index

GitHub Actions generated the following `main` artifacts. Repository users with
Actions access can read them. The configured retention is 14 days; GitHub
records expiry on 2026-08-09, and an authorized repository operator may delete
them earlier.

| Artifact | GitHub artifact ID | GitHub artifact digest | Expiry |
|---|---:|---|---|
| `verification-reports` | `8632653712` | `sha256:8f59a209e27506d7099d1a85173970edf177fbf20f69c08d05694061354f973a` | 2026-08-09 13:40:48Z |
| `mychandha-oci-818c9b2d1d991bed67c51b6f3a9978998ab8c7b2` | `8632654380` | `sha256:4807e3f271f77bfd8480b8aa9a7e93d7abbcae45316d456757c5bc8b67ead65e` | 2026-08-09 13:40:49Z |

The retained OCI artifact contains this sanitized checksummed index:

| File | Media type | SHA-256 | Sensitivity |
|---|---|---|---|
| `mychandha.oci.tar` | `application/vnd.oci.image.layout.v1.tar` | `4a22c9569814b375e46f4f4d913bdcc1c5292e7a6eda88038156890e2f2f5943` | Internal build artifact |
| `image-metadata.json` | `application/json` | `75578ea90188f3d3bd2049ba4e45aa03b17efb0a9f231bc0532514abd9116ff0` | Sanitized internal metadata |
| `release-evidence.json` | `application/json` | `49b38081daf069fed75d5d3f202f0d82a14eced5936bce7021cc049bda763226` | Sanitized evidence |
| `mychandha.cdx.json` | `application/vnd.cyclonedx+json` | `c0bd17038fa485247c5e4ea203ecc3107eadc622303632002f0032079dc8a4d8` | Internal dependency inventory |
| `trivy-vulnerability-report.json` | `application/json` | `788b5be1b62091924ea84c60a20e040b2d4c6b1a75a938c7b1aecd8d3ff29d75` | Sanitized security evidence |
| `gitleaks-evidence.txt` | `text/plain` | `4a3e296dec2ad3ce0c66e10dd34465976dcea40c7fea7cce55b269301ada1459` | Sanitized security evidence |

No credentials, tokens, personal data, provider identifiers, or customer data
were found in the retained evidence inspected for this record.

## EP-001 disposition

### Passed for the repository and CI gate

- [x] Gate, evidence version, repository, branches, full commits, PR, workflow,
      run, job, attempt, timestamps, and retention recorded.
- [x] Approved repository scope, non-goals, and `CC-001` boundary recorded.
- [x] Clean source state and local/CI toolchain evidence recorded.
- [x] Unit, architecture, security, contract, integration, negative, and
      tamper-rejection results recorded.
- [x] Static analysis and coverage generation passed.
- [x] OCI name, platform, media type, digest, checksum, and source binding
      recorded.
- [x] SBOM, vulnerability scan, secret scan, and manifest verification passed.
- [x] `MCDC-001` version and adapter revision are identified.
- [x] Material deviations: none.
- [x] Unapproved external actions confirmed not performed.

### Not applicable to the repository-only Gate C execution

The following items were not executed because external-resource and staging
execution approval has not been granted:

- database bootstrap, migration execution, deployed schema/grant, TLS, and
  network-path evidence;
- registry publication and no-rebuild promotion;
- provider service creation or configuration;
- deployed API readiness and dispatcher process-state evidence;
- JWT, tenant, audit, idempotency, inbox, outbox, and log-safety staging
  acceptance;
- routing, metrics, alerts, logs, backups, restore, rollback, forward-fix,
  RPO/RTO, cost, and cleanup evidence.

These are deferred, not waived. They become blocking at the applicable
external-resource and staging-execution gates.

## Security and residual risks

- No secret value or external provider identifier was added.
- Migration credentials remain structurally prohibited from long-lived
  services.
- API and dispatcher credential classes remain mutually isolated.
- Mutable-image and source-build paths remain rejected.
- The accepted `main` OCI archive is not published and expires on 2026-08-09
  unless a separately approved action retains or promotes it.
- The exact migration runner and network mechanism remain blocking decisions.
- Dispatcher backlog collection, alerting, and log routing remain blocking
  provider-capability decisions.
- Plans, costs, owners, backups, retention, and cleanup remain unselected.
- The non-live example must never be renamed or applied with placeholders.

## Acceptance record

- [x] Approved Gate C repository scope implemented.
- [x] Local validation completed without weakening existing gates.
- [x] Implementation committed and pushed.
- [x] Draft PR `#4` opened and pull-request CI passed.
- [x] PR `#4` merged and post-merge `main` CI passed.
- [x] Artifact and evidence-manifest integrity independently verified.
- [x] Material deviations: none.
- [x] Unapproved external actions confirmed not performed.
- [ ] Gate C CI evidence explicitly accepted by the user.
- [ ] Evidence branch committed, pushed, reviewed, and merged.
- [ ] Exact external-resource proposal approved.
- [ ] Staging execution approved and completed.

## Exact next gate

Review this evidence package. If it is accepted, the next independent
repository action is approval to commit these evidence-only changes, push
`codex/phase-1-gate-c-evidence`, and open a draft evidence pull request.

After that record is merged and its `main` CI is green, Gate C can be recorded
as accepted. The next design gate will be the exact non-production
external-resource proposal required by `CC-001`, including:

- Supabase, Render, PostgreSQL, and registry resources;
- region, plans, estimated cost, owners, operators, retention, and cleanup;
- immutable-image publication and access policy;
- migration-runner identity and network path;
- secrets storage and rotation;
- TLS, rate limits, metrics, alerts, log routing, and backups; and
- restore, rollback, forward-fix, RPO/RTO, and hard-stop criteria.

Evidence acceptance does not authorize that proposal's implementation,
resource provisioning, release execution, publication, migration, deployment,
staging acceptance, Phase 1 closure, or Phase 2.
