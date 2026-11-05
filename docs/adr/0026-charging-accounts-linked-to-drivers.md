# ADR-0026: Charging accounts (idTags) are linked to drivers by e-mail and phone, looked up in the Zella app

**Status:** Accepted · **Date:** 2026-11-05

**Context.** The OCPP platform's sessions only carry an `idTag`. The link `idTag -> person` exists only in the Zella Charging app, which also serves public customers whose data must not reach NDZela. We want Sessions du réseau to show the driver and the car.

**Decision.**
- **A lookup, not a copy.** NDZela sends the drivers it wants resolved (lowercase e-mail, E.164 phone, batches of at most 200) to `POST /v1/partners/ndzela/id-tags/lookup` (`X-Api-Key`, key bound to one country, `ZELLA_BASE_URL`, `ZELLA_API_KEY`, `ZELLA_COUNTRY`). Zella answers `idTag`, `customerId`, names and `matchedOn`. Nobody NDZela did not name is learned.
- **Table `id_tags`** (V45, country-scoped, unique `(country_id, id_tag)`, one `LINKED` row per driver): `LINKED`, `SUGGESTED` (only one of e-mail and phone matched) or `UNLINKED` (refused by staff).
- **Auto-link only when e-mail and phone both matched.** A partial match is a suggestion that staff confirm. Only staff unlink; an unlinked or already-linked driver is never looked up or linked again by itself. The link follows the `idTag` and `customerId`, so a later change of e-mail or phone on either side does not break it.
- **When.** Right after a driver is created (a `DriverCreatedEvent`, handled after the commit and asynchronously) and by a job every `ZELLA_LOOKUP_INTERVAL` (15 min) for drivers neither linked nor refused. A Zella failure is logged and retried; it never fails a driver creation.
- **Shown at read time.** The driver comes from the linked `idTag` of each session, the car from the driver's assignment at the session's `startedAt` (`vehicle_assignments`). Nothing is stored per session, so linking applies to past sessions at once. An unlinked `idTag` is a public customer ("Public").
- **Permission `charging:link`** (ADMIN, GESTIONNAIRE, SUPER_ADMIN) for the review endpoints. Every link, suggestion and unlink is audited (`ID_TAG_LINKED`, `ID_TAG_SUGGESTED`, `ID_TAG_UNLINKED`).
- The Zella feed of what each driver paid (phase 3) is not part of this decision.

**Consequences.** (+) The driver and car show on every session with no new data from the OCPP platform; personal data shared is limited to our own drivers. (-) A driver who registered in Zella with other contact details stays "Public" until staff confirm the suggestion; a new dependency on the Zella API; the contracts must mention that name, e-mail and phone are shared with Zella.

**Alternatives.** Zella sending all its customers (rejected: public customers' data); matching on one field alone (rejected: wrong links); storing driver and car on each session (rejected: stale after a re-link).

**Amendment (2026-11-05): the contract Zella actually built** (their `INTEGRATION_CONTRACTS.md` section 11).
- The lookup takes `{drivers:[{ref,email,phoneE164}]}`, where `ref` is our driver id, and answers `{results:[{ref,matches:[{customerId,idTag,matchedOn,quality,firstName,lastName}]}]}`. `quality` is `FULL` (e-mail and phone; names sent) or `PARTIAL` (one field; no names). `idTag` is null until the customer's first charge, so **`customerId` is the stable key** (V46: `id_tag` optional, unique `(country_id, customer_id)`). A linked driver without an idTag is looked up again until it arrives.
- Zella's payment feed only serves **linked** customers, so we tell it: `PUT /links/{customerId}` after an automatic link or a staff confirmation, `DELETE` when staff unlink (idempotent, 204). The column `zella_notified` records what Zella knows and a failed call is retried by the next run.
- Only well-formed contacts are sent (one bad contact makes Zella refuse the whole batch of 200). Limit: 120 requests a minute per key, 429 with `Retry-After`.

**Amendment (2026-11-05): the payment feed (phase 3, built).**
- `ZellaFeedJob` reads `GET /sessions?cursor=&limit=200` every 30 s (`app.zella.feed-interval`) and stores each item in `session_payments` (V47, country-scoped, unique `(country_id, transaction_id)`; amounts in minor units; `payment_state` PENDING, CHARGED, NOT_CHARGED, UNDER_REVIEW or REFUNDED, `breakdown_estimated`). Items apply **by version** (`updatedAt`): a newer state replaces the old one (a refund is a state change on the same transaction), an older one is ignored, so replaying a page is harmless. The cursor is stored as received in `zella_sync_state` in the same transaction as the page; one run at a time across instances (Redis lock); a 429 pauses the feed for `Retry-After`; a refused cursor (`PARTNER_CURSOR_INVALID`) restarts from the beginning.
- The global cursor has already moved past the earlier sessions of a driver just linked, so once Zella has been told about the link we read that customer's history once with `?customerId=` and its own cursor (`id_tags.history_read`).
- Sessions answer `payment` (state, total, energy, VAT, service fee, method, `breakdownEstimated`, `capped`), matched by the transaction id. `capped` is true when the charge was limited to the authorised budget and the parts add up to more than the total, which is what the wallet really captured (`totalChargedMinor`). The dashboard shows the total and its state, a detail view for disputes, and the exports carry the total and the state.
- A session the wallet paid (`CHARGED`) is shown "Payée · Zella" and no longer offers "Enregistrer le paiement", which stays for payments taken outside the app. The recharge figures of the Recharges tab and the sessions band still use the calculated amount; counting wallet payments in the recettes is not decided.
