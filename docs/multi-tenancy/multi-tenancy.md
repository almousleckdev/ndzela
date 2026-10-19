# Multi-tenancy

**Status: Implemented** for countries, staff accounts, notifications and the automatic filter (`TenantOwned`). Business tables (vehicles, drivers, contracts, payments) will reuse it when they are built. Design history: `docs/superpowers/specs/2026-09-30-tenancy-foundation-design.md` (local only).

## Model

**Focus country: Gabon.** Gabon and Mali both exist as active countries (migrations V7 and V8); all business work targets Gabon first, and Mali's payment providers and contract template come later.

- **Tenant = country** (Mali, Gabon, later more). One PostgreSQL database, shared tables, a `country_id` column on every **tenant-owned** table ("approach A": application-level filter).
- **Staff belong to one or more countries, or are global** ([ADR-0023](../adr/0023-multi-country-staff.md)): `users.is_global` plus a `user_countries` join table hold the actual reach; `users.country_id` is only the default/home country. Global is independent of role, not only SUPER_ADMIN.
- **Roles and the permission catalogue are global** (not per country). Only the super admin manages roles and countries.
- **Country data**: code (`ML`, `GA`), name, currency (`XOF`, `XAF`; displayed as FCFA), phone code and format, timezone, default language, active/inactive. **Also per country (from the business requirements, to confirm):** the enabled **payment providers** (Gabon: Airtel Money, Moov Money Gabon; Mali: Orange Money, Moov Money Mali) with their credentials in secrets, and the **legal entity data** of the lessor (raison sociale, forme juridique, siège, RCCM, phone, email), the **contract PDF template** (French, Gabonese law and courts for Gabon) and other legal wording. Arrears thresholds are evaluated in the country's timezone.
- **Switcher UX**: a global user gets a country filter in the header ("All countries", "Mali", "Gabon"), default all; a multi-country (not global) user gets the same switcher restricted to their own countries, never "all"; single-country staff see no selector.

## What is built

- **Data:** migration `V7` creates `countries` (Gabon: `GA`, `XAF`, `+241`, `Africa/Libreville`, `fr`); `V8` adds Mali (`ML`, `XOF`, `+223`, `Africa/Bamako`, `fr`) and `users.country_id`; existing non-super-admin users became Gabon; permission `countries:read` is granted to every role.
- **Context:** `TenantContext` (modes `COUNTRY`, `ALL`, `NONE`) held per thread by `TenantHolder`; set per request by `security.TenantContextFilter` after the JWT filter, from `TenantContextResolver`. Header `X-Country` (country code) is honoured for a global user (any active country) or a multi-country user (only among their own `user_countries`); ignored for a single-country user; an unknown, inactive, or not-assigned code answers `400 INVALID_COUNTRY`.
- **Business data:** extend `tenant.TenantOwned` (Hibernate `@TenantId`, resolver `TenantIdentifierResolver`, guard `TenantOwnedListener`); see [ADR-0009](../adr/0009-tenant-id-for-business-data-explicit-scope-for-staff.md). Proven by `TenantMechanismTest` with a test-only entity.
- **Staff accounts:** scoped explicitly by `UserScope` in the users and roles services (identity table: sign-in happens before any country is known), via `user_countries` membership. Another country's id answers `404`; role member counts are counted in the caller's country; `notifyHolders` reaches the same country plus global staff only.
- **Rules on staff:** an actor can never grant a target user broader reach than their own. A single- or multi-country actor may only assign the one country they currently work in, and can never make a target global. Only a global actor may assign several countries at once or grant global access, and must send at least one country (or `isGlobal`) when working in "all countries" (`COUNTRY_REQUIRED`); email stays unique across all countries. See [ADR-0023](../adr/0023-multi-country-staff.md).
- **Fail closed:** a user with no assigned country and `is_global = false` gets an empty context (sees and creates nothing).
- **Inactive country:** its staff cannot sign in (`423 ACCOUNT_LOCKED`, same as a disabled account), existing tokens stop working (`401`) and refresh is refused; global users are unaffected.
- **Country-owned tables built on it:** `vehicles`, `training_centers`, `drivers`, `vehicle_assignments` (all extend `TenantOwned`); `audit_logs` is scoped explicitly (global events have no country).
- **API and UI:** `GET /api/v1/countries` (all active countries for every user, needed for phone codes; data stays scoped), `isGlobal`/`countries`/`country` (default) in `/profile/me` and in user summaries; the admin header shows a country selector for a global or multi-country user and a label for single-country staff; `features/tenancy` in the frontend.

## Which data is tenant-owned

| Global (no `country_id`) | Tenant-owned (`country_id NOT NULL`) |
| --- | --- |
| `countries`, `permissions`, `roles`, `role_permissions` | staff `users` (except a global user), `user_countries`, and **every future business table**: vehicles, drivers (and their emergency contact and documents), contracts and contract PDFs, payments and the ledger, arrears state, in-app notifications of tenant users, audit records |

Rule of thumb: if a row belongs to a business operation in one country, it is tenant-owned. If unsure, **ask**.

## Enforcement rules (binding)

1. **Server-side only.** The country of a request is derived from the authenticated user's record, once per request, into a request-scoped tenant context. The client is never trusted for it.
2. **Automatic filtering.** Tenant-owned entities are filtered by the tenant context through one mechanism (Hibernate `@TenantId` on the `TenantOwned` base class, driven by `TenantIdentifierResolver`). Developers do **not** write the country condition by hand in each query, and a missing filter must fail closed, not open.
3. **Global/multi-country selection.** The header filter sends an explicit country selection (`X-Country` header carrying the country code, validated against active `countries`). For a global user, no selection = all countries; for a multi-country user, the code must be one of their own `user_countries`, and no selection defaults to one of them. For a single-country user the header is ignored.
4. **Writes inherit the country.** New tenant-owned rows get `country_id` from the context (or the explicit selection for the super admin, who must pick a country when creating country data). A client-supplied `country_id` in a body is rejected or ignored. In "all countries" mode **creating** a country-owned row is refused (`422 COUNTRY_REQUIRED`); **updating or deleting** an existing row is allowed, because the row keeps its own country.
5. **No cross-tenant references.** A foreign key between two tenant-owned rows must be validated to belong to the same country.
6. **Identifiers.** Look-ups by id are country-scoped too: a Gabon user requesting a Mali resource id gets `404`, not `403` and never the data.
7. **Uniqueness** rules (e.g. registration plates, phone numbers) are decided per country vs global per module. State it in the module spec.
8. **Jobs, webhooks, background work** have no request user: they must set the tenant context explicitly (`TenantHolder.callAs`) from the record they process, **before** any transaction starts (`TenantHolder` refuses to change it inside one).
9. **Drivers, vehicles and contracts must share one country**: a contract cannot link a driver and a car of different countries. **Caches and Redis keys** holding tenant data are namespaced by country. Files stored on disk/object storage use a per-country prefix.
10. **Reports/exports** for country staff are country-limited; only the super admin can produce cross-country aggregates, and they are labelled by country.

## Required tests (per tenant-owned feature)

A country-A user cannot list, read, update or delete country-B data (expect 404); a country-A create never lands in country B; the super admin sees both;
the header selection is ignored for non-super-admins; the filter is applied in every repository path used by the feature. Details: [testing](../testing/strategy.md).

## Migration of existing data

Existing non-super-admin users were assigned to **Gabon** by migration `V7`, which also creates the Gabon row (currency XAF, timezone `Africa/Libreville`). Providers, legal entity data and the contract template are added to `countries` by the contract and payment modules.
Existing super admins stay global. New non-super-admin staff cannot be created without a country.

## Not decided yet

PostgreSQL row-level security as a second line of defence (kept as a later hardening option, ADR-0002); per-country subdomains or URLs.
