# Roles and permissions

Source of truth for the catalogue: Flyway migrations (`V1`, `V3`, `V4`, `V6`). The frontend mirror is `frontend/src/features/access/lib/permissions.ts`
(manual sync, see findings). Roles are **shared across all countries** (Decided): the user's country, not the role, limits the data.

## Permission catalogue (Implemented)

| Resource | Actions | Backing feature today |
| --- | --- | --- |
| `users` | read, create, update, deactivate, lock, delete | Implemented |
| `roles` | read, create, update, delete | Implemented |
| `transactions` | read, create, approve, cancel | **Seeded only** (no module yet) |
| `accounts` | read, create, update | **Seeded only** |
| `cash` | operate | **Seeded only** |
| `reports` | read, export | **Seeded only** |
| `countries` | read | Implemented (granted to every role by migration V7; lists countries for the super admin's selector) |

`users`/`roles` are the **administrative resources** subject to anti-escalation in `AccessPolicy`.

## Seeded roles (Implemented)

| Role | Intent | Permissions |
| --- | --- | --- |
| `SUPER_ADMIN` | Global, all permissions, protected (cannot be edited or deleted) | all |
| `ADMIN` | Manages people, not money | users: read, create, update, deactivate, lock, delete; roles: read |
| `GESTIONNAIRE` | Manager of money movement | transactions: read, create, approve, cancel; accounts: read, create, update; reports: read, export; users: read |
| `COMPTABLE` | Accountant, cannot move money | transactions: read; accounts: read, update; reports: read, export |
| `CAISSIER` | Cashier | transactions: read, create; cash: operate |
| `VIEWER` | Read-only | users, roles, transactions, accounts, reports: read |

Role names are `UPPER_SNAKE_CASE` (`^[A-Z][A-Z0-9_]{2,49}$`, same rule on backend and frontend). Custom roles can be created by holders of `roles:create`.

## Procedure to add or change a permission

1. New Flyway migration inserting into `permissions` (`ON CONFLICT (name) DO NOTHING`) and, if needed, `role_permissions`. `SUPER_ADMIN` must always end up with **all** permissions.
   Never edit an applied migration.
2. Guard the endpoint with `@PreAuthorize`.
3. Add the constant, label and group in the frontend `permissions.ts` (resource labels, action labels, ordering).
4. Update this document. If it is a new administrative resource, add it to `AccessPolicy.ADMINISTRATIVE_RESOURCES` (backend) and the frontend list.
5. Add or extend tests (see [testing](../testing/strategy.md)).

## Planned (Proposed names, role matrix Open)

| Resource | Actions |
| --- | --- |
| `vehicles` | read, create, update, delete |
| `drivers` | read, create, update, suspend, delete |
| `contracts` | read, create, update, terminate, print |
| `payments` | read, record (manual), reverse |
| `arrears` | read, manage |
| `countries` | create, update (super admin, when a second country is onboarded; `read` already exists) |

Decided assignments: **GESTIONNAIRE** creates, edits and deletes drivers, vehicles and contracts and manages the training-centre list; **ADMIN** suspends and reactivates drivers and lifts arrears suspensions. Driver-facing capabilities are **not** staff permissions ([ADR-0008](../adr/0008-driver-accounts-separate-from-staff.md)). The product owner must confirm the role matrix per module before implementation.
