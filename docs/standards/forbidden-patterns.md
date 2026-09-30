# Forbidden patterns and anti-patterns

Each item is a rule. Violations are defects, not style issues. If you believe an exception is needed, ask the user and write an ADR.

## Tenancy (highest severity)
- Reading or writing tenant-owned data without the country scope; a repository method or native query that bypasses the tenant filter.
- Trusting a `country_id` (or country header) from a non-super-admin client. Accepting a `country_id` in a request body for tenant-owned creates.
- Referencing a row from another country (foreign keys across countries).
- Shipping a tenant-owned feature without its isolation tests. Caching or storing files for tenant data without a country namespace.

## Security
- An endpoint without `@PreAuthorize` (except the documented public ones). Access decisions on role names. Client-side-only authorization.
- Skipping `AccessPolicy` when granting roles/permissions. Building SQL/JPQL by string concatenation.
- Logging or returning passwords, tokens, OTPs, MFA seeds, hashes. Putting secrets in `NEXT_PUBLIC_*`, the repo, images or logs.
- Storing tokens or personal data in `localStorage`/`sessionStorage`. Adding a staff sign-in path that skips OTP/TOTP.
- Comparing secrets with `==`; `Random` instead of `SecureRandom` for security values; storing reset/refresh tokens unhashed.
- Trusting client-provided file type/name for uploads. Committing `.env` files.

## Architecture
- Backend: controller with business logic or repository access; a service returning entities to the API; `domain` depending on `api`/`application`; a feature using another feature's repository; cyclic application-layer dependencies; `infrastructure`/`common`/`config` depending on a feature; `security` calling feature services.
- Frontend: `components/ui`, `hooks`, `lib`, `config` importing from `features`; raw `fetch` outside `apiClient`; business rules in services/components; mutual imports between features; fat pages in `app/`.
- A second mechanism for something that already exists (another HTTP client, another error format, another token store, another permission helper).
- Frontend and backend sharing code by importing each other; either side making assumptions the API contract does not state.
- Undocumented patterns, new libraries, or new layers without an ADR. Speculative abstractions (single-implementation interfaces, generic base classes, "framework" helpers with one user).
- Files that mix responsibilities or keep growing past ~200 lines instead of being split.

## Database
- Editing an applied Flyway migration. Hibernate creating/altering schema (`ddl-auto` other than `validate`).
- `float`/`double` for money; amounts without a currency; updating or deleting posted ledger records.
- Enum ordinals; unindexed foreign keys or tenant list queries; unbounded list endpoints on growing tables.

## API
- A response error shape other than the standard error JSON; clients branching on `message` text; exposing entities, stack traces or internal ids of other tenants.
- Breaking a `v1` contract in place. Business logic in DTOs beyond simple mapping.

## Process
- Guessing business rules or building a module before its spec/questions are settled.
- Large drive-by refactors mixed with feature work; weakening or deleting a test to get green; claiming done without running the checks.
- Committing, pushing, or rewriting history without being asked. Letting docs drift from the code.
