# API Versioning and Error Contract

Status: Approved design standard
Last updated: 2026-07-25

## Versioning

- Application REST endpoints use a URI major version under `/api/v1`.
- Actuator endpoints are operational endpoints and are not part of the product
  REST version.
- Additive fields and endpoints may ship within `v1` when existing clients
  remain compatible.
- Removing, renaming, or changing the meaning/type of a field, status, or
  required input is breaking and requires a new major API version or an
  approved compatibility migration.
- Provider API versions and payload types remain inside infrastructure
  adapters.
- Deprecation requires documentation, telemetry-based usage evidence, a
  migration path, and an approved removal date.
- Mutable resources use optimistic concurrency through `ETag`/`If-Match` where
  appropriate.
- Sensitive commands require a validated `Idempotency-Key`.

## Success responses

- Return the narrowest appropriate HTTP status and representation.
- Collections are immutable in application responses.
- Identifiers in URLs are opaque and never contain email, phone, token, or
  other personal data.
- Large collections use cursor pagination and allow-listed filters/sorts.

## Error media type

Errors use RFC 9457 Problem Details:

`application/problem+json`

Required fields:

| Field | Contract |
|---|---|
| `type` | Stable MyChandha problem URI |
| `title` | Stable human-readable category, not exception text |
| `status` | HTTP status |
| `detail` | Safe generic explanation |
| `code` | Stable machine-readable MyChandha error code |
| `correlationId` | Safe validated correlation identifier |

`instance` is optional and must never contain secrets or personal data.

## Validation errors

Validation failures use one Problem Details response with a stable top-level
code such as `VALIDATION_FAILED` and an immutable `errors` collection.

Each field error contains only:

- `field`: an allow-listed API field name or safe JSON pointer;
- `code`: a stable validation code; and
- `message`: a safe client-facing explanation.

Do not echo rejected values, database constraint text, stack traces, class
names, SQL, tokens, or provider responses.

## Stable error categories

At minimum, future endpoints reuse consistent categories for:

- authentication required or invalid;
- organization context invalid;
- organization access denied;
- permission denied;
- validation failed;
- idempotency conflict;
- optimistic concurrency conflict;
- resource not found without account/tenant enumeration;
- unsupported media type or method; and
- safe internal error.

Authentication and authorization failures must not reveal whether a user,
organization, membership, or resource exists.

## Correlation and headers

- Every HTTP response includes `X-Correlation-Id`.
- A caller-supplied correlation ID is retained only after safe format and
  length validation.
- A separate server-generated request ID is available in structured logs and
  may be returned through an approved header.
- Security, caching, content-type, and concurrency headers are tested as part
  of the API contract.

## Contract tests

Automated tests must verify:

- all application controller routes are under `/api/v1`;
- Problem Details media type and required fields;
- stable error codes and correlation behavior;
- validation error shape and non-reflection of rejected values;
- no stack trace, SQL, provider payload, token, or personal data leakage;
- immutable error and success collections; and
- compatibility behavior for idempotency and optimistic concurrency.
