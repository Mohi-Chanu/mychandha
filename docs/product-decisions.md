# Product decision register

This file records approved decisions that must survive the move from ChatGPT
development to repository-based Codex work. It is a concise engineering
companion to the full enterprise product requirements, not a replacement for
them.

## Product posture and boundaries

| Area | Approved decision |
|---|---|
| Release posture | Build the enterprise GA baseline; do not reduce it to an MVP, demo, or proof of concept. |
| Platform role | MyChandha is multi-tenant SaaS and a payment-orchestration layer. It does not custody organizer funds. |
| Financial disputes | MyChandha records disputes and their evidence; it does not decide the underlying financial dispute. |
| Platform fees | No hidden platform transaction fees and no donor convenience fee. |
| URLs | GA uses platform-managed URLs. Custom domains are deferred. |
| Localization | GA languages are English, Telugu, and Hindi. |
| Tenant identity | Organization IDs are immutable. Cross-tenant access is allowed only through explicit, audited administrative workflows. |

## Providers and integration boundaries

| Capability | Approved decision |
|---|---|
| Application stack | Java 21, Spring Boot 3.x, Maven, PostgreSQL, Flyway, and OCI containers. |
| Identity | Managed OIDC with a pluggable identity boundary; Supabase Auth is the initial provider. MyChandha owns additional OTP/risk controls and step-up policy. |
| Deployment | Cloud-neutral OCI image with Render as the initial deployment adapter. |
| Payments | Razorpay Route / Connected Accounts, with direct settlement to committee bank accounts. |
| Refunds | Season 1 refunds are handled manually in Razorpay and recorded by MyChandha. |
| Email | Resend. |
| SMS | MSG91. |
| WhatsApp | Meta WhatsApp Business API through Interakt as the selected initial provider. Keep the application boundary replaceable. |
| Messaging backbone | PostgreSQL transactional outbox/inbox initially. Kafka is deferred until workload evidence justifies it. |
| Media | Layered moderation rather than relying on a single automated or manual control. |

Provider-specific types and payloads must remain in infrastructure adapters.
Changing a provider or the financial flow is a decision requiring approval,
migration analysis, failure-mode analysis, and test updates.

## Compliance and trust

- Verify 80G, FCRA, and GST eligibility manually before displaying or using the
  corresponding status.
- Foreign donations remain disabled unless the receiving organization has
  verified FCRA eligibility and the approved payment flow supports it.
- Published events require a verified organization, verified settlement
  details, required public disclosures, an abuse-report route, and content
  review.
- Guest claims use OTP, with step-up verification for sensitive claims.
- Cash contributions require dual counting and bank-deposit controls before
  reconciliation.
- Event verification levels and badges are derived only from verified evidence;
  they are never self-asserted by an organizer.

## Core lifecycle decisions

The organization and event flow is:

1. Organization onboarding.
2. Organization verification and membership assignment.
3. Event edition creation.
4. Draft and review.
5. Published.
6. Live.
7. Completed.
8. Archived.

Transitions must be explicit, authorized, validated, and audited. Publication
uses an immutable publication snapshot so later edits cannot silently change
what donors saw.

## Security and data invariants

- Tenant isolation uses both application authorization and PostgreSQL RLS.
- Identity-provider subject is the stable external identity key. Email and phone
  are contact attributes, not authorization keys.
- Secrets and authentication artifacts never enter source control, logs, audit
  payloads, or URLs.
- Audit evidence is append-only, hash-linked per organization, and
  reproducible from the stored canonical representation.
- Sensitive commands are idempotent; external events use inbox deduplication
  and payload-substitution detection.
- Money is represented in integer minor units and an ISO currency.
- Public and administrative actions preserve correlation and audit evidence
  without exposing personal data.

## Change process

Add a dated entry below when an approved decision changes. Record the reason,
approval, compatibility/migration impact, and affected tests. Do not erase the
previous decision.

### Decision history

- 2026-07-23: Phase 1 platform-foundation architecture and provider boundaries
  approved.
- 2026-07-24: Phase 1 CI acceptance completed without weakening quality or
  vulnerability gates.
