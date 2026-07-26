# Change control

Status: Normative
Rule identifier: `CC-001`
Effective for proposals prepared on or after: 2026-07-26

## CC-001 — Approval-gated change lifecycle

Every future phase, gate, major module, provider integration, security-boundary
change, schema change, deployment change, and external-resource change must
follow this rule.

### Required lifecycle

1. **Baseline**
   - Read the current status, product decisions, architecture, security,
     roadmap, and applicable prior evidence.
   - Inspect the relevant implementation, migrations, tests, working tree,
     branch, and dependency state.
2. **Proposal**
   - Define scope and explicit non-goals.
   - Describe architecture and data-flow impact.
   - Describe schema/migration and compatibility impact, including `none`.
   - Describe authorization, tenancy, secrets, privacy, audit, and supply-chain
     impact.
   - Describe test, evidence, deployment, rollback, recovery, cost, ownership,
     and cleanup impact.
   - Identify unresolved decisions and hard-stop conditions.
3. **Implementation approval**
   - Obtain explicit approval for the bounded repository change.
   - Proposal preparation or review is not implementation approval.
4. **Repository implementation**
   - Make only the approved cohesive change.
   - Preserve unrelated work and update tests and authoritative documents in
     the same change.
   - Stop and return to proposal review if a material scope, provider, security,
     migration, cost, or operational assumption changes.
5. **Local verification**
   - Run the approved test and validation plan.
   - Record failures and corrections without weakening gates.
6. **GitHub change approval**
   - Commit, push, pull-request creation, merge, package publication,
     protected-environment configuration, and workflow execution require the
     approval applicable to those actions.
   - Repository implementation approval does not silently authorize them.
7. **CI and evidence acceptance**
   - Produce the standardized evidence package in
     `docs/evidence-package.md`.
   - A green workflow is evidence, not automatic gate acceptance.
   - Record explicit acceptance or rejection and remaining risks.
8. **External-resource approval**
   - Before provisioning or modifying an external system, approve the exact
     provider resources, identifiers, region, plan, cost, owner, operators,
     secrets, network policy, retention, backup, alerting, and cleanup.
9. **Execution approval**
   - Deployment, migration, data movement, provider-side configuration, and
     staging or production acceptance require explicit execution approval.
10. **Closure**
    - Close the gate only after required evidence is accepted and blocking
      risks are resolved or explicitly dispositioned.
    - Record the exact next gate. Do not roll approval into the next phase.

### Approval separation

Unless an approval explicitly combines named actions, these approvals are
independent:

| Approval | Does not implicitly authorize |
|---|---|
| Proposal preparation | Implementation, GitHub changes, or external changes |
| Repository implementation | Commit, push, pull request, merge, publication, or deployment |
| Commit/push/pull request | Merge, workflow execution, package publication, or deployment |
| CI evidence acceptance | External-resource creation or deployment |
| External-resource design | Provisioning or execution |
| Staging execution | Production execution or the next product phase |
| Gate closure | The next gate or phase |

### Material-change rule

A change is material when it affects any approved:

- product or provider decision;
- domain, module, or deployment boundary;
- organization isolation or authorization rule;
- database schema, ownership, RLS, or migration sequence;
- secret class, trust boundary, network path, or data exposure;
- external resource, plan, cost, owner, retention, or recovery target;
- API compatibility or public behavior;
- evidence threshold, scanner policy, test requirement, or rollback approach.

When a material change is discovered, implementation must pause. Amend the
proposal, state the impact, and obtain new explicit approval for the changed
scope. A test failure, provider limitation, or implementation convenience is
not permission to narrow a control.

### Required gate reference

Every future gate proposal and closure record must contain a section named
`CC-001 compliance` with:

- current lifecycle step;
- approvals granted and not granted;
- repository and external boundaries;
- material deviations, or `none`;
- evidence-package location and status; and
- next approval required.

Roadmap entries may reference this rule once for a sequence of subordinate
gates, but each detailed gate document must reference it directly.

### Exceptions

There is no implicit emergency or convenience exception. An exception requires
an explicit, recorded approval that states:

- the exact rule being varied;
- reason and time limit;
- security, tenancy, data, and recovery impact;
- compensating controls;
- accountable owner; and
- evidence and removal date.

Credentials, customer data, destructive history rewrites, and silent weakening
of security or evidence gates remain prohibited.
