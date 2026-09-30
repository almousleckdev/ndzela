# Payments and arrears (impayés)

**Status: rules Decided from the product owner's answers and the Gabon contract; remaining gaps listed below. Not built. Focus: Gabon, backend and admin first.**

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
