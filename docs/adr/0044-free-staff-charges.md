# 0044. Free staff charges are marked, never shown as unpaid

## Context

NDZela staff charge for free through Zella. The feed reported them as "not charged", so the dashboard showed a red "Non débité" and an "Enregistrer le paiement" button, as if a payment were missing.

## Decision

- Zella adds `freeCategory` (`NDZELA_STAFF` or `OTHER`) to each feed item when the charge was free of charge.
- NDZela stores it on `session_payments.free_category` and returns it in `payment.freeCategory`.
- The sessions list and detail show an "Offert" badge with the category and hide the payment button. Exports say "Offert".
- Free energy is consumption, not revenue: the amount collected never includes it.

## Consequences

Staff energy is read in the Zella dashboard (page "Personnel NDZela"). NDZela only marks the sessions.

The value `FREE_SUNDAY` (a driver's free Sunday charge, [ADR-0048](0048-free-sunday-charging-for-selected-drivers.md)) is stored, labelled and excluded from revenue exactly like the staff categories; only `NDZELA_STAFF` energy is reported apart as staff energy.
