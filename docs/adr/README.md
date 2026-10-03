# Architecture Decision Records

One file per decision: `NNNN-short-title.md`. Never delete; supersede. Status values: **Accepted** (in force, and built), **Decided** (approved, not built),
**Proposed** (awaiting the user), **Superseded by ADR-XXXX**.

| ADR | Title | Status |
| --- | --- | --- |
| [0001](0001-modular-monolith-package-by-feature.md) | Modular monolith, package by feature with api/application/domain layers | Accepted |
| [0002](0002-shared-database-country-tenancy.md) | Shared database, country as tenant, application-level filter | Accepted |
| [0003](0003-staff-single-country-global-super-admin.md) | Staff belong to one country; super admin is global with a header filter | Accepted |
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

Template for a new ADR: title, status, date, context, decision, consequences (positive and negative), alternatives considered.
