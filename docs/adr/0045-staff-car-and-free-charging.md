# 0045. A staff member gets a car and free charging from Users

## Context

NDZela staff charge for free. Only a Zella administrator could grant free charging, and NDZela users had no car and no link to a Zella account.

## Decision

- The super administrator (`users:staff-access`) opens "Voiture et recharge gratuite" on a user: a fleet car or a plate, and a free charging switch.
- The Zella account is found with the user's e-mail and phone number; only one account matching both is accepted, otherwise `ZELLA_ACCOUNT_NOT_FOUND`.
- NDZela links the account, then asks Zella (`PUT /v1/partners/ndzela/staff-access/{id}`) to give free charging as `NDZELA_STAFF`; the reason names the person and the car. Zella only accepts linked customers and never touches another kind of free access.
- Removing the access takes the free charging back and unlinks the account, unless it is also a linked driver.
- Every change is audited on both sides (`STAFF_FREE_CHARGING_GRANTED`, `STAFF_ZELLA_ACCESS_SAVED`, `STAFF_ZELLA_ACCESS_REMOVED`).

## Consequences

Free staff charges are marked "Offert" in the sessions (ADR-0044) and counted as energy in the Zella page "Personnel NDZela". A Zella outage refuses the change with `ZELLA_UNAVAILABLE` and saves nothing.
