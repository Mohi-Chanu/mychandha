# Governance decision index

Status: Repository navigation authority
Last updated: 2026-07-29

## Purpose

This index gives stable identifiers to MyChandha architecture, deployment,
evidence, change-control, operational-budget, and gate decision records.
Gate documents should reference these identifiers instead of restating the
full policy.

Existing canonical documents remain in their established locations to preserve
history and links. This index does not duplicate or supersede their content.

## Architecture decision records

| ID | Decision | Status | Canonical record |
|---|---|---|---|
| `ADR-001` | Runtime profile separation | Accepted | [`docs/adr/ADR-001-runtime-profiles.md`](../adr/ADR-001-runtime-profiles.md) |
| `ADR-002` | Database role separation | Accepted | [`docs/adr/ADR-002-database-roles.md`](../adr/ADR-002-database-roles.md) |
| `ADR-003` | Durable dispatcher model | Accepted | [`docs/adr/ADR-003-dispatcher-model.md`](../adr/ADR-003-dispatcher-model.md) |
| `ADR-004` | Immutable no-rebuild release | Accepted | [`docs/adr/ADR-004-immutable-release.md`](../adr/ADR-004-immutable-release.md) |
| `ADR-005` | Forward-only Flyway strategy | Accepted | [`docs/adr/ADR-005-flyway-strategy.md`](../adr/ADR-005-flyway-strategy.md) |

## Gate decision records

| ID | Decision | Status | Canonical record |
|---|---|---|---|
| `DR-001` | Migration environment isolation | Accepted for Gate D | [`DR-001-migration-environment-isolation.md`](DR-001-migration-environment-isolation.md) |
| `DR-002` | Application rate limiting | Accepted for Gate D | [`DR-002-application-rate-limiting.md`](DR-002-application-rate-limiting.md) |
| `DR-003` | OCI rollback policy | Accepted for Gate D | [`DR-003-oci-rollback-policy.md`](DR-003-oci-rollback-policy.md) |
| `PF-R-001` | Render client-address adapter | Accepted for Gate D | [`docs/phase-1-gate-d-provider-conformance-remediation-proposal.md`](../phase-1-gate-d-provider-conformance-remediation-proposal.md) |
| `PF-R-002` | Supabase CA materialization | Accepted for Gate D | [`docs/phase-1-gate-d-provider-conformance-remediation-proposal.md`](../phase-1-gate-d-provider-conformance-remediation-proposal.md) |
| `PF-R-003` | Render staging ownership and budget | Accepted for Gate D | [`docs/phase-1-gate-d-provider-conformance-remediation-proposal.md`](../phase-1-gate-d-provider-conformance-remediation-proposal.md) |
| `PF-R-004` | Minimum staging alert and bounded-check contract | Accepted for Gate D | [`docs/phase-1-gate-d-provider-conformance-remediation-proposal.md`](../phase-1-gate-d-provider-conformance-remediation-proposal.md) |

## Time-bounded exceptions

| ID | Exception | Status | Canonical record |
|---|---|---|---|
| `PF-EX-001` | Render control-plane backup limitation | Accepted for synthetic Phase 1 staging; expires no later than 2026-09-30 | [`PF-EX-001-render-control-plane-operator.md`](PF-EX-001-render-control-plane-operator.md) |
| `PF-EX-002` | Staging alert-delivery limitation | Accepted for bounded synthetic Phase 1 staging; expires no later than 2026-09-30 | [`PF-EX-002-staging-alert-delivery.md`](PF-EX-002-staging-alert-delivery.md) |

## Normative controls

| ID | Control | Status | Canonical record |
|---|---|---|---|
| `CC-001` | Approval-gated change lifecycle | Normative | [`docs/change-control.md`](../change-control.md) |
| `MCDC-001` | Provider-neutral deployment contract | Normative version 1 | [`docs/deployment-contract.md`](../deployment-contract.md) |
| `EP-001` | Standard evidence package | Normative | [`docs/evidence-package.md`](../evidence-package.md) |

## Governed operational budgets

| ID | Budget | Status | Canonical record |
|---|---|---|---|
| `R1-OPB-001` | Release 1 approved operational budget | Accepted for Gate D | [`R1-OPB-001-release-1-operational-budget.md`](R1-OPB-001-release-1-operational-budget.md) |

## Maintenance rule

- Identifiers are permanent and must not be reused.
- A superseding decision links to the prior record and retains its history.
- A material decision change follows `CC-001`.
- Gate documents reference the identifier and record only gate-specific
  evidence or exceptions.
- Moving an existing canonical record requires a separately reviewed link and
  authority migration; an index update alone does not move authority.
