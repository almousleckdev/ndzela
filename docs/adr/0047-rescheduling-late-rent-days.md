# ADR-0047: Late rent days can be pushed to the end of the contract (rééchelonner)

- **Status:** Accepted
- **Date:** 2026-11-19

## Context
A driver may have 4 late days that staff agree he will pay later. Cancelling them (ADR-0043) would lose the rent; the days should be pushed to the end of the contract.

## Decision
- A super admin or admin with `payments:reschedule_days` (SUPER_ADMIN, ADMIN by default, V70) picks late unpaid days of the driver's active contract (same rule as cancelling: `DAY_NOT_PAYABLE` otherwise) and a mandatory reason: `POST /payments/accounts/{driverId}/rescheduled-days {days, reason}`, listed by `GET`, undone by `DELETE .../{id}`. Each day is stored in `rescheduled_rent_days` with the next working day (Sundays skipped) after the contract end, or after the last pushed day.
- The original day is no longer charged (so arrears levels, the automatic suspension and the app recompute), and the rent is charged on its new day once that day comes, so the contract is extended by as many working days. The contract's printed end date and 1 248 days do not change. Undoing a push charges the original day again.
- Audited: `RENT_DAYS_RESCHEDULED`, `RENT_RESCHEDULE_UNDONE`. Dashboard: "Rééchelonner des jours" in the driver actions.

## Consequences
Pushing a day that is itself a pushed day moves it again to a later day. Leaves and pauses on the new day do not move it.
