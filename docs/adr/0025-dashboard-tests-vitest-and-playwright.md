# ADR-0025: Dashboard tests are Vitest with Testing Library, and Playwright with a mocked API

**Status:** Accepted · **Date:** 2026-10-20

**Context.** The dashboard had only lint and typecheck (finding C8). Money screens (payments, contracts) were growing with no automated check of their rules or flows.

**Decision.**
- **Unit and component tests: Vitest + React Testing Library**, environment `happy-dom` (jsdom's optional `canvas` peer breaks npm's resolver here), files `src/**/*.test.ts(x)` beside the code. They cover pure logic (`lib/`, zod schemas, request building) and small components. `npm test`.
- **End-to-end: Playwright (Chromium)**, files in `dashboard/e2e/`. The app is built and served on port 3100 (a separate port and a production build, so a running `next dev` is never disturbed) and **every call to `/api/v1` is answered in the browser** by `MockApi` (`e2e/support/mock-api.ts`). The flows therefore need no backend, database or secrets, run in about ten seconds, and still check what the user sees and the exact request the screen sends. `npm run test:e2e`.
- The backend's integration tests remain the authority on business rules and permissions; the e2e mocks must follow the API contract ([api conventions](../api/conventions.md)).
- CI (`.github/workflows/dashboard.yml`) runs lint, typecheck, unit tests, the build and the e2e flows.

**Consequences.** (+) Regressions in forms, permission-based rendering and request shapes are caught before review; the flows document the screens. (-) Mocks can drift from the real API (a contract test or a run against a seeded backend is the next step if that happens); Playwright adds a browser download to CI; first run builds the app (about a minute).

**Alternatives considered.** Jest (slower, weaker ESM and TypeScript story with this Next.js); Cypress; e2e against the real backend with Testcontainers (valuable later, much heavier: OTP and authenticator codes need test hooks).
