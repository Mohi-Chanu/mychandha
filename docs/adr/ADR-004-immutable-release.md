# ADR-004: Immutable Release Identity

Status: Accepted
Date: 2026-07-25

## Context

The current CI builds and scans an OCI image but does not publish the exact
verified image for deployment. Rebuilding from the same commit can produce a
different image and breaks direct provenance between verification and staging.

## Decision

The deployable release identity is an OCI digest, not a mutable tag.

The normal verification workflow:

- uses immutable action and container inputs;
- runs tests, static analysis, secret scanning, SBOM generation, and the
  configured vulnerability gate;
- exports the verified image as a retained OCI artifact; and
- records the commit, image digest, SBOM digest, and scan evidence.

A separate protected release workflow promotes that exact retained image to an
approved registry. It must not rebuild the image or deploy by mutable tag
alone.

GitHub Container Registry is the recommended adapter because GitHub already
hosts the repository and CI. Creating or using the registry package, enabling
package-write permissions, and publishing remain separate external actions.

## Consequences

- Staging evidence identifies the exact bytes tested by CI.
- Artifact retention, storage cost, access, and cleanup must be approved.
- Normal pull-request verification remains read-only.
- Release workflow permissions and environment protection require review.
- Release signing remains a later production trust-model decision.

## Alternatives rejected

- Render source rebuild as release evidence: rejected because it does not
  deploy the exact verified image.
- Mutable tags such as `latest`: rejected as a release identity.
- Giving ordinary verification jobs package-write permission: rejected because
  it unnecessarily expands CI authority.
