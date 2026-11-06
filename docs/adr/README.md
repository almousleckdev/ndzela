# Architecture Decision Records

One file per decision: `NNNN-short-title.md`. Never delete; supersede. Status values: **Accepted** (in force, and built), **Decided** (approved, not built),
**Proposed** (awaiting the user), **Superseded by ADR-XXXX**.

| ADR | Title | Status |
| --- | --- | --- |
| [0001](0001-modular-monolith-package-by-feature.md) | Modular monolith, package by feature with api/application/domain layers | Accepted |
| [0002](0002-shared-database-country-tenancy.md) | Shared database, country as tenant, application-level filter | Accepted |
| [0003](0003-staff-single-country-global-super-admin.md) | Staff belong to one country; super admin is global with a header filter | Superseded by 0023 |
| [0004](0004-permission-based-authorization.md) | Authorization on permissions, shared global roles | Accepted (roles global: Decided) |
| [0005](0005-token-strategy.md) | JWT in memory + rotating opaque refresh cookie, MFA mandatory for staff | Accepted |
| [0006](0006-flyway-owns-the-schema.md) | Flyway owns the schema, Hibernate validates | Accepted |
| [0007](0007-frontend-feature-folders.md) | Frontend organised by feature | Accepted |
| [0008](0008-driver-accounts-separate-from-staff.md) | Driver accounts separate from staff, created by staff only | Decided |
| [0009](0009-tenant-id-for-business-data-explicit-scope-for-staff.md) | `@TenantId` for country-owned data, explicit scoping for staff accounts | Accepted |
| [0010](0010-contract-pdf-with-openpdf-from-a-snapshot.md) | Contract PDFs rendered on demand with OpenPDF from a frozen snapshot | Accepted |
| [0011](0011-tanstack-table-and-the-console-design-system.md) | TanStack Table v9 for every table and one console design system | Accepted |
| [0012](0012-driver-app-stack-and-structure.md) | Driver app stack (Riverpod, go_router, dio, secure storage) and feature-first structure | Accepted |
| [0013](0013-driver-api-token-audience-and-login-lookup.md) | Driver API: own token audience, filter and permissions, cross-country sign-in lookup | Accepted |
| [0014](0014-pvit-mobile-money-payments.md) | Mobile-money payments through PVit: the callback is a hint, PVit's status API is the truth | Accepted |
| [0015](0015-public-website.md) | The public website is a separate Next.js project (`web/`), static-first, no admin code | Accepted |
| [0016](0016-cloudinary-file-storage.md) | Files live in Cloudinary: private assets read through the backend, public CDN images only for profile pictures | Accepted |
| [0017](0017-ocpp-sessions-from-the-fleet-api-sync.md) | OCPP sessions come from the Fleet API incremental sync; webhooks only ask for a sync | Accepted |
| [0018](0018-railway-hosting-and-domains.md) | The backend runs on Railway; the API is `api.ndzela.com` | Accepted |
| [0019](0019-arrears-levels-follow-the-number-of-unpaid-days.md) | Arrears levels follow the number of unpaid days (1 reminder, 2 urgent, 3+ suspension) | Accepted |
| [0020](0020-table-exports-as-excel-or-pdf.md) | Table exports are an Excel file or a PDF, never a CSV | Accepted |
| [0021](0021-day-based-rent-payments-and-receipts.md) | Rent is paid by day, and each payment records the days it settled | Accepted |
| [0022](0022-single-device-sessions.md) | Single-device sessions: one session id per account, checked per request | Accepted |
| [0023](0023-multi-country-staff.md) | Staff may belong to one or more countries, or be global, independent of role | Accepted |
| [0024](0024-explicit-deployment-environment.md) | The deployment environment is explicit (`APP_ENVIRONMENT`): staging may use the PVit sandbox, production may not | Accepted |
| [0025](0025-dashboard-tests-vitest-and-playwright.md) | Dashboard tests: Vitest and Testing Library for logic, Playwright with a mocked API for flows | Accepted |
| [0026](0026-charging-accounts-linked-to-drivers.md) | Charging accounts (idTags) are linked to drivers by e-mail and phone, looked up in the Zella app | Accepted |
| [0027](0027-refresh-token-reuse-window.md) | A rotated refresh token still works for 30 seconds, so a lost answer does not end a session | Accepted |
| [0028](0028-public-sessions-from-the-zella-feed.md) | Sessions of other Zella customers reach NDZela as "Public", without identity | Accepted |
| [0029](0029-rent-is-due-the-same-day-at-noon.md) | Rent is due the same day at noon and can be paid from midnight | Accepted |

Template for a new ADR: title, status, date, context, decision, consequences (positive and negative), alternatives considered.
