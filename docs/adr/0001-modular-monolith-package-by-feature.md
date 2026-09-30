# ADR-0001: Modular monolith, package by feature

**Status:** Accepted · **Date:** 2026-09-30 (recorded; refactor applied the same day)

**Context.** The backend grew with auth, users/roles, profile and notifications, with inconsistent layouts (some features flat, some layered). Business modules (fleet, contracts, payments) are coming.

**Decision.** One deployable Spring Boot application, organised **by feature**, every feature split `api / application / domain`, with shared `security`, `common`, `config`, `infrastructure`, `bootstrap`. Boundaries are defined in [module-boundaries](../architecture/module-boundaries.md).

**Consequences.** (+) Features are understandable and changeable in isolation; consistent shape for new modules; can extract a service later if ever needed. (-) Boundaries are by convention, not compiler-enforced: reviews (and later an architecture test, e.g. ArchUnit) must guard them. Existing deviations are in findings.

**Alternatives.** Package by layer (rejected: scatters a feature). Microservices (rejected: no need, higher cost).
