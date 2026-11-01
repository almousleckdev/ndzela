# NDZela documentation

This folder is the engineering source of truth. `CLAUDE.md` at the repository root tells every agent to read it.

## How to read the status labels

Every rule and feature in these docs is marked so nobody mistakes intent for reality:

| Label | Meaning |
| --- | --- |
| **Implemented** | Exists in the code today and the doc describes it accurately. |
| **Decided** | The user approved it; it is **not yet built**. Build it exactly as written. |
| **Proposed** | A recommendation awaiting the user's decision. Do not build it without approval. |
| **Open** | An unanswered business or technical question. Ask; never guess. |

Where the code deviates from the intended rule, the deviation is recorded in
[findings/architecture-findings.md](findings/architecture-findings.md), not silently "fixed".

## Index

| Area | Document | Governs |
| --- | --- | --- |
| Business | [business/README.md](business/README.md) | What the product is, glossary, scope, actors |
| | [business/functional-requirements.md](business/functional-requirements.md) | Modules, build order, decisions taken, **open questions** |
| | [business/vehicles.md](business/vehicles.md), [drivers.md](business/drivers.md), [contracts.md](business/contracts.md) | Cars, the 5-step driver wizard, contracts and the financial recap/PDF |
| | [business/payments-and-arrears.md](business/payments-and-arrears.md), [admin-quick-access.md](business/admin-quick-access.md), [ocpp-integration.md](business/ocpp-integration.md) | Payments per country, unpaid-day escalation, payments by day and receipts, follow-up shortcuts, future charging billing |
| | [business/legacy-charging-network-spec.md](business/legacy-charging-network-spec.md) | **Obsolete** spec of a different product (kept for history) |
| Architecture | [architecture/system-overview.md](architecture/system-overview.md) | Components, runtime, data flow, tech stack |
| | [architecture/module-boundaries.md](architecture/module-boundaries.md) | Allowed dependencies between modules (backend and frontend) |
| Backend | [backend/architecture.md](backend/architecture.md) | Package layout and layering |
| | [backend/standards.md](backend/standards.md) | How to write backend code |
| Frontend | [frontend/architecture.md](frontend/architecture.md) | Folder layout, data flow, state |
| | [frontend/standards.md](frontend/standards.md) | How to write frontend code |
| Public website | [web/architecture.md](web/architecture.md) | The NDZela public site in `web/` |
| Mobile | [mobile/architecture.md](mobile/architecture.md) | Flutter + Riverpod driver app, scope and API contract (not started) |
| Multi-tenancy | [multi-tenancy/multi-tenancy.md](multi-tenancy/multi-tenancy.md) | Country isolation rules and design |
| Auth | [auth/authentication.md](auth/authentication.md) | Sign-in, tokens, sessions, MFA |
| | [auth/authorization.md](auth/authorization.md) | How access decisions are made |
| | [auth/roles-and-permissions.md](auth/roles-and-permissions.md) | Permission catalogue, seeded roles, change procedure |
| Database | [database/database.md](database/database.md) | Schema, Flyway rules, conventions, money |
| API | [api/conventions.md](api/conventions.md) | REST conventions, error format, DTOs |
| Testing | [testing/strategy.md](testing/strategy.md) | What to test, how, required isolation tests |
| Infrastructure | [infrastructure/infrastructure-and-deployment.md](infrastructure/infrastructure-and-deployment.md) | Environments, config, Docker, deployment |
| Migration of the old platform | [migration/legacy-import.md](migration/legacy-import.md) | The old Firebase platform, the dry run on 2 riders, the decisions before the real import |
| Workflow | [workflow/git-workflow.md](workflow/git-workflow.md) | Branches, commits, reviews, definition of done |
| Decisions | [adr/README.md](adr/README.md) | Architecture Decision Records |
| Standards | [standards/forbidden-patterns.md](standards/forbidden-patterns.md) | What must never be done |
| Findings | [findings/architecture-findings.md](findings/architecture-findings.md) | Existing risks, inconsistencies, decisions needed |

## Keeping the docs true

- A change that alters a rule, a schema convention, a permission, or an API contract updates the matching doc in the same commit.
- A new architectural decision gets an ADR. A superseded decision is not deleted: mark it `Superseded by ADR-XXXX`.
- Docs describe *rules and reasons*, not code listings. If a paragraph would just repeat the code, delete it.
