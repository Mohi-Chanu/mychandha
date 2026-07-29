# PF-EX-001 — Render control-plane backup limitation

Status: Accepted for synthetic Phase 1 staging only
Accepted: 2026-07-29
Expires: Cleanup or 2026-09-30, whichever occurs first
Owner: `Mohi-Chanu`

## Exception

Render Hobby permits one workspace member and Render Pro would exceed
`R1-OPB-001`. `Mohi-Chanu` is therefore the sole Render workspace member.
`hazwaTech` is the GitHub protected-environment reviewer and may initiate or
review approved GitHub-mediated operations but cannot independently perform
Render dashboard recovery.

## Compensating controls

- Synthetic Phase 1 staging data only; no production use.
- The owner is available for every separately approved execution window.
- One operator initiates and the other reviews each GitHub-mediated operation;
  self-approval is prohibited.
- No account, personal API key, credential, or browser session is shared.
- Protected GitHub environment credentials remain purpose-scoped.
- `EP-001` records role verification, owner coverage, incidents, and removal.

## Termination

The exception terminates immediately on owner unavailability, customer data,
production traffic, a requirement for unattended operation, a second direct
control-plane operator requirement, or expiry. Closure evidence records
resource deletion and credential revocation.

Changing this boundary requires `CC-001`. Acceptance does not authorize
GitHub/provider configuration, provisioning, spending, or execution.
