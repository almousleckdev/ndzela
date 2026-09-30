# ADR-0004: Authorization on permissions; roles shared globally

**Status:** Accepted (permission model) / Decided (roles global across countries) · **Date:** 2026-09-30

**Decision.** Access checks use `resource:action` permissions (`@PreAuthorize("hasAuthority(...)")`), never role names. Roles are bundles of permissions, **shared across all countries**; only the super admin edits roles. `AccessPolicy` prevents privilege escalation on `users`/`roles`. A user's country restricts *data*, permissions restrict *actions*.

**Consequences.** (+) Fine-grained, easy to reason about, UI and API share vocabulary. (-) The permission catalogue is duplicated in migrations and the frontend and must be kept in sync by procedure ([roles-and-permissions](../auth/roles-and-permissions.md)).
