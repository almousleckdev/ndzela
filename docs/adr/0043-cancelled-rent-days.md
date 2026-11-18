# ADR-0043: Staff can cancel rent days a driver does not owe

- **Status:** Accepted
- **Date:** 2026-11-18

## Context
The system bills every day of an active contract. A driver suspended for a long time, or one who resigned, accrues days nobody expects them to pay; they blocked closing the contract and archiving (ADR-0041, ADR-0042). Leaves cannot start in the past, a waiver is an amount rather than a day, and incidents only cover the next days.

## Decision
- New permission `payments:cancel_days` (V66, SUPER_ADMIN and ADMIN, assignable to any role) and table `cancelled_rent_days` (one row per contract day, with reason and author).
- `POST /payments/accounts/{driverId}/cancelled-days {days, reason}` cancels unpaid or open days of the active contract (`DAY_NOT_PAYABLE` for a paid day); `GET` lists them; `DELETE .../{id}` restores a day. Each change is audited (`RENT_DAYS_CANCELLED`, `RENT_DAY_RESTORED`) and the driver's arrears are recomputed at once.
- `AccountCalculator` skips a cancelled day like a leave: no rent, no penalty, so the days disappear from the app, the dashboard and the late count.
- Archiving or closing no longer trusts stale figures of a driver without an active contract. The dashboard offers "Annuler des jours de loyer" in the driver actions and in the "loyers restent à payer" dialog, and `CONTRACT_HAS_ARREARS` has its own message.

## Consequences
Rent history is rewritten by an explicit, reasoned, reversible act rather than deleted. The cancelled day never reaches the ledger, so earnings are unchanged.
