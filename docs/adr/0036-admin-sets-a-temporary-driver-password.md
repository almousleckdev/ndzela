# ADR-0036: An admin sets a temporary password for a driver, and changes their email and phone

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
After the legacy import most drivers no longer remember their password, and some no longer remember their email. The only recovery today is "forgot password", which sends a code to the driver's email, so it fails exactly for a driver who lost it. Staff need a way to get a driver back in, without weakening the rules about secrets (a password is never returned or logged).

## Decision
- A new permission `drivers:credentials` (SUPER_ADMIN, ADMIN only; migration V58) allows two actions, both country-scoped: `PUT /drivers/{id}/contact` (new email and/or phone, checked for duplicates, audited with masked values) and `POST /drivers/{id}/password-reset`.
- For the password, **the admin chooses it** (typed, or generated in the browser) and the backend **never returns, logs or audits it**. The driver must change it at the next sign-in (`mustChangePassword`); setting it ends the driver's sessions (refresh tokens and `session_id`) and clears a login lock. An archive profile (`password_hash = '!'`) is refused.
- The admin passes the password to the driver by phone or in person. Nothing is sent by email or SMS.
- A changed email or phone triggers the same Zella lookup as a new driver.

## Consequences
- Positive: a driver with no email can be recovered; no secret travels back through the API; the action is audited (`DRIVER_PASSWORD_RESET`, `DRIVER_CONTACT_CHANGED`).
- Negative: the admin sees the temporary password and is trusted to relay it; it is valid until the driver changes it.

## Alternatives considered
- Email a reset link to a corrected email: fails when the email is the thing that was lost; kept only as the existing "forgot password" path.
- Generate the password in the backend and return it once: returns a secret, against the project rules.
- Reuse the driver update endpoint: it needs `drivers:update`, which ADMIN does not hold, and mixes a security-sensitive change with ordinary edits.
