# ADR-0007: Frontend organised by feature

**Status:** Accepted · **Date:** 2026-09-30 (refactor applied the same day)

**Decision.** `src/features/<feature>` holds a feature's components, hooks, services, schemas, store, lib and types; `src/components/ui` and `layout`, `hooks`, `lib`, `config` are shared and feature-agnostic; `src/app` contains thin routes. See [frontend architecture](../frontend/architecture.md) and [module boundaries](../architecture/module-boundaries.md).

**Consequences.** (+) A feature is one place to read and change; scales with new modules. (-) Cross-feature imports must be watched (mutual imports exist today, see findings).
