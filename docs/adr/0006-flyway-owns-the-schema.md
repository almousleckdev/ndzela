# ADR-0006: Flyway owns the schema

**Status:** Accepted · **Date:** 2026-09-30 (recorded)

**Decision.** All schema and platform seed data change through immutable, sequential Flyway migrations. Hibernate `ddl-auto` is `validate`. See [database](../database/database.md).

**Consequences.** (+) Reproducible, reviewable schema history. (-) Mistakes need corrective migrations. Note: V2/V3 are written idempotently because early databases were created by Hibernate, and `baseline-on-migrate` is enabled (see findings).
