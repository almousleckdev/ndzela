# ADR-0041: Contracts close only when paid; staff can lift the all-or-nothing rule for one driver

- **Status:** Accepted
- **Date:** 2026-11-17

## Context
A driver may give up the contract while owing rent, and "remove the vehicle" ended the contract without looking at the debt. Separately, a driver with 3 or more late days must pay them all at once in the app ([ADR-0021](0021-day-based-rent-payments-and-receipts.md)), which is wrong for a driver with a good reason.

## Decision
- **Closing:** `POST /drivers/{id}/vehicle/remove` (shown as "Clôturer le contrat") needs `drivers:update` and the new `contracts:terminate`, and is refused with `CONTRACT_HAS_ARREARS` while the driver has late rent. The dialog shows what is owed and offers Encaisser (ADR-0037); once nothing is late it asks for the reason. Rent still open before 12:00 does not block. Suspending, blocking or deactivating a driver still frees the car without this check (unchanged).
- **Exception:** `PUT /drivers/{id}/partial-payment {allowed}` (`drivers:partial_payment`, SUPER_ADMIN and ADMIN by default) sets `drivers.partial_payment_allowed` (V62). While it is on, `PaymentSelection.options` offers every number of late days (1..N, oldest first) instead of only "all" from 3. It is cleared automatically when the driver has no late day left, and every change is audited (`DRIVER_PARTIAL_PAYMENT_CHANGED`). The lock (payment screen only) is unchanged.
- Both permissions are catalogue entries any role can hold; the super admin assigns them in the role editor.

## Consequences
GESTIONNAIRE keeps its current ability to remove a vehicle through `contracts:terminate`; removing the permission from a role takes that ability away. `contracts:create` was not added: nothing would enforce it.
