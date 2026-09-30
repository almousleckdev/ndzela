# Testing strategy

## Current state (Implemented)

Backend: JUnit 5 + Mockito unit tests using `TestProperties`, plus **integration tests on real PostgreSQL and Redis** (Testcontainers, **Docker must be running**): `AbstractIntegrationTest` is the base class (full application context, MockMvc, helpers `country()`, `staff()`, `bearer()`; Mali comes from migration V8 and serves as the second country; a test-only table `tenant_test_items` proves the `TenantOwned` mechanism), with `MigrationV7Test`, `TenantMechanismTest`, `UserIsolationTest`, `UserCreationCountryTest`, `NotificationCountryScopeTest`, `CountryApiTest`, `InactiveCountryTest`. **No** frontend tests and **no** CI yet. Run: `cd backend && mvn clean test`.

## Required going forward

| Level | Tool | What it must cover |
| --- | --- | --- |
| Unit (backend) | JUnit 5, Mockito | Business rules in services and policies: happy path and each rejection. |
| Integration (backend) | Spring Boot Test + **Testcontainers** (PostgreSQL, Redis): **in place** | Repositories, Flyway migrations from empty, security rules (`@PreAuthorize` per endpoint), the tenant filter. |
| API/contract | MockMvc or RestAssured | Status codes, error shape and `code`s, validation, 401/403/404 behaviour. |
| Frontend | (Open: Vitest + React Testing Library recommended) | Form validation, permission-based rendering, error mapping. Decision needed before adding UI-heavy modules. |
| End-to-end | (Open: Playwright recommended) | Critical journeys: sign-in with MFA, invite user, country isolation as seen in the UI. |

## Non-negotiable tests

1. **Tenant isolation** for every tenant-owned feature: country-A user gets `404` on country-B ids; lists contain only own country; creates land in own country; the super admin sees all; `X-Country` is ignored for others.
2. **Authorization** per endpoint: forbidden without the permission, allowed with it.
3. **Escalation**: `AccessPolicy` cases for any new administrative capability.
4. **Money**: rounding, currency and arrears/schedule calculations get exhaustive table-driven unit tests before they ship.
5. **Migrations** run cleanly on an empty database and on the previous schema version.
6. Security-sensitive flows (OTP, MFA replay, lockout, token rotation) keep their tests; a bug fix ships with a regression test.

## Practices

- Tests are deterministic (no real SMTP, time via injected clock where relevant, fixed properties from `TestProperties`).
- Test names state behaviour. One reason to fail per test. No test depends on another's data.
- Do not weaken or delete a test to make a build pass; report it.
- `mvn clean test` must be green before a change is considered done (use `clean` after refactors to avoid stale classes).
