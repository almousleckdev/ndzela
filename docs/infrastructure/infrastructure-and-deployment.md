# Infrastructure and deployment

## Local development (Implemented)

`docker compose up -d` starts **PostgreSQL 16** (`localhost:5433`, db `ndzela_admin`, user/password `postgres`, **dev only**) and **Redis 7** (`localhost:6379`), both with health checks and named volumes.
Run the backend with `mvn spring-boot:run` (port 8080) and the frontend with `npm run dev` (port 3000). Setup steps are in the root `README.md`.

## Configuration and secrets

Backend settings are environment variables read by `application.yml` (optionally from `backend/.env`, git-ignored, template `backend/.env.example`). **Required, no default** (startup fails without them):
`JWT_SECRET` (base64, >= 256 bit: `openssl rand -base64 48`), `MFA_ENCRYPTION_KEY` (base64 of 32 bytes: `openssl rand -base64 32`), and the four `CLOUDINARY_*`. `MAIL_USERNAME` and `MAIL_PASSWORD` are only needed with `MAIL_PROVIDER=smtp`.

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL | local dev values |
| `REDIS_HOST`, `REDIS_PORT` | Redis | `localhost`, `6379` |
| `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` | Links in emails, CORS | `http://localhost:3000` |
| `PUBLIC_SITE_ORIGINS` | Origins of the public website, allowed to post to `/api/v1/public/**` only and never with credentials (`CORS_ALLOWED_ORIGINS` is the web admin and may send the cookie) | `http://localhost:3001` |
| `PORT` | Port to listen on (set by Railway and similar hosts) | `8080` |
| `REDIS_PASSWORD` | Redis password, when it has one | empty |
| `COOKIE_SECURE` | `Secure` flag on the refresh cookie | `false` (**must be `true` behind HTTPS in every deployed environment**) |
| `APP_ENVIRONMENT` | `development`, `staging` or `production` ([ADR-0024](../adr/0024-explicit-deployment-environment.md)). **Production** turns on the strict startup checks (no default or empty `DB_PASSWORD`, no `PVIT_SANDBOX_AMOUNT`, every PVit credential present). **Staging** is a deployed test environment (HTTPS, real URL) that may use the PVit sandbox. Unset: `COOKIE_SECURE=true` means production, so an existing deployment keeps its checks | empty |
| `MAIL_PROVIDER` | `smtp`, `resend` or `log` (console only, local dev) | `smtp` |
| `MAIL_HOST/PORT`, `RESEND_API_KEY`, `MAIL_FROM` | Email delivery | Gmail SMTP / Resend defaults |
| `FORWARD_HEADERS_STRATEGY` | `native` behind a reverse proxy so the rate limits and the audit trail see the visitor's address; `none` when the app is reachable directly (the forwarded headers could be forged) | `none` |
| `RATE_LIMIT_PVIT_CALLBACK` | PVit callbacks one address may send per minute (429 beyond; the recheck job settles any refused) | `120` |
| `RATE_LIMIT_RESET_PER_ADDRESS`, `RATE_LIMIT_RESET_PER_ACCOUNT` | Password-reset requests per hour: one address can send, one account receives (staff `/auth/forgot-password`, driver `/driver/auth/forgot-password`) | `10`, `3` |
| `RATE_LIMIT_LOGIN_PER_IP` | Login attempts one address may send per minute, staff and driver combined (429 beyond; checked before account lookup) | `20` |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `CLOUDINARY_ROOT_FOLDER` | File storage ([ADR-0016](../adr/0016-cloudinary-file-storage.md)). Credentials from the Cloudinary console; **required, no defaults**; the secret never leaves the backend. The root folder separates environments in one account (`ndzela-dev`, `ndzela-prod`); files live under `<root>/<country code>/...`. `CLOUDINARY_LIVE_TEST=true` enables the opt-in test that talks to the real account | - |
| `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` | First SUPER_ADMIN, created only on an empty database; enrolment of MFA required at first login | password empty = skipped |
| `PVIT_*` | PVit mobile money ([ADR-0014](../adr/0014-pvit-mobile-money-payments.md)): `PVIT_BASE_URL`, `PVIT_OPERATION_ACCOUNT_CODE`, `PVIT_SECRET_PASSWORD`, `PVIT_CODE_URL_SECRET/PAYMENT/STATUS/FEES/KYC`, `PVIT_CALLBACK_URL_CODE` (the code of the public callback URL registered in the PVit dashboard, type Callback, pointing at `/api/callback`), `PVIT_AGENT`, `PVIT_PRODUCT`, `PVIT_OWNER_CHARGE` (`CUSTOMER` or `MERCHANT`), `PVIT_SANDBOX_AMOUNT` (sandbox only; empty in production: the application **refuses to start** with it set when `APP_ENVIRONMENT` is production) | PVit sandbox values from the merchant space |
| `OCPP_BASE_URL`, `OCPP_FLEET_API_KEY` (falls back to `OCPP_API_KEY`), `OCPP_WEBHOOK_SECRET`, `OCPP_WEBHOOK_HEADER`, `OCPP_COUNTRY`, `OCPP_SYNC_ENABLED`, `OCPP_SYNC_INTERVAL` | OCPP platform ([ADR-0017](../adr/0017-ocpp-sessions-from-the-fleet-api-sync.md)). The Fleet key is read-only and scoped by the platform to granted charge points; with no key nothing syncs. The webhook is refused while its secret is empty. `OCPP_LIVE_TEST=true` enables the opt-in test that reads the real API | `https://zella-ocpp.com`, empty, empty, `X-Webhook-Secret`, `GA`, `true`, `PT10S` |
| `DRIVER_LOCK_AFTER_UNPAID_DAYS` | From how many unpaid rent days a driver is limited to the payment screen | `3` |
| `SWAGGER_ENABLED` | Swagger UI / OpenAPI | `false` |

Frontend: `NEXT_PUBLIC_API_URL` in `dashboard/.env.local` (template `.env.example`). Everything `NEXT_PUBLIC_*` is public: never put secrets in it.

Rules: no secret in the repository, in Docker images, or in logs. `.env*` files are git-ignored except `.env.example` (keep it in sync when adding a variable).

## Containers

- `backend/Dockerfile`: multi-stage (Maven build, then `eclipse-temurin:21-jre-alpine`), runs as a non-root user, exposes 8080. `backend/.dockerignore` excludes `target`, `uploads`, `.env`.
- **No frontend Dockerfile and no application entries in `docker-compose.yml` yet** (Open).

## Environments and payment go-live (PVit)

Three environments, told apart by `APP_ENVIRONMENT` ([ADR-0024](../adr/0024-explicit-deployment-environment.md)):

| Environment | Backend | PVit | Driver app build |
| --- | --- | --- | --- |
| development | `mvn spring-boot:run`, local Postgres and Redis | Sandbox values, `PVIT_SANDBOX_AMOUNT=150` | `flutter run` |
| staging (the test deployment for TestFlight and Google Play internal testing) | A second Railway environment, `APP_ENVIRONMENT=staging`, `COOKIE_SECURE=true`, its own database, `CLOUDINARY_ROOT_FOLDER=ndzela-staging`, `MAIL_PROVIDER=resend` | **Sandbox** account and `PVIT_SANDBOX_AMOUNT=150`: payments succeed below 1 000 XAF and no real money moves | `env/staging.json` ([release guide](../mobile/release.md)) |
| production | The `production` Railway environment, `APP_ENVIRONMENT=production` | The **real** operation account, `PVIT_SANDBOX_AMOUNT` empty | `env/production.json` |

### Payment go-live (PVit)
Do these in order; each step is checked before the next.
1. **Test to test (staging).** With the sandbox, a test driver with unpaid days pays from the app: the attempt moves PENDING to SUCCESS through the callback, the ledger shows one `PAYMENT` and one `FEE` line, the receipt PDF opens, and the dashboard's Transactions table lists it. Repeat once with a refusal on the phone (FAILED) and once with the callback blocked (the one-minute recheck settles it).
2. **Production account.** In the PVit merchant space create the production operation account and its APIs; take the new `PVIT_OPERATION_ACCOUNT_CODE`, the secret password, every `PVIT_CODE_URL_*` (secret, payment, status, fees, KYC) and register the public callback `https://api.ndzela.com/api/callback` (type Callback) to get `PVIT_CALLBACK_URL_CODE`. None of these values is committed: set them only in Railway.
3. **Set the production variables:** `APP_ENVIRONMENT=production`, the PVit values above, `PVIT_OWNER_CHARGE` (`CUSTOMER`: the driver pays PVit's fee on top; `MERCHANT`: NDZELA absorbs it), `PVIT_SANDBOX_AMOUNT` **removed**. The application refuses to start (and Railway keeps the previous deployment) when one is missing or a sandbox amount is left.
4. **First real payment.** Use a driver with a small amount due (one rent day, or a day selected in the app), pay from a real phone, and check in this order: the callback arrives (`PVit callback received` in the logs), the attempt is SUCCESS, the amount in the ledger is what PVit confirmed, the fee line exists, the money reached the PVit balance, the receipt is correct, and a second callback (replay) records nothing.
5. **Network.** Restrict `/api/callback` to PVit's published addresses at the proxy when PVit supplies them (a forged callback is already harmless: the status API is the truth, see [ADR-0014](../adr/0014-pvit-mobile-money-payments.md)).
6. **Operate.** Watch for attempts that stay PENDING (`select reference, created_at from payment_intents where status = 'PENDING' and created_at < now() - interval '10 minutes'`) and for `Could not settle payment attempt` in the logs; staff can enter a payment by hand from the driver's page when PVit confirmed money the system could not record.

## Not yet defined (Open, needs the user's decisions)

Hosting for the dashboard and website, and the choice of Railway region (backend: [Railway deployment](railway-deployment.md)); SMS/push provider per country; payment-provider credentials and webhook endpoints per country; PDF generation library and storage for contract PDFs; scheduled jobs (arrears escalation) and their timezone handling; environments (dev/staging/production) and promotion; CD (CI exists, see the git workflow); managed PostgreSQL/Redis, backups and retention; secrets manager; TLS/domain layout (single domain vs per country); monitoring, alerting and log aggregation (Actuator health exists; `mail` health check makes `/actuator/health` report DOWN when SMTP is unreachable); rate limiting/WAF; payment-provider webhook exposure; disaster-recovery targets.

## Deployment rules (binding when deployment exists)

The backend is deployed to Railway: see [railway-deployment.md](railway-deployment.md).


1. Flyway runs at application start; a deployment must not start an incompatible schema/app pair. Schema changes are backwards compatible with the previous app version for one release.
2. Production: HTTPS only, a stable public callback address for PVit (restrict it to PVit's published addresses at the proxy), `PVIT_SANDBOX_AMOUNT` empty and the production operation account, `COOKIE_SECURE=true`, Swagger off, real secrets, non-default database credentials (the application **refuses to start** in production with an empty or `postgres` `DB_PASSWORD`, with a `PVIT_SANDBOX_AMOUNT`, or with a missing PVit credential), `MAIL_PROVIDER` not `log`, `FORWARD_HEADERS_STRATEGY=native` when behind a proxy.
   The web admin and the website send `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy`, HSTS and a CSP limited to `frame-ancestors`, `base-uri`, `form-action` and `object-src`. A `script-src` policy needs per-request nonces and therefore dynamic rendering of every page: open decision.
3. Health, readiness and logs must be observable before go-live. Logs carry `requestId`.
4. Backups are tested by restoring them.
