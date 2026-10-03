# Public website architecture

**Status: scaffolded (2026-10-02). Content and design are to be provided.** Decision record: [ADR-0015](../adr/0015-public-website.md).

`web/` is the company's public website (NDZela). It is **not** the admin (`dashboard/`) and shares no code, package or deployment with it.

- **Stack:** Next.js (App Router, `src/` folder, TypeScript strict), React 19, Tailwind 4, `clsx` + `tailwind-merge` (`cn`), `lucide-react`, `next/font` (Bricolage Grotesque for headings, IBM Plex Sans for text, as in the admin). Package manager **pnpm**.
- **Folders:** `app/` holds routing only (pages stay thin and compose a feature); `components/ui` and `components/layout` are shared pieces; `features/<name>/components` hold the sections of a page; `config/site.ts` is the single place for company facts, navigation and contact; `lib/` pure helpers. Cross-feature imports go through a feature's public components, never its internals. One responsibility per file, aim for under ~200 lines.
- **Rendering:** static by default; add dynamic behaviour only when a page needs it. Metadata through the `metadata` export and `robots.ts` / `sitemap.ts` (list a new page in `app/sitemap.ts`).
- **Config:** `NEXT_PUBLIC_SITE_URL` (see `.env.example`). Never put a secret in a `NEXT_PUBLIC_` variable.
- **Language:** French only for now (`lang="fr"`); no i18n library until a second language is decided.
- **Boundaries:** the site never reads tenant data. It only *sends* two things to the backend's public endpoints (`/api/v1/public/contact` and `/applications`, see `docs/api/conventions.md`): the contact form and the driver application with its documents. `lib/api.ts` is the only code that calls the backend (`NEXT_PUBLIC_API_URL`); forms use `react-hook-form` + `zod`, files are validated in the browser (type, 5 MB) and again by the server. Each form has a hidden trap field for robots. The backend's `CORS_ALLOWED_ORIGINS` must list the website's address.
- **Legal pages (store requirement):** `/confidentialite`, `/conditions`, `/suppression-compte`, written as data in `features/legal/content/*` and drawn by `LegalPage`; linked from the footer and the sitemap, and from the mobile app (`SITE_URL`). The texts are a **draft to be reviewed by a lawyer** (retention periods are described, not quantified).
- **App showcase:** the home page's "Application" section (`features/app`) shows the screens in `public/appui` and the App Store / Google Play buttons (`components/ui/store-button.tsx`). The store links are `siteConfig.app.appStore` and `googlePlay` in `config/site.ts`; while they are `null` touching a button explains that the app is coming and links to WhatsApp: set them when the app is published. The buttons are repeated in the footer.
- **Pages:** `/` (hero, offers, driver journey, what is included, app, charging, contact band), `/devenir-chauffeur` (application), `/contact` (form, details, hours with a live open/closed badge). A floating WhatsApp button is shown on every page (`components/ui/whatsapp-button.tsx`, number in `config/site.ts`). Photos live in `public/images/cars` and `public/images/charge`. The company's RCCM is deliberately not shown.
- **Brand:** NDZela orange (`--color-primary` in `globals.css`). Angata (Mali) has no public site yet.
- **Checks:** `pnpm lint && pnpm typecheck && pnpm build`.
