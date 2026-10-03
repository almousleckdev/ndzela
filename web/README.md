# NDZela public website

Next.js (App Router, `src/`), React 19, Tailwind 4, pnpm. It is a public, static-first site: no login, no tenant data, no calls to the admin backend. Architecture: [docs/web/architecture.md](../docs/web/architecture.md).

```bash
pnpm install
cp .env.example .env.local   # then set NEXT_PUBLIC_SITE_URL
pnpm dev                     # http://localhost:3000
pnpm lint && pnpm typecheck && pnpm build
```

Before writing Next.js code read the guides in `node_modules/next/dist/docs/` (see `AGENTS.md`).

```
src/
  app/          routes only: layout, pages, not-found, error, robots, sitemap, globals.css
  components/   ui/ (Container, ButtonLink, ...) and layout/ (SiteHeader, SiteFooter)
  features/     one folder per page or section group (home/components/hero.tsx)
  config/       site.ts: company facts, navigation, contact
  lib/          utils.ts (cn) and other pure helpers
public/         static assets (images/logo.png)
```
