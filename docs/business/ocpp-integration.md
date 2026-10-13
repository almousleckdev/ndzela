# Charging billing and OCPP integration (future)

**Status: the manual part is built (2026-09-30, "Recharge Énergie"); the OCPP import is planned (answers received 2026-10-03, plan below, waiting for approval).**

## As built: recharges recorded by staff (migration V16)

- **Decided by the product owner:** nobody has a prepaid balance. **Drivers and the public pay for each recharge at once, at the station, with their own money**; NDZELA gives no credit and no card balance. The charging cost never enters the rent or the arrears rules.
- **Record** (`charging_entries`, country-owned, **append-only by trigger**): `RECHARGE` (an NDZELA vehicle, whose driver is filled in from its current assignment, **or** a public car given as plate or description, never both; an NDZELA vehicle without a driver is refused), the amount paid (whole FCFA), the method (the mobile-money operators of the country: Gabon Airtel and Moov, Mali Orange and Moov; the transaction reference is always required; cash and other are no longer accepted since 2026-10-02), the **battery level before and after** ("Solde après" is the battery percentage; after is not below before), optional kWh, the station and who recorded it. `REVERSAL` cancels one recharge with a reason (once only, admin). A client `requestId` and an advisory lock make a double submit record once.
- **Stations** (`stations`): a managed list per country (`charging:manage`); a station with records cannot be deleted.
- **Screen "Recharge Énergie":** metrics Recharges du jour, FCFA du jour, de la semaine (Monday to Sunday) and du mois (money received, cancelled recharges excluded, each country measured in its own timezone), **Comptes critiques** (distinct vehicles that arrived today at or below the critical battery level, `countries.charging_critical_percent`, default 20), **Régularisations** (recharges cancelled this month) and **Gestionnaires** (distinct staff who recorded recharges today); the table Heure, Véhicule, Chauffeur, Montant, Solde après, Gestionnaire, Station, Référence, Actions for a chosen day (critical arrivals tinted red); Exporter for a day, week, month or year as CSV with totals (audited); and the dashboard card.
- **Permissions:** `charging:read` (all finance roles and VIEWER), `charging:create` (GESTIONNAIRE, CAISSIER), `charging:reverse` (ADMIN), `charging:export` (ADMIN, COMPTABLE), `charging:manage` (GESTIONNAIRE, ADMIN); SUPER_ADMIN has all. **API:** `/api/v1/charging/recharges`, `/recharges/{id}/reverse`, `/summary`, `/export`, `/vehicles`, `/stations`. Audit: `CHARGING_RECORDED`, `CHARGING_REVERSED`, `CHARGING_EXPORTED`, `STATION_*`.
- **Still later:** the OCPP import (the optional kWh and reference fields are where it will land), tariffs and kWh pricing, taxes, invoices, a link to rent arrears, and the questions below that concern the import.

## Business context (clarified)

**NDZELA operates its own electric charging stations, and every company car is electric.** Both **the drivers and the public** use NDZELA stations to charge. The platform must therefore also **manage the charging billing and the resulting income**, reported by **day, week, month and year**. The charging stations run on a **separate OCPP platform that already exists**; this platform will **call the OCPP platform's APIs** to obtain the charging data once the earlier modules are done.

Related facts from the driver contract ([contracts.md](contracts.md)): the **charging cost is not included in the daily rent**; drivers must **charge only at NDZELA stations** and receive a **charging access card**; the deposit is meant to cover, among others, "crédit de recharge électrique".

So this module covers **two customer types**: drivers (contract holders) and **public customers**. The legacy charging spec ([legacy-charging-network-spec.md](legacy-charging-network-spec.md)) is obsolete and must not be used, but a few of its themes (charging sessions, tariffs, payment, reports) will return here and be re-specified.

## Constraints already known

- Integration is **backend to backend**; the web admin and the driver app never call the OCPP platform directly.
- OCPP credentials live in secrets; billing data imported is **tenant-scoped** by country like everything else.
- Financial data is stored in the same **append-only ledger** approach as rents ([database](../database/database.md)).

## The OCPP engineer's answers (2026-10-03)

- **Access:** base URL `https://zella-ocpp.com` (UAT and production share the host); server-to-server `X-API-Key` header, no OAuth; **60 requests per minute** per key (HTTP 429 with `Retry-After`); a Swagger/OpenAPI document exists and is exchanged separately with the credentials. The key lives only in `backend/.env`, never in the dashboard or the apps.
- **Stations:** the Partner API lists charge points and connectors with their status; each connector has a QR token resolved by `GET /api/v1/c/{token}` to `chargePointId` and `connectorId`. Commercial labels stay on our side (`stations`).
- **Sessions (the main source):** `GET /api/v1/sessions`, `/sessions/{transactionId}`, `/sessions/{transactionId}/metering`, `GET /api/v1/events`. `transactionId` is the unique reference of a recharge. A session carries `chargePointId`, `connectorId`, `idTag`, start and end times, status, measured energy, stop reason, `costMinor`, currency and tariff.
- **Amount:** the engine computes `costMinor` from the measured energy and the tariff (Gabon: 350 XAF per kWh, **before VAT**). It is **not** the amount paid: payment status, method, the mobile-money transaction, receipts and refunds belong to the charging application's payment system.
- **Battery:** state of charge is optional (the charger or the car may not send it), so before and after must be nullable.
- **Vehicle and driver:** the OCPP platform knows neither. `idTag` is only a session correlation key. We must keep `idTag -> vehicle -> driver` ourselves. The recommended chain with the Zella application is `orderId -> idTag -> transactionId`.
- **Real time:** webhooks `session.started`, `session.metering`, `session.stopped`, delivered at least once (deduplicate on `eventId`). Recovery by `events` and `sessions` with overlapping windows; there is no checkpoint protocol.
- **Statistics:** computed by us from the sessions (counts, kWh, amounts, per station, vehicle, driver, period, durations, failed or abandoned sessions).
- **Public customers:** OCPP cannot tell a fleet driver from a public customer; that link, and the payment, come from the application layer.

## Plan (to be approved before any code)

1. **Phase 1, the technical source (built 2026-10-03 and 2026-10-13, [ADR-0017](../adr/0017-ocpp-sessions-from-the-fleet-api-sync.md)).** Sessions are stored in the country-scoped table `charging_sessions` (unique `transaction_id`, charge point, connector, `id_tag`, times, status, energy, duration, `cost_minor`, currency and decimals, price per kWh, stop reason, nullable battery levels, the platform's `updatedAt`, the raw item). They are filled **only** by the Fleet API's incremental sync (`GET /api/v1/sessions/sync`, `X-API-Key`, opaque cursor stored as received, resumed after a restart); the webhook only asks for a sync. Shown, with kWh and the technical amount, in the Recharges screen ("Sessions du réseau"). From a finished session, staff record its payment as a recharge (the form is prefilled; [ADR-0017](../adr/0017-ocpp-sessions-from-the-fleet-api-sync.md)). The battery level comes from the per-session metering (see ADR-0017). The screens refresh by themselves. Connector sync into `stations` is still to do.
2. **Phase 2, who charged.** An `id_tags` table (`id_tag -> vehicle`, driver taken from the vehicle's assignment at that time) so sessions show the vehicle and driver and the per-vehicle and per-driver statistics work.
3. **Phase 3, money.** Link each session to what was paid, from the charging application's payment system (amount, method, mobile-money reference, status). The manual recharge entry stays for payments taken outside the application. Until then the screen shows the technical amount and says it is not proof of payment.

## The Fleet API contract (2026-10-13)

`https://zella-ocpp.com`, header `X-API-Key` (no bearer token). `GET /api/v1/charge-points`, `GET /api/v1/sessions`, `GET /api/v1/sessions/{transactionId}`, `GET /api/v1/sessions/sync?limit=1..1000&cursor=`. `cursor` is opaque (store exactly, never parse) and cannot be combined with `updatedSince` (only for a fresh bootstrap). Answer `{items, nextCursor, hasMore}`; an item has `transactionId, deviceId, connectorId, idTag, status, startedAt, stoppedAt, energyKwh, durationSeconds, stopReason, costMinor, currency, currencyDecimals, pricePerKwhMinor, updatedAt`, any of which may be null. Our credential is limited to a granted charge point (`DY202609110007`) and to sessions starting from 2026-10-03T00:00:00Z, so an empty `items` is a valid answer. Verified read-only against the live API (`OcppFleetLiveTest`, opt-in).

**Operating it.** The sync state is `ocpp_sync_state` (cursor, last attempt, last success, last error). To re-read everything the key may see, set `sync_cursor` to null; it is safe because sessions are upserted by `transactionId`. **Still to ask the platform:** what an invalid or expired cursor returns, the list of `status` values, and whether 60 requests a minute still holds.

## Still open (to ask)

1. The Swagger/OpenAPI file and the API key (to be sent by a secure channel, then put in `backend/.env`).
2. **Webhooks:** how a delivery is authenticated (signature or shared secret), and which public URL they will call.
3. **The charging application:** who runs it, how the `idTag` is chosen when a user scans and pays, and whether it has an API or a webhook that gives, per `orderId`, the amount paid, the method, the mobile-money reference and the user. Without it, phases 2 and 3 are not possible.
4. How a driver's `idTag` is tied to an NDZELA driver (for example the app user's phone number matching the driver's).
5. Whether VAT applies to what the customer pays, since the engine amount is before VAT.
6. Whether the Mali stations will share the same API host and key.
