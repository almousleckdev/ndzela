# ADR-0023: Staff may belong to one or more countries, or be global, independent of role

**Status:** Accepted (implemented) · **Date:** 2026-10-19 · **Supersedes:** ADR-0003

**Decision.** A staff `users` row now carries a boolean `is_global` and a `user_countries` join table (`user_id`, `country_id`) holding the full set of countries that user may work in. `country_id` on `users` stays, but its meaning changes: it is now only the user's *default* country (where the country switcher starts, and which brand an email uses), never the authorization boundary. `TenantContextResolver` decides the request's tenant from `is_global` and `user_countries`, not from a role name or from `country_id` being null:

- `is_global = true`: sees every country; `X-Country` header picks one, same as before. Any role, not only SUPER_ADMIN.
- `user_countries` has 2+ rows: the `X-Country` header picks among *that user's own* countries only; absent, it defaults to one of them. Same switcher component as a global user, but the choices are restricted.
- `user_countries` has exactly 1 row: unchanged from ADR-0003 — locked to that one country, the header is ignored.

**Who can grant what.** An actor can never grant a target user broader country reach than the actor's own: a single- or multi-country actor may only assign the country they are currently working in (`TenantHolder.current()`), never pick others, and can never make a target global. Only a global actor may assign several countries at once or grant global access. This is enforced in `UserManagementService.resolveCountryAssignment`, not just hidden in the UI.

**Extra phone numbers.** A separate `user_phone_numbers` table (plain list, no country tagging) holds additional numbers on top of the existing single `phone_number` column — mainly useful for a global or multi-country user who is reachable through more than one line.

**Consequences.** (+) Matches the real shape of regional staff without forcing one account per country. (+) The existing single-country and SUPER_ADMIN-global behaviors are unchanged byte-for-byte; this is purely additive. (-) `UserSummary`/`ProfileResponse` now carry `countries: []` + `isGlobal` instead of a single `country`, and `CreateUserRequest`/`UpdateUserRequest` carry `countryIds`/`isGlobal` instead of `countryId` — a breaking API shape change, updated across backend and dashboard together. (-) One more join table to keep in mind when writing a new tenant-owned query that touches `users`.
