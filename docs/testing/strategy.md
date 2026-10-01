# Testing strategy

## Current state (Implemented)

Backend: JUnit 5 + Mockito unit tests using `TestProperties`, plus **integration tests on real PostgreSQL and Redis** (Testcontainers, **Docker must be running**): `AbstractIntegrationTest` is the base class (full application context, MockMvc, helpers `country()`, `staff()`, `bearer()`; Mali comes from migration V8 and serves as the second country; a test-only table `tenant_test_items` proves the `TenantOwned` mechanism), with `MigrationV7Test`, `TenantMechanismTest`, `UserIsolationTest`, `UserCreationCountryTest`, `NotificationCountryScopeTest`, `CountryApiTest`, `InactiveCountryTest`, `RoleVisibilityTest`, the audit tests (`AuditTrailTest`, `AuditApiTest`), the vehicle tests (`VehicleApiTest`, `VehicleIsolationTest`) and the driver tests (`DriverCreationTest`, `DriverAssignmentTest` including a concurrent race and direct database checks of the one-open-assignment indexes, `DriverLifecycleTest`, `DriverIsolationTest`, `TrainingCenterApiTest`, `DriverReasonsTest`, `DriverFilesTest`, `DriverLeaveTest` including a concurrent overlap race, `ExportsTest`, `ContractTest`: end-date rule against the sample, sequential numbers under concurrency, one active contract per driver and vehicle, snapshot immutability, PDF text content, country isolation). `AccountCalculatorTest` (pure rules: deadline, levels, Sunday, leaves, partial and advance payments, reversal, penalty), `PaymentApiTest` (permissions, validation, idempotency under concurrency, reversal, append-only trigger, isolation), `ArrearsJobTest` (escalation once per step, penalty, automatic suspension, notifications). `ChargingApiTest` (driver auto-fill, public cars, battery and amount validation, idempotency under concurrency, cancellation, append-only trigger, metrics, exports, stations, isolation). `ListSortAndRangeTest` (whitelisted sort and day ranges on every list, search by driver, reference and plate). Contract updates (revision, refreshed parties, PDF note, permissions) are in `ContractTest`. `DeletionTest` (erasure and car release, all-or-nothing refusal on payments, recharges and arrears, vehicle history). About 243 backend tests. **No** frontend tests and **no** CI yet; the driver wizard and vehicle screens were exercised once in a real browser against a throwaway backend (2026-09-30). Run: `cd backend && mvn clean test`.

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
