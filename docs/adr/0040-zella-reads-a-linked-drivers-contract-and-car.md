# ADR 0040: The Zella app reads a linked driver's contract and car

Date: 2026-11-16. Status: accepted.

## Context

The Zella Charging dashboard shows, for a customer linked to NDZela, who the driver is, their contract number and the
car they hold. That data belongs to NDZela. Until now only NDZela called Zella (id tag lookups, payment feed, commands).

## Decision

NDZela exposes one read-only endpoint, `GET /api/v1/partners/zella/customers/{customerId}/driver`.

- It follows the pattern of the OCPP webhook: the route is public for the JWT filters and the controller checks a shared
  secret (`X-Api-Key` against `ZELLA_PARTNER_KEY`, constant-time compare). An empty key refuses every call.
- It answers for the country of `app.zella.country-code` only, through the same tenant call as the Zella feed job.
- Only a LINKED id tag is answered. The response holds the driver's name, phone, e-mail and status, the active contract
  (number, status, period, daily rent) and the car (fleet number, plate, brand, model, year, colour). Identity documents,
  address, birth data, VIN and purchase price never leave NDZela.
- Rate limited to 120 requests a minute (Redis).

## Consequences

- A second shared key to rotate (`ZELLA_PARTNER_KEY`, the mirror of `ZELLA_API_KEY`).
- Zella keeps no copy: the page shows NDZela's current data, and "unavailable" while NDZela is down.
- Covered by `ZellaPartnerDriverTest`.
