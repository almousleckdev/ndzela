# Module boundaries

Boundaries keep the codebase changeable as modules (fleet, contracts, payments...) are added. They are **rules**, and the
current deviations are listed in [findings](../findings/architecture-findings.md).

## Backend (package by feature)

Base package `com.ndzela.admin`. Features: `auth`, `user` (users and roles), `profile`, `notification`, `country`, `audit`, `vehicle`, `driver`; future: `contract`, `payment`, `report`. `driver` uses `vehicle` and `country` only through their application services; `audit` is called by every feature. Shared: `security`, `tenant`, `common`, `config`, `infrastructure`, `bootstrap`.

```
<feature>/api          controllers + request/response DTOs (api/dto)     -> may call: same feature's application, common
<feature>/application  services / use cases, transactions, orchestration -> may call: same feature's domain, other features' *application services*, security (CurrentActor), common, infrastructure
<feature>/application/model  value records and flow enums of the services (results, inputs, tokens)
<feature>/domain       entities, repositories, domain enums              -> may depend on: nothing above it; references other features by ID
<feature>/domain/snapshot    records serialised as JSON snapshots (e.g. contract)
```

Each file holds one top-level type; the full placement table is in [backend standards](../backend/standards.md#language-and-style).

Rules:

1. **Direction is `api -> application -> domain`.** Never the reverse. `domain` knows nothing about HTTP or DTOs.
2. **A feature never uses another feature's repository or entity internals.** It calls the other feature's application service.
   Cross-feature references between tables use IDs (e.g. `notification` stores `user_id`, not a `User` entity).
   *Allowed exception:* the `user.domain` types (`User`, `Role`, `Permission`, `UserStatus`) are the platform's identity model and may be
   read by `auth`, `profile`, `security`, `notification`.
3. **No cycles between features' application layers.** If A needs B and B needs A, extract the shared part.
4. **`common`, `config`, `infrastructure` never depend on a feature.** `infrastructure` holds adapters to the outside (email, storage,
   encryption); it exposes an interface or a small class, and features depend on it, never the reverse.
5. **`security` is cross-cutting infrastructure**: it may load a user (`user.domain`) but must not depend on feature application
   services. Side effects such as notifications are triggered by the feature, or via an event, not from `security`.
6. Every **tenant-owned** feature applies the tenancy mechanism ([multi-tenancy](../multi-tenancy/multi-tenancy.md)): business entities extend `tenant.TenantOwned`. `tenant` is cross-cutting like `security`: it depends on `country.domain`, `user.domain` and `common`, never on a feature's application layer, and `security` may use it.

## Frontend (feature folders)

```
src/app            routes only: compose a feature's page component; no business logic
src/features/<f>   components, hooks, services, schemas, store, lib, types of ONE feature
src/components/ui  generic building blocks, no business knowledge, never import from features
src/components/layout   the application shell (sidebar, header, nav items): the only shared place allowed to import features
src/hooks, src/lib, src/config   generic and feature-agnostic
```

Rules:

1. `components/ui`, `hooks`, `lib`, `config` **never import from `features/`.**
2. A feature imports another feature only through that feature's **service, type, schema or hook** that is meant to be shared
   (e.g. `features/access` permissions, `features/auth` session). Never another feature's private component internals.
3. Avoid mutual imports between features (A imports B and B imports A). Extract the shared piece or invert the dependency.
4. Pages in `app/` stay thin: `page.tsx` renders one feature component.
5. New page = new route in `app/`, a component in its feature, an entry in `components/layout/nav-items.ts` with the guarding permission.

## Backend/frontend contract

The only coupling between the two is the HTTP API ([api conventions](../api/conventions.md)) and the permission names
([roles-and-permissions](../auth/roles-and-permissions.md)). Neither side imports the other's code. Types are duplicated on purpose
until a contract-generation approach is decided (see findings).
