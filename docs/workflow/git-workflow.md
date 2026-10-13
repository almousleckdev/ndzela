# Development and Git workflow

**Status of these conventions: Proposed as the working default: confirm or amend with the user.** The repository is one Git repo (monorepo) containing `backend/` and `dashboard/`; the first commit is `chore: initial commit`.

## Branches
- `main` is always releasable. Work happens on short-lived branches: `feat/<topic>`, `fix/<topic>`, `chore/<topic>`, `docs/<topic>`.
- No direct pushes to `main` now that CI exists; changes arrive by pull request. In GitHub, protect `main` and require the checks below (Settings, Branches).

## Commits
- Conventional Commits: `type(scope): summary`, imperative, at most ~72 characters. Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `build`, `ci`, `perf`. Scope optional (`backend`, `frontend`, `tenancy`, `auth`...).
- One logical change per commit; a commit builds and passes tests. Body explains *why* when not obvious.
- Never commit secrets, `.env`, build output, `node_modules`, uploads. Never rewrite shared history. Agents commit or push **only when asked**.

## Pull requests
- Small and focused; description states what/why, how it was tested, and any doc/ADR updated. A tenancy-relevant change lists the isolation tests added.
- Review checklist: follows [module boundaries](../architecture/module-boundaries.md); tenant scope applied; `@PreAuthorize` present; no forbidden patterns ([list](../standards/forbidden-patterns.md)); migration is new (not edited); tests and docs updated.

## Continuous integration (`.github/workflows`)
One workflow per project, each started only by changes in its folder (and its own workflow file), on every pull request and on pushes to `main`:

| Workflow | Runs |
| --- | --- |
| `backend.yml` | `mvn -B clean verify` on Java 21 (Docker is available on the runner: the integration tests use Testcontainers). Uploads the test reports when it fails |
| `dashboard.yml` | `npm ci`, `lint`, `typecheck`, `build`, and `npm audit` (fails on a critical advisory only) on Node 22 |
| `web.yml` | `pnpm install --frozen-lockfile`, `lint`, `typecheck`, `build` on Node 22 |
| `driver.yml` | `flutter pub get`, `analyze`, `test` on the pinned Flutter version (`3.44.8`, change it together with the local SDK) |

Rules: CI needs **no secrets and no `.env`** (tests carry their own test values; a test that only passes with a developer's `.env` is a defect); each job has a timeout; permissions are read-only; a new push cancels the older run of the same branch. A red check blocks the merge. Run the same commands locally before pushing (see Definition of done).

## Definition of done
Code follows the docs; `mvn clean test`, `npm run lint && npm run typecheck` (dashboard and web) and, for the driver app, `flutter analyze && flutter test` pass, and the CI checks are green; tests added (isolation and authorization where relevant); docs/ADR updated; no unrelated changes.

## Spec-first for features
A feature starts with an approved spec and plan (module by module, see [functional requirements](../business/functional-requirements.md)). Unanswered business questions block the module until the user answers them.
