# ADR-003: Durable Dispatcher Model and Terminology

Status: Accepted
Date: 2026-07-25

## Context

Phase 1 implements a PostgreSQL transactional outbox/inbox and an in-process
component that claims outbox rows, delivers events, retries failures, reclaims
stale claims, and dead-letters exhausted work.

Terms such as `worker` or `event-worker` may be more familiar, but they can
imply a general-purpose job platform that is not part of the approved scope.

## Decision

Retain `dispatcher` as the canonical term in code and documentation.

The dispatcher:

- handles only the durable outbox responsibility;
- runs through the `dispatcher` runtime profile;
- uses the approved dispatcher database routines;
- invokes versioned, idempotent consumers; and
- does not become a general scheduler, cache, queue, or unrelated job system.

## Consequences

- Existing terminology remains consistent.
- Operational resources may use a descriptive name such as
  `mychandha-outbox-dispatcher`.
- Future background workloads require their own evidence and approval instead
  of being silently attached to the dispatcher.

## Alternatives rejected

- Rename everything to `worker`: rejected because it broadens the perceived
  responsibility and creates unnecessary churn.
- Create a generic worker platform now: rejected because no approved use case
  requires it.
