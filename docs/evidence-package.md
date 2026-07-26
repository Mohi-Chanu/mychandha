# Standard evidence package

Status: Normative
Checklist version: `EP-001`

## Purpose

Every repository, release, deployment, acceptance, and closure gate uses this
checklist. A gate may add evidence, but it must not silently remove an
applicable item. Mark an item `not applicable` only with a recorded reason.

Evidence proves what ran and what was accepted. It must not contain
credentials, tokens, raw personal data, unredacted provider payloads, unsafe
URLs, or copied customer data.

## Package identity

- [ ] Gate or release identifier
- [ ] Evidence-package version
- [ ] Repository and branch
- [ ] Full commit SHA
- [ ] Pull request and merge commit, when applicable
- [ ] CI workflow, run, job, attempt, and timestamps
- [ ] Initiator, reviewer, approver, and evidence recorder by approved identity
- [ ] Environment class and approved external resource references, when
      applicable
- [ ] Evidence retention location, expiry, deletion owner, and access boundary

## Scope and approval

- [ ] Approved proposal and `CC-001` lifecycle step
- [ ] Implemented scope
- [ ] Explicit non-goals
- [ ] Deviations from the approved proposal, or `none`
- [ ] Repository actions authorized
- [ ] External actions authorized
- [ ] Unapproved actions confirmed not performed
- [ ] Open decisions, risks, exceptions, and expiry dates

## Source and build

- [ ] Clean or explained working-tree state
- [ ] Toolchain and runtime versions
- [ ] Dependency-lock or immutable-input evidence
- [ ] Build command and result
- [ ] Unit, architecture, contract, security, and integration test summaries
- [ ] Static-analysis and coverage results
- [ ] Relevant negative and tamper-rejection tests

## Artifact and supply chain

- [ ] Artifact name, platform, media type, and immutable digest
- [ ] Artifact checksum
- [ ] Source commit-to-artifact binding
- [ ] SBOM format, checksum, and generation result
- [ ] Vulnerability scanner/version/policy and result
- [ ] Secret scanner/version/scope and redacted result
- [ ] Evidence-manifest checksum and verification result
- [ ] Registry reference and no-rebuild promotion record, when applicable
- [ ] Current and rollback artifact retention confirmed

## Database and migration

- [ ] Migration versions and checksums
- [ ] Forward-only and compatibility review
- [ ] Bootstrap identity and role/grant evidence
- [ ] Migration execution identity, image digest, start/end time, and result
- [ ] Schema version and ownership verification
- [ ] RLS, tenant-binding, and negative-privilege probes
- [ ] Forward-fix and restore decision points

## Deployment

- [ ] Canonical deployment-contract version and instance
- [ ] Deployment-adapter name and version/revision
- [ ] Exact artifact digest used by every process
- [ ] Process/profile and credential-class mapping
- [ ] Configuration inventory without values
- [ ] TLS and network-policy verification
- [ ] Deployment sequence and provider event identifiers
- [ ] API readiness and dispatcher process-state evidence
- [ ] Migration completed before runtime rollout
- [ ] Routing decision and acceptance timestamp

## Security and functional acceptance

- [ ] JWT issuer, audience, signature, time, and subject checks
- [ ] Same-tenant authorized access
- [ ] Cross-tenant and unauthorized-access denial
- [ ] RFC 9457, correlation, and safe-log checks
- [ ] Audit-chain recomputation
- [ ] Idempotency replay and mismatch behavior
- [ ] Inbox duplicate and payload-substitution behavior
- [ ] Outbox publish, retry, stale-claim recovery, and dead-letter behavior
- [ ] No credential or personal-data exposure in evidence

## Operations and recovery

- [ ] Liveness, readiness, startup, and durable-delivery signals
- [ ] Metrics access and bounded labels
- [ ] Alert route and test event
- [ ] Log route, retention, redaction, and access review
- [ ] Backup status and retention
- [ ] Restore drill and integrity result
- [ ] Rollback compatibility decision and rehearsal
- [ ] Forward-fix rehearsal
- [ ] RPO/RTO result or approved limitation
- [ ] Resource cost and cleanup status

## Package manifest

The evidence package must include a machine-readable manifest or an equivalent
checksummed index containing:

- evidence version and gate identifier;
- full commit and immutable artifact digest;
- source workflow/run identity;
- every included file path, media type, SHA-256 checksum, and sensitivity
  classification;
- generation timestamp;
- retention expiry; and
- references to approvals without embedding secrets.

Sanitized summaries may be retained longer than raw logs. If provider logs
cannot be exported safely, record their immutable provider event identifier,
retention period, access boundary, and reviewer.

## Acceptance record

- [ ] All blocking checklist items passed
- [ ] Every `not applicable` item has a reason
- [ ] Findings are resolved or explicitly dispositioned
- [ ] Residual risk owner and review date recorded
- [ ] Evidence package integrity verified
- [ ] Explicit gate acceptance recorded
- [ ] Exact next approval gate recorded

A successful CI or deployment event does not complete this checklist by
itself. Gate acceptance remains explicit under `CC-001`.
