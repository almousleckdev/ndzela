# ADR-0042: Resigned drivers are archived with their whole history

- **Status:** Accepted
- **Date:** 2026-11-17

## Context
`INACTIVE` ("Anciens") holds the old riders and drivers set aside by hand. A driver who resigns needs a clear end: no car, no login, nothing owed, and every record (contract, car, payments) still findable.

## Decision
- New status `ARCHIVED` and permission `drivers:archive` (V65, SUPER_ADMIN and ADMIN by default, assignable to any role). `POST /drivers/{id}/archive {reason}` archives a driver who is not blocked, only when no rent is late (`CONTRACT_HAS_ARREARS`, same rule as closing a contract, ADR-0041); it ends the active contract, frees the car, and keeps every contract, assignment and ledger line. The dashboard dialog shows what is owed and offers Encaisser first.
- Chauffeurs gets an **Archivés** tab (`scope=ARCHIVED`, with its count in `GET /drivers/stats`); Anciens stays `INACTIVE` only, Actuels excludes both. The driver page shows the past contracts with their cars and the payment history without needing an active contract.
- An archived driver is left out of the arrears follow-up and the candidate lists like an inactive one, counts as a former rider in the payments scopes and earnings, and cannot sign in to the app. `drivers:update` can reactivate one.

## Consequences
Nothing is deleted; archiving can be undone. The "Anciens"/"former" payment scope now covers both statuses.
