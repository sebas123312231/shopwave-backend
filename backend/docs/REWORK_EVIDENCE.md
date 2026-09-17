# ShopWave backend rework evidence

## Implemented contract

The active Spring component scan is limited to `com.shopwavefusion.rework`. The single API contract is [`../openapi/shopwave-v1.yaml`](../openapi/shopwave-v1.yaml). Legacy controllers and models are no longer part of the source tree.

Implemented areas:

- MySQL/Flyway schema with UUIDs, cents in BOB, variants, carts, snapshots and optimistic versions.
- JWT Bearer authentication with issuer/audience checks, token-version revocation, explicit roles and JSON 401/403 responses.
- Catalog pagination/filtering/facets and admin versioned product changes.
- Locked cart mutations, server-side checkout totals, stock checks, snapshot orders and idempotent retries.
- MOCK/SIMULATED payment only; no card fields or provider integration.
- Demo seed/admin are disabled by default and require explicit environment flags.

## Verification boundary

The local tests use H2 for fast isolated feedback. `./mvnw.cmd test` passed with 10 tests, and `./mvnw.cmd verify -Pintegration` passed with the 10 Surefire tests plus 9 Failsafe integration tests. H2 does not prove MySQL locking or Flyway equivalence. A Docker/Testcontainers run against a new disposable MySQL database must be recorded separately. No remote or legacy database is part of this rework.

A local HTTP smoke test also passed with H2 and demo seed data: catalog, authentication, BFF session, profile, cart, MOCK checkout, order retrieval, USER/ADMIN authorization and the admin page were exercised without connecting to a real or remote database. Browser Playwright execution remains unverified because the local browser binaries are not installed.

Do not publish a default admin credential, a real JWT secret, personal addresses, payment data or a hosted URL without separate evidence and authorization.
