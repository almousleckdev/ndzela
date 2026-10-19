# ADR-0003: Staff belong to one country; super admin is global

**Status:** Superseded by [ADR-0023](0023-multi-country-staff.md) · **Date:** 2026-09-30

**Decision.** Each staff `users` row has exactly one `country_id`; only the SUPER_ADMIN has `country_id = null` (global) and sees all countries. A person working in two countries uses two accounts. The super admin selects a country filter in the header ("All countries" by default); single-country staff have no selector.

**Consequences.** (+) Very simple isolation rule, easy to test. (-) No multi-country regional managers without a new decision; changing this later requires a user-country link table and a revisit of the tenant context.
