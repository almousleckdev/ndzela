# Importing the old NDZELA platform

**Status (2026-10-14): all 44 active riders and 59 former riders were imported into the LOCAL platform and checked. Nothing is imported in production. The backend has no import or back-dating code: the tool in `tools/legacy-migration` uses the normal API plus checked SQL.**

## The old platform (read-only facts)
A single-page app on Vercel (`my-ndzela-admin`), data in **Firebase** (Firestore, Firebase sign-in, file storage). 44 drivers, 48 vehicles, about 11 000 rent rows (`loyers`), 9 000 mobile-money transactions (`transactions`, PVit), 556 stops (`arrets`), 856 charging recharges and 271 regularisations (prepaid charging cards), 4 managers. The contract is not a record of its own: it is stored on the driver (`numeroContrat`, `dateDebut`, `dureeJours`, `loyerJournalier`, `caution`). **Security finding:** every driver's password is stored readable in the database, and the database keys are public in the site's code; shut the old platform down (or lock its database) after the import, and never migrate those passwords.

## How the data was read
Signed in with the old admin account through Firebase's public API and read the collections the app itself uses. Read-only. The export is a JSON file kept **outside the repository** (it holds personal data). A temporary sign-in token was deleted afterwards.

## The dry run (`tools/legacy-migration`)
`python3 dry_run.py EXPORT.json [--as-of ISO] [--out proposed.json]`. It maps each rider (driver, vehicle, contract, payments, stops) to the new platform's fields, checks them with the new platform's validation, and **predicts the account** with a port of `AccountCalculator` (`engine.py`). It writes to no database. Old timestamps are instants (many are midnight at UTC+1, stored as 23:00 UTC): business days are always taken in Libreville time.

## What the dry run found (2 riders)
| Topic | Finding | Decision needed |
| --- | --- | --- |
| Contract start | Original date (30 Jan 2026); the new platform always uses the creation date | Import mode with a start-date override |
| Payment dates | Original dates; the new platform has no back-dating | Import mode with historical dates |
| Total price | Old 32 448 000 (26 000 x 1 248) vs new rent x days = 28 704 000 (23 000) | New rule wins (rent from the contract) |
| Vehicle rent | Old vehicle 26 000, contract 23 000 | Use 23 000 |
| Payments without proof | 13 to 19 paid rows per rider without method or transaction | Cash? Import as labelled legacy credit |
| Payments above the rent | 26 000 instead of 23 000 (14 to 15 per rider) | Late fee? Keep as credit or as a penalty |
| Half-day payments | 11 500 (1 to 3 per rider) | Create HALF_DAY incidents |
| Sundays | Old platform billed Sundays; a few payments were for Sundays | They become credit |
| Unconfirmed money | 1 to 2 confirmed provider transactions linked to no paid row (26 000 to 49 000) | Credit them or not |
| Pending rows | Mostly stale attempts on days that were later paid | Ignore |
| Passwords | Not migrated | New random password per rider, change at first sign-in |

Predicted result with every paid row counted: both riders up to date, 8 to 9 days of credit.

## Decisions applied (the defaults, 2026-10-14)
Paid rows with no method or transaction: imported as `OTHER` payments with reference `LEGACY-<id>` and the reason "Legacy – no payment reference". Payments of 26 000: kept whole as credit. Payments of exactly half a rent: a HALF_DAY incident on that day. Sunday payments: credit. Confirmed provider transactions linked to no paid row: credited (and their fee recorded). Total price: rent x days. Passwords: a random one per rider, written to a local file (mode 600), changed at first sign-in. Pending rows: ignored (stale attempts). Failed and cancelled transactions: not imported.

## The local import (`import_local.py`)
Refuses to talk to anything but `http://localhost:8080` and the local database container. It creates the training centre, the vehicle and the driver (with its contract) through the API, with a 1x1 **placeholder photo** (the old data holds no photo; real photos and documents must still be collected), then, per rider in one transaction: the original contract number and dates, the ledger lines (payments and fees, request id = a deterministic id of the old row, so a second run adds nothing), the stop and half-day incidents, and an audit line `LEGACY_IMPORT`. Dates are always the Libreville business day.

## Result on the 2 riders (checked against the platform, not only the dry run)
The platform's own account endpoint and an independent port of its rules agree exactly: 213 days on time, 0 late, 0 unpaid, credit 275 000 (contract `NDZ-86487417`); 215 on time, 0 late, 0 unpaid, credit 232 000 (`NDZ-57491967`). Contracts keep their number, start (2026-01-30), end (2030-01-25), 28 704 000 total; both print as PDF; the 5-minute arrears job fills the list columns by itself. 446 payment lines (10 305 000) and 412 fee lines.

## Full local import (2026-10-14)
Tools: `export_legacy.py` (read-only export of the whole old database), `summary.py` (one line per rider), `import_local.py` (active riders and vehicles), `import_archive.py` (former riders), `verify_local.py` (every contract: the platform's account against an independent calculation).
- **44 active riders**: all imported, all 44 accounts equal the independent calculation. 44 vehicles assigned, 4 available. 17 records had gaps filled with "Non renseigné" (listed for staff to complete); invalid secondary phones were dropped; training-centre names were normalised (a trailing space made "NDZELA " a duplicate).
- **59 former riders** (their profile no longer exists in the old platform; 2 760 rent rows, 56 108 000 FCFA paid): imported as **archive profiles**: status INACTIVE, no login (`password_hash = '!'`), contract `ANC-0001…` TERMINATED over their period of activity, the vehicle when known (30) else the placeholder vehicle `ARCHIVE` (OUT_OF_SERVICE). Real data only where the old platform had it (name for 44, phone for 32); every other required field is a visible placeholder (`Non renseigné`, `ARCHIVE-xxxxxxxx`, birth date 1900-01-01), never invented identity data.
- **Ledger**: 9 520 payments (219 889 000 FCFA) and 6 615 fee lines (3 827 150), of which 2 100 active-rider payments (48 317 000) have no proof and are labelled `OTHER` / `LEGACY-…`. 290 stop and half-day incidents on active riders, 302 on the archive.
- **Not imported**: charging cards (856 recharges, 271 regularisations: a different concept), staff accounts, the old passwords, photos and documents (none in the old data).

## Matching the old "Gestion des impayés" (decided 2026-10-14)
The old screen counts, for each active rider, every billable day from the contract start day to **yesterday** (no Sunday, no stop) with **no paid rent row for that exact day**; extra money never offsets another day; 3 or more is "critical". The new platform instead settles total money against total rent, oldest first, from the day after the start. To show the same unpaid days (`parity.py` proves it for all 44 riders, 10 critical in both):
- **The start day is billed**: the contract starts the day before the old start date (the platform bills from the day after), so the first rent day is the old start date.
- **Only the money the old screen counted is credited** (`allocation.py`): for each billable day, the first paid row, up to that day's rent (half on a half day). Kept aside, in a CSV review list with the reason: later payments for an already paid day, the part above the rent (the 3 000 that looks like a late fee), payments dated on a Sunday / a stop day / before the start, and confirmed provider transactions no paid row refers to. For the 44 active riders about 4.7 million FCFA is kept aside. **These amounts are not in the ledger or in the "collected" totals** until staff decide what to do with them.
- **Half days** come from the old per-rider list (`demiJourneesChauffeur`), plus a day paid exactly half a rent.
- **Unpaid days older than the migration must not escalate by themselves.** The platform escalates by the number of unpaid days (1 reminder, 2 urgent, 3 suspension, ADR-0019). Left alone, the arrears job e-mailed the imported riders and suspended 4 of them. `silence_history.py` records the steps already reached at import time (the platform records each step once per rider, day and level), so nothing is sent or suspended for old arrears; steps reached later trigger normally. **In production it must run in the same step as the import, before the job.**

## Known effects to decide
- The payments summary and the follow-up list count every driver by arrears level, whatever the status, so the INACTIVE archive profiles appear as "up to date". Recommended: exclude INACTIVE drivers from those counts and lists.
- Riders at the RED level keep showing it to staff (no automatic action). Whether those riders should be suspended is the owner's decision, per rider.

## Next
Check the local dashboard against the old platform; decide what to do with the money kept aside; collect photos and documents; then the production import (the same tools, pointed at production on purpose, after a database backup, with `silence_history.py` in the same step), and shut the old platform down.
