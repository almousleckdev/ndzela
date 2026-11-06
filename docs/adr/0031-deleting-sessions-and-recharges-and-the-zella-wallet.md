# ADR-0031: Super administrators and administrators can delete sessions and recharges; recharges are paid from the Zella Wallet

**Status:** Accepted · **Date:** 2026-11-06

**Context.** Staff need to remove wrong or test network sessions and recharges. Recharges were append-only at the database level (V16 trigger) and sessions come back with every sync. The product owner also stated that every recharge is paid from the Zella Wallet.

**Decision.** A new permission `charging:delete` (V49) is given to `SUPER_ADMIN` and `ADMIN` only. `DELETE /charging/sessions/{id}` and `DELETE /charging/recharges/{id}` take a mandatory reason and write an audit entry (`CHARGING_SESSION_DELETED`, `CHARGING_RECHARGE_DELETED`). A deleted session leaves a tombstone (`deleted_sessions`) that the Zella sync skips, so it does not return, and its Zella payment details are removed with it. A session that still has a recharge is refused (`SESSION_HAS_RECHARGES`): delete the recharge first. A recharge is deleted together with its cancellation; a cancellation alone cannot be deleted (`ENTRY_NOT_A_RECHARGE`). The recharge table stays append-only: updates are always refused, and the trigger accepts a delete only inside the deletion service's transaction (`app.charging_delete` set locally). Every new recharge is stored with the method `ZELLA_WALLET`, whatever the client sends; older rows keep the method they had.

**Consequences.** (+) Staff can clean the data with a trace of who deleted what and why. (-) A deletion is permanent: the audit entry is the only record. The deletion trigger exception means a bug in the service could delete ledger rows, so it is covered by tests and limited to the service's transaction.
