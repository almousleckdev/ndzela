# ADR-0024: The deployment environment is explicit (`APP_ENVIRONMENT`)

**Status:** Accepted · **Date:** 2026-10-20

**Context.** The startup guards that protect production (no default database password, no PVit sandbox amount) treated `COOKIE_SECURE=true` as "this is production". A deployed **test** environment (needed for TestFlight and Google Play internal testing, over HTTPS and with the PVit sandbox) also needs secure cookies, so it could not start without turning the guards off for everyone.

**Decision.**
- `APP_ENVIRONMENT` is `development`, `staging` or `production` (any other value stops the startup). `DeploymentEnvironment` resolves it once; the guards ask it, never the cookie flag.
- **Unset keeps the previous behaviour** (`COOKIE_SECURE=true` means production), so a deployment that never heard of the variable stays protected.
- **Production refuses to start** with a default or empty `DB_PASSWORD`, with `PVIT_SANDBOX_AMOUNT` set, or with a missing PVit credential (`PvitProductionGuard` names every missing variable). Staging and development may use the sandbox.
- Staging is a separate Railway environment with its own database, Cloudinary root folder and PVit sandbox account: test money and real money never share a database.

**Consequences.** (+) A real test deployment exists for the stores' test tracks, and production keeps its fail-fast checks. (-) One more variable to set; a production deployment that sets `APP_ENVIRONMENT=staging` by mistake loses the checks (the runbook sets it explicitly, and the production values must be reviewed at go-live).
