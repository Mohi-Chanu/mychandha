# PF-EX-002 — Staging alert-delivery limitation

Status: Accepted for bounded synthetic Phase 1 staging only
Accepted: 2026-07-29
Expires: Cleanup or 2026-09-30, whichever occurs first
Owner: `Mohi-Chanu`
Evidence reviewer: `hazwaTech`

## Exception

The selected native plans do not continuously deliver every application,
dispatcher, database, backup, and restore signal to two operators. Gate D uses
Render-native owner notifications only where Render documents them and uses
bounded pre/during/post acceptance checks for the remaining signals. No
monitoring or notification provider is added.

## Compensating controls

- Synthetic Phase 1 staging data only; no production use.
- Render-native owner notification tests cover documented deploy, image-pull,
  one-off-job, and API health events.
- Bounded checks cover dispatcher recovery, outbox age/dead letters,
  authentication/authorization anomalies, Supabase capacity, backups, and
  restore.
- `hazwaTech` reviews the sanitized `EP-001` record.
- A missing route, bounded check, review, or 24-hour log review fails Gate D.
- Evidence excludes tokens, addresses, contact data, certificate contents,
  connection strings, and copied personal data.

## Termination

The exception terminates immediately on owner unavailability, customer data,
production traffic, need for continuous dual-operator paging, unattended
operation, or expiry. Production requires a separately approved monitoring and
incident-response design.

Changing this boundary requires `CC-001`. Acceptance does not authorize an
external integration, provisioning, spending, or execution.
