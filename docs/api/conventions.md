# API conventions

Base path: **`/api/v1`**. JSON in and out (`application/json`), except uploads (`multipart/form-data`): the profile picture (max 2 MB, PNG/JPEG/WebP) and the driver files (max 5 MB, PNG/JPEG/WebP, PDF for documents; driver creation is one multipart request with a `data` JSON part).

Restrictive actions take a mandatory body `{ "reason": "..." }` (max 255): driver `suspend` and `block`, `POST /drivers/{id}/vehicle/remove`, user `disable` and `lock`; `POST /vehicles/{id}/status` takes `{ status, reason }` with the reason required except for `AVAILABLE` (`REASON_REQUIRED`, 422). **List parameters (2026-10-01):** every table list accepts `q` (search), `sort=field,asc|desc` against a whitelist per endpoint (an unknown field or direction answers `400 INVALID_PARAMETER`; the id is always the last tie-breaker) and `from` / `to` (inclusive whole days, `yyyy-MM-dd`, each country measured in its own timezone; either may be omitted) on the date that makes sense for the list: creation day for vehicles and drivers, `createdAt` for the ledger, recharges and audit, `lateFrom` / `lateTo` (overdue since) and `paidFrom` / `paidTo` (money collected in the period, returned as `collected`) for `GET /payments/accounts`; `GET /payments/summary?from&to` adds `collectedRange`. The recharges list replaces its former `day` parameter with `from` / `to`.

Charging: `GET/POST /charging/recharges` (client `requestId`), `POST /charging/recharges/{id}/reverse`, `GET /charging/summary`, `/export?period=DAY|WEEK|MONTH|YEAR&date=`, `/vehicles`, and `/charging/stations`; errors `CHARGING_TARGET_INVALID`, `VEHICLE_HAS_NO_DRIVER`, `BATTERY_LEVELS_INVALID`, `STATION_NAME_TAKEN`, `STATION_IN_USE`.

Payments: `GET /payments/accounts`, `/accounts/{driverId}`, `/summary`, `/history`, `/export`; `POST /payments` (body includes a client `requestId` for idempotency), `POST /payments/{id}/reverse`, `POST /arrears/waivers`; `GET /drivers?late=`. Errors `PAYMENT_REFERENCE_REQUIRED`, `NO_ACTIVE_CONTRACT`, `ENTRY_ALREADY_REVERSED`, `ENTRY_NOT_REVERSIBLE`.

Deletion: `DELETE /drivers/{id}` and `DELETE /vehicles/{id}` erase the record with its contracts and assignments only while no payment or recharge exists; errors `DRIVER_HAS_HISTORY`, `DRIVER_HAS_ARREARS`, `VEHICLE_HAS_HISTORY`, `VEHICLE_ASSIGNED` (all 422).

Contract update: `PUT /drivers/{id}/contract {deposit}` (`contracts:update`), error `CONTRACT_NOT_ACTIVE`; contract responses carry `revision` and `updatedAt`.

Contracts: `GET /contracts?driverId=`, `GET /contracts/{id}`, `GET /contracts/{id}/pdf` (`application/pdf`, audited), `POST /contracts/preview`; errors `CONTRACT_TEMPLATE_UNAVAILABLE`, `CONTRACT_ALREADY_ACTIVE`, `DEPOSIT_INVALID`. `POST /drivers` now requires `vehicleId` and `contract.deposit`; `POST /drivers/{id}/vehicle` requires `deposit`.

Drivers: `GET /drivers/stats` (replaces `/counts`), `GET /drivers?status=&onLeave=&q=`, `GET /drivers/export`, `POST /drivers/{id}/deactivate`, leaves under `/drivers/{id}/leaves`. Leave errors: `LEAVE_IN_PAST`, `LEAVE_DATES_INVALID`, `LEAVE_TOO_LONG`, `LEAVE_NOTE_REQUIRED`, `LEAVE_OVERLAP`, `LEAVE_STARTED`; `DRIVER_NOT_ACTIVE`. Exports are CSV (UTF-8 with BOM, `;` separated, formula-like cells neutralised, max 10 000 rows): `GET /drivers/export`, `GET /vehicles/export`.

File errors: `INVALID_FILE`, `FILE_TOO_LARGE`, `PHOTO_REQUIRED`; a blocked driver: `DRIVER_BLOCKED`.

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

`errors` is only present for `VALIDATION_ERROR` (400). Status mapping: 400 validation/malformed (a bad query parameter answers `INVALID_PARAMETER`), 401 unauthenticated/`SESSION_EXPIRED`, 403 forbidden, 404 not found,
405, 422 business rule (`code` identifies the rule), 423 account locked, 500 unexpected (generic message, details only in logs).
The `code` is the contract: clients branch on `code`, never on `message`. Messages are English and technical; the UI shows its own French text per `code`.

## DTO rules

- Java records under `<feature>/api/dto`; naming `...Request`, `...Response`, `...Summary`. Never expose entities. Never expose password hashes, MFA secrets, tokens.
- IDs are UUID strings; timestamps ISO-8601 with offset; enums as their string names; money as decimal strings or integer minor units with an explicit currency (decided per module).
- Requests validated with bean validation; unknown/extra fields are ignored; role names and codes are `UPPER_SNAKE_CASE`.

## Evolution

Additive changes (new optional fields, new endpoints) stay in `v1`. A breaking change needs `v2` (or a versioned resource) and an ADR. Lists that can grow are paginated
(**Implemented** for audit, vehicles and drivers: query `page` from 0 and `size` (default 20, max 100); response `{items, page, size, total}`). The older lists (`/users`, `/roles`) are still unpaginated and recorded in findings.

## Documentation

OpenAPI/Swagger UI is available when `SWAGGER_ENABLED=true` (`/swagger-ui/index.html`, `/v3/api-docs`); it is disabled by default and must stay disabled in production unless deliberately exposed.
