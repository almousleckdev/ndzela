# Business overview

## What NDZela is

A **multi-country fleet and driver-contract platform**. The company **owns the vehicles** and puts them in the hands of drivers under a **contract**.
Each driver pays a **fixed daily rent (loyer journalier, in FCFA)** until the contract's duration or total price is reached. Staff manage cars,
drivers, contracts and payments in the web admin. Drivers use a **Flutter mobile app** to pay and follow their contract.

All the company's cars are **electric**, used by the drivers for **VTC activity** (the contract restricts use to VTC in Libreville, under the NDZELA brand). The company also **operates its own charging stations**, used by the drivers **and the public**, on a separate, already-built OCPP platform; managing that **charging billing and income** is a later module of this platform ([ocpp-integration.md](ocpp-integration.md)).
This is **not** a ride-hailing platform (there is no rider app), and [legacy-charging-network-spec.md](legacy-charging-network-spec.md) describes a different, obsolete product (kept for history; do not implement from it).

**Current focus: Gabon only, backend and web admin first.** The driver app, Mali and the charging billing come later.

## Tenants

Multi-tenant, **one tenant per country**: Mali and Gabon first, more later. Each country has its own currency (XOF for Mali, XAF for Gabon; shown to users as **FCFA**),
phone format, timezone, default language, **payment providers**, **legal entity data** (name, legal form, head office, RCCM, contacts) and **contract template with country law**. Data of one country is never visible to another country's staff.
See [../multi-tenancy/multi-tenancy.md](../multi-tenancy/multi-tenancy.md).

## Actors

| Actor | Uses | Scope |
| --- | --- | --- |
| **Super admin** | Web admin | Global: all countries, all data, roles and countries. (Role Implemented; country handling Decided.) |
| **Country staff** (admin, gestionnaire, comptable, caissier, viewer...) | Web admin | Exactly one country. (Roles seeded; scoping Decided.) |
| **Driver** | Flutter mobile app (later phase) | **Never registers themselves.** Staff create the driver, assign a car and a contract; the driver then installs the app and signs in with **phone or email + password**. Sees and pays only their own contract. **Decided**, not built. |

## Business documents

| Document | Governs |
| --- | --- |
| [functional-requirements.md](functional-requirements.md) | Module list, build order, all **open questions** |
| [vehicles.md](vehicles.md) | Adding and managing company cars |
| [drivers.md](drivers.md) | The 5-step driver creation wizard, fields and validations |
| [contracts.md](contracts.md) | Contract data, financial recap, contract PDF per country |
| [payments-and-arrears.md](payments-and-arrears.md) | Daily payments, providers per country, unpaid-day escalation, payments by day and receipts, suspension |
| [admin-quick-access.md](admin-quick-access.md) | The follow-up shortcuts (Suivi paiements, Gérer impayés, Historique...) |
| [ocpp-integration.md](ocpp-integration.md) | Charging billing and income (drivers and public) via the OCPP platform, later |
| [samples/contract-gabon.pdf](samples/contract-gabon.pdf) | Reference Gabon contract (test data) |

## Out of scope for now

- Charging-station operation (OCPP): a **separate platform already exists**. Only the future billing and income management is planned here ([ocpp-integration.md](ocpp-integration.md)).
- Public map, station reservation, ride-hailing, VTC marketplace.

## Glossary

| Term | Meaning |
| --- | --- |
| Tenant / country | An isolated data space (Mali, Gabon...). |
| Staff | People operating the platform through the web admin (`users` table). |
| Chauffeur / driver | Person holding a contract for a company car; pays daily. |
| Véhicule / car | Company-owned vehicle available for contracts. |
| Contrat | Rental with ownership transfer: car + driver + start date + duration (billable days) + daily rent + deposit (caution). Ownership passes to the driver if all days are paid and the contract is respected. |
| Jour facturé | Monday to Saturday; Sunday is a rest day, not billed. |
| Échéance | Daily rent is due before 12:00 the same day. |
| Loyer journalier | Fixed daily rent the driver owes (FCFA). |
| Caution | Deposit (FCFA). |
| Impayé / arrears | Unpaid daily rent. |
| Récapitulatif financier | Financial summary shown at the end of the contract step and on the contract. |
| Permission | One allowed action `resource:action`. Access is decided on permissions. |
