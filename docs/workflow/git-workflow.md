# Development and Git workflow

**Status of these conventions: Proposed as the working default: confirm or amend with the user.** The repository is one Git repo (monorepo) containing `backend/` and `dashboard/`; the first commit is `chore: initial commit`.

## Branches
- `main` is always releasable. Work happens on short-lived branches: `feat/<topic>`, `fix/<topic>`, `chore/<topic>`, `docs/<topic>`.
- No direct pushes to `main` once a remote and CI exist; changes arrive by pull request.

## Commits
- Conventional Commits: `type(scope): summary`, imperative, at most ~72 characters. Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `build`, `ci`, `perf`. Scope optional (`backend`, `frontend`, `tenancy`, `auth`...).
- One logical change per commit; a commit builds and passes tests. Body explains *why* when not obvious.
- Never commit secrets, `.env`, build output, `node_modules`, uploads. Never rewrite shared history. Agents commit or push **only when asked**.

## Pull requests
- Small and focused; description states what/why, how it was tested, and any doc/ADR updated. A tenancy-relevant change lists the isolation tests added.
- Review checklist: follows [module boundaries](../architecture/module-boundaries.md); tenant scope applied; `@PreAuthorize` present; no forbidden patterns ([list](../standards/forbidden-patterns.md)); migration is new (not edited); tests and docs updated.

## Definition of done
Code follows the docs; `mvn clean test` and `npm run lint && npm run typecheck` pass; tests added (isolation and authorization where relevant); docs/ADR updated; no unrelated changes.

## Spec-first for features
A feature starts with an approved spec and plan (module by module, see [functional requirements](../business/functional-requirements.md)). Unanswered business questions block the module until the user answers them.
