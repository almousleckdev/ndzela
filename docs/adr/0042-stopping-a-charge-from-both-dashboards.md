# ADR-0042: A running charge can be stopped from the NDZela admin and from the Zella dashboard

**Status:** Accepted · **Date:** 2026-11-17

**Context.** Several times a charger kept delivering energy after the Zella app showed the charge as interrupted, and staff had to stop it by hand on the OCPP platform. Both admins list the sessions but neither could act on a running one.

**Decision.** The Zella app owns the charge, so it is the only system that talks to the charger. The Zella dashboard gets a `Stop` action (permission `SESSIONS_STOP`) and the NDZela admin gets one on `/charging` (permission `charging:stop`, V63, given to `SUPER_ADMIN`, `ADMIN` and `GESTIONNAIRE`). The NDZela backend does not call the charger: `POST /charging/sessions/{id}/stop` forwards the order to the Zella app with the shared partner key (`POST /v1/partners/ndzela/sessions/{transactionId}/stop`), which stops the charge exactly as the customer's stop button does and also accepts a charge it closed as "charger offline" in the last 3 hours, since the car may still be charging. Both sides write an audit entry (`CHARGING_SESSION_STOP_REQUESTED`, `PARTNER_SESSION_STOP_REQUESTED`, `SESSION_STOPPED_BY_ADMIN`). A refusal comes back with its code (`SESSION_NOT_ACTIVE`, `STOP_REFUSED`) and the screen explains it. The partner key can stop any session of its own country, linked driver or public customer, because NDZela runs the network.

**Consequences.** (+) Staff stop a runaway charge in two clicks, with a trace. (+) One stop path, so the same safeguards apply. (-) The NDZela stop depends on the Zella app being reachable (`ZELLA_UNAVAILABLE`). (-) The button only appears for sessions NDZela sees as running; a charge the Zella app closed by mistake shows as ended in NDZela and is stopped from the Zella dashboard.

**Alternatives considered.** NDZela calling the OCPP platform directly: rejected, it would bypass the Zella billing and settlement of the charge.
