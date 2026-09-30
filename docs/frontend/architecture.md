# Frontend architecture

Next.js 16 (App Router), React 19, TypeScript `strict`, Tailwind 4, react-hook-form + zod, zustand, lucide-react. Path alias `@/*` -> `src/*`.
**This Next.js version has breaking changes: read `frontend/node_modules/next/dist/docs/` (and `frontend/AGENTS.md`) before writing Next.js code.**

## Layout (Implemented)

```
src/app/                 routes: (auth)/login, (auth)/reset-password, (dashboard)/{page,users,roles,profile,settings}; layouts; globals.css
src/features/<feature>/  auth, users, roles, access, profile, notifications, settings, tenancy
    components/ hooks/ services/ schemas/ store/ lib/ types.ts      (only the folders a feature needs)
src/components/ui/       Button, Input, FormField, Dialog, Dropdown, Table, Card, Badge, Avatar, Select, FilterTabs, ConfirmDialog, Toaster...
src/components/layout/   Sidebar (collapsible), Header, nav-items.ts
src/hooks/               use-fetch, use-toast   (generic)
src/lib/                 utils (cn), errors, time, media, image, api/{api-client,api-error}
src/config/env.ts        zod-validated NEXT_PUBLIC_* variables
public/images/           logo.png, image.png
```

Feature responsibilities: `auth` (login flow, session hook, auth store, reset password), `access` (permission catalogue, `usePermissions`,
`RequirePermission`, permission matrix, role chips), `users`, `roles`, `profile`, `notifications`, `settings` (change password, MFA status).

## Data flow

1. A page component calls a feature **service** (`features/<f>/services/*.service.ts`), which uses the single `apiClient`.
2. `apiClient` sends JSON with `credentials: "include"`, adds `Authorization: Bearer <access token>` and `X-Request-ID`, and on a 401
   performs **one shared silent refresh** via the HttpOnly refresh cookie, then retries. If the refresh fails, the session is cleared.
3. Server errors arrive as the standard error JSON ([api conventions](../api/conventions.md)) wrapped in `ApiException`.
   `lib/errors.getErrorMessage` maps `code` to a French message. Server text is never shown directly.
4. Loading is client-side with the `useFetch` hook (`data, error, isLoading, reload, setData`). No server-side data fetching or query cache exists yet.

## State

- `zustand` `useAuthStore`: `status` (`unknown | authenticated | anonymous`), `accessToken` (memory only), `user` (profile + `roles` + `permissions`).
  **Nothing sensitive is stored in localStorage/sessionStorage.** After a reload the session is restored from the refresh cookie (`useRequireSession`).
- Component-local state for everything else. Do not add a global store for data that belongs to one screen.

## Access control in the UI

`usePermissions().can(PERMISSIONS.X)` and `<RequirePermission>` decide what to show; `nav-items.ts` filters the sidebar by permission.
This is **usability only**; the backend enforces every rule. Never gate on role names.

## Styling and design system

Tailwind utilities; brand tokens in `globals.css` (`--color-primary` orange `#F97316`, `--color-sidebar` `#0F172A`); fonts Inter and Bricolage
Grotesque (`font-display`). Reuse `components/ui` instead of raw HTML controls. The shell: collapsible sidebar with logo, header with notification bell
and avatar-only profile menu.

## Environment

`NEXT_PUBLIC_API_URL` (e.g. `http://localhost:8080/api/v1`) is the only frontend variable; validated at startup in `config/env.ts`.

## Tenancy in the UI (Implemented)

`features/tenancy`: the header shows a country selector only for a global user ("Tous les pays" or a country) and a read-only country label for country staff. The selection lives in `useCountrySelectionStore` (session only, not sensitive) and is sent as `X-Country` from one place: a header provider registered on `apiClient` (`registerHeaderProvider`), only for a global user. The dashboard layout keys `<main>` by the selection so pages reload their data. The country list request opts out of the header (`sendContext: false`). The header and the users list show the country flag (`CountryFlag`: local SVG in `public/flags` for Gabon and Mali, emoji fallback for other countries). The invitation form always lets a global user choose the country (default: the header selection).

Phone numbers are entered with a country-code dropdown (`PhoneField`, codes from the country list) and stored as one international string such as `+241 62821855`; `lib/phone.ts` splits and joins them and validates 6 to 12 digits.
