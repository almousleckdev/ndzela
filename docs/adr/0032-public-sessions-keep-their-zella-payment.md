# ADR-0032: Public sessions keep what was paid through Zella, still without identity

**Status:** Accepted · **Date:** 2026-11-11 · **Amends:** [ADR-0028](0028-public-sessions-from-the-zella-feed.md)

**Context.** ADR-0028 kept no payment for public sessions, so the sessions list showed "Sans objet (public)" for a charge that was in fact paid from the Zella wallet, while a NDZela driver's charge showed "Payée · Zella". Every session charged through Zella is paid; the kind of customer does not change that.

**Decision.** The public feed already carries the payment state and amounts. NDZela now stores them in `session_payments` for public sessions, with no customer id and no idTag (`customer_id` is nullable, V50), and the sessions list shows "Payée · Zella" for both kinds. V50 also resets the public cursor once, so the sessions read before this change are read again with their payment. Replays are harmless.

**Consequences.** (+) One payment label for every session paid through Zella; the "Total payé" column and the export carry the amounts for public sessions. (-) NDZela stores what other customers were billed, as it already did on the session itself; still never who.
