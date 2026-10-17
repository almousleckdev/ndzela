# Admin quick access (Accès rapide)

**Status: Suivi paiements, Gérer impayés and Historique are built (2026-09-30, see [payments-and-arrears.md](payments-and-arrears.md)); the rest of the list is Open.**

A quick-access area of the web admin to **follow up the drivers**. The product owner named:

- **Suivi paiements**: follow daily payments per driver/contract (who paid, who is late).
- **Gérer impayés**: manage arrears, with the 24/48/72 h colour levels from [payments-and-arrears.md](payments-and-arrears.md).
- **Historique**: history of payments and events.
- "...and so on": **the remaining shortcuts are Open**.

## Drivers list behaviour (Decided)

Each driver's row is coloured by arrears level: none, **yellow** "Rappel" (1 unpaid day), **red** "Urgent" (2 days) and **deep red** "Suspendu" (3 days or more, the driver is suspended). The list, filters and the colours come from server-computed state.

## Rules

- Country staff see only their country's drivers; the super admin uses the header country filter ([multi-tenancy](../multi-tenancy/multi-tenancy.md)).
- Each shortcut is guarded by a permission (e.g. `payments:read`, `arrears:manage`): to be defined with the roles matrix.
- The current dashboard shows placeholder numbers ([findings C5](../findings/architecture-findings.md)); real KPIs come with the reporting module.
