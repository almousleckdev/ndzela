# ADR-0008: Driver accounts are separate from staff users; created by staff only

**Status:** Decided · **Date:** 2026-09-30

**Context.** Drivers use a Flutter app to pay and follow a contract; staff use the web admin with email + password + OTP + TOTP and role permissions. The product owner stated that **drivers cannot create their own account**: staff create the driver, assign a car and a contract, after which the driver installs the app and signs in with **phone or email + password**.

**Decision.**
- Drivers are their own entity/table with their own credentials, their own token audience, and their own endpoint prefix (Proposed `/api/v1/driver/**`).
- **No public registration endpoint exists.** Accounts are created only by authorised staff, atomically with the contract (the last wizard step sets the initial password).
- Drivers do not get staff roles, the staff MFA flow, or access to admin endpoints. Their capability set is fixed: their own contract and own payments.
- A driver always belongs to one country.
- A driver suspended for arrears gets a restricted session limited to the payment flow (not a generic login rejection).

**Decided details.** Drivers sign in with **email or phone + password only, no second factor**; the password set by the gestionnaire has a **minimum of 8 characters** and **must be changed at first login**. Because the password is the only factor, the design must include sign-in throttling and lockout per identifier and IP, and password hashing at least as strong as staff (BCrypt cost 12). Only the **gestionnaire** role creates drivers.

**Consequences.** (+) Clear security boundary; the staff permission model stays clean; different lifecycle (contracts, not employment). (-) Two identity models to maintain; the admin sets an initial password, which needs a forced-change policy to avoid staff knowing driver passwords. Alternative considered: drivers as `users` with a DRIVER role (rejected: mixes populations, and MFA/email/permission assumptions do not fit).
