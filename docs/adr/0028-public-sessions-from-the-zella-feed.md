# ADR-0028: Sessions of other Zella customers reach NDZela as "Public", without identity

**Status:** Accepted · **Date:** 2026-11-06

**Context.** The charging pages must tell NDZela's own drivers (and the car they rent) from other people charging at the same stations. NDZela does not call the OCPP platform; the Zella app is the only source ([ADR-0017](0017-ocpp-sessions-from-the-fleet-api-sync.md) addendum), and its partner feed only served the customers linked to NDZela.

**Decision.** Zella adds `GET /v1/partners/ndzela/public-sessions` (same key, same cursor paging) for the sessions of customers **not** linked to NDZela. An item carries what was charged (station, connector, times, energy, battery, power, price and the amount breakdown) and **never who**: no customer id, no idTag, no name, e-mail or phone. NDZela reads it with its own cursor (`zella_sync_state.public_sync_cursor`) after the drivers' feed, stores the sessions in `charging_sessions` with no idTag, and keeps no driver payment for them. A session is a **driver's** when its idTag is linked to one of NDZela's drivers, otherwise **public**: `GET /charging/sessions`, `/summary` and `/export` take `kind=ALL|DRIVER|PUBLIC`, and the pages show a "Chauffeur NDZela" or "Public" badge.

**Consequences.** (+) The NDZela rental fleet and the public traffic are told apart everywhere, with totals per kind. (-) NDZela learns when and where other customers charge and what they were billed, though not who they are; this was decided by the product owner. A customer who later becomes a NDZela driver leaves the public feed and joins the driver's, so their earlier sessions are read again under their own cursor.
