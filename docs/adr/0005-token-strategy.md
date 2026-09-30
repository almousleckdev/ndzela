# ADR-0005: Token strategy and mandatory MFA for staff

**Status:** Accepted · **Date:** 2026-09-30 (recorded)

**Decision.** Three-step login (password, email OTP, TOTP). 15-minute JWT access token kept only in browser memory; opaque single-use rotating refresh token in an HttpOnly, SameSite=Strict cookie scoped to `/api/v1/auth`, stored hashed in Redis. Authorities are loaded from the database on every request (the JWT holds no permissions). Details: [authentication](../auth/authentication.md).

**Consequences.** (+) XSS cannot read the refresh token; permission and status changes apply immediately. (-) One database read per request; email is currently the Redis key/identity (see findings); Strict cookies constrain cross-site deployments. Driver authentication is a separate decision (ADR-0008).
