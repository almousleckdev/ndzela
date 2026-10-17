# Functional requirements and build order

Product overview: [README.md](README.md). Only the **admin foundation** exists (sign-in with MFA, users, roles/permissions, profile, notifications). No business module exists yet, although finance permissions (`transactions:*`, `accounts:*`, `cash:operate`, `reports:*`) are already seeded.

## Current scope (Decided 2026-09-30)

**Country: Gabon only. Phase: backend and web admin first. The driver mobile app and its design come later.** The platform stays multi-tenant (Mali later) but only Gabon is configured and built for now.

## Modules and order (each gets its own spec, then plan, then build)

| # | Module | Phase | Status | Documents |
| --- | --- | --- | --- | --- |
| 1 | **Tenancy foundation** (countries, country-scoped staff, global super admin, header filter, isolation tests; Gabon as the first country) | Now | **Built** (tenancy foundation plan, tasks 1 to 9) | [multi-tenancy](../multi-tenancy/multi-tenancy.md) |
| 2 | **Vehicles** (add car, recap, list, statuses) | Now | **Built** | [vehicles.md](vehicles.md) |
| 3 | **Drivers + contracts** (5-step wizard, financial recap, French contract PDF for Gabon, training-centre list) | Now | **Built**: drivers, assignment, training centres, contract terms, recap and the French Gabon PDF (Mali template pending) | [drivers.md](drivers.md), [contracts.md](contracts.md) |
| 4 | **Payments and arrears, phase 1** (ledger, staff-recorded cash/manual payments, arrears engine, 24/48/72 h levels, email warnings, automatic suspension state) | Now | **Built for Gabon, phase 1** (ledger, staff-recorded payments, arrears engine, 24/48/72 h levels, email warnings, automatic suspension, screens); caution, terminated-contract debts and PVit later | [payments-and-arrears.md](payments-and-arrears.md) |
| 5 | **Admin quick access** (Suivi paiements, Gérer impayés, Historique, Chauffeurs, Véhicules, Contrats, Rapports) | Now | Menu built: a "Finance" sidebar group (Suivi paiements, Gérer impayés, Historique) and "Recharge Énergie", as "Bientôt disponible" pages guarded by the provisional `transactions:read` until the payment permissions are created in round B; the audit page is now "Journal d'audit" | [admin-quick-access.md](admin-quick-access.md) |
| 6 | **Audit trail** (who did what and when; kept at least 5 years) | Now | **Built** (users, roles, sign-in, vehicles, drivers, assignments, training centres) | [database](../database/database.md), [api](../api/conventions.md) |
| 7 | **Driver mobile API and Flutter app**, in-app payments via **PVit** (Airtel Money, Moov Money Gabon), push notifications | Later | Decided (stack), design to be supplied | [mobile](../mobile/architecture.md) |
| 8 | **Real dashboard and reports** | Dashboard now, reports later | **Dashboard built** (2026-09-30): cards Chauffeurs and Véhicules from the backend, each shown only to users who can read that module; Paiements journaliers and Gestion recharge énergie show real figures (rounds B and C). Reports open | [admin-quick-access.md](admin-quick-access.md) |
| 9 | **Charging billing and income** (drivers and public, day/week/month/year) via the OCPP platform's APIs | Manual part now | **Built** (recharges recorded by staff, battery levels, stations, metrics, exports); OCPP import later | [ocpp-integration.md](ocpp-integration.md) |
| 10 | **Mali** (Orange Money, Moov Mali, its own aggregator, contract template, legal entity) | Later | Decided | [contracts.md](contracts.md) |

Design consequence: because the app comes later, driver records are created in phase 1 with their credentials (initial password hash, forced-change flag) but no driver sign-in API is built yet; rents are recorded by staff; warnings are **email only**.

## Decisions already taken by the user

**Platform and tenancy**
- One shared database, `country_id` on tenant-owned data, application-level filter (approach A). Staff belong to **exactly one country**; only the SUPER_ADMIN is global. **Roles are shared** across countries. The super admin has a **country filter in the header**.
- **Existing users belong to Gabon.** Country data: currency, phone code/format, timezone, language, active status, **payment providers, legal entity data and the contract template with country law**.

**Drivers and vehicles**
- Drivers **never self-register**: the **gestionnaire** creates them (5-step wizard, **all-or-nothing, no draft**), assigns a car and creates the contract; the driver later signs in with **email or phone + password only** and **must change the initial password** (min. 8 characters) at first login. The **ADMIN** suspends, blocks and reactivates drivers (every restriction needs a reason); the driver photo is mandatory and documents are uploads.
- Driver must be **18 or older** on the creation date. Civilité: Male/Female. ID type selected, then the number typed (no upload). Licence category: A, B, C or D. Training centre from a **managed list** (gestionnaire). ID number, licence number, phone, email **unique per country**. N° Certificat optional.
- Vehicles: **Marque, Modèle, Année, Immatriculation, Couleur, Prix d'achat, Loyer journalier, Numéro du véhicule** required; **VIN optional**; vehicle number and plate **unique**; statuses `AVAILABLE`, `ASSIGNED`, `IN_MAINTENANCE`, `OUT_OF_SERVICE`; the daily rent is fixed on the car. One driver = one contract; one car = one driver. The **gestionnaire** manages vehicles, drivers and contracts.

**Contracts and payments**
- Contract in **French**, generated after successful driver creation, **one template per country** (Gabon sample provided). **1 248 billable days** (Mon-Sat), daily rent **23 000 FCFA**, start date = creation date, number `NDZ-` + 8 digits.
- Deposit follows the contract: returned only if all is paid and the car is returned in good condition; kept on breach; used for the ownership-transfer costs. **Ownership transfers if all days are paid and no violation ended the contract** (a simple warning does not cancel it).
- Rent is due **Mon-Sat before 12:00 the next day**. Levels follow the **number of unpaid days** ([ADR-0019](../adr/0019-arrears-levels-follow-the-number-of-unpaid-days.md)): **1 day yellow "Rappel" + warning, 2 days red "Urgent" + urgent reminder + penalty (to be paid), 3 days or more deep red, automatic suspension**. Payments settle whole days, oldest first. **Cash is allowed.** Warnings by **push and email**.
- **Aggregator in Gabon: PVit** (Airtel Money, Moov Money Gabon).
- **Audit:** record who did what and when for every change to drivers, contracts, vehicles, payments (including cash), suspensions and role changes, **kept at least 5 years** (legal minimum to be checked with the accountant).
- **Quick access shortcuts:** Suivi paiements, Gérer impayés, Historique, Chauffeurs, Véhicules, Contrats, Rapports.

## Latest answers (2026-09-30)

- **72 hours** confirmed. **Prix total véhicule** = rent x days, computed. **NDZELA holds the PVit merchant accounts; the driver pays the fees.** Payments below one day's rent are kept as credit. Contract number is **sequential** per country. Documents provided = **checkboxes** in step 3.
- **Focus now: the backend and the web dashboard (admin).**

## Decisions from the last round (2026-09-30)

- **Saturday's rent is due Sunday 12:00** (same rule every day; configurable calendar per country).
- **Penalty:** amount to be provided later; per-country setting, **default 0**.
- **Remote vehicle blocking (GPS):** **not in this platform for now**; the dashboard shows an alert from 3 unpaid days and staff block the car in the GPS supplier's tool.

## Open questions still to answer

1. **PVit fee rate** and how it is added to the driver's payment (needed only with the later payment integration).
2. **Penalty amount** (setting left at 0 until given).
3. Hosting, environments, CI/CD, storage for contract PDFs, email provider (phase 1).
