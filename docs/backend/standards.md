# Backend standards

## Language and style
- Java 21 features welcome (records for DTOs and value types, `switch` expressions, `var` where the type is obvious).
- Lombok is allowed: `@RequiredArgsConstructor` for constructor injection, `@Getter/@Setter` on entities, `@Slf4j`. Do not stack
  `@Data` on entities. A class with non-trivial construction logic (e.g. `AuthService`) may use an explicit constructor.
- Constructor injection only. No field `@Autowired`.
- Code, names and messages in English.
- **Comments are rare.** One short line of class doc at most (`/** ... */`), and a `//` note only for a non-obvious *why*
  (a lock, a constraint, an ordering that matters). No multi-line Javadoc, no comments that restate the code. Applied
  migrations are never touched, comments included.
- **One top-level type per file.** Never group several records, enums or classes in a holder class (`FooDtos`) and never nest
  unrelated types. A small private helper type used by one class alone may stay nested.
- **Do not mix kinds of types in a package.** Where each kind lives:

  | Kind | Package |
  | --- | --- |
  | Controllers | `<feature>/api` |
  | Request/response records | `<feature>/api/dto` |
  | Entities, repositories, domain enums | `<feature>/domain` |
  | Value records and flow enums used by services (inputs, results, tokens, states) | `<feature>/application/model` |
  | JSON snapshot records stored in a column | `<feature>/domain/snapshot` |
  | Services and use cases | `<feature>/application` |
  | `@ConfigurationProperties` records | `config/properties` |
  | Shared response/request records | `common/web/dto` |
  | Tenant and security value types | `tenant/model`, `security/model` |

  A record or enum used by several layers belongs to the lowest layer that owns it (`domain` before `application/model`).
- Files stay small and single-purpose (target under ~200 lines). A service growing past that is doing too much: split by use case.

## Layers
- **Controllers** are thin: bind and validate the request, call one service method, return a DTO. No business rules, no repositories.
  Each endpoint has `@PreAuthorize("hasAuthority('<permission>')")` unless it is deliberately public (only `/api/v1/auth/**`).
- **Services** own business rules, transactions (`@Transactional`, `readOnly = true` for queries) and orchestration.
  Get the caller with `CurrentActor.from(authentication)`; pass it to the service, do not reach into `SecurityContextHolder` from services.
- **Repositories** are Spring Data interfaces in `domain`. Only the owning feature uses them.
- **Entities** never leave the service layer through the API: map to a DTO record. Do not add JSON annotations to entities.

## DTOs and validation
- Request/response DTOs are records in `<feature>/api/dto`, suffixes `...Request`, `...Response`, `...Summary`.
- Validate input with bean validation on the DTO (`@NotBlank`, `@Email`, `@Size`, `@StrongPassword`...). Business validation
  (uniqueness, state, permission) in the service, expressed as a typed exception.
- Normalise identifiers at the boundary (emails are trimmed and lower-cased).

## Errors
Throw one of the typed exceptions in `common.exception`; never return error bodies by hand and never catch-and-ignore:

| Exception | HTTP | `code` |
| --- | --- | --- |
| `BusinessRuleViolationException(code, message)` | 422 | your code (UPPER_SNAKE) |
| `ResourceNotFoundException` | 404 | `RESOURCE_NOT_FOUND` |
| `InsufficientPrivilegesException` | 403 | `INSUFFICIENT_PRIVILEGES` |
| `InvalidCredentialsException` | 401 | `INVALID_CREDENTIALS` |
| `SessionExpiredException` | 401 | `SESSION_EXPIRED` |
| `AccountLockedException` | 423 | `ACCOUNT_LOCKED` |

Add a new exception type only when the HTTP semantics differ. A new business `code` must also be mapped in the frontend
(`lib/errors.ts`, French message).

## Data access
- No queries built by string concatenation. Use derived queries, `@Query` with parameters, or the criteria API.
- Avoid `FetchType.EAGER` on new associations; fetch what you need explicitly (see findings for the existing eager roles).
- List endpoints that can grow must be paginated. New ones are paginated from day one.
- **Every tenant-owned query is country-scoped** by the tenancy mechanism. Never write a native query that bypasses it without an ADR.

## Security-sensitive code
- Randomness for tokens/OTP: `SecureRandom` via `OpaqueTokens` / `OtpService`. Compare secrets with `constantTimeEquals`.
- Tokens stored server-side are stored **hashed**. Never log tokens, OTPs, passwords, MFA seeds.
- Anything that grants permissions goes through `AccessPolicy` (no escalation).
- Uploaded files: type decided from bytes, size-capped, random names, stored outside the web root (`AvatarStorage` is the model).

## Logging
- SLF4J. `INFO` for business events, `WARN` for rejected/suspicious actions, `ERROR` only for unexpected failures.
- The request id (`X-Request-ID`, MDC `requestId`) must be preserved; do not create parallel correlation mechanisms.

## Money (from the first payment module)
`BigDecimal` or integer minor units, always with an explicit currency from the country. Never `float`/`double`. Rounding rule
defined once in a shared value type. See [database](../database/database.md).

## Configuration
Never hardcode URLs, secrets, TTLs or limits: add to `AppProperties`. See [backend/architecture](architecture.md#configuration).
