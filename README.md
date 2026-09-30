# NDZela Admin

Admin back office: authentication with email OTP and TOTP MFA, user and role management, profile and notifications.

| Part | Stack |
| --- | --- |
| `backend/` | Java 21, Spring Boot 4, PostgreSQL (Flyway), Redis, JWT |
| `frontend/` | Next.js (App Router), React 19, Tailwind 4, react-hook-form + zod, zustand |
| `docker-compose.yml` | PostgreSQL 16 (port 5433) and Redis 7 for local development |

## Getting started

Prerequisites: JDK 21, Maven, Node.js 20+, Docker.

```bash
# 1. Infrastructure
docker compose up -d

# 2. Backend (http://localhost:8080)
cd backend
cp .env.example .env        # fill in the secrets, see the comments inside
mvn spring-boot:run

# 3. Frontend (http://localhost:3000)
cd frontend
cp .env.example .env.local  # NEXT_PUBLIC_API_URL must point at the backend
npm install
npm run dev
```

Useful checks: `mvn test` (backend), `npm run lint && npm run typecheck` (frontend).

## Backend layout

Package by feature, each feature split the same way. Base package: `com.ndzela.admin`.

```
auth/  user/  profile/  notification/      one package per feature
  api/            controllers, request/response DTOs (api/dto)
  application/    services and use cases
  domain/         entities, repositories (only where the feature owns data)
security/         JWT, Spring Security config, current caller, brute-force protection
common/           exceptions and their JSON handler, web filters, shared validation
config/           typed application properties and small framework configs
infrastructure/   adapters to the outside world: email, file storage, encryption
bootstrap/        first-run data (initial super admin)
```

Rules of thumb: controllers only talk to services in `application`; `domain` never imports from `api`;
a feature reaches another feature through its service, not its repository. The database schema is owned by
Flyway (`src/main/resources/db/migration`); add a new `V<n>__name.sql`, never edit an applied one.

## Frontend layout

```
src/app/            routes only, kept thin
src/features/       one folder per feature: auth, users, roles, access, profile, notifications, settings
  <feature>/components  hooks  services  schemas  store  lib  types
src/components/     ui/ (shared building blocks) and layout/ (sidebar, header)
src/hooks/          generic hooks (useFetch, useToast)
src/lib/            generic helpers and the API client (lib/api)
src/config/         validated environment variables
```

A feature may import from `components/ui`, `hooks`, `lib` and other features' public services and types, but
`components/ui` never imports from a feature.

## Security model

- The access token lives in memory only. The refresh token is an HttpOnly, SameSite cookie set by the backend.
- Sign-in flow: password, then email OTP, then TOTP (set up on first login).
- Access is decided by permissions (`users:read`), never by role names. Roles are titles that bundle permissions.
  Guard a page by adding it to `frontend/src/components/layout/nav-items.ts` with its `permission`.
- Secrets (`JWT_SECRET`, `MFA_ENCRYPTION_KEY`, mail credentials) have no defaults: the backend refuses to start
  without them. Never commit a real `.env`.

## API documentation

Set `SWAGGER_ENABLED=true` in `backend/.env` to expose Swagger UI at `/swagger-ui/index.html`.
