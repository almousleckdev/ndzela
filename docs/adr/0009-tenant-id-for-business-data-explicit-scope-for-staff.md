# ADR-0009: Hibernate `@TenantId` for country-owned data, explicit scoping for staff accounts

**Status:** Accepted · **Date:** 2026-09-30

**Context.** The tenancy design (ADR-0002, ADR-0003) needs every country-owned table filtered and stamped automatically, a global super admin who can see all countries, and fail-closed behaviour. A spike on a real PostgreSQL showed that Hibernate's `@Filter` is not applied to primary-key loads (what `findById`, `deleteById` and `getReferenceById` do), so it would need a guard in every repository.

**Decision.**
- Country-owned entities extend `TenantOwned`, whose `country_id` column is annotated `@TenantId`. `TenantIdentifierResolver` supplies the country from the request's `TenantContext`. Hibernate then filters queries **and** primary-key loads and deletes, and stamps inserts.
- "All countries" is Hibernate's *root* tenant (`isRoot`): reads see every country. A missing context resolves to a value no row has, so reads return nothing (fail closed).
- `TenantOwnedListener` (`@PrePersist`) refuses to create a row unless exactly one country is selected (`COUNTRY_REQUIRED`). Updating or deleting an existing row in "all countries" is allowed: the row keeps its own country.
- The `users` table is **not** mapped with `@TenantId`: sign-in and token refresh look users up by email before any country is known, and global users have no country. Staff accounts are scoped explicitly by `UserScope`, and the administration services apply it to every list and every action (another country's id answers 404).
- `TenantContextFilter` (in `security`) sets the context after the JWT filter. It is created with `new` in `SecurityConfig` and is deliberately not a Spring bean, because a bean would also run in the servlet container chain before authentication.

**Consequences.** (+) New business tables get isolation by extending one class; the isolation is proven by `TenantMechanismTest` on a real database. (-) Two mechanisms exist by design (identity table vs business data); a new table holding staff identity data must follow `UserScope`. Background jobs must set the context explicitly with `TenantHolder.callAs`.

**Alternatives.** `@Filter` plus per-repository guards (rejected: easy to forget, `deleteById` bypass). PostgreSQL row-level security (deferred: harder with JPA and connection pooling; remains a possible later hardening).
