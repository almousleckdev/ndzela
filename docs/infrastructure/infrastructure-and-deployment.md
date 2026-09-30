# Infrastructure and deployment

## Local development (Implemented)

`docker compose up -d` starts **PostgreSQL 16** (`localhost:5433`, db `ndzela_admin`, user/password `postgres`, **dev only**) and **Redis 7** (`localhost:6379`), both with health checks and named volumes.
Run the backend with `mvn spring-boot:run` (port 8080) and the frontend with `npm run dev` (port 3000). Setup steps are in the root `README.md`.

## Configuration and secrets

Backend settings are environment variables read by `application.yml` (optionally from `backend/.env`, git-ignored, template `backend/.env.example`). **Required, no default** (startup fails without them):
`JWT_SECRET` (base64, >= 256 bit: `openssl rand -base64 48`), `MFA_ENCRYPTION_KEY` (base64 of 32 bytes: `openssl rand -base64 32`), `MAIL_USERNAME`, `MAIL_PASSWORD`.

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL | local dev values |
| `REDIS_HOST`, `REDIS_PORT` | Redis | `localhost`, `6379` |
| `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` | Links in emails, CORS | `http://localhost:3000` |
| `COOKIE_SECURE` | `Secure` flag on the refresh cookie | `false` (**must be `true` behind HTTPS in every deployed environment**) |
| `MAIL_PROVIDER` | `smtp`, `resend` or `log` (console only, local dev) | `smtp` |
| `MAIL_HOST/PORT`, `RESEND_API_KEY`, `MAIL_FROM` | Email delivery | Gmail SMTP / Resend defaults |
| `UPLOAD_DIR` | Avatar storage directory | `./uploads` |
| `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` | First SUPER_ADMIN, created only on an empty database; enrolment of MFA required at first login | password empty = skipped |
| `SWAGGER_ENABLED` | Swagger UI / OpenAPI | `false` |

Frontend: `NEXT_PUBLIC_API_URL` in `frontend/.env.local` (template `.env.example`). Everything `NEXT_PUBLIC_*` is public: never put secrets in it.

Rules: no secret in the repository, in Docker images, or in logs. `.env*` files are git-ignored except `.env.example` (keep it in sync when adding a variable).

## Containers

- `backend/Dockerfile`: multi-stage (Maven build, then `eclipse-temurin:21-jre-alpine`), runs as a non-root user, exposes 8080. `backend/.dockerignore` excludes `target`, `uploads`, `.env`.
- **No frontend Dockerfile and no application entries in `docker-compose.yml` yet** (Open).

## Not yet defined (Open, needs the user's decisions)

Hosting/cloud provider and regions; SMS/push provider per country; payment-provider credentials and webhook endpoints per country; PDF generation library and storage for contract PDFs; scheduled jobs (arrears escalation) and their timezone handling; environments (dev/staging/production) and promotion; CI/CD; managed PostgreSQL/Redis, backups and retention; object storage for uploaded documents (local disk is not tenant-aware or scalable); secrets manager; TLS/domain layout (single domain vs per country); monitoring, alerting and log aggregation (Actuator health exists; `mail` health check makes `/actuator/health` report DOWN when SMTP is unreachable); rate limiting/WAF; payment-provider webhook exposure; disaster-recovery targets.

## Deployment rules (binding when deployment exists)

1. Flyway runs at application start; a deployment must not start an incompatible schema/app pair. Schema changes are backwards compatible with the previous app version for one release.
2. Production: HTTPS only, `COOKIE_SECURE=true`, Swagger off, real secrets, non-default database credentials, `MAIL_PROVIDER` not `log`.
3. Health, readiness and logs must be observable before go-live. Logs carry `requestId`.
4. Backups are tested by restoring them.
