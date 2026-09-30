# CLAUDE.md: NDZela Admin

Instructions for every AI agent (and human) working in this repository. **Read this file completely before doing any work.**

NDZela is a **multi-tenant (one tenant per country: Mali, Gabon, more later) fleet and driver-contract platform**. The
company owns the cars. Drivers sign a contract for a car and make fixed daily payments. Staff run the operation from
this web admin; drivers (created by staff, never self-registered) use a separate **Flutter + Riverpod** mobile app to pay their daily rent. This repository holds the **backend** (`backend/`, Java 21 /
Spring Boot 4) and the **web admin frontend** (`frontend/`, Next.js / TypeScript). Start with [docs/README.md](docs/README.md).

## The documentation is the source of truth

1. **Before modifying code, read the `docs/` files that govern the area you touch** (table below). If the docs and the code
   disagree, do not guess: the docs describe the *intended* rules, [docs/findings](docs/findings/architecture-findings.md)
   lists known deviations. Ask the user which one wins.
2. **Update the docs in the same change** when you alter an architectural decision, a convention, a schema rule, a
   permission, or an API contract. Add an ADR ([docs/adr](docs/adr/README.md)) for any new architectural decision.
3. **Never introduce an undocumented architectural pattern.** If you need something the docs do not cover, stop, propose it
   to the user, and document it once approved.
4. **Ask for clarification** when requirements conflict or are ambiguous. Do not guess business rules. Open questions are
   tracked in [docs/business/functional-requirements.md](docs/business/functional-requirements.md).
5. Files under `docs/business/legacy-charging-network-spec.md` are **obsolete** (a different product). Do not implement from them.

| If you touch... | Read first |
| --- | --- |
| Anything | this file, [docs/architecture/system-overview.md](docs/architecture/system-overview.md), [docs/standards/forbidden-patterns.md](docs/standards/forbidden-patterns.md) |
| Backend code | [docs/backend/architecture.md](docs/backend/architecture.md), [docs/backend/standards.md](docs/backend/standards.md) |
| Frontend code | [docs/frontend/architecture.md](docs/frontend/architecture.md), [docs/frontend/standards.md](docs/frontend/standards.md) |
| Tenant-owned data, queries, endpoints | [docs/multi-tenancy/multi-tenancy.md](docs/multi-tenancy/multi-tenancy.md) |
| Login, tokens, sessions, MFA | [docs/auth/authentication.md](docs/auth/authentication.md) |
| Permissions, roles, guards | [docs/auth/authorization.md](docs/auth/authorization.md), [docs/auth/roles-and-permissions.md](docs/auth/roles-and-permissions.md) |
| Tables, migrations | [docs/database/database.md](docs/database/database.md) |
| Endpoints, DTOs, errors | [docs/api/conventions.md](docs/api/conventions.md) |
| Tests | [docs/testing/strategy.md](docs/testing/strategy.md) |
| Docker, env vars, deployment | [docs/infrastructure/infrastructure-and-deployment.md](docs/infrastructure/infrastructure-and-deployment.md) |
| Commits, branches, PRs | [docs/workflow/git-workflow.md](docs/workflow/git-workflow.md) |
| The driver mobile app or its API | [docs/mobile/architecture.md](docs/mobile/architecture.md), [docs/adr/0008-driver-accounts-separate-from-staff.md](docs/adr/0008-driver-accounts-separate-from-staff.md) |
| A business rule | [docs/business/](docs/business/README.md) |

## Non-negotiable rules

**Architecture**
- **Never mix backend and frontend responsibilities.** Business rules, validation of truth, authorization and tenant
  filtering live in the backend. The frontend validates for usability only and hides UI by permission for convenience;
  it is never the security boundary.
- **Follow existing module boundaries** (backend feature packages, frontend `features/`). Cross-feature access goes through
  the other feature's service or public type, never its repository or internals. See
  [docs/architecture/module-boundaries.md](docs/architecture/module-boundaries.md).
- **Never create unnecessary abstractions.** No interface with a single implementation "just in case", no generic
  base classes or helpers before there are at least two real users. Three similar lines beat a premature abstraction.
- **Never duplicate existing functionality.** Search first (`docs/`, then code). Reuse `components/ui`, `lib/`, `common/`
  and existing services.
- **Keep files maintainable and reasonably sized**: aim for under ~200 lines per file and one clear responsibility. Split
  by responsibility, not by arbitrary line count.

**Tenant isolation (highest priority)**
- **Never violate tenant isolation.** Every business table carries `country_id`; every query is scoped to the caller's
  country by the mechanism in [docs/multi-tenancy/multi-tenancy.md](docs/multi-tenancy/multi-tenancy.md). Only the
  SUPER_ADMIN is cross-country. Never trust a country sent by the client for anything but the super admin's explicit selection.
- Any new tenant-owned entity ships with an isolation test proving another country's user cannot read or change it.

**Security and correctness**
- **Never bypass** authentication, `@PreAuthorize` permission checks, `AccessPolicy`, bean validation, the global exception
  handler, or the repository/service layering. Authorization is decided on **permissions, never role names**.
- Never log or return secrets, tokens, OTPs, passwords, MFA seeds. Never commit `.env` files.
- Never edit an applied Flyway migration. Schema changes are new `V<n>__*.sql` files only.
- Money is never `float`/`double`. See [docs/database/database.md](docs/database/database.md).

**Process**
- Read a file before editing it. Make the smallest change that solves the task; do not refactor unrelated code.
- Do not implement features before the relevant spec/plan is approved by the user. Do not start coding when told not to.
- Run the checks before saying a task is done: backend `cd backend && mvn clean test`; frontend
  `cd frontend && npm run lint && npm run typecheck`. Report failures honestly.
- Follow [docs/workflow/git-workflow.md](docs/workflow/git-workflow.md). Never commit, push or rewrite history unless asked.
- The frontend runs a Next.js version with breaking changes: also follow `frontend/AGENTS.md` (read the guides in
  `frontend/node_modules/next/dist/docs/` before writing Next.js code).

## Commands

| Task | Command |
| --- | --- |
| Start Postgres and Redis | `docker compose up -d` |
| Backend run / test | `cd backend && mvn spring-boot:run` / `mvn clean test` (Docker must be running: integration tests use Testcontainers; use `clean` after refactors: stale classes cause `ClassFormatError`) |
| Frontend run / check | `cd frontend && npm run dev` / `npm run lint && npm run typecheck` |
