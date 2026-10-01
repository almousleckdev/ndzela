# Architecture findings and recommendations

Result of the repository inspection on 2026-09-30. **Nothing here has been changed in the code.** Each item is a deviation from the rules in
these docs, or a risk. Severity: **High** (fix before building on it), **Medium** (fix soon), **Low** (tidy). "Decision" means the user must decide before implementation.

## A. Blocking for the tenancy foundation

| # | Finding | Severity | Recommendation |
| --- | --- | --- | --- |
| A1 | **Resolved by the tenancy foundation** (migration V7, `tenant` package, `X-Country`). Email stays globally unique; OTP, refresh and challenge keys are still keyed by email. | Done | Prefer user id over email as the Redis/session key when touching auth again. |
| A2 | **Resolved:** existing users were assigned to Gabon by migration V7. | Done | None. |
| A3 | `JwtAuthenticationFilter` loads the user with eager roles and permissions from the database on **every request**. Simple and always fresh, but every request costs a query and the country would be read the same way. | Medium | Acceptable for now; if it becomes a hotspot add a short-lived cache invalidated on role/status change. Do not put permissions or country in the JWT without an ADR. |
| A4 | **Resolved:** the audit trail is built (migration V9, `AuditService`, `GET /api/v1/audit`, screen "Historique"). | Done | Every new module must call `AuditService.record` for each change and assert it in its tests. |
| A5 | **Two scoping mechanisms by design:** business tables use `TenantOwned` (automatic, Hibernate `@TenantId`); the `users` identity table is scoped explicitly by `UserScope` (sign-in happens before any country is known). | Low | Keep the split (ADR-0009). Any new table that holds staff identity data must follow `UserScope`, and every new administration action must call it. |
| A6 | **Integration tests need Docker** (Testcontainers starts PostgreSQL and Redis). There is still no CI. | Medium | Add CI with a Docker-capable runner before the team grows. |

## B. Backend structure

| # | Finding | Severity | Recommendation |
| --- | --- | --- | --- |
| B1 | **Layer inversion:** application services import `api.dto` (`UserManagementService`, `RoleManagementService`, `AuthService`, `MfaService`, `ProfileService`, `NotificationService`) and DTOs such as `UserSummary.from(User)` know entities. | Medium | Either declare DTOs the application's public contract (simple, current practice) or move them out of `api`. Pick one and document; do not mix. **Decision.** |
| B2 | `security.LoginAttemptService` still depends on `notification.application` (the unused `JwtService` import was removed). | Medium | Move the notification side effect out of `security` (feature-triggered or domain event). |
| B3 | Feature coupling: `user.application` uses `auth.application` (`PasswordResetService`, `RefreshTokenService`); `profile.application` uses `auth.application`; `auth` uses `user.domain`. Direction is acceptable, but user/profile/auth are tightly interwoven. | Low | Keep application-to-application calls only; consider events (user disabled -> revoke sessions) as modules grow. |
| B4 | `UserManagementService.list()` and role lists are **unpaginated** (`findAll`). | Medium | Paginate before data volume grows; adopt the pagination convention in [api](../api/conventions.md). |
| B5 | Entities use `FetchType.EAGER` for `User.roles` and `Role.permissions` and are built with Lombok `@Builder/@Setter/@AllArgsConstructor`. N+1 and large graphs are likely at scale. | Medium | Keep for identity model (needed per request) but do not copy the pattern; fetch explicitly in new entities. |
| B6 | Hard-coded strings: the `SUPER_ADMIN` role name in `RoleManagementService` and frontend; French notification titles/messages written by the backend. | Low | Centralise constants; decide the i18n approach (see D3). |
| B7 | `RoleController` `GET /roles` allows `roles:read` **or** `users:create` **or** `users:update`. Intentional (user form) but undocumented in code. | Low | Documented in [authorization](../auth/authorization.md); keep explicit. |
| B8 | Refresh rotation deletes the used token, but **reuse of an already-rotated token is not treated as theft** (no token-family revocation). | Low | Optional hardening: on reuse, revoke all of the user's tokens. |
| B9 | No general **rate limiting** beyond login lockout and OTP cooldown; public `/auth/forgot-password` can be hammered. | Medium | Add per-IP/per-account rate limits before public exposure. |
| B10 | **Fixed:** the `StrongPassword` Javadoc now names `features/auth/schemas/password.ts`. | Done | None. |

## C. Frontend structure

| # | Finding | Severity | Recommendation |
| --- | --- | --- | --- |
| C1 | `lib/api/api-client.ts` imports `features/auth` (store and type), so shared code depends on a feature. | Medium | Invert: `auth` registers a token getter and a session-cleared callback with the client. |
| C2 | Mutual feature imports: `auth` <-> `profile` (session hook loads the profile; profile uses auth types/store); `users` <-> `roles`. | Medium | Extract the shared piece (e.g. current-user into `auth`) and keep one direction. |
| C3 | Permission catalogue and role labels are duplicated by hand in `features/access/lib/permissions.ts` and the migrations. Drift = broken UI or invisible features. | Medium | Keep the change procedure strict now; consider serving the catalogue from the API (`GET /permissions` exists) and generating types. **Decision.** |
| C4 | All data loading is client-side with a hand-rolled `useFetch` (no cache, dedupe, or pagination support). Every page is `"use client"`. | Medium | **Decision before adding many data screens:** adopt a query library or keep `useFetch` deliberately. |
| C5 | The dashboard shows **fake metrics** ("Metric 1", "12,345"). | Low (must not ship) | Replace with real KPIs (reporting module) or hide until then. |
| C6 | `<html lang="en">` while the UI is French. | Low | Set the language from the country/user preference once i18n is decided. |
| C7 | `packageManager` says pnpm but the lockfile is npm's `package-lock.json`. | Low | Choose one; align the field, lockfile and docs. **Decision.** |
| C8 | No frontend tests, no e2e tests. | High (before finance) | See [testing](../testing/strategy.md); pick the tools. |
| C9 | The frontend types mirror backend DTOs by hand. | Low | Acceptable now; consider OpenAPI-generated types later. |

## D. Cross-cutting and operational

| # | Finding | Severity | Recommendation |
| --- | --- | --- | --- |
| D1 | **Tests:** unit tests plus Testcontainers integration tests for tenancy now exist; still no CI, no frontend tests, and no integration tests for the other features yet. | Medium | Extend the integration harness to auth and the next modules; add CI. |
| D2 | **Flyway history:** V1 seed is duplicated by V3 (idempotent workaround for databases created by Hibernate), `baseline-on-migrate: true` is enabled, V4/V6 delete a permission and a role. | Medium | Never edit these. Consider disabling `baseline-on-migrate` outside dev once all environments are baselined. |
| D3 | **Language:** UI French, API errors English, backend notifications French, no i18n. Mali and Gabon are French-speaking, but more countries are planned. | Medium | **Decision:** i18n scope (UI only? notifications? emails?). |
| D4 | **Files** are stored on the local disk (`uploads/`, served at `/media/**`, public and immutable-cached). Not tenant-aware, not scalable, and unsuitable for sensitive driver/contract documents. | High (before driver documents) | **Decision:** object storage with per-country prefixes and access control. |
| D5 | **Infrastructure:** compose has only Postgres/Redis; no frontend Dockerfile; no CI/CD; no environments; dev DB credentials (`postgres/postgres`) are also the fallback defaults in `application.yml`. | Medium | See [infrastructure](../infrastructure/infrastructure-and-deployment.md) "Not yet defined"; remove insecure defaults for non-dev profiles. |
| D6 | `/actuator/health` is public and includes the mail health check, so an SMTP outage marks the whole service DOWN. | Low | Exclude mail from readiness or split liveness/readiness. |
| D7 | Notifications are in-app only. Drivers and daily payments will need SMS/push/WhatsApp. | Medium | **Decision** with the mobile and payments modules. |
| D8 | Local tool `mvn spring-boot:run` on top of previously compiled classes produced `ClassFormatError` after the package move. | Low | Use `mvn clean` after structural refactors (noted in CLAUDE.md). |

## E. Decisions required from the product owner before implementation

The business questions are tracked with identifiers in [functional-requirements](../business/functional-requirements.md). Answered so far: Gabon first, backend and admin first; 1 248 days; deposit per contract; arrears clock and levels; PVit; roles for drivers and suspensions. **Still open: the 10 items at the end of the functional requirements (Saturday deadline, penalty, 72 h confirmation, total price, PVit fees, partial payments, GPS blocking scope, contract number, documents checklist, infrastructure).**

Technical decisions (architecture-level):

1. **DTO placement** (B1): keep in `api` as the public contract, or move.
2. **Audit scope and retention** (A4), needed before finance and driver personal data.
3. **Frontend data layer** and **test tooling** (C4, C8); **package manager** (C7).
4. **File storage** for identity documents and contract PDFs (D4); **hosting, environments and CI/CD** (D5).
5. **i18n scope** (D3, C6) and **notification channels** for warnings (D7).
6. **Sign-in throttling for driver accounts** (password is the only factor, ADR-0008) and whether **GPS/remote vehicle blocking** is part of this platform (adds an integration module).
7. **Mobile repository location** and Flutter tooling choices ([mobile](../mobile/architecture.md)).
8. **Payment-provider integration approach** (direct vs aggregator), and the scheduled-job mechanism for arrears.
9. **Git workflow**: confirm branches/commit conventions in [git-workflow](../workflow/git-workflow.md).
