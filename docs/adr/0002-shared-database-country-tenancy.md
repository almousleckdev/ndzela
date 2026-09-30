# ADR-0002: Shared database, country as tenant, application-level filter

**Status:** Accepted (implemented; mechanism in ADR-0009) · **Date:** 2026-09-30

**Context.** The platform serves several countries (Mali, Gabon, more) and must isolate each country's data.

**Decision.** One PostgreSQL database, shared tables, `country_id` on every tenant-owned table; the country is resolved server-side per request from the authenticated user and applied automatically by Hibernate (`@TenantId`, see ADR-0009); isolation is proven by automated tests. See [multi-tenancy](../multi-tenancy/multi-tenancy.md).

**Consequences.** (+) Simple operations, cross-country reporting for the super admin, one schema to migrate. (-) Isolation depends on application discipline and tests; a defect could leak data. Mitigation: fail-closed filter, mandatory isolation tests, code review, optional PostgreSQL row-level security later (a hardening option, not part of v1).

**Alternatives.** Database or schema per country (rejected: heavy to operate, hard cross-country reports). Row-level security from day one (deferred: harder with JPA and pooling).
