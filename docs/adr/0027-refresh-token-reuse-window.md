# ADR-0027: A rotated refresh token still works for 30 seconds

**Status:** Accepted · **Date:** 2026-11-06

**Context.** Refresh tokens were single use ([authentication](../auth/authentication.md)). A refresh whose answer never reached the client (a dropped connection, the app killed at the wrong moment) lost the session, and two requests that renewed together made the second one fail. The driver app also never renewed its access token, so a driver was signed out every 15 minutes. Both the dashboard and the driver app showed "Votre session a pris fin".

**Decision.**
- For **30 seconds** after a refresh token is rotated, presenting it again is accepted and issues a new pair for the same subject and session (Redis keys `auth:rt-grace:*`, with an index per subject, `auth:rt-grace-user:*`). After that it is refused like any unknown token.
- Signing out (`revoke`) and `revokeAll` (password change, disabling, a new login) also delete the window, so a signed-out token never comes back.
- The session id check stays: a token of a session replaced by a newer login is refused whatever the window says ([ADR-0022](0022-single-device-sessions.md)).
- The driver app renews its access token on a `401` through one shared renewal (`TokenRefresher`), retries the request, and signs the driver out only when the server rejects the refresh token.

**Consequences.** (+) A lost answer or two simultaneous renewals no longer end a session. (+) The driver stays signed in for the life of the refresh token (24 h, 30 days for the Zella app). (-) A stolen refresh token can be replayed for up to 30 seconds after its owner used it; it is the same trade-off that Auth0 and others call "reuse leeway", and logging out closes it at once.

**Alternatives considered.** Keeping strict single use and deduplicating only in the clients (rejected: it cannot cover a lost response). A longer window (rejected: widens the replay risk for no benefit). Sliding refresh tokens without rotation (rejected: a leaked token would then live as long as the session).
