# API conventions

Base path: **`/api/v1`**. JSON in and out (`application/json`), except the profile picture upload (`multipart/form-data`, max 2 MB, PNG/JPEG/WebP).

## Resources and verbs (Implemented pattern)

- Plural nouns: `/users`, `/roles`, `/notifications`. Standard verbs: `GET` list/read, `POST` create, `PUT` replace/update, `DELETE` remove.
- State transitions and actions are `POST /<resource>/{id}/<action>` (e.g. `/users/{id}/disable`, `/lock`, `/reset-mfa`). Keep actions as verbs of the *domain*.
- The current user's own data is under `/profile/me`, `/profile/password`, `/profile/picture`, `/profile/mfa/reset`.
- Auth endpoints (public) are under `/auth/**` (login, verify-otp, resend-otp, mfa/setup, verify-totp, refresh, logout, forgot-password, reset-password).
- Success: `200` with body, `201` on create, `204` when there is no body.

## Authentication and authorization

`Authorization: Bearer <access token>` on everything except `/auth/**`. Each endpoint is guarded by `@PreAuthorize` on a permission
([authorization](../auth/authorization.md)). Country scope is applied server-side ([multi-tenancy](../multi-tenancy/multi-tenancy.md)).

## Headers

| Header | Direction | Purpose |
| --- | --- | --- |
| `X-Request-ID` | request/response | Correlation id. Generated if absent; echoed back; present in error bodies and logs. |
| `Authorization` | request | Bearer access token. |
| `X-Country` | request | The super admin's country filter (a country code such as `GA`); ignored for everyone else; absent means all countries. An unknown or inactive code answers `400 INVALID_COUNTRY`. |

CORS: allowed origins from `CORS_ALLOWED_ORIGINS`; allowed headers `Authorization, Content-Type, X-Request-ID, X-Country`; credentials allowed (refresh cookie).

Country error codes: `INVALID_COUNTRY` (400), `COUNTRY_REQUIRED` (422, a country must be selected), `COUNTRY_NOT_ALLOWED` (422, creating a user outside your own country), `COUNTRY_INACTIVE` (403, defensive: users of an inactive country are normally stopped earlier with 401 or 423).

## Error format (single shape, Implemented)

```json
{ "timestamp": "2026-09-30T14:50:45+01:00", "status": 401, "code": "INVALID_CREDENTIALS",
  "message": "Invalid email or password.", "path": "/api/v1/auth/login", "requestId": "…",
  "errors": [ { "field": "email", "message": "…" } ] }
```

`errors` is only present for `VALIDATION_ERROR` (400). Status mapping: 400 validation/malformed, 401 unauthenticated/`SESSION_EXPIRED`, 403 forbidden, 404 not found,
405, 422 business rule (`code` identifies the rule), 423 account locked, 500 unexpected (generic message, details only in logs).
The `code` is the contract: clients branch on `code`, never on `message`. Messages are English and technical; the UI shows its own French text per `code`.

## DTO rules

- Java records under `<feature>/api/dto`; naming `...Request`, `...Response`, `...Summary`. Never expose entities. Never expose password hashes, MFA secrets, tokens.
- IDs are UUID strings; timestamps ISO-8601 with offset; enums as their string names; money as decimal strings or integer minor units with an explicit currency (decided per module).
- Requests validated with bean validation; unknown/extra fields are ignored; role names and codes are `UPPER_SNAKE_CASE`.

## Evolution

Additive changes (new optional fields, new endpoints) stay in `v1`. A breaking change needs `v2` (or a versioned resource) and an ADR. Lists that can grow are paginated
(**Proposed** convention: `page`, `size`, response with `items`, `total`), the existing unpaginated lists (`/users`, `/roles`) are recorded in findings.

## Documentation

OpenAPI/Swagger UI is available when `SWAGGER_ENABLED=true` (`/swagger-ui/index.html`, `/v3/api-docs`); it is disabled by default and must stay disabled in production unless deliberately exposed.
