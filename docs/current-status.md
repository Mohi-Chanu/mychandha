# Current development status

Last updated: 2026-07-25

## Outcome

The previously accepted Phase 1 foundation and the Gate A repository checks
are green. The latest successful GitHub Actions evidence is run `30160139310`,
job `89684030456`, on commit
`2e42b4ad82cb77a9e62bfdd389f25e1ef1fa4f37`. It passed:

- Java 21 `mvn verify`.
- 52 unit, architecture, security, migration, RLS, runtime-profile, and
  PostgreSQL integration tests with zero failures, errors, or skips.
- Checkstyle, PMD, JaCoCo, and SpotBugs.
- OCI image build.
- CycloneDX SBOM generation and artifact upload.
- The configured Trivy HIGH/CRITICAL vulnerability gate with zero findings.

The Trivy summary showed `-` in the secrets column and defined that value as
not scanned. The successful run therefore does not prove zero secret findings.
An explicit blocking secret-scanning control is required before staging
deployment.

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

This workstation has no Docker-compatible runtime. GitHub Actions run
`30160139310` supplied the required Docker-backed evidence: all 15 PostgreSQL/
Testcontainers tests passed, including the V2 role, RLS, dispatcher-routine,
and runtime-profile integration checks.

Draft PR `#1` triggered Gate A CI run `30159737559`. Its Docker-backed suite
exposed two deterministic corrections before the later image gates could run:
the custom `startup` health contributor clashed with the health group of the
same name, and the audit immutability test still expected the trigger response
through an API role that V2 now correctly denies table mutation. The branch
renames the contributor to `startupState` and separately verifies API privilege
denial and owner-level trigger enforcement. The corrected rerun
`30160139310` passed all configured CI stages.

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

## What is not done

- No Supabase or Render environment has been provisioned or changed.
- Staging deployment and acceptance tests have not run.
- No environment has applied the V2 database roles or runtime-profile
  separation; rate limits, alerts, backups, restore drill, log drain, and
  rollback rehearsal remain open.
- The CI-built image is not published as an immutable deployable digest, and
  explicit secret-scanning evidence is missing.
- Phase 2 implementation has not begun.

## Next action

Review and explicitly accept Gate A run `30160139310` and its role-boundary
evidence. Do not begin Gate B until that evidence is accepted and Gate B
receives separate approval.

Do not provision or modify Supabase, Render, PostgreSQL, GitHub, an artifact
registry, or another external resource until the applicable later proposal
receives explicit approval.

After staging acceptance passes, request approval for the Phase 2 design. Do
not begin Phase 2 code in the same approval step.
