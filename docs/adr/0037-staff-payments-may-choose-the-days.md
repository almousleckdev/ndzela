# ADR-0037: A staff payment may choose which days it pays

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
Payments settle whole days, oldest first, and nothing is stored per day ([ADR-0021](0021-day-based-rent-payments-and-receipts.md)). Some drivers owe 9 or 10 days. At the counter staff need to record that the driver paid specific days, including past ones, even when older days stay unpaid.

## Decision
- Encaisser lets staff tick any **payable day**: an unpaid day, or today's open day (rent plus its penalty, if any). Staff holding `payments:create` only; the driver app is unchanged (oldest first, 1..N days, all from 3 unpaid days).
- A payment may carry `selected_days` (`DATE[]`, V59, written with the row because the ledger is append-only). `AccountCalculator` settles those days at the payment's time and leaves them and the payment out of the oldest-first credit; every other credit still settles the remaining days oldest first. A reversed payment is ignored as before, so its days become unpaid again.
- The request sends `days` instead of `amount`; the server derives the amount (`ChosenDays.pick`), refuses a duplicate, a paid, future, Sunday or leave day with `DAY_NOT_PAYABLE`, and ignores a client amount. `ledger_entry_days` is written from the chosen days. The audit line lists them.
- `GET /payments/accounts/{driverId}` returns `payableDays`, so the dashboard has no rule of its own. An amount without days keeps the old behaviour (partial payments and credit).

## Consequences
- Positive: staff record what the driver really paid; the arrears level, emails and suspension follow the days really unpaid.
- Negative: a driver can pay a recent day while an older one stays unpaid, and may stay at 3 or more unpaid days (suspended) after paying; a held credit keeps covering the oldest days, not the chosen ones.

## Alternatives considered
- One payment per day with oldest first: settles the wrong days.
- A per-day status table: a second source of truth next to the ledger.
