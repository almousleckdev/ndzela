# Deploying the backend to Railway

Decision: [ADR-0018](../adr/0018-railway-hosting-and-domains.md). Domains: the public website `ndzela.com` (and `www`), the web admin later at `dashboard.ndzela.com`, the API at `api.ndzela.com` (the DNS is at PlanetHoster; leave the existing records of `ndzela.com`, `www` and the mail alone).

## Services (one Railway project; a `production` and a `staging` environment with the same services)
| Service | What | Notes |
| --- | --- | --- |
| `backend` | The Dockerfile in `backend/` (Root Directory `backend`; `backend/railway.toml` sets the build, the health check `/actuator/health` and the restart policy) | One replica to start. Railway gives the port in `PORT` |
| `Postgres` | Railway PostgreSQL | Flyway builds the schema at the first start. Turn on backups |
| `Redis` | Railway Redis | Ephemeral auth state, rate limits, the sync lock |

Use the **private network** between services (the reference variables below do): no database or Redis port is exposed publicly.

## Variables of the `backend` service
Reference variables (`${{Service.NAME}}`) are filled by Railway. **Generate new production secrets**: never reuse a development value.

| Variable | Value |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DB_USERNAME`, `DB_PASSWORD` | `${{Postgres.PGUSER}}`, `${{Postgres.PGPASSWORD}}` |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | `${{Redis.REDISHOST}}`, `${{Redis.REDISPORT}}`, `${{Redis.REDISPASSWORD}}` |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `MFA_ENCRYPTION_KEY` | `openssl rand -base64 32` (losing it locks every enrolled user out of MFA: keep a copy in the team's secret store) |
| `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` | The first SUPER_ADMIN, created only when no user has that e-mail. **Delete `INITIAL_ADMIN_PASSWORD` once you have signed in** |
| `COOKIE_SECURE` | `true` |
| `APP_ENVIRONMENT` | `production` for the production environment, `staging` for the test environment (see the [environments table](infrastructure-and-deployment.md#environments-and-payment-go-live-pvit)) |
| `SWAGGER_ENABLED` | `false` |
| `FORWARD_HEADERS_STRATEGY` | `native` (Railway's proxy sets `X-Forwarded-*`; the app is only reachable through it) |
| `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` | `https://dashboard.ndzela.com` (exact origin, no trailing slash). **Only the web admin goes here**: these origins may send the sign-in cookie |
| `PUBLIC_SITE_URL` | `https://ndzela.com`: the QR code of a receipt opens `/recu/<payment id>` there. |
| `PUBLIC_SITE_ORIGINS` | `https://ndzela.com,https://www.ndzela.com`: the public website, which may only post its forms to `/api/v1/public`, never with the cookie (see [ADR-0018](../adr/0018-railway-hosting-and-domains.md)) |
| `MAIL_PROVIDER`, `RESEND_API_KEY`, `MAIL_FROM` | `resend`, the Resend key, a sender on the verified domain (for example `no-reply@ndzela.com`). Railway's smaller plans block outgoing SMTP, so do not use `smtp`; never `log` |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | From the console. `CLOUDINARY_ROOT_FOLDER=ndzela-prod` |
| `OCPP_FLEET_API_KEY`, `OCPP_SIGNING_SECRET`, `OCPP_WEBHOOK_SECRET`, `OCPP_WEBHOOK_HEADER`, `OCPP_COUNTRY` | The Fleet key, the HMAC signing secret of our account (a third secret, different from the other two; ask the OCPP team), the webhook signing secret the OCPP developer gives you (they sign with it; it is not a value we choose), `X-Webhook-Signature`, `GA` |
| `PVIT_*` | Staging: the sandbox values and `PVIT_SANDBOX_AMOUNT=150`. Production: the real account, no `PVIT_SANDBOX_AMOUNT`; follow the [payment go-live checklist](infrastructure-and-deployment.md#payment-go-live-pvit) before announcing payments |

## Domain `api.ndzela.com` (PlanetHoster DNS)
1. Railway, service `backend`, Settings, Networking, Custom Domain: add `api.ndzela.com`. Railway shows a **CNAME target** and a **TXT verification** record.
2. In the PlanetHoster DNS zone of `ndzela.com` add: `CNAME api -> <target Railway shows>` and the `TXT` record Railway shows (name as shown, for example `_railway-verify.api`). If the zone has a `CAA` record, it must allow `letsencrypt.org`.
3. Wait for Railway to mark the domain verified; the TLS certificate is issued by itself (minutes, sometimes up to an hour for DNS to spread).
4. Resend: verify the sending domain `ndzela.com` (the SPF and DKIM records Resend lists, added in the same DNS zone), or the e-mails of the login code and password reset will not arrive.

## First deployment checklist
1. Create the project, add Postgres and Redis, create `backend` from the GitHub repository (Root Directory `backend`), set the variables, deploy.
2. The deployment is healthy when `https://api.ndzela.com/actuator/health` answers `{"status":"UP"}` (the log shows Flyway applying V1 to the latest, then `Created initial SUPER_ADMIN`).
3. Check that `https://api.ndzela.com/swagger-ui/index.html` is **not** available, and that a request from another origin gets no CORS approval.
4. Sign in with the SUPER_ADMIN, receive the e-mail code, enrol MFA, then **remove `INITIAL_ADMIN_PASSWORD`**.
5. Point the OCPP platform's webhook at `https://api.ndzela.com/api/v1/webhooks/ocpp` with the signing secret they hold; our endpoint verifies the HMAC of the body.
6. Check `select * from ocpp_sync_state` (a recent `last_success_at`, no `last_error`).
7. Rotate every secret that was shared during development (the Fleet key, the Cloudinary secret, the initial password).

## Operating
- Logs and metrics: the Railway service view; alert on a failing deployment and on `STORAGE_UNAVAILABLE` or `last_error` in `ocpp_sync_state`.
- A new push to `main` redeploys (enable "Wait for CI" so a red build is never deployed).
- Rolling back: redeploy the previous successful deployment in Railway. Schema changes stay backward compatible for one release (see the deployment rules).
- Backups: Railway's Postgres backups, and a **tested restore** before real money flows.
