# Architecture Decision Records

One file per decision: `NNNN-short-title.md`. Never delete; supersede. Status values: **Accepted** (in force, and built), **Decided** (approved, not built),
**Proposed** (awaiting the user), **Superseded by ADR-XXXX**.

| ADR | Title | Status |
| --- | --- | --- |
| [0041](0041-contract-closing-and-partial-payment-exception.md) | Contracts close only when paid; staff can lift the all-or-nothing rule for one driver | Accepted |
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
| [0029](0029-rent-is-due-the-same-day-at-noon.md) | Rent is due the same day at noon and can be paid from midnight | Superseded by 0038 |
| [0030](0030-receipt-qr-opens-a-public-payment-page.md) | The QR code of a receipt opens a public payment page on the website | Accepted |
| [0031](0031-deleting-sessions-and-recharges-and-the-zella-wallet.md) | Admins can delete sessions and recharges; recharges are paid from the Zella Wallet | Accepted |
| [0032](0032-public-sessions-keep-their-zella-payment.md) | Public sessions keep what was paid through Zella, still without identity | Accepted |
| [0033](0033-arrears-suspension-in-both-apps.md) | Three unpaid days suspend the driver in NDZela and in Zella; paying everything restores both | Accepted |
| [0034](0034-change-car-and-start-date-on-an-active-contract.md) | An active contract can move to another car and its start date can be corrected | Accepted |
| [0035](0035-purging-a-driver-with-their-money-and-half-days-for-today.md) | A driver can be erased with their money; half days can be granted for today and the coming week | Accepted |
| [0036](0036-admin-sets-a-temporary-driver-password.md) | An admin sets a temporary password for a driver and changes their email and phone | Accepted |
| [0037](0037-staff-payments-may-choose-the-days.md) | A staff payment may choose which days it pays | Accepted |
| [0038](0038-rent-is-due-the-next-day-at-noon.md) | Rent is due the next day at noon (replaces 0029) | Accepted |
| [0039](0039-yesterdays-rent-is-owed-from-midnight.md) | Yesterday's rent is owed from midnight, late after noon | Superseded by 0040 |
| [0040](0040-only-late-rent-is-shown-and-payable.md) | Only late rent is shown and payable: nothing before 12:00 | Accepted |
| [0042](0042-resigned-drivers-are-archived.md) | Resigned drivers are archived with their whole history | Accepted |
| [0043](0043-cancelled-rent-days.md) | Staff can cancel rent days a driver does not owe | Accepted |
| [0042](0042-stopping-a-charge-from-both-dashboards.md) | A running charge can be stopped from the NDZela admin and from the Zella dashboard | Accepted |
| [0046](0046-pauses-keep-the-contract.md) | A driver keeps his contract when set inactive or when the car is taken; the rent is paused | Accepted |
| [0047](0047-rescheduling-late-rent-days.md) | Late rent days can be pushed to the end of the contract (rééchelonner) | Accepted |
| [0040](0040-zella-reads-a-linked-drivers-contract-and-car.md) | The Zella app reads a linked driver's contract and car | Accepted |
| [0044](0044-free-staff-charges.md) | Free staff charges are marked, never shown as unpaid | Accepted |
| [0045](0045-staff-car-and-free-charging.md) | A staff member gets a car and free charging from Users | Accepted |
||||||| 9158784 (feat: count yesterday's rent as owed from 00:00, late after 12:00)

Template for a new ADR: title, status, date, context, decision, consequences (positive and negative), alternatives considered.
