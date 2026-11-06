# Payments and arrears (impayés)

**Status: Built for Gabon, phase 1 (2026-09-30): staff-recorded payments, ledger, arrears engine whose levels follow the number of unpaid days (1 reminder, 2 urgent, 3 or more suspension), email warnings, automatic suspension, and the Finance screens. PVit and the driver app come later. Rules below remain the reference.**

## As built (migration V15)

- **Ledger** `ledger_entries` (country-owned, **append-only: a database trigger refuses UPDATE and DELETE**): `PAYMENT` (credit), `REVERSAL` (cancels one payment or waiver, with a reason; unique per reversed row), `WAIVER` (rent forgiven by an administrator, with a reason, a credit that settles the oldest charges), `PENALTY` (charge, one per unpaid day, `UNIQUE (contract_id, business_day)`). Whole-FCFA amounts, `amount > 0`. A client `requestId` (unique per country) makes a double submit record one payment; the driver row is locked while recording, so simultaneous identical requests collapse to one.
- **Methods:** cash, Airtel Money, Moov Money, other (no bank transfer); a reference is required except for cash. **No backdating**: a payment is dated when it is recorded; a mistake is reversed and re-recorded. Recording needs an **active contract** (`NO_ACTIVE_CONTRACT`).
- **Nothing is stored per day.** `AccountCalculator` (pure) derives the account from the contract (start, end, rent), the leaves and the ledger at a given instant: rent days Monday to Saturday from the day after the start, skipping leave days, each due **the same day at 12:00 in the country's timezone** (open for payment from 00:00, [ADR-0029](../adr/0029-rent-is-due-the-same-day-at-noon.md)); charges (rents and penalties) are settled **oldest first, whole charges only** by the net credits (payments and waivers not reversed); the rest of a partial payment stays as **credit**. Output: unpaid charges, overdue amount, credit, oldest unpaid deadline, level, per-day status (paid on time, paid late, unpaid) and the counts.
- **Levels** follow the **number of unpaid rent days** (changed 2026-10-14 to match the previous platform and the contract's sanctions table): 1 unpaid day = `YELLOW` (reminder), 2 = `DARK_YELLOW` (urgent), 3 or more = `RED` (suspension). `LATE` (under 24 h) is no longer produced. A rent day is unpaid once its deadline (D 12:00) has passed and the credits do not cover it. The level, overdue amount and "since" (the oldest unpaid deadline) are copied onto the driver row (`arrears_level`, `overdue_amount`, `overdue_since`) by the payment feature, so the Chauffeurs list filters "En retard" without the driver feature depending on payments.
- **Job** (`ArrearsJob`, every minute, `app.arrears.job-enabled`, `app.arrears.cron`): per active country, the tenant context set explicitly, per active contract in its own transaction (`ArrearsEvaluator`). Each step is recorded once per (contract, unpaid day, level) in `arrears_events` (`INSERT ... ON CONFLICT DO NOTHING`), so emails, notifications and the suspension never repeat, even with two servers. 1st unpaid day: warning email; 2nd: urgent email and the **penalty** (`countries.late_penalty`, **0 by default**, so no row is created until set); 3rd: email, **automatic suspension** of an active or inactive driver (reason "Impayés...", audited as DRIVER_SUSPENDED by `system`; a suspended or blocked driver is left alone) and a notification to the holders of `arrears:manage` ("bloquez le véhicule dans l'outil GPS"). Only one email per contract per run, for the highest new step.
- **Paying everything clears the level but not the suspension**: only an administrator reactivates (`drivers:suspend`). An administrator can also grant a delay (waiver).
- **Screens:** Suivi paiements (day's collection, total overdue, tabs by level, record a payment), Gérer impayés (overdue drivers, oldest first, red alert banner, record a payment, grant a delay), Historique (ledger with filters, reversal, Excel or PDF export), a Paiements card on each driver (level, amount due, credit, on time / late / unpaid, last 14 days, last entries), "En retard" metric, tab and row colours on Chauffeurs, and the dashboard's Paiements card.
- **Permissions:** `payments:read` (all finance roles and VIEWER), `payments:create` (GESTIONNAIRE, CAISSIER, SUPER_ADMIN), `payments:reverse` and `payments:export` (ADMIN, SUPER_ADMIN; export also COMPTABLE), `arrears:manage` (ADMIN, SUPER_ADMIN).
- **API:** `GET /payments/accounts?filter=&q=`, `GET /payments/accounts/{driverId}`, `GET /payments/summary`, `GET /payments/history`, `GET /payments/export`, `POST /payments`, `POST /payments/{id}/reverse`, `POST /arrears/waivers`. Audit: `PAYMENT_RECORDED`, `PAYMENT_REVERSED`, `ARREARS_WAIVED`, `ARREARS_LEVEL_REACHED`, `PAYMENTS_EXPORTED`.
- **Not in this round:** the caution (deposit refund or retention), unpaid rent on contracts ended by a vehicle removal (the job tracks active contracts only), PVit and driver-app payments, the restricted session of a suspended driver, the inactivity alert.

## Daily rent

- Amount: the car's daily rent (sample: **23 000 FCFA**), due **Monday to Saturday**; **Sunday is not billed** ([contracts.md](contracts.md)).
- **Deadline: before 12:00 (noon) of the same day**, country timezone (Gabon: Africa/Libreville). Unpaid after the deadline = **impayé**.
- Payment platform in the contract: **my-ndzela** (the driver app). Charging electricity is **not** included ([ocpp-integration.md](ocpp-integration.md)).

## How drivers and staff pay (Decided)

| Channel | Decision |
| --- | --- |
| Driver in the app (later phase) | Mobile money: **Gabon: Airtel Money, Moov Money Gabon** (via PVit); Mali (later): Orange Money, Moov Money Mali. |
| Integration | Through an **aggregator**. **Gabon: PVit** (Decided). The Mali aggregator is decided later. |
| Merchant accounts and fees | **NDZELA holds the merchant accounts; the driver pays the aggregator's fees** (Decided). The fee is shown to the driver and recorded as its own ledger line, separate from the rent (fee rate per provider: Open, needed with the payment integration). |
| Staff | **Cash and other manual payments** can also be recorded (Decided), by staff holding the payment permissions (caissier/gestionnaire; matrix Open). |

**Phasing (Decided scope: backend and admin first, Gabon only).** The driver app comes later, so in the first phase payments are **recorded by staff** (cash and manual). The PVit integration and in-app payment come with the driver app. The ledger and arrears engine are built in the first phase so that the later channels plug into them.

Every payment is a **ledger row** (append-only, exact decimal amounts, country currency). A mobile-money payment is confirmed by the aggregator's callback/webhook, never by the app ([mobile](../mobile/architecture.md)). A cash payment is recorded with who, when and amount, and is audited.

## Settlement of arrears (Decided)

Payments settle **whole daily rents, oldest first** ([ADR-0021](../adr/0021-day-based-rent-payments-and-receipts.md)). **In the driver app:** with 1 or 2 unpaid days the driver chooses how many days to pay (1..N, the oldest first, never skipping a day); with **3 or more** all of them must be paid in one payment. The server computes the options and the amount (a day's price is its rent plus its penalty, minus credit already held); the app sends only a number of days. The **penalty** (day 2) is settled together with its day. A payment below one daily rent is accepted and kept as **credit** that counts once it reaches a full day (Decided). Staff-recorded payments are unchanged: they record what was received.

**Which day a payment paid.** Each recorded payment freezes the days (and penalties) it settled in `ledger_entry_days` (V35, append-only), computed at that instant. Example: Monday and Tuesday are unpaid, the driver pays one day on Wednesday at 11:05: the snapshot says Monday, the receipt reads "Loyer du lundi … payé le mercredi … à 11:05", the history and the driver card show Monday paid at 11:05 and Tuesday unpaid. A reversal later changes balances, never an issued receipt. Days are allocated from the amount PVit **confirmed**, not the amount asked; a shortfall is credit and is audited `PAYMENT_UNDERPAID`.

**The payment week.** The week's rents (Monday to Saturday) are due by **Sunday 12:00** (Saturday's deadline). `weekClosesAt` and `weekSettled` are exposed on the account; the app warns from Friday, the driver card shows "Semaine réglée / non réglée". Nothing new blocks a driver.

**Receipt.** One server-drawn ticket-style PDF (80 mm wide) per confirmed or recorded payment: company, number, date and time, driver, contract, operator, masked phone, transaction, a line per rent day with the exact payment time, penalties, credit kept, fee, total, a QR code that opens the receipt's public page on the website ([ADR-0030](../adr/0030-receipt-qr-opens-a-public-payment-page.md)). **No RCCM.** Staff download it from the Transactions table ("Reçu"); a reversed payment's receipt says ANNULÉ.

**Former riders** are the archived (`INACTIVE`) drivers: Chauffeurs has the tabs Actuels and Anciens, and the Transactions table a Chauffeurs filter (`scope=CURRENT|FORMER`).

## Escalation (Decided; reconciled with the contract on 2026-10-14)

**The level follows the number of unpaid days, as on the previous platform's "Gestion des impayés" screen:** 1 unpaid day = **Rappel** (yellow row, reminder e-mail); 2 = **Urgent** (red row, urgent e-mail; a penalty applies to a day unpaid for more than 48 h when the country sets one); 3 or more = **Suspendu** (deep red row, e-mail, the driver is **suspended automatically**, staff are notified to block the vehicle in the GPS tool). Warnings go by **e-mail and in-app notification** (no SMS). Each step is recorded **once** per contract, rent day and level: the day that made the count reach 1, 2 and 3.

This is the contract's article 7 by day (the earlier 24/48/72-hour timing is dropped):

| Contract | Consequence in the contract | Level |
| --- | --- | --- |
| Day 1 unpaid | Warning by notification | Rappel (yellow) |
| Day 2 unpaid | **Urgent reminder + penalty** | Urgent (red) |
| Day 3 unpaid or more | **Remote blocking of the vehicle**; NDZELA may keep the deposit and recover the car; unblocking only after full settlement | Suspendu (deep red), account suspended |

Decided: the suspension is **automatic and enforced by the backend**; a suspended driver only reaches the payment flow; the admin driver-list colours come from the same server-side state.

### Rent awaiting its deadline (2026-10-14, ADR-0019 addendum)
Yesterday's rent is *awaiting* until its deadline (12:00). It is shown in "À régler avant midi" and counted in "À recouvrer (hier inclus)" but does not change the level. Riders who will reach 3 unpaid days at noon are counted as *at risk* and announced by a banner. The list is sorted by unpaid days, most first; archived (`INACTIVE`) riders are excluded. The job runs every minute and the pages refresh every 15 s.

### Decisions from the latest answers

- **Clock (Decided):** the rent of day D is due **D at 12:00** and can be paid from D 00:00 (Gabon: Africa/Libreville). Rents are due **Monday to Saturday**; Sunday is not a payment or billing day. Levels follow the number of unpaid days once their deadline has passed (ADR-0019): **1st yellow + warning, 2nd dark yellow + urgent reminder + penalty, 3rd red/urgent** with automatic **suspension**.
- **Penalty:** the driver must pay it and must pay every day. **Amount or formula: not provided yet.** Modelled as a per-country configurable value, default 0 until the product owner sets it.
- **Who lifts a suspension or grants a delay:** the **ADMIN** role (Decided), with SUPER_ADMIN.
- **Warnings:** push and email. Until the driver app exists only **email** can be sent.

### Still open

1. **Saturday's rent is due Saturday 12:00** (Decided 2026-11-06, ADR-0029): the rule is the same every day (due before 12:00 the same day); Sunday itself carries no rent. The week stays open until **Sunday 12:00, the last warning**; Monday starts a new week. The deadline calendar is configurable per country.
2. **Penalty**: amount **to be provided later**; a per-country setting, **default 0** (Decided).
3. **Remote vehicle blocking and GPS** (cars have a GPS and a remote-blocking box): **out of this platform for now** (Decided). The dashboard raises a clear **"block the car" alert** at the 3rd unpaid day; staff block the car in the GPS supplier's own tool. A later integration with the supplier is possible.
4. **Inactivity clause** (6 consecutive inactive days: NDZELA may recover the car): **Proposed** an alert for staff only.
5. **Suspended driver experience** (driver app, later): after login only a payment screen with the amount due.

## Backend constraints (binding once built)

- Arrears state and level are **computed on the server** by a scheduled job per country (timezone-aware, Monday-Saturday calendar, noon deadline) and derived consistently for the admin list and the app; the job sets the tenant context explicitly ([multi-tenancy](../multi-tenancy/multi-tenancy.md) rule 8).
- Each escalation step is **idempotent** (never send the same warning twice), produces a notification (push + email) and an **audit record**.
- Sign-in of a suspended driver returns a **restricted session** limited to the payment flow, not a generic rejection.
- Money: exact decimals/minor units with the country's currency; a payment or reversal is always a new ledger row.
