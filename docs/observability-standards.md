# Observability Standards

Status: Approved design standard
Last updated: 2026-07-25

## Principles

- Observability must not weaken privacy, tenant isolation, or authentication.
- Logs, metrics, and traces are separate from append-only audit evidence.
- Do not place tokens, contact data, external identity subjects, request
  bodies, event payloads, or tenant-controlled labels in telemetry.
- Avoid user-, organization-, event-, and correlation-level metric labels.
- A backend/exporter is an infrastructure decision and requires approval.

## Health categories

### Liveness

Answers whether the process can continue making progress. It must not fail
solely because an external dependency is temporarily unavailable.

API evidence: `/actuator/health/liveness`.

Dispatcher evidence: process/scheduler heartbeat and absence of a fatal loop
failure.

### Readiness

Answers whether the process should receive or perform work.

API readiness includes required configuration and database connectivity under
the API role. Dispatcher readiness includes database connectivity under the
dispatcher role and successful access to its controlled routines.

API evidence: `/actuator/health/readiness`.

### Startup

Answers whether initialization completed within the allowed startup budget.
The API and dispatcher profiles must expose or emit a deterministic startup
signal after configuration validation and dependency initialization. The
migration profile succeeds only after Flyway completes and the process exits
with code zero.

Startup checks must tolerate normal initialization without masking a permanent
configuration or migration failure.

## Structured logging

Logs use stable structured fields where applicable:

| Field | Rule |
|---|---|
| `timestamp` | UTC, machine-readable |
| `level` | Stable severity |
| `service` | Application/runtime identity |
| `runtimeProfile` | `api`, `dispatcher`, or `migration` |
| `traceId` / `spanId` | Present when tracing is active |
| `requestId` | Server-generated unique HTTP request identifier |
| `correlationId` | Safe workflow identifier; caller value accepted only after validation |
| `organizationId` | Present only after authorized tenant context exists |
| `actorReference` | Optional opaque internal reference only when operationally necessary |
| `eventId` / `eventType` | Safe durable-delivery identifiers |
| `errorCode` | Stable safe code; never raw exception detail |

Do not log raw `userId` merely because it is available. Never log provider
subject, email, phone, access/refresh token, secret, request body, outbox/inbox
payload, stack trace in API responses, or unvalidated correlation data.

`requestId` identifies one HTTP request. `correlationId` may connect multiple
requests and asynchronous operations. They must not be conflated.

## Metrics

Required metric groups:

- HTTP request count, latency, status class, and saturation using bounded
  route templates rather than raw paths;
- database pool utilization and acquisition latency;
- outbox pending/processing depth and oldest age;
- outbox published, retry, failure, stale-claim recovery, and dead-letter
  counts;
- dispatcher polling and delivery duration; and
- JVM/process resource and startup measurements.

Existing `mychandha.outbox.published` and `mychandha.outbox.failed` metrics
remain compatible or receive a documented migration. New names use the
`mychandha.*` namespace and bounded tags.

Do not use organization ID, user/actor ID, event ID, correlation ID, raw URI,
exception message, or provider payload value as a metric tag.

## Tracing

Phase 1 must be OpenTelemetry-compatible without selecting an exporter or
backend in this gate.

Design requirements:

- accept and propagate standard W3C trace context at trusted HTTP boundaries;
- continue trace context across outbox publication and dispatch using safe
  trace metadata, never credentials or personal data;
- create spans for HTTP handling, authorized tenant binding, database
  operations, and dispatcher delivery where instrumentation is later enabled;
- do not put organization, user, token, payload, or contact data in baggage;
- sample and export only through an approved policy; and
- preserve correlation IDs independently of trace sampling.

Adding an OpenTelemetry SDK, agent, collector, exporter, or hosted backend
requires a later dependency/security/cost decision. Compatibility design does
not itself add infrastructure.

## Alert intent

Staging must prove alert paths for:

- API readiness failure;
- dispatcher process failure;
- outbox oldest age above five minutes;
- dead-letter growth;
- repeated authentication or authorization anomalies without user
  enumeration;
- database saturation; and
- backup/restore failure.

Exact thresholds, destinations, retention, and on-call owners belong to the
external-resource proposal.
