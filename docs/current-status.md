# Current development status

Last updated: 2026-07-26

## Outcome

The previously accepted Phase 1 foundation and the Gate A and Gate B repository
checks are green. Gate A evidence was explicitly accepted on 2026-07-25, PR `#1`
merged, and the post-merge `main` GitHub Actions run `30161138292`, job
`89686534126`, passed on merge commit
`605b4fff9a026c0dbb804c97686e054b4b0370cb`. It passed:

- Java 21 `mvn verify`.
- 52 unit, architecture, security, migration, RLS, runtime-profile, and
  PostgreSQL integration tests with zero failures, errors, or skips.
- Checkstyle, PMD, JaCoCo, and SpotBugs.
- OCI image build.
- CycloneDX SBOM generation and artifact upload.
- The configured Trivy HIGH/CRITICAL vulnerability gate with zero findings.

The Gate A Trivy summary showed `-` in the secrets column and defined that
value as not scanned. Gate B closed that evidence gap with an explicit
blocking, pinned Gitleaks full-history scan.

The accepted local foundation commit chain ends at `284673d`:

```text
284673d Remediate scanned image vulnerabilities
c17a249 Avoid Testcontainers during image build
79f0732 Remove obsolete SpotBugs suppression
d336f73 Resolve SpotBugs findings
86a06cd Fix audit timestamp JDBC binding
67f9dfa Fix Phase 1 CI test compilation
c4e792a Establish validated Phase 1 platform foundation
```

The remote repository may use different commit IDs if individual patches were
committed on another workstation. The successful CI run, file content, and
tests are authoritative; do not rewrite history merely to match these local
IDs.

The remote-equivalent foundation chain ends at `d7496e7`, followed by the
Codex Desktop handoff commit `b12ff66`. The original handoff object
`3f43a6ad0216b23907a835b746488c30df5fe13d` is not stored in this clone, but
`b12ff66` applies the repository instructions, status, product decisions, and
roadmap handoff content.

Gate A repository implementation was approved and completed locally on
2026-07-25. It adds:

- mutually exclusive API, dispatcher, and migration runtime profiles, with a
  guarded local combined mode;
- the forward-only V2 migration, stable non-login group roles, controlled
  security-definer dispatcher routines, and credential-free bootstrap script;
- API-only web/security composition and dispatcher-only scheduling;
- RFC 9457 error consistency, separate request/correlation IDs, and safe W3C
  trace-context continuity through the outbox;
- startup/readiness/durable-delivery health and bounded outbox metrics;
- module/API/SQL-boundary tests and local PostgreSQL Compose support.

Local Java 21 evidence currently shows:

- all production and test sources compile;
- 37 non-Docker unit, architecture, configuration, API, and observability
  tests pass;
- Checkstyle reports zero violations;
- PMD passes;
- SpotBugs reports zero findings; and
- `scripts/validate-foundation.sh` passes.

At the time of Gate A validation, this workstation had no Docker-compatible
runtime. GitHub Actions run `30160139310` supplied the required Docker-backed
evidence: all 15 PostgreSQL/Testcontainers tests passed, including the V2 role,
RLS, dispatcher-routine, and runtime-profile integration checks.

Draft PR `#1` triggered Gate A CI run `30159737559`. Its Docker-backed suite
exposed two deterministic corrections before the later image gates could run:
the custom `startup` health contributor clashed with the health group of the
same name, and the audit immutability test still expected the trigger response
through an API role that V2 now correctly denies table mutation. The branch
renames the contributor to `startupState` and separately verifies API privilege
denial and owner-level trigger enforcement. The corrected rerun
`30160139310` passed all configured CI stages.

Gate B repository implementation was approved on 2026-07-25 and is implemented
on `codex/phase-1-gate-b`. It:

- pins GitHub Actions, PostgreSQL, scanner images, the Dockerfile frontend, and
  Docker build/runtime images to immutable revisions;
- checks out complete history and runs blocking, redacted Gitleaks scanning;
- exports the single CI-built `linux/amd64` image as a retained OCI archive;
- records commit, OCI digest, archive digest, SBOM digest, vulnerability-report
  digest, secret-scan evidence digest, and workflow run ID;
- verifies the evidence bundle before upload;
- retains the release candidate and sanitized evidence for 14 days; and
- adds a protected manual workflow that accepts a successful `main` CI run,
  re-verifies its evidence, reruns both security gates, and uses ORAS to
  promote the exact archive without rebuilding.

Gate B was committed and pushed to `codex/phase-1-gate-b`. PR `#2` passed its
corrected CI rerun and merged into `main` as
`e34239f34056ea1b6bf5769e5e7920a8ceedf053` on 2026-07-25. The post-merge
`main` run `30166358486`, job `89699959544`, passed on that exact commit. It
recorded:

- a redacted Gitleaks `8.30.1` scan over full Git history with no leaks;
- Java 21 `mvn verify` with all 52 tests passing and no failures, errors, or
  skips;
- Checkstyle, PMD, JaCoCo, and SpotBugs;
- a retained `linux/amd64` OCI archive with manifest digest
  `sha256:befc26d564687ce34ee826f7c77bf418b43d83e861b9ec9edfa6cba3057633ba`;
- CycloneDX SBOM generation and a successful Trivy `0.72.0`
  HIGH/CRITICAL gate;
- successful release-evidence manifest verification; and
- successful upload of `verification-reports` and
  `mychandha-oci-e34239f34056ea1b6bf5769e5e7920a8ceedf053`, retained through
  2026-08-08 unless an authorized operator deletes them earlier.

The user explicitly accepted Gate B evidence on 2026-07-26. No image was
published, the manual release workflow was not executed, and no package,
GitHub environment, deployment, or other external resource was created or
changed.

Gate B local validation passed Actionlint `1.7.12`, POSIX shell syntax,
checksum-verified Gitleaks `8.30.1` over all 11 existing commits with no leaks,
positive and tamper-rejection release-evidence fixtures, the structural
validator, and Java `21.0.12`/Maven `3.9.11` verification for all 37 non-Docker
tests plus Checkstyle, PMD, JaCoCo reporting, and SpotBugs.

After Docker Desktop became available, local revalidation on 2026-07-26 used
Java `21.0.12`, Maven `3.9.16`, Docker Engine `29.6.2`, and PostgreSQL `17.10`
through Testcontainers. Full `mvn verify` passed all 52 tests with zero
failures, errors, or skips, plus Checkstyle, PMD, JaCoCo, and SpotBugs.

## Closed validation issues

- Corrected tenant-role composite ownership and tenant-scoped inbox identity.
- Serialized concurrent idempotency-key use.
- Added stale outbox-lease recovery and dead-letter behavior.
- Made audit timestamp storage JDBC-compatible while preserving deterministic
  microsecond hash evidence.
- Added executable audit-chain verification.
- Required a non-empty Supabase JWT subject.
- Made API records defensively immutable and correlation IDs explicitly safe.
- Resolved static-analysis findings without broad suppressions.
- Kept Testcontainers in the CI verification stage rather than inside the
  Docker build.
- Added SBOM generation and a non-bypassed HIGH/CRITICAL Trivy gate.
- Upgraded runtime packages and PostgreSQL JDBC to remove scan findings.

## Readiness package

Preparation of the Phase 1 Platform Foundation Readiness package was approved
on 2026-07-25. The approval covered repository-owned design documentation only
and explicitly rejected treating reference examples as requirements. The user
reviewed and approved the package's open design decisions on the same date.

The prepared package is:

- `docs/phase-1-platform-foundation-readiness.md`;
- `docs/phase-1-repository-change-proposal.md`;
- `docs/phase-1-exit-report.md`;
- `docs/adr/`, module boundaries, observability standards, API/error contract,
  and developer-experience guidelines; and
- the aligned roadmap, decision, and operations references.

It introduces no new provider or infrastructure dependency. Redis, generic
workers, frontend/CDN/storage infrastructure, and other reference examples are
out of scope. The existing durable outbox dispatcher remains in scope because
it is implemented and its recovery behavior is an existing acceptance
requirement.

## Gate C repository implementation

Gate C implementation was explicitly approved on 2026-07-26, merged through
PR `#4`, and is present on `main` as
`818c9b2d1d991bed67c51b6f3a9978998ab8c7b2`. It:

- removes the unsafe auto-discovered root `render.yaml`;
- adds a deliberately non-live, image-backed API/dispatcher example under
  `deploy/render/`;
- maps `production,api` and `production,dispatcher` to separate credential
  allowlists and the same immutable digest placeholder;
- leaves the protected migration runner and network path as blocking decisions
  for the exact external-resource proposal;
- adds the Render deployment runbook and provider conformance table;
- adds structural adapter validation and tamper-rejection fixtures;
- integrates adapter validation into the foundation validator; and
- records Gate C evidence in `docs/phase-1-gate-c-evidence.md`.

The implementation conforms to `MCDC-001`, uses `EP-001`, and follows
`CC-001`. It adds no Redis, Key Value, Render Workflows, Kafka, generic worker
platform, database, application code, SQL migration, or external resource.

Local Java 21/Docker verification passes all 52 tests plus Checkstyle, PMD,
JaCoCo, SpotBugs, the foundation validator, and the adapter positive and
tamper-rejection suite.

PR CI run `30203978892`, job `89798724750`, passed before merge. Post-merge
`main` run `30204430920`, job `89799925991`, then passed on the exact merge
commit. It recorded all 52 tests passing, zero Checkstyle and SpotBugs
findings, PMD and JaCoCo completion, full-history Gitleaks success, one
`linux/amd64` OCI archive, CycloneDX `1.7`, zero Trivy HIGH/CRITICAL findings,
and verified release-evidence hashes. The authoritative `main` OCI manifest
digest is
`sha256:48a4f9b0f44703344bb9dcdc524c59f7fc6c355e4e3b5ae7ba018f87ea28cd11`.

The evidence is recorded in `docs/phase-1-gate-c-evidence.md` and was
explicitly accepted by the user on 2026-07-26. Publication and merge of the
evidence-only record remain open.

## What is not done

- No Supabase or Render environment has been provisioned or changed.
- Staging deployment and acceptance tests have not run.
- No environment has applied the V2 database roles or runtime-profile
  separation; rate limits, alerts, backups, restore drill, log drain, and
  rollback rehearsal remain open.
- The accepted CI-built OCI archive is retained only as a GitHub Actions
  artifact; it has not been published to an approved registry as a deployable
  digest.
- The Gate C evidence-only repository record has not yet been reviewed and
  merged.
- Phase 2 implementation has not begun.

## Next action

Commit and push the accepted evidence-only changes on
`codex/phase-1-gate-c-evidence` and open the approved draft evidence pull
request. Review and merge remain separate actions.

After that record merges and `main` CI is green, the next design gate is the
exact non-production external-resource proposal. Preparing or accepting the
evidence record does not authorize any resource or execution action.

Running the release workflow, creating a GitHub package or protected
environment, publishing an OCI image, provisioning resources, deploying, or
starting Phase 2 remain separate approval gates.

Do not provision or modify Supabase, Render, PostgreSQL, GitHub, an artifact
registry, or another external resource until the applicable later proposal
receives explicit approval.

After staging acceptance passes, request approval for the Phase 2 design. Do
not begin Phase 2 code in the same approval step.
