# Frontend standards

## TypeScript and files
- `strict` is on. No `any`; no non-null `!` to silence errors; no `@ts-ignore`. Use `unknown` and narrow.
- One component per file, named export, file name `kebab-case.tsx` matching the component. Keep files small (~150 lines). Split a growing component by responsibility.
- Client components declare `"use client"`; keep it only where hooks or browser APIs are needed.
- Imports use the `@/` alias, ordered: external, then `@/features`, `@/components`, `@/hooks`, `@/lib`. No deep relative `../../` chains.
- Code, identifiers and comments in English. **User-visible text in French.**

## Components
- Reuse `components/ui` (Button, Input, FormField, Dialog...). Do not restyle raw `<button>`/`<input>` in features.
- A new generic component is created **only** when two features need it. Feature-specific UI stays in the feature.
- Accessibility: buttons that only show an icon get `aria-label`; dialogs are labelled; form errors are associated with their fields.

## Forms
react-hook-form + a zod schema in `features/<f>/schemas`. Mirror the backend rule for usability (e.g. `password.ts` mirrors `@StrongPassword`),
but the backend response is the truth. On submit failure show `getErrorMessage(error, "fallback")`.

## Services and API access
- All HTTP goes through `apiClient`. No `fetch` in components. One service object per feature (`usersService`, `rolesService`...).
- Services are thin (build the request, map blank strings to null). No business rules and no permission logic in services.
- New endpoint = new method on the feature's service + a typed response in the feature's `types.ts`.

## Errors and feedback
- Map every new backend error `code` in `lib/errors.ts` (French). Show feedback with `useToast`. Never display raw server messages.
- Handle `SESSION_EXPIRED` via the existing `apiClient` path; do not build a second refresh flow.

## Permissions
- The permission names in `features/access/lib/permissions.ts` must match the database ([roles-and-permissions](../auth/roles-and-permissions.md)).
  Add both in the same change. Guard routes/nav/actions with `usePermissions` / `RequirePermission` / `nav-items`.
- Never make an access decision from a role name.

## Data loading
Use `useFetch` for now. Before adding many data-heavy screens, a decision on a query/cache library is required (open, see findings); do not introduce one ad hoc.

## Do not
- Store tokens or personal data in browser storage. Put secrets in `NEXT_PUBLIC_*`. Import a feature from `components/ui`/`lib`/`hooks`.
- Duplicate a util already in `lib/`. Add `console.log` to committed code.

## Definition of done (frontend)
`npm run lint` and `npm run typecheck` pass; the screen works for a user *with* and *without* the permission; error and empty states are handled; French copy reviewed; docs updated if a convention changed.
