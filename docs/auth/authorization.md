# Authorization

## Principle (Implemented)

**Access is decided on permissions, never on role names.** A permission is `resource:action` (`users:read`). A role is a named bundle of permissions
(a job title); users hold roles; the effective authorities of a request are the union of their roles' permissions (+ `ROLE_<NAME>` markers, which must not be used for decisions).

## Enforcement points

1. **Endpoint**: `@PreAuthorize("hasAuthority('<resource:action>')")` on every controller method (method security is enabled). Only `/api/v1/auth/**`, `/actuator/health`,
   Swagger (when enabled) and `GET /media/**` are public; everything else requires authentication (`SecurityConfig`).
2. **Service rules**: `AccessPolicy` prevents **privilege escalation** for the administrative resources `users` and `roles`:
   - you cannot grant or assign a role carrying an administrative permission you do not hold yourself;
   - you cannot manage (edit, block, delete...) a user whose roles carry administrative permissions beyond yours;
   - you cannot change your own roles, disable, block, delete or reset the MFA of your own account.
   Business permissions (transactions, accounts, reports, cash...) may be granted freely by anyone who can assign roles.
3. **Protected role**: `SUPER_ADMIN` cannot be edited or deleted; a role still assigned to users cannot be deleted.
4. **UI**: `usePermissions`/`RequirePermission`/nav filtering hide what the user cannot do. This is convenience only.

## Tenant dimension (Implemented)

Permissions say *what* a user may do; the **country** says *on which data*. Both apply. Country staff are limited to their own country
whatever their permissions; the SUPER_ADMIN is global. See [multi-tenancy](../multi-tenancy/multi-tenancy.md). A permission must never be
used to widen a user's country.

## Rules for changes

- A new endpoint without `@PreAuthorize` is a defect unless it is in the public list above and documented.
- New permissions follow the procedure in [roles-and-permissions](roles-and-permissions.md).
- Do not add role-name checks (`hasRole`, `role.name === ...`) except for the documented `SUPER_ADMIN` protection.
- The Roles page reads `GET /api/v1/roles/visible` (`roles:read`): role managers (holders of `roles:create`, `roles:update` or `roles:delete`) see every role, everyone else only the roles they hold.
- `GET /api/v1/roles` intentionally also allows `users:create`/`users:update` so the user form can list roles; keep such widenings explicit and documented.
