# ADR-0046: A driver keeps his contract when set inactive or when the car is taken; the rent is paused

- **Status:** Accepted
- **Date:** 2026-11-19
- **Supersedes:** the 2026-11-05 rules that setting a driver inactive, blocking him, freeing his car on suspension or "Retirer le véhicule" all ended the contract.

## Context
Staff set a driver inactive for an accident or a holiday, take a car back, or block a driver, and expect him to come back to the same contract. Each of these ended the contract; reactivating or giving a car again opened a new contract from zero (4-5 drivers affected). Changing the car already kept the contract.

## Decision
- **Only two actions end a contract:** "Clôturer le contrat" (`POST /drivers/{id}/vehicle/remove`, ADR-0041, refused while rent is late) and archiving (ADR-0042). Nothing else terminates it.
- **Inactive is a pause.** `deactivate` only changes the status; the car, assignment and contract stay. The tab is called **Inactifs**; inactive drivers are no longer "former riders" in payments and earnings (only archived ones are).
- **Taking the car keeps the contract.** `POST /drivers/{id}/vehicle/release {reason}` (`drivers:update`), blocking, and suspending with "Libérer le véhicule" free the car and set `contracts.vehicle_id` to null (V69). `POST /drivers/{id}/vehicle` on a driver whose contract has no car continues that contract on the new car (rent, dates, number and history as signed); without a contract it opens one as before. A car change (`PUT /vehicle`) is unchanged: same contract.
- **No rent during a pause.** `driver_pauses` (V69) holds `INACTIVE` pauses (set inactive until reactivated or any other status change) and `NO_CAR` pauses (car taken until the next car). `AccountService` passes the pause days to the account calculator like declared leaves; unpaid rent from before stays owed. A pause lifted the day it began is deleted.
- **Restoring the drivers already hit:** `backend/scripts/restore-deactivated-contracts.sql` (dry run, then COMMIT) reopens the contract that a deactivation ended, with its payments. A driver who was reactivated and then got a new empty contract ("Jour 0 sur 1 248") gets the original back on the car he has now; the empty contract is closed. A new contract that already has a payment, incident or cancelled day is only listed. Audited `CONTRACT_RESTORED`.

## Consequences
A driver without a car stays "assigned a contract" in the app and the dashboard (no vehicle shown); the driver app answers `DRIVER_HAS_NO_VEHICLE` for the car. An automatic arrears suspension of an inactive driver still applies (ADR-0033) and ends his pause.
