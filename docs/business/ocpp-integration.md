# Charging billing and OCPP integration (future)

**Status: Decided as a later module. Nothing to build now.**

## Business context (clarified)

**NDZELA operates its own electric charging stations, and every company car is electric.** Both **the drivers and the public** use NDZELA stations to charge. The platform must therefore also **manage the charging billing and the resulting income**, reported by **day, week, month and year**. The charging stations run on a **separate OCPP platform that already exists**; this platform will **call the OCPP platform's APIs** to obtain the charging data once the earlier modules are done.

Related facts from the driver contract ([contracts.md](contracts.md)): the **charging cost is not included in the daily rent**; drivers must **charge only at NDZELA stations** and receive a **charging access card**; the deposit is meant to cover, among others, "crédit de recharge électrique".

So this module covers **two customer types**: drivers (contract holders) and **public customers**. The legacy charging spec ([legacy-charging-network-spec.md](legacy-charging-network-spec.md)) is obsolete and must not be used, but a few of its themes (charging sessions, tariffs, payment, reports) will return here and be re-specified.

## Constraints already known

- Integration is **backend to backend**; the web admin and the driver app never call the OCPP platform directly.
- OCPP credentials live in secrets; billing data imported is **tenant-scoped** by country like everything else.
- Financial data is stored in the same **append-only ledger** approach as rents ([database](../database/database.md)).

## Open questions (for when this level is reached)

1. How are drivers charged for charging: added to what they owe (with the daily rent), a separate wallet/prepaid credit, or invoiced? Does it count in the arrears/suspension rules?
2. How does a **public customer** pay (mobile money, card, prepaid) and how are they identified: accounts, app, card, QR code?
3. Which identifier links a driver or vehicle to a charging session in the OCPP platform?
4. Does the OCPP platform provide day/week/month/year aggregates, or must this platform aggregate raw sessions? Pull (scheduled) or push (webhook)?
5. Tariffs (per kWh, per duration, by power), taxes, and who defines them per country.
6. Reports and income views needed (per station, per period, per customer type).
7. OCPP API documentation, authentication, sandbox, and whether it is one instance per country.
