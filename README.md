# ShopWave Backend — Secure Commerce API v1

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.1.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-stateless-6DB33F?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Flyway](https://img.shields.io/badge/Flyway-schema%20migrations-CC0200)](https://documentation.red-gate.com/flyway)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0.3-6BA539?logo=openapiinitiative&logoColor=white)](https://www.openapis.org/)
[![Docker](https://img.shields.io/badge/Docker-local%20runtime-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

ShopWave Backend is the Spring Boot API powering the ShopWave e-commerce system. The current implementation exposes a versioned REST contract for authentication, catalog discovery, variant inventory, carts, simulated checkout, orders, profiles, and admin operations, backed by a new MySQL/Flyway persistence model and explicit security and concurrency boundaries.

> This repository documents the backend of a collaborative ShopWave project. It is maintained as a portfolio-oriented rework showing the current API contract, domain model, persistence strategy, security decisions, and verification boundaries.

The codebase began in a university Web Technologies 2 context, but the current repository is presented primarily as a full-stack engineering project. The rework removed the legacy application surface and rebuilt the active implementation under com.shopwavefusion.rework instead of continuing to patch incompatible controllers, entities, and response contracts.

## About the Project

The API is consumed by the [ShopWave Next.js frontend](https://github.com/sebas123312231/shopwave-frontend) through a same-origin BFF. Authorized API clients may also use the documented Bearer contract directly.

~~~text
ShopWave Frontend
  browser -> Next.js BFF -> Bearer request
                              │
                              ▼
                    Spring Boot API v1
                      controllers / DTOs
                              │
                              ▼
                    services / transactions
                              │
                              ▼
                    JPA repositories / entities
                              │
                              ▼
                         MySQL 8
                      Flyway V1 schema
~~~

The application entry point limits component scanning to the reworked package, enables method security, configures JPA repositories and entity scanning, and publishes the API security scheme for OpenAPI tooling.

## Core Features

### Authentication, authorization, and accounts

- User registration with normalized email, BCrypt password hashing, profile fields, and automatic cart creation.
- Login issuing a signed JWT with issuer/audience checks, user UUID subject, role, token version, issue time, and a 30-minute expiration.
- Stateless Bearer authentication with active-user verification and token-version revocation on logout.
- Explicit USER and ADMIN roles, with admin routes protected at the security filter chain and controller method level.
- Per-instance registration/login attempt guard: ten attempts per action and remote address within a five-minute window.
- Authenticated profile retrieval/update and saved-address retrieval for the current user.

### Catalog and variant inventory

- Public product, category, facet, and product-detail endpoints.
- Page responses with bounded page sizes and stable pagination metadata.
- Search across title, brand, and description.
- Category descendant filtering, color filtering, variant-label filtering, price-range filtering, stock availability filtering, and deterministic sorting.
- Product variants with labels, normalized labels, active state, and independent stock quantities.
- Admin product creation, update, and archive/activation with category validation, unique variants, image URL restrictions, and optimistic version checks.

### Cart and checkout

- One cart per user with add, quantity update, and removal operations.
- Pessimistic cart locking for mutation paths and stock-aware variant validation.
- Per-variant quantity limit of ten, active-product checks, and insufficient-stock conflicts.
- Server-generated cart totals, discount totals, currency, cart version, and SHA-256 quote fingerprint.
- Checkout totals calculated from the locked server state rather than trusting browser prices.
- Variant locks during order creation, stock deduction, shipping-address snapshot, order-item price/title/image snapshot, and cart clearing after a successful order.
- Idempotency-Key required as a UUID; duplicate requests replay the original order when the request hash matches and conflict when it does not.
- Explicit MOCK/SIMULATED payment model with no card fields or payment-provider integration.

### Orders and administration

- User-scoped paginated order history and order detail retrieval.
- Order lifecycle: PLACED → CONFIRMED → SHIPPED → DELIVERED, with controlled cancellation paths.
- Stock restoration and simulated payment voiding when an order is cancelled.
- Admin summary metrics, product management, paginated order management, order detail, and status transitions.
- Optimistic version checks on product and order changes to detect stale admin screens.

## API Surface

The normative contract is [backend/openapi/shopwave-v1.yaml](./backend/openapi/shopwave-v1.yaml). The active base path is /api/v1.

| Area | Endpoints |
| --- | --- |
| Auth | POST /auth/register, POST /auth/login, POST /auth/logout |
| Catalog | GET /products, GET /products/facets, GET /products/{id}, GET /categories |
| Account | GET/PATCH /me, GET /me/addresses |
| Cart | GET /cart, POST /cart/items, PATCH/DELETE /cart/items/{id} |
| Orders | POST /orders, GET /orders, GET /orders/{id} |
| Admin summary | GET /admin/summary |
| Admin products | GET/POST /admin/products, GET/PUT /admin/products/{id}, PATCH /admin/products/{id}/archive |
| Admin orders | GET /admin/orders, GET /admin/orders/{id}, PATCH /admin/orders/{id}/status |

Public catalog routes are available without authentication. Account, cart, order, and admin routes require the appropriate Bearer identity. Unknown API routes are denied by default.

## Technical Architecture

### Security model

- Spring Security is stateless: form login, HTTP Basic, and server sessions are disabled.
- JWTs are signed with an HMAC key loaded from JWT_SECRET; the application refuses to start when the secret is shorter than 32 bytes.
- Tokens require the shopwave issuer and shopwave-api audience.
- The authentication filter resolves the user by UUID, checks active status, and compares the persisted token version before creating the security context.
- Logout increments the persisted token version, invalidating previously issued tokens without storing a token blacklist.
- CORS accepts only configured origins and explicitly allows the headers required by the frontend/API contract.
- API authentication and authorization failures return JSON application/problem+json responses instead of HTML redirects.
- The rate limiter is deliberately described as a small per-instance guard; an edge-level limit would still be required for a hardened public deployment.

### Persistence model

The current schema is defined by Flyway migration [V1__shopwave_schema.sql](./backend/src/main/resources/db/migration/V1__shopwave_schema.sql):

- UUID identifiers stored in MySQL BINARY(16) columns.
- Money stored as integer minor units in BOB to avoid floating-point arithmetic in prices and totals.
- Separate categories, products, product variants, users, carts, cart items, addresses, orders, and order-item snapshots.
- Unique constraints for user emails, category paths, product variant labels, one cart per user, order numbers, and per-user idempotency keys.
- Supporting indexes for active/price catalog queries, categories, user order history, and order items.
- JPA @Version fields for products, variants, carts, and orders where stale writes must be detected.
- spring.jpa.hibernate.ddl-auto=validate, open-in-view=false, and Flyway validation for runtime schema discipline.

The rework intentionally uses a new schema/database boundary. It does not migrate or attach the legacy database.

### Error contract

ProblemAdvice centralizes API failures as a stable problem document containing type, title, HTTP status, machine-readable code, detail, request instance, trace ID, and field errors. It maps domain conflicts, validation errors, malformed requests, unsupported media types, data-integrity conflicts, security failures, not-found conditions, and unexpected exceptions to explicit JSON responses.

## My Role & Rework Contributions

The current backend rework is directly represented by the repository history under my identity. My work covered the domain model, API contract, security, commerce consistency, test coverage, runtime configuration, and removal of the legacy application surface.

### Domain, persistence, and API design

- **Rebuilt the commerce schema:** implemented the Flyway MySQL schema, UUID-based entities, products/variants, carts, addresses, orders, snapshots, money-in-minor-units, constraints, indexes, and version fields in [e25d13c](https://github.com/sebas123312231/shopwave-backend/commit/e25d13c) and the related entity commits.
- **Formalized API v1:** implemented common response types, DTO validation, product mapping, pagination, problem responses, and the normative OpenAPI contract in [db36702](https://github.com/sebas123312231/shopwave-backend/commit/db36702), [ba277cd](https://github.com/sebas123312231/shopwave-backend/commit/ba277cd), and [2201a02](https://github.com/sebas123312231/shopwave-backend/commit/2201a02).
- **Implemented catalog behavior:** added public catalog endpoints, filtering specifications, category/facet behavior, product detail mapping, and admin product operations through [2cc7887](https://github.com/sebas123312231/shopwave-backend/commit/2cc7887), [7e65094](https://github.com/sebas123312231/shopwave-backend/commit/7e65094), and [7fc71d5](https://github.com/sebas123312231/shopwave-backend/commit/7fc71d5).

### Security and commerce correctness

- **Implemented authentication security:** added JWT issuance/parsing, issuer/audience validation, active-user checks, token-version revocation, stateless security rules, JSON security handlers, configured CORS, and BCrypt password encoding in [33e5083](https://github.com/sebas123312231/shopwave-backend/commit/33e5083) and [6426744](https://github.com/sebas123312231/shopwave-backend/commit/6426744).
- **Added authentication abuse controls:** implemented the registration/login rate limiter in [64b126c](https://github.com/sebas123312231/shopwave-backend/commit/64b126c).
- **Implemented cart consistency:** added locked cart loading, variant stock validation, quantity caps, cart versioning, totals, and quote fingerprints in [e2fd789](https://github.com/sebas123312231/shopwave-backend/commit/e2fd789) and [39869e7](https://github.com/sebas123312231/shopwave-backend/commit/39869e7).
- **Implemented checkout and order lifecycle:** added server-side totals, pessimistic variant locks, stock deduction, immutable order snapshots, idempotency replay/conflict handling, status transition rules, cancellation stock restoration, and admin order operations in [e994446](https://github.com/sebas123312231/shopwave-backend/commit/e994446), [c73f270](https://github.com/sebas123312231/shopwave-backend/commit/c73f270), and [7fc71d5](https://github.com/sebas123312231/shopwave-backend/commit/7fc71d5).

### Rework isolation and delivery

- **Removed the incompatible legacy surface:** deleted legacy controllers, security configuration, entities, repositories, request/response contracts, services, and domain enums so the active component scan has one coherent implementation. The cleanup is recorded across [6426744](https://github.com/sebas123312231/shopwave-backend/commit/6426744), [2d51cb7](https://github.com/sebas123312231/shopwave-backend/commit/2d51cb7), [f85cbd4](https://github.com/sebas123312231/shopwave-backend/commit/f85cbd4), [16c3b0c](https://github.com/sebas123312231/shopwave-backend/commit/16c3b0c), [5f8f7ef](https://github.com/sebas123312231/shopwave-backend/commit/5f8f7ef), and [5f2ef3a](https://github.com/sebas123312231/shopwave-backend/commit/5f2ef3a).
- **Added controlled demo infrastructure:** implemented profile-based configuration, opt-in seed/admin initialization, Docker runtime updates, and safe defaults that keep demo credentials and seed behavior disabled unless explicitly enabled. See [035832c](https://github.com/sebas123312231/shopwave-backend/commit/035832c), [e248e87](https://github.com/sebas123312231/shopwave-backend/commit/e248e87), and [16a01cc](https://github.com/sebas123312231/shopwave-backend/commit/16a01cc).

The bullets describe the current rework contribution. They do not claim that every feature in the original academic codebase was authored by me before this reimplementation.

## Engineering Highlights

- **Server-authoritative commerce:** the browser submits identifiers, quantities, cart version, quote fingerprint, and address data; the API recomputes prices and totals from locked persisted state.
- **Idempotent checkout:** a UUID idempotency key and request hash distinguish safe retries from a reused key carrying a different request.
- **Concurrency-aware stock:** cart and variant mutations use pessimistic database locks where stock can be changed concurrently; JPA version fields protect stale writes in admin flows.
- **Snapshot orders:** order items and shipping details are copied into the order so later product or profile changes do not rewrite historical purchase data.
- **Explicit state machine:** order transitions are enumerated and validated; cancellation restores stock and voids the simulated payment state.
- **Stable API errors:** all important failure paths are represented by codes and problem documents suitable for a typed frontend client.
- **Demo safety:** seed data and demo admin creation are opt-in; production profiles disable both by default; no real payment provider or card data path exists.

## API Documentation

- Normative contract: [backend/openapi/shopwave-v1.yaml](./backend/openapi/shopwave-v1.yaml)
- Swagger UI when running locally: http://localhost:8080/swagger-ui/index.html
- Generated OpenAPI JSON: http://localhost:8080/v3/api-docs
- Frontend integration: [sebas123312231/shopwave-frontend](https://github.com/sebas123312231/shopwave-frontend)

The OpenAPI file documents the v1 routes, Bearer security scheme, request/response models, pagination, problem responses, conflict semantics, and the Idempotency-Key checkout requirement.

## Verification & Testing

The checked-in [backend/docs/REWORK_EVIDENCE.md](./backend/docs/REWORK_EVIDENCE.md) records the following local verification on 2026-09-16:

~~~text
./mvnw.cmd test                 PASS (10 tests)
./mvnw.cmd verify -Pintegration PASS (10 Surefire + 9 Failsafe tests)
~~~

The test suite covers application context loading, public catalog and pagination contracts, authentication behavior, logout revocation, USER/ADMIN boundaries, commerce checkout and idempotent retry behavior, and admin product version/archive operations. Fast local tests use isolated H2. The Maven build includes MySQL/Testcontainers dependencies, but a Docker/Testcontainers run proving MySQL locking and Flyway equivalence is not claimed by this README.

The rework evidence also records a local HTTP smoke test through the Next.js BFF covering catalog, authentication, session, profile, cart, simulated checkout, order retrieval, USER/ADMIN authorization, and the admin page. Browser Playwright execution remains an environment-dependent verification boundary.

## Tech Stack

| Area | Technologies |
| --- | --- |
| Runtime | Java 17, Spring Boot 3.1.2, Maven Wrapper |
| Web/API | Spring Web MVC, REST controllers, API v1, DTOs, OpenAPI 3.0.3 |
| Security | Spring Security, stateless JWT Bearer auth, JJWT 0.11.5, BCrypt, CORS, rate limiting |
| Persistence | Spring Data JPA, Hibernate, MySQL 8, Flyway 9.22.3 |
| Domain | Users/roles, categories, products, variants, carts, addresses, orders, simulated payments |
| Testing | JUnit 5, Spring Boot Test, MockMvc, H2, Testcontainers MySQL, JaCoCo, Failsafe |
| Runtime packaging | Multi-stage Dockerfile, Docker Compose with MySQL healthcheck |

## Local Setup

The backend requires an explicit database and JWT configuration. The repository intentionally does not commit an .env.example; create local values in your shell or in a local Compose environment, and never commit secrets.

### Requirements

- JDK 17.
- Docker Desktop or another Docker runtime when using the MySQL Compose path.
- A new disposable MySQL database for local runtime verification, or H2 for the isolated test profile.
- The Maven Wrapper under [backend/](./backend/).

### Run directly with a local MySQL database

From the backend/ directory, set the required environment variables and start Spring Boot:

~~~powershell
$env:DB_HOST='localhost'
$env:DB_PORT='3306'
$env:DB_NAME='shopwave_rework'
$env:DB_USER='shopwave'
$env:DB_PASSWORD='local-only-password'
$env:JWT_SECRET='replace-with-a-local-secret-at-least-32-bytes-long'
$env:APP_ORIGIN='http://localhost:3000'
.\mvnw.cmd spring-boot:run
~~~

The default local profile uses MySQL, ddl-auto=validate, and Flyway migrations from backend/src/main/resources/db/migration. It does not create or modify the legacy schema.

### Run the Docker environment

From the repository root, create a local .env containing at least DB_PASSWORD and JWT_SECRET, optionally set DB_NAME, DB_USER, DB_PORT, and APP_ORIGIN, then run:

~~~bash
docker compose --env-file .env up --build
~~~

The Compose file starts MySQL 8 and the backend, waits for the database healthcheck, and uses the controlled demo profile. Demo seeding is enabled by the Compose defaults; demo admin creation remains disabled unless SHOPWAVE_DEMO_ADMIN_ENABLED=true and a local SHOPWAVE_DEMO_ADMIN_PASSWORD are explicitly supplied. Use only a new disposable database for this path.

### Test and inspect the API

~~~powershell
.\mvnw.cmd test
.\mvnw.cmd verify -Pintegration
~~~

When the application is running, inspect Swagger at /swagger-ui/index.html and the generated contract at /v3/api-docs. The frontend expects the backend at http://localhost:8080 by default through its server-side BACKEND_URL setting.

## Project Structure

~~~text
backend/src/main/java/com/shopwavefusion/rework/
  api/v1/       controllers, DTOs, mappers, problem responses
  config/       security, JWT, CORS, rate limiting, demo initialization
  domain/       JPA entities and commerce enums
  repository/   Spring Data repositories and query specifications
  service/      authentication, catalog, cart, current-user, and order logic
backend/src/main/resources/
  application*.properties  local, demo, prod, and test configuration
  db/migration/             Flyway schema
backend/openapi/            normative ShopWave API v1 contract
backend/src/test/            context, security, contract, commerce, and admin tests
Dockerfile                  multi-stage Spring Boot image
docker-compose.yml          disposable MySQL + backend runtime
~~~

## Current Status

The current snapshot is the reworked ShopWave API v1: the active Spring application, MySQL/Flyway schema, JWT security, catalog, cart, checkout, orders, profile, admin operations, Docker runtime, OpenAPI contract, and local verification suite are aligned around one implementation.

The payment model is explicitly simulated. MySQL/Testcontainers parity, browser E2E execution, public deployment, production rate limiting at the edge, external monitoring, user volume, and production performance are not claimed here.

## Related Repositories & Context

- Frontend: [sebas123312231/shopwave-frontend](https://github.com/sebas123312231/shopwave-frontend)
- Backend repository: [sebas123312231/shopwave-backend](https://github.com/sebas123312231/shopwave-backend)

ShopWave originated as a university Web Technologies 2 project. The current backend presents the later rework as a portfolio-oriented engineering exercise in replacing a legacy application surface with a versioned API, explicit domain boundaries, secure authentication, persistence invariants, and testable commerce behavior. No client, production deployment, or real payment provider is implied.
