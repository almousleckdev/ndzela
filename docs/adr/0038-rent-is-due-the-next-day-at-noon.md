# ADR-0038: Rent is due the next day at noon

- **Status:** Accepted (replaces the deadline of [ADR-0029](0029-rent-is-due-the-same-day-at-noon.md))
- **Date:** 2026-11-16

## Context
ADR-0029 made the rent of day D due on D at 12:00. The product owner corrected it: a driver must pay Monday's rent **before Tuesday 12:00**, not on Monday at noon.

## Decision
- The rent of day D (Monday to Saturday) is **open from D 00:00** and **due D+1 at 12:00** in the country's timezone. Saturday's rent is therefore due **Sunday 12:00**, the same moment the payment week closes; Sunday itself carries no rent. Everything else in ADR-0029 stands (first rent day is the day after signing, payable early, the 1-2 / 3 rule counts the days to settle).
- It is one rule in `AccountCalculator.dueAt`; every figure derived from it follows: unpaid and awaiting days, the level, the suspension, on-time or late, the "since" date, `settleBy` in the driver app.
- A rent day's penalty keeps its formula: due 48 h after the rent's deadline. **Open question for the owner:** whether the penalty should keep following the deadline (now a day later than under ADR-0029).

## Consequences
- Positive: matches how the owner describes the rule; arrears levels and the automatic suspension trigger a day later than under ADR-0029 (3 unpaid days at noon of the fourth day).
- Negative: because nothing is stored per day, **running contracts are recalculated at once**: some drivers who were late or suspended today become on time, and the next arrears run restores a driver suspended for arrears who no longer has 3 unpaid days. At most two rent days are open at once (yesterday's before noon, today's), so `awaitingDays` can be 2.
- Receipts and history already issued keep their frozen "on time / late" flags (`ledger_entry_days`); only the account derived from the ledger moves.
- The legacy-import parity note (docs/migration/legacy-import.md) was written for the same-day clock.
