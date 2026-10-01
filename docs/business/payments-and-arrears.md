# Payments and arrears (impayés)

**Status: Built for Gabon, phase 1 (2026-09-30): staff-recorded payments, ledger, arrears engine with the 24/48/72 h levels, email warnings, automatic suspension, and the Finance screens. PVit and the driver app come later. Rules below remain the reference.**

## As built (migration V15)

- **Ledger** `ledger_entries` (country-owned, **append-only: a database trigger refuses UPDATE and DELETE**): `PAYMENT` (credit), `REVERSAL` (cancels one payment or waiver, with a reason; unique per reversed row), `WAIVER` (rent forgiven by an administrator, with a reason, a credit that settles the oldest charges), `PENALTY` (charge, one per unpaid day, `UNIQUE (contract_id, business_day)`). Whole-FCFA amounts, `amount > 0`. A client `requestId` (unique per country) makes a double submit record one payment; the driver row is locked while recording, so simultaneous identical requests collapse to one.
- **Methods:** cash, Airtel Money, Moov Money, other (no bank transfer); a reference is required except for cash. **No backdating**: a payment is dated when it is recorded; a mistake is reversed and re-recorded. Recording needs an **active contract** (`NO_ACTIVE_CONTRACT`).
- **Nothing is stored per day.** `AccountCalculator` (pure) derives the account from the contract (start, end, rent), the leaves and the ledger at a given instant: rent days Monday to Saturday from the day after the start, skipping leave days, each due **the next day at 12:00 in the country's timezone**; charges (rents and penalties) are settled **oldest first, whole charges only** by the net credits (payments and waivers not reversed); the rest of a partial payment stays as **credit**. Output: unpaid charges, overdue amount, credit, oldest unpaid deadline, level, per-day status (paid on time, paid late, unpaid) and the counts.
- **Levels** from the oldest unpaid deadline: `LATE` (under 24 h), `YELLOW` (24 h), `DARK_YELLOW` (48 h), `RED` (72 h). The level, overdue amount and "since" are copied onto the driver row (`arrears_level`, `overdue_amount`, `overdue_since`) by the payment feature, so the Chauffeurs list filters "En retard" without the driver feature depending on payments.
- **Job** (`ArrearsJob`, every 5 minutes, `app.arrears.job-enabled`, `app.arrears.cron`): per active country, the tenant context set explicitly, per active contract in its own transaction (`ArrearsEvaluator`). Each step is recorded once per (contract, unpaid day, level) in `arrears_events` (`INSERT ... ON CONFLICT DO NOTHING`), so emails, notifications and the suspension never repeat, even with two servers. 24 h: warning email; 48 h: urgent email and the **penalty** (`countries.late_penalty`, **0 by default**, so no row is created until set); 72 h: email, **automatic suspension** of an active or inactive driver (reason "Impayés...", audited as DRIVER_SUSPENDED by `system`; a suspended or blocked driver is left alone) and a notification to the holders of `arrears:manage` ("bloquez le véhicule dans l'outil GPS"). Only one email per contract per run, for the highest new step.
- **Paying everything clears the level but not the suspension**: only an administrator reactivates (`drivers:suspend`). An administrator can also grant a delay (waiver).
- **Screens:** Suivi paiements (day's collection, total overdue, tabs by level, record a payment), Gérer impayés (overdue drivers, oldest first, red alert banner, record a payment, grant a delay), Historique (ledger with filters, reversal, CSV export), a Paiements card on each driver (level, amount due, credit, on time / late / unpaid, last 14 days, last entries), "En retard" metric, tab and row colours on Chauffeurs, and the dashboard's Paiements card.
- **Permissions:** `payments:read` (all finance roles and VIEWER), `payments:create` (GESTIONNAIRE, CAISSIER, SUPER_ADMIN), `payments:reverse` and `payments:export` (ADMIN, SUPER_ADMIN; export also COMPTABLE), `arrears:manage` (ADMIN, SUPER_ADMIN).
- **API:** `GET /payments/accounts?filter=&q=`, `GET /payments/accounts/{driverId}`, `GET /payments/summary`, `GET /payments/history`, `GET /payments/export`, `POST /payments`, `POST /payments/{id}/reverse`, `POST /arrears/waivers`. Audit: `PAYMENT_RECORDED`, `PAYMENT_REVERSED`, `ARREARS_WAIVED`, `ARREARS_LEVEL_REACHED`, `PAYMENTS_EXPORTED`.
- **Not in this round:** the caution (deposit refund or retention), unpaid rent on contracts ended by a vehicle removal (the job tracks active contracts only), PVit and driver-app payments, the restricted session of a suspended driver, the inactivity alert.

## Daily rent

- Amount: the car's daily rent (sample: **23 000 FCFA**), due **Monday to Saturday**; **Sunday is not billed** ([contracts.md](contracts.md)).
- **Deadline: before 12:00 (noon) of the next day**, country timezone (Gabon: Africa/Libreville). Unpaid after the deadline = **impayé**.
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

Payments settle **whole daily rents, oldest first**. If the driver owes **3 days** and pays **1 day**, **2 days remain** and the driver stays in arrears; the account returns to normal only when **everything due is paid** (the contract: unblocking only after full settlement). The **penalty** (day 2) also has to be paid. A payment below one daily rent is accepted and kept as **credit** that counts once it reaches a full day (Decided).

## Escalation (Decided levels; timing to reconcile)

Your rule: **unpaid 24 h = warning + yellow row; 48 h = dark yellow; 72 h = red and urgent; beyond that: the account is suspended automatically and the driver's login redirects to payment.** Warnings go by **push notification and email** (Decided; no SMS).

The contract's sanctions table (art. 7) states the same idea by day:

| Contract | Consequence in the contract | Your levels |
| --- | --- | --- |
| Day 1 unpaid | Warning by notification | 24 h: yellow + warning |
| Day 2 unpaid | **Urgent reminder + penalty** | 48 h: dark yellow |
| Day 3 unpaid | **Remote blocking of the vehicle**; NDZELA may keep the deposit and recover the car | 72 h: red, urgent |
| After day 3 (4th day) | Right to withdraw the vehicle and keep the deposit; unblocking only after full settlement | Suspension of the app account |

Decided: the suspension is **automatic and enforced by the backend**; a suspended driver only reaches the payment flow; the admin driver-list colours come from the same server-side state.

### Decisions from the latest answers

- **Clock (Decided):** the rent of day D is due **D+1 at 12:00** (Gabon: Africa/Libreville). Rents are due **Monday to Saturday**; Sunday is not a payment or billing day. Levels are counted from the moment the deadline passes: **+24 h yellow + warning, +48 h dark yellow + urgent reminder + penalty, +72 h red/urgent**, then automatic **suspension** (**72 hours**, confirmed).
- **Penalty:** the driver must pay it and must pay every day. **Amount or formula: not provided yet.** Modelled as a per-country configurable value, default 0 until the product owner sets it.
- **Who lifts a suspension or grants a delay:** the **ADMIN** role (Decided), with SUPER_ADMIN.
- **Warnings:** push and email. Until the driver app exists only **email** can be sent.

### Still open

1. **Saturday's rent is due Sunday 12:00** (Decided): the rule is the same every day (due before 12:00 the next day); Sunday itself carries no rent. The deadline calendar is configurable per country.
2. **Penalty**: amount **to be provided later**; a per-country setting, **default 0** (Decided).
3. **Remote vehicle blocking and GPS** (cars have a GPS and a remote-blocking box): **out of this platform for now** (Decided). The dashboard raises a clear **"block the car" alert** at +72 h; staff block the car in the GPS supplier's own tool. A later integration with the supplier is possible.
4. **Inactivity clause** (6 consecutive inactive days: NDZELA may recover the car): **Proposed** an alert for staff only.
5. **Suspended driver experience** (driver app, later): after login only a payment screen with the amount due.

## Backend constraints (binding once built)

- Arrears state and level are **computed on the server** by a scheduled job per country (timezone-aware, Monday-Saturday calendar, noon deadline) and derived consistently for the admin list and the app; the job sets the tenant context explicitly ([multi-tenancy](../multi-tenancy/multi-tenancy.md) rule 8).
- Each escalation step is **idempotent** (never send the same warning twice), produces a notification (push + email) and an **audit record**.
- Sign-in of a suspended driver returns a **restricted session** limited to the payment flow, not a generic rejection.
- Money: exact decimals/minor units with the country's currency; a payment or reversal is always a new ledger row.
