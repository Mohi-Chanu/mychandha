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
- 2026-07-25: Preparation of the Phase 1 Platform Foundation Readiness package
  approved as documentation-only work. Reference examples do not approve new
  providers or infrastructure. Add only capabilities necessary for the
  application context; Redis, generic workers, and other examples remain out of
  scope unless explicitly and separately approved. Staging acceptance remains
  a mandatory execution stage before Phase 1 closure.
- 2026-07-25: The readiness package's open design decisions were reviewed and
  approved. This approves the control directions, not repository
  implementation or external resources. Exact resource names, regions, plans,
  costs, owners, credentials, and provider-side changes remain subject to later
  explicit approval.
- 2026-07-25: The repository-change proposal review requested ADRs, explicit
  module/dependency boundaries, observability standards, API versioning,
  unified RFC 9457 errors, and developer-experience guidance. These are
  documentation enhancements, not implementation approval. The established
  `dispatcher` term is retained; empty future modules, feature-flag
  infrastructure, and an OpenTelemetry backend remain deferred until an
  approved use case requires them.
- 2026-07-25: Gate A repository implementation was approved. The approval is
  limited to runtime/profile separation, V2 database-role and dispatcher
  routines, API/error and observability contracts, module-boundary evidence,
  tests, and local developer experience. It does not approve Gate B CI/release
  changes, Gate C deployment configuration, external resources, or Phase 2.
- 2026-07-25: Gate A CI evidence was accepted after PR `#1` merged and the
  post-merge `main` workflow passed. Gate B repository implementation was
  separately approved. The approval covers immutable CI inputs, blocking
  full-history Gitleaks, retained OCI/SBOM/scan evidence, and a protected
  no-rebuild promotion workflow. It does not authorize a push, GitHub package
  or environment configuration, image publication, Gate C, deployment,
  external resources, or Phase 2.
- 2026-07-25: Committing and pushing Gate B to
  `codex/phase-1-gate-b` and opening draft PR `#2` were separately approved.
  This does not authorize merge, release-workflow execution, GitHub package or
  environment configuration, image publication, Gate C, deployment, external
  resources, or Phase 2.
- 2026-07-26: Gate B evidence was explicitly accepted after PR `#2` merged as
  `e34239f34056ea1b6bf5769e5e7920a8ceedf053` and post-merge `main` CI run
  `30166358486`, job `89699959544`, passed the complete Java/PostgreSQL,
  static-analysis, full-history Gitleaks, retained OCI, CycloneDX, Trivy, and
  evidence-verification gates. This accepts the repository implementation and
  CI evidence only. It does not authorize release-workflow execution, package
  or protected-environment creation, image publication, Gate C implementation,
  deployment, external resources, or Phase 2.
- 2026-07-26: Preparation of the Gate C deployment-adapter proposal was
  approved. The proposal may define only the repository mapping and later
  operational contract for the existing API, durable dispatcher, and migration
  profiles. It does not approve implementation, a generic worker platform,
  Redis/Key Value, Render Workflows, external resources, image publication,
  deployment, or Phase 2.
- 2026-07-26: Gate C proposal review required a canonical provider-neutral
  deployment contract, an explicit deployment-adapter abstraction, an
  environment capability matrix, Gate C non-goals, standardized evidence, and
  reusable change control. These are recorded as `MCDC-001`, `EP-001`, and
  `CC-001`. Every future gate must follow `CC-001`; deployment providers remain
  adapters and do not become domain dependencies. This refinement does not
  grant Gate C implementation or external-change approval.
- 2026-07-26: Gate C repository implementation was explicitly approved
  according to the refined proposal. The approval covers removal of the unsafe
  root Blueprint, a non-live Render adapter example, provider conformance
  runbook, structural validation, tamper-rejection fixtures, `EP-001` local
  evidence, and documentation alignment. It does not authorize commit, push,
  pull request, merge, workflow execution, package/image publication,
  external-resource changes, migration, deployment, staging acceptance, or
  Phase 2.
- 2026-07-26: Committing and pushing Gate C to
  `codex/phase-1-gate-c` and opening draft PR `#4` were separately approved.
  The user subsequently merged PR `#4` after pull-request CI run
  `30203978892` passed, confirmed post-merge `main` run `30204430920` was
  green, and directed preparation of the next evidence step. This authorizes
  the evidence-record preparation only. Explicit Gate C evidence acceptance,
  evidence-record publication, release execution, package/image publication,
  external-resource changes, migration, deployment, staging acceptance,
  Phase 1 closure, and Phase 2 remain separate.
- 2026-07-26: The Gate C CI evidence was explicitly accepted after PR `#4`
  merged as `818c9b2d1d991bed67c51b6f3a9978998ab8c7b2` and post-merge
  `main` run `30204430920`, job `89799925991`, passed. Committing and pushing
  `codex/phase-1-gate-c-evidence` and opening a draft evidence pull request
  were approved in the same instruction. Evidence-record merge, release
  execution, image publication, external resources, migration, deployment,
  staging acceptance, Phase 1 closure, and Phase 2 remain separate.
- 2026-07-26: The user merged Gate C evidence PR `#5` and confirmed both its
  pull-request CI and resulting `main` CI were successful. The merge commit is
  `6cf89fa62464c9e2f16ca1df29a47748edebf6eb`; post-merge run
  `30207094828`, job `89806953729`, passed. The user directed progression to
  the next step, which authorizes preparation of the Gate D staging-resource
  proposal only.
- 2026-07-26: Gate D proposal preparation retains one Supabase project for
  Auth and PostgreSQL, the required API and durable dispatcher executions,
  GHCR, and the Render adapter. Redis, Kafka, Render Key Value/Workflows,
  generic workers, an additional database, and third-party observability are
  not proposed. All Gate D decisions, repository prerequisites, publication,
  GitHub/provider changes, migration, deployment, and acceptance remain
  unapproved.
- 2026-07-26: Gate D proposal review requested permanent decision IDs,
  categorized hard stops, a release-scoped governed operational budget,
  positive exit criteria, an assumption register, explicit application-layer
  rate-limit criteria, and a central governance index. These are editorial
  governance refinements and do not approve the Gate D decisions,
  implementation, external changes, spending, migration, deployment, or
  staging execution.
- 2026-07-26: The Gate D staging-resource proposal decisions were explicitly
  accepted as documented. This accepts `DR-001` migration environment
  isolation, `DR-002` application-layer rate limiting, `DR-003` digest-only
  rollback, `R1-OPB-001`, the minimal Singapore staging resource envelope,
  ownership, retention, and cleanup boundaries. It authorizes preparation of
  the bounded Gate D repository implementation proposal only. Repository
  implementation, commit, push, pull request, GitHub/provider changes,
  spending, publication, credential creation, migration, deployment,
  acceptance, cleanup, Phase 1 closure, and Phase 2 remain separately gated.
- 2026-07-27: The bounded Phase 1 Gate D repository proposal was explicitly
  approved for local implementation. The approval covers only the source,
  tests, workflow contract, non-live Render adapters, operational scripts, and
  documentation listed in that proposal. It does not authorize a commit,
  push, pull request, merge, GitHub environment or package change, credential,
  provider resource, spending, bootstrap, migration, deployment, acceptance
  run, restore, rollback, cleanup, Phase 1 closure, or Phase 2 work.
- 2026-07-27: Gate D repository implementation PR `#6` merged as
  `cc75d0c3c59dc9d11ef748bff2f3633770854ffd`, and its post-merge repository
  CI evidence was explicitly accepted. Evidence PR `#7` subsequently merged as
  `afbb2109c6eb442baf01331296cdf3e0be294503`, and resulting `main` CI passed.
  These approvals and results do not authorize image publication, GitHub
  environment/package configuration, provider resources, spending, migration,
  deployment, or staging execution.
- 2026-07-27: Preparation of the Gate D external-resource preflight package
  was approved using read-only provider verification only. The preflight
  retains the accepted minimal resource envelope and records current
  capability, price, owner/operator, budget, OCI, security, recovery,
  retention, alert, and cleanup findings. Preparing this package does not
  approve a repository remediation, GitHub change, publication, provider
  configuration, resource creation, spending, migration, deployment,
  workflow execution, acceptance, or cleanup.
- 2026-07-29: Preparation of the bounded Gate D provider-conformance
  remediation proposal was explicitly approved for `PF-HS-001` through
  `PF-HS-004` only. The prepared recommendations address the Render
  client-address adapter, Supabase CA materialization, the Render Hobby
  owner/backup limitation, and the minimum staging alert/check contract.
  Preparation records recommendations only; no resolution decision,
  implementation, commit, push, publication, provider/GitHub change,
  provisioning, spending, workflow, migration, deployment, acceptance,
  cleanup, Phase 1 closure, or Phase 2 work is approved.
- 2026-07-29: The user explicitly accepted `PF-R-001` through `PF-R-004`
  and the staging-only `PF-EX-001`/`PF-EX-002` exceptions exactly as
  documented. The exceptions apply only to synthetic Phase 1 staging, expire
  at cleanup or 2026-09-30, and never apply to production. This decision
  acceptance does not authorize repository implementation, commit, push, pull
  request, merge, workflow execution, publication, GitHub/provider changes,
  provisioning, spending, migration, deployment, acceptance execution,
  cleanup, Phase 1 closure, or Phase 2.
- 2026-07-29: The user explicitly approved implementation of only the bounded
  repository scope documented in
  `docs/phase-1-gate-d-provider-conformance-remediation-proposal.md`. This
  authorizes local source, configuration, validation, evidence-contract, and
  documentation changes for `PF-HS-001` through `PF-HS-004`. It does not
  authorize a commit, push, pull request, merge, workflow execution,
  publication, GitHub/provider configuration, provisioning, spending,
  migration, deployment, live acceptance, restore, rollback, cleanup, Phase 1
  closure, or Phase 2.
- 2026-07-29: The bounded provider-conformance repository implementation was
  completed and locally validated. Java 21 offline Maven verification passed
  80 tests with PostgreSQL 17/Testcontainers and all configured analysis.
  Repository structural, adapter, workflow, shell, evidence, Actionlint,
  Gitleaks, Markdown-link, and whitespace checks passed. The previously
  accepted local Avast/Docker TLS limitation prevented the OCI build, SBOM,
  and Trivy steps without weakening certificate verification; authoritative
  CI remains mandatory. This local result grants no GitHub or external action.
- 2026-07-29: Provider-conformance implementation commit
  `94dd85d45da3aae655c468c9d2afbbe25908fec3` passed PR `#8` CI and merged as
  `87d0b7bc0891f29482ad9c46856e9a3e4b7a22ad`. The user explicitly accepted
  PR run `30440420020`, job `90538148786`, post-merge `main` run
  `30441738217`, job `90542418893`, and remediation-containing OCI digest
  `sha256:7b97937aed5a44b4ad7a177ebeadf8f631740b7051949c7d2e56f17cfbdf0829`.
  Preparation of the local evidence-only documentation update was also
  approved. This acceptance does not authorize a commit or push of the
  evidence record, workflow execution, package/image publication,
  GitHub/provider configuration, provisioning, spending, migration,
  deployment, live acceptance, restore, rollback, cleanup, Phase 1 closure,
  or Phase 2.
