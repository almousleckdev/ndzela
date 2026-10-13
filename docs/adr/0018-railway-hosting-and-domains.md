# ADR-0018: The backend runs on Railway; the API is `api.ndzela.com`

**Status:** Accepted · **Date:** 2026-10-13

**Context.** The platform needs a production home for the backend, PostgreSQL and Redis, with HTTPS, a stable public address for the PVit callback and the OCPP webhook, and little operations work for a small team. The domain `ndzela.com` is registered and its DNS is at PlanetHoster.

**Decision.**
- **Railway** hosts three services in one project: the backend (built from `backend/Dockerfile`), PostgreSQL and Redis, connected over Railway's private network. Configuration is in `backend/railway.toml` and environment variables; secrets exist only in Railway (and the team's secret store).
- **Domains.** The API is `https://api.ndzela.com`; the web admin will be `https://dashboard.ndzela.com`; both are sub-domains of one site, so the refresh cookie (`SameSite=Strict`, `Secure`, host-only on the API) keeps working and CORS allows exactly the dashboard's origin. DNS stays at PlanetHoster (a `CNAME` and a `TXT` record per Railway domain).
- **Application changes for it.** The port comes from `PORT`; Redis accepts a password; the SMTP credentials are optional (production sends through the Resend HTTPS API); the mail server is not part of the health check, so a mail outage cannot get the service restarted; the JVM sizes its heap from the container (`MaxRAMPercentage`).
- **Single replica first.** The scheduled jobs are safe with several (database constraints, row locks, a Redis lock for the OCPP sync) but one replica is enough until load says otherwise.

**Consequences.** (+) Deploys from Git, TLS and domains handled, managed databases with backups, no servers to patch. (-) A third party holds the production data and secrets (limit who has access, turn on two-factor sign-in on the Railway account); Railway's outbound SMTP limits force an HTTPS mail provider; the region is chosen once and is hard to move; the company's data protection rules for driver data must be checked against Railway's regions.

**Alternatives considered.** A VPS with Docker Compose (cheapest, but patching, backups and TLS are ours); a European managed platform (to be reconsidered if data residency requires it); the dashboard on Vercel (a separate decision, when it is deployed).
