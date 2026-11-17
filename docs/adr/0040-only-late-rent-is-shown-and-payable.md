# ADR-0040: Only late rent is shown and payable

- **Status:** Accepted
- **Date:** 2026-11-17

## Context
[ADR-0039](0039-yesterdays-rent-is-owed-from-midnight.md) showed yesterday's rent as owed from 00:00 on the staff screens. The business wants nothing shown as unpaid before the 12:00 deadline: drivers with 2 late days were displayed with 3.

## Decision
- Staff screens and the driver app show the **late** days and amount only (`unpaid_days`, `overdue_amount`). The grace display is removed; the `grace_*` columns (V60, already applied) stay unused and are dropped by V61.
- In the driver app's payment screen the options are the **late days only**: a driver with 2 late days sees 1 or 2, with 3 or more only "all". A rent still open is not offered to the driver; staff can still collect it from Encaisser (ADR-0037).

## Consequences
The count the staff see always equals the count that triggers the level, the penalty and the lock. Supersedes ADR-0039; ADR-0038 (deadline D+1 12:00) is unchanged.
