# Authentication

Applies to **staff** (web admin). **Drivers** are created by staff and sign in to the Flutter app with **phone or email + password** ([ADR-0008](../adr/0008-driver-accounts-separate-from-staff.md), [mobile](../mobile/architecture.md)); their flow is a separate design (**password only, forced change of the initial password at first login, throttling and lockout required**) and must not reuse the staff flow blindly.

## Sign-in flow (Implemented)

Three steps, each proven by a short-lived **login challenge token** kept in Redis (`auth:challenge:*`, TTL `challenge-ttl` = 10 min; the token is stored hashed):

1. `POST /api/v1/auth/login` (email + password). Password is checked with BCrypt (cost 12). Unknown email still runs a decoy hash check (timing protection).
   On success an **email OTP** (6 digits, TTL 3 min, max 3 attempts, 60 s resend cooldown) is sent and a challenge token is returned.
2. `POST /api/v1/auth/verify-otp`. The challenge advances. The response says whether the next step is `MFA_SETUP` (first time) or `MFA_VERIFY`.
3. `POST /api/v1/auth/mfa/setup` (only when enrolling: returns the TOTP seed and QR) then `POST /api/v1/auth/verify-totp`.
   The TOTP code is checked and each code can be used **once** (replay guard in Redis, 120 s). A valid first code activates the account (`MFA_SETUP` -> `ACTIVE`).
   Tokens are issued only here.

MFA is **mandatory for every staff account**. Unknown/wrong credentials always return the same `INVALID_CREDENTIALS` message.

## Tokens and sessions (Implemented)

| Token | Form | Lifetime | Stored |
| --- | --- | --- | --- |
| Access token | JWT HS256, claims: `sub` (email), `iss`, `jti`, `iat`, `exp` | 15 min | **Frontend memory only** |
| Refresh token | Opaque 256-bit random | 24 h, **rotated on every use** (single-use) | HttpOnly cookie `ndzela_refresh`, `SameSite=Strict`, path `/api/v1/auth`, `Secure` when `COOKIE_SECURE=true`; server side only its SHA-256 hash in Redis (`auth:rt:*`) |

- The JWT carries **no roles or permissions**. On each request `JwtAuthenticationFilter` loads the user and builds authorities from the database, so
  role changes take effect immediately, and `DISABLED`/`LOCKED` users are rejected at once.
- `POST /api/v1/auth/refresh` rotates the refresh token; if the user is no longer `ACTIVE` all their refresh tokens are revoked.
- `POST /api/v1/auth/logout` revokes the refresh token and clears the cookie. Password change, MFA reset, disabling, blocking and deleting a user call `revokeAll`.
- CSRF protection is disabled because the API is stateless; the only ambient credential (the refresh cookie) is `SameSite=Strict` and limited to the auth path.

## Account states and lockout (Implemented)

`UserStatus`: `MFA_SETUP` (invited/enrolling), `ACTIVE`, `LOCKED`, `DISABLED`.
- 5 failed attempts (password, OTP or TOTP) lock the account for 30 min (`LOCKED`, auto-release on next attempt after expiry). Holders of `users:update` in the same country, and the global super admins, are notified.
- **Driver app:** wrong passwords on sign-in **and on the password change** both count toward the lockout, and the change is refused while the driver is locked (`423 ACCOUNT_LOCKED`). The failure count is written in its own transaction so it survives the refused request.
- **Staff of an inactive country** cannot sign in (`423 ACCOUNT_LOCKED`, like a disabled account); their existing access tokens are rejected (`401`) and refresh is refused. Global users are unaffected.
- An admin can **block** (indefinite `LOCKED`), **suspend** (`DISABLED`), unblock, reactivate, delete, or reset MFA.

## Invitation and password reset (Implemented)

New staff are created with an unusable random password and receive an emailed link (`/reset-password?token=...`, TTL 48 h) to choose one.
Forgot-password uses the same mechanism (TTL 15 min) and never reveals whether the email exists. Tokens are single-use and stored hashed in Redis.
Password policy: 8-128 chars with digit, lower, upper and special (`@StrongPassword`, mirrored in `features/auth/schemas/password.ts`).

## Secrets at rest

MFA seeds are AES-encrypted in the database (`MfaSecretConverter`, key `MFA_ENCRYPTION_KEY`, base64 of 32 bytes). `JWT_SECRET` is base64, at least 256 bits.

## Rules for changes

- Never widen the token contents to hold data that can change (permissions, country) without an ADR; today authority is read fresh from the DB.
- Any new credential or token is generated with `SecureRandom`, stored **hashed**, expiring, and single-use where applicable.
- Do not add a new login path that skips OTP or TOTP for staff.
- Under tenancy (Decided), the user's country is read from the user record per request; it is never a client-supplied value ([multi-tenancy](../multi-tenancy/multi-tenancy.md)).
