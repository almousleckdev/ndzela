# Backend architecture

Java 21, Spring Boot 4.1.1, Maven. Modular monolith, **package by feature**, each feature layered `api / application / domain`
([module-boundaries](../architecture/module-boundaries.md)). Source: `backend/src/main/java/com/ndzela/admin`.

## Layout (Implemented)

| Package | Contents |
| --- | --- |
| `auth/` | `api` (AuthController, RefreshTokenCookie, dto), `application` (AuthService, LoginChallengeService, OtpService, MfaService, RefreshTokenService, PasswordResetService) |
| `user/` | `api` (UserController, RoleController, dto), `application` (UserManagementService, RoleManagementService, AccessPolicy), `domain` (User, Role, Permission, UserStatus + repositories) |
| `profile/` | `api` (ProfileController, dto), `application` (ProfileService) |
| `notification/` | `api`, `application` (NotificationService), `domain` (Notification, repository): in-app only |
| `country/` | `api` (CountryController, dto), `application` (CountryService), `domain` (Country + repository): read-only country list |
| `tenant/` | Country context: TenantContext, TenantHolder, TenantContextResolver, TenantIdentifierResolver (Hibernate), TenantOwned base class and its listener |
| `security/` | SecurityConfig, `jwt/` (JwtService, JwtAuthenticationFilter), TenantContextFilter, CurrentActor, SecurityUser, CustomUserDetailsService, LoginAttemptService, OpaqueTokens |
| `common/` | `exception` (typed exceptions, GlobalExceptionHandler, ApiErrorResponse), `web` (RequestCorrelationFilter, MessageResponse), `validation` (StrongPassword) |
| `config/` | AppProperties (typed `app.*` properties), MediaConfig, TotpConfig |
| `infrastructure/` | `email` (EmailProvider + smtp/resend/log), `storage` (AvatarStorage), `crypto` (SecretEncryptor, MfaSecretConverter) |
| `bootstrap/` | SuperAdminBootstrap (creates the first SUPER_ADMIN from env on empty DB) |

Resources: `application.yml`, `db/migration/V*.sql`, `templates/*.html` (Thymeleaf email templates).

## Request flow

`RequestCorrelationFilter` (assigns `X-Request-ID`, puts it in MDC) -> Spring Security filter chain (CORS, `JwtAuthenticationFilter`
loads the user and its permission authorities) -> controller (`@PreAuthorize("hasAuthority('resource:action')")`, `@Valid` body) ->
application service (transaction, business rules, `AccessPolicy`) -> repository -> DTO back. Any exception goes through
`GlobalExceptionHandler`, which produces the single error JSON. 401/403 from Security are routed to the same handler.

## Configuration

All settings live in `application.yml` under `app.*`, bound by the immutable record `AppProperties`. Secrets have no defaults
(`JWT_SECRET`, `MFA_ENCRYPTION_KEY`, `MAIL_USERNAME`, `MAIL_PASSWORD`) and the app fails fast without them. Add a new setting by
adding it to `AppProperties` + `application.yml` + `backend/.env.example` + [infrastructure doc](../infrastructure/infrastructure-and-deployment.md).

## Tenancy in the architecture (Implemented)

The country context is resolved once per request from the authenticated user (`TenantContextFilter`, after the JWT filter) and applied by Hibernate (`@TenantId`) to every entity extending `TenantOwned`.
Staff accounts are scoped explicitly by `UserScope`. Details and rules: [multi-tenancy](../multi-tenancy/multi-tenancy.md), [ADR-0009](../adr/0009-tenant-id-for-business-data-explicit-scope-for-staff.md).

## Adding a feature (checklist)

1. Read the module spec and the business questions. Create `<feature>/{api,application,domain}` (+ `api/dto`).
2. New tables via a Flyway migration; entity in `domain`; tenant-owned tables carry `country_id`.
3. Permissions for the feature: add via migration, register in [roles-and-permissions](../auth/roles-and-permissions.md) and the frontend catalogue.
4. Endpoints under `/api/v1/<plural-resource>` with `@PreAuthorize`; DTOs as Java records with bean validation.
5. Business rules in the service; throw the typed exceptions; no `try/catch` that swallows.
6. Tests: unit tests for rules, plus the tenant-isolation test for tenant-owned data.
7. Update the docs and add an ADR if a new pattern was needed.
