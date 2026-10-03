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

1. **Phase 1, the technical source (built 2026-10-03: storage, webhook and the "Sessions du réseau" tab; the recovery job and the connector sync wait for the Swagger document).** Store every session in a new country-scoped table `charging_sessions` (unique `transaction_id`, charge point, connector, `id_tag`, times, status, energy, `cost_minor`, currency, stop reason, nullable battery before and after, raw event id). Fill it by a webhook endpoint (deduplicated by `eventId`, secret checked) and by a recovery job that re-reads `sessions` with overlapping windows, within the 60 requests per minute. Sync the connectors into `stations`. Show the sessions, with kWh and the technical amount, in the Recharges screen.
2. **Phase 2, who charged.** An `id_tags` table (`id_tag -> vehicle`, driver taken from the vehicle's assignment at that time) so sessions show the vehicle and driver and the per-vehicle and per-driver statistics work.
3. **Phase 3, money.** Link each session to what was paid, from the charging application's payment system (amount, method, mobile-money reference, status). The manual recharge entry stays for payments taken outside the application. Until then the screen shows the technical amount and says it is not proof of payment.

## Still open (to ask)

1. The Swagger/OpenAPI file and the API key (to be sent by a secure channel, then put in `backend/.env`).
2. **Webhooks:** how a delivery is authenticated (signature or shared secret), and which public URL they will call.
3. **The charging application:** who runs it, how the `idTag` is chosen when a user scans and pays, and whether it has an API or a webhook that gives, per `orderId`, the amount paid, the method, the mobile-money reference and the user. Without it, phases 2 and 3 are not possible.
4. How a driver's `idTag` is tied to an NDZELA driver (for example the app user's phone number matching the driver's).
5. Whether VAT applies to what the customer pays, since the engine amount is before VAT.
6. Whether the Mali stations will share the same API host and key.
