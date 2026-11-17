# ADR-0041: Contracts close only when paid; staff can lift the all-or-nothing rule for one driver

- **Status:** Accepted
- **Date:** 2026-11-17

## Context
A driver may give up the contract while owing rent, and "remove the vehicle" ended the contract without looking at the debt. Separately, a driver with 3 or more late days must pay them all at once in the app ([ADR-0021](0021-day-based-rent-payments-and-receipts.md)), which is wrong for a driver with a good reason.

## Decision
- **Closing:** `POST /drivers/{id}/vehicle/remove` (shown as "Clôturer le contrat") needs `drivers:update` and the new `contracts:terminate`, and is refused with `CONTRACT_HAS_ARREARS` while the driver has late rent. The dialog shows what is owed and offers Encaisser (ADR-0037); once nothing is late it asks for the reason. Rent still open before 12:00 does not block. Suspending, blocking or deactivating a driver still frees the car without this check (unchanged).
- **Exception (per day, revised 2026-11-17):** staff with `drivers:partial_payment` (SUPER_ADMIN and ADMIN by default) pick **which late days** a driver with 3 or more late days may pay alone: `PUT /payments/accounts/{driverId}/allowed-days {days}` replaces the whole set (an empty list revokes everything), stored in `payment_allowed_days` (V64; V62's all-or-nothing flag is dropped). Only late, unpaid days can be allowed (`DAY_NOT_PAYABLE` otherwise); every change is audited (`PAYMENT_DAYS_ALLOWED`). In the driver app the options become one per allowed day, oldest first (1 = the first allowed day, 2 = the first two...); the intent keeps the chosen days (`payment_intents.selected_days`) and they are settled as chosen days when PVit confirms, or the oldest days if one was paid meanwhile. A day paid by someone else drops out. Below 3 late days the allowance changes nothing; the payment-screen lock is unchanged.
- Both permissions are catalogue entries any role can hold; the super admin assigns them in the role editor.

## Consequences
GESTIONNAIRE keeps its current ability to remove a vehicle through `contracts:terminate`; removing the permission from a role takes that ability away. `contracts:create` was not added: nothing would enforce it.
