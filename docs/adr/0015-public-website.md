# ADR-0015: The public website is a separate Next.js project

**Status:** Accepted, 2026-10-02.

**Context.** NDZela needs a public website. The admin (`dashboard/`) is an authenticated, tenant-scoped application with its own dependencies (tables, forms, stores, API client).

**Decision.** The website lives in `web/`, a separate Next.js project with its own package and deployment, using the same stack conventions as the dashboard (App Router, `src/`, Tailwind 4, `cn`, feature folders) but none of its code. It is static-first, has no authentication, and does not call the admin backend. Forms, i18n and a CMS are not added until needed; each is a new decision.

**Consequences.** Positive: the public surface cannot leak admin code or tenant data, and each app deploys and scales on its own. Negative: shared design tokens (colours, fonts) are duplicated; a change to the brand must be made in both. Alternative rejected: a route group inside the dashboard (mixes a public site with an authenticated app and ships admin code to visitors).
