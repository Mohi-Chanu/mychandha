# ADR-001: Runtime Profiles

Status: Accepted
Date: 2026-07-25

## Context

The Phase 1 application currently runs the servlet API, Flyway, and durable
outbox dispatcher from one Spring Boot process and datasource credential. That
prevents least-privilege runtime credentials and controlled migration
execution.

The product remains a modular monolith. Runtime isolation must not create new
domain services or introduce a generic worker platform.

## Decision

One OCI artifact supports three explicit execution profiles:

- `api`: servlet API and Actuator; Flyway and scheduling disabled;
- `dispatcher`: non-web durable outbox dispatcher; Flyway disabled; and
- `migration`: non-web, one-off Flyway execution that exits after completion.

Staging and production use separate processes and credentials for these
profiles. A combined mode may exist only for documented local development and
must fail under the production profile.

## Consequences

- API compromise does not automatically grant migration or dispatcher
  database permissions.
- API and dispatcher can be started, stopped, and scaled independently.
- Deployment requires profile-specific commands and credentials.
- Profile exclusivity and startup behavior require automated tests.
- The dispatcher profile is an existing Phase 1 responsibility, not a generic
  worker framework.

## Alternatives rejected

- One combined production process: rejected because it cannot enforce the
  approved credential boundary.
- Separate codebases or microservices: rejected because the workload does not
  justify breaking the modular monolith.
- Adding Redis, Kafka, or another runtime dependency: rejected because the
  existing PostgreSQL outbox/inbox is the approved backbone.
