# Roles and permissions

Source of truth for the catalogue: Flyway migrations (`V1`, `V3`, `V4`, `V6`). The frontend mirror is `dashboard/src/features/access/lib/permissions.ts`
(manual sync, see findings). Roles are **shared across all countries** (Decided): the user's country, not the role, limits the data.

## Permission catalogue (Implemented)

| Resource | Actions | Backing feature today |
| --- | --- | --- |
| `users` | read, create, update, deactivate, lock, delete, export | Implemented (`export` : SUPER_ADMIN, ADMIN) |
| `roles` | read, create, update, delete | Implemented |
| `transactions` | read, create, approve, cancel | **Seeded only** (no module yet) |
| `accounts` | read, create, update | **Seeded only** |
| `cash` | operate | **Seeded only** |
| `reports` | read, export | **Seeded only** |
| `audit` | read, export | Implemented (SUPER_ADMIN, ADMIN) |
| `vehicles` | read, create, update, delete | Implemented (GESTIONNAIRE all four; ADMIN, COMPTABLE, VIEWER read) |
| `drivers` (purge), `incidents` (declare) | purge, declare | Implemented, V55 (SUPER_ADMIN, ADMIN; assignable to any role). `drivers:purge` erases a driver with their money; `incidents:declare` declares and cancels half days and stops. |
| `payments`, `arrears` | payments: read, create, reverse, export; arrears: manage | Implemented (read: finance roles and VIEWER; create: GESTIONNAIRE, CAISSIER; reverse and export: ADMIN, export also COMPTABLE; arrears:manage: ADMIN). `payments:export` also gates `GET /payments/accounts/export` (the Progression/Arrears accounts view), separate from the transaction-history export. |
| `charging` | read, create, reverse, export, manage | Implemented (read: finance roles and VIEWER; create: GESTIONNAIRE, CAISSIER; reverse: ADMIN; export: ADMIN, COMPTABLE; manage stations: GESTIONNAIRE, ADMIN) |
| `contracts` | read, print, update | Implemented (GESTIONNAIRE, ADMIN, COMPTABLE: read and print; VIEWER: read; GESTIONNAIRE and ADMIN also update) |
| `drivers` | read, create, update, suspend, block, export, delete, unlock | Implemented (GESTIONNAIRE: read, create, update, delete; ADMIN: read, suspend, block, unlock; COMPTABLE, VIEWER read). `unlock` clears a login-attempt lockout only, never the `DriverStatus` set by suspend/block. |
| `training_centers` | manage | Implemented (GESTIONNAIRE) |
| `countries` | read | Implemented (granted to every role by migration V7; lists countries for the super admin's selector) |

`users`/`roles` are the **administrative resources** subject to anti-escalation in `AccessPolicy`.

## Seeded roles (Implemented)

| Role | Intent | Permissions |
| --- | --- | --- |
| `SUPER_ADMIN` | Seeded as global, all permissions, protected (cannot be edited or deleted) | all |
| `ADMIN` | Manages people, not money | users: read, create, update, deactivate, lock, delete, export; roles: read; drivers: suspend, block, unlock; audit: read, export; payments: read, reverse, export |
| `GESTIONNAIRE` | Manager of money movement | transactions: read, create, approve, cancel; accounts: read, create, update; reports: read, export; users: read |
| `COMPTABLE` | Accountant, cannot move money | transactions: read; accounts: read, update; reports: read, export |
| `CAISSIER` | Cashier | transactions: read, create; cash: operate |
| `VIEWER` | Read-only | users, roles, transactions, accounts, reports: read |

Role names are `UPPER_SNAKE_CASE` (`^[A-Z][A-Z0-9_]{2,49}$`, same rule on backend and frontend). Custom roles can be created by holders of `roles:create`.

Being global, or scoped to several countries, is independent of role ([ADR-0023](../adr/0023-multi-country-staff.md)): any role may be assigned that way, not only `SUPER_ADMIN` — only `SUPER_ADMIN` is seeded global by default.

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
| `contracts` | read, create, update, terminate, print |
| `payments` | read, record (manual), reverse |
| `arrears` | read, manage |
| `countries` | create, update (super admin, when a second country is onboarded; `read` already exists) |

Decided assignments: **GESTIONNAIRE** creates, edits and deletes drivers, vehicles and contracts and manages the training-centre list; **ADMIN** suspends and reactivates drivers and lifts arrears suspensions. Driver-facing capabilities are **not** staff permissions ([ADR-0008](../adr/0008-driver-accounts-separate-from-staff.md)). The product owner must confirm the role matrix per module before implementation.
