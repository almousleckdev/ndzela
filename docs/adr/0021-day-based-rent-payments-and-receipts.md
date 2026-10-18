# ADR-0021: Rent is paid by day, and each payment records the days it settled

**Status:** Accepted · **Date:** 2026-10-17

**Context.** A driver-app payment always collected the whole arrears, and the ledger could not say which rent day a payment settled: the account is a running balance (`AccountCalculator` settles charges oldest first from pooled credit). Staff and drivers need to see which days a payment paid, with the exact payment time, and the receipt must say so. The business also wants drivers with one or two unpaid days to pay them one at a time, while three or more must be cleared at once (the suspension level, ADR-0019).

**Decision.**
- **Choosing days.** With U unpaid rent days (deadline passed): U = 1 or 2 lets the driver pay 1..U days, always the oldest first, never skipping a day; U ≥ 3 allows only all U. A day's price is its rent plus its penalty, minus credit already held. The server computes the options and the amount; the app sends a number of days, never an amount. Rent awaiting its deadline is not payable in the app.
- **Which day was paid.** A new append-only table `ledger_entry_days` (country-owned, update and delete refused by a trigger like `ledger_entries`) stores, in the payment's own transaction, the days the payment covered (`business_day`, `RENT` or `PENALTY`, `amount`, `paid_on_time`), found by replaying `AccountCalculator` at that instant. It is a snapshot for display and receipts, so a receipt never changes, even if another payment is reversed later. Balances and levels still come only from the calculator.
- **Order of charges.** `AccountCalculator` now settles charges by rent day, a day's penalty right after its rent (it used to follow every later rent), so paying a day pays its penalty too; `AccountState.unpaidCharges` exposes what is still unpaid.
- **Confirmed money counts.** Days are allocated from the amount PVit confirmed, not the amount asked. A shortfall covers fewer whole days, the rest stays credit, and `PAYMENT_UNDERPAID` is audited.
- **Weekly rule.** The week's rents (Monday to Saturday) are due by Sunday 12:00, Saturday's existing deadline. The account exposes `weekClosesAt` and `weekSettled`; nothing new blocks a driver.
- **Receipt.** (QR code drawn with ZXing `core` 3.5.3, OpenPDF has none.) One server-drawn ticket-style PDF per confirmed payment (no RCCM): a line per rent day with its payment date and time, penalties, credit kept, fee, total, and a QR code carrying only the receipt reference. Staff-recorded payments get the same receipt. There is no public verification URL.
- **Former riders** are the `INACTIVE` (archived) drivers; the dashboard separates them from current riders without a new column.
- **Hardening.** `/api/callback` stays untrusted (the status is re-read from PVit by our reference) and gets a rate limit; the application refuses to start with `PVIT_SANDBOX_AMOUNT` set in production.

**Consequences.** (+) Every payment states exactly which days it settled; receipts are immutable; partial payment follows the contract's by-day logic; the amount can no longer be influenced by the client. (-) One more ledger-side table to keep consistent with the calculator (tests replay both); a payment below one whole day is credit and its receipt lists no day. Supersedes the "pay 1 of 3, 2 remain" sentence of `payments-and-arrears.md`: with three or more unpaid days a partial payment is no longer possible in the app (staff-recorded payments are unchanged).

**Alternatives considered.** Deriving the days live at receipt time (rejected: a later reversal would change an issued receipt). Letting the driver pick any day (rejected: breaks oldest-first and the unpaid-day count). A public receipt verifier (rejected: a new unauthenticated endpoint).
