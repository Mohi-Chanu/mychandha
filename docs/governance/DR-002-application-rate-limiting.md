# DR-002 — Application rate limiting

Status: Accepted for Phase 1 Gate D
Prepared: 2026-07-26
Accepted: 2026-07-26
Governing controls: `CC-001`, `MCDC-001`, `EP-001`

## Decision

Public staging must have a documented application-layer throttling strategy
before routing is enabled.

Provider DDoS or network-flood protection alone is not sufficient for
business-level throttling of targeted Layer 7 traffic. The strategy must cover
the security contract's IP, authenticated subject, organization, and endpoint
classes without user enumeration or unbounded state.

## Phase 1 Gate D criteria

- No public staging traffic before the strategy is approved, implemented, and
  verified.
- Rate-limit rejection uses RFC 9457 Problem Details and HTTP `429`.
- Trusted proxy headers are parsed only through an approved boundary.
- State and metrics have bounded cardinality and memory use.
- The single-instance staging design must not claim multi-instance or
  production-scale correctness.
- Redis or another distributed store is not required for the one-instance
  staging design.

## Evolution rule

Implementation technology may evolve through a later `CC-001` decision when
scale or topology changes. The application-layer security requirement itself
cannot be deferred or treated as satisfied by provider edge protection alone.
