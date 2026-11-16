# ADR-0039: Yesterday's rent is owed from midnight, late after noon

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
With [ADR-0038](0038-rent-is-due-the-next-day-at-noon.md) the rent of day D is due D+1 at 12:00. The old platform, which staff compare with, counts the rent of a day that has ended as owed from D+1 00:00 (its "Jours impayés" and "Montant dû"), and only treats it as late after noon. On Tuesday morning it showed one more day than the new dashboard for the same driver.

## Decision
- `AccountCalculator` exposes `graceDays` and `graceAmount`: the unpaid rent of days that have already ended, before their noon deadline. Credits still settle the oldest days first, so the grace day is the last one covered.
- They are stored on the driver row (`grace_days`, `grace_amount`, V60) next to `awaiting_*` and **shown by the staff screens only**: the owed days are `unpaid_days + grace_days` and the owed amount `overdue_amount + grace_amount` (Suivi paiements, Gérer impayés, the exports, the driver card and its progress block, the Chauffeurs list, the summary totals, the result of a payment). "Loyer du jour" now means today's open rent only.
- **Nothing that acts follows them:** the level, the penalty, the arrears steps, the emails, the automatic suspension, the driver app's lock, the Zella suspension amount and the "paying everything restores the driver" rule keep using the late count at noon. A rent never counts twice: at noon it moves from grace to late.
- The *at risk* banner counts the drivers who reach 3 late days at noon: late days plus yesterday's rent.

## Consequences
- Positive: the staff screens match the old platform at every hour; the 12:00 rules are unchanged.
- Negative: the list still sorts by the late count, so a driver can sit one place off the displayed count between 00:00 and 12:00; the displayed day count and the level can differ in that window (for example 3 days shown, level not yet RED).
