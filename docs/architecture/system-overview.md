# System overview

## Components

| Component | Tech | Path | Status |
| --- | --- | --- | --- |
| Backend API | Java 21, Spring Boot 4.1, Spring Security 7, Spring Data JPA, Flyway | `backend/` | Implemented (admin foundation) |
| Web admin | Next.js 16 (App Router), React 19, TypeScript (strict), Tailwind 4, react-hook-form + zod, zustand | `dashboard/` | Implemented (admin foundation) |
| Driver mobile app | **Flutter + Riverpod** | not in this repo (yet) | Decided, not started; see [mobile](../mobile/architecture.md) |
| PostgreSQL 16 | system of record | `docker-compose.yml` (dev) | Implemented |
| Redis 7 | short-lived auth state (login challenges, OTP, refresh tokens, reset links, TOTP replay guard) | `docker-compose.yml` (dev) | Implemented |
| OCPP platform | separate product, already built | outside | Future: backend calls its APIs for charging billing (day/week/month/year), see [ocpp-integration](../business/ocpp-integration.md) |
| Payment providers | Airtel Money, Moov Money Gabon (Gabon); Orange Money, Moov Money Mali (Mali) | external | Decided, not built |
| PDF generation | contract PDF per country, server-side | in backend | Decided, library Open |

## Runtime picture

```
Browser (Next.js SPA-style client) --HTTPS JSON--> Backend API (/api/v1) --> PostgreSQL (business data, Flyway-owned schema)
                                                                       \--> Redis (ephemeral auth state)
                                                                       \--> SMTP / Resend (email)
                                                                       \--> local disk (avatars, /media/**)
Driver app (future) ------------HTTPS JSON--------------------------> Backend API (driver endpoints)
Payment providers (future, per country) <----API + webhooks----> Backend API
OCPP platform (future) <----API (charging billing)---- Backend API
```

The backend is a **modular monolith**: one deployable, split into feature packages with enforced boundaries
([module-boundaries.md](module-boundaries.md)). Do not split into microservices without an ADR.

## Cross-cutting principles

1. **Backend is the authority.** It decides authorization, tenant scope, validation of truth and money. The UI is a client.
2. **Tenant isolation is a system property**, not a per-query habit ([multi-tenancy](../multi-tenancy/multi-tenancy.md)).
3. **Permissions, not roles,** decide access ([authorization](../auth/authorization.md)).
4. **The database schema is owned by Flyway** ([database](../database/database.md)); Hibernate only validates it.
5. **One error format, one API style** ([api conventions](../api/conventions.md)).
6. **Secrets come from the environment**; the backend refuses to start without required ones ([infrastructure](../infrastructure/infrastructure-and-deployment.md)).
7. Stateless API: no server-side HTTP sessions. Auth state lives in a short-lived JWT and Redis-backed opaque tokens.

## Languages

Code, identifiers, API field names, error `code`s and server messages are **English**. The web UI text is **French**. The
backend currently also writes French text in in-app notifications (see findings). No i18n framework exists yet.
