# miniloan

Personal loan intake, assessment, approval, disbursement, repayment and closure — built through the `aeon` req → design → mock → dev pipeline. Requirement source of truth: `.aeon/req/requirements.json`. Design source of truth: `.aeon/design/`.

## Architecture

Two apps, strictly separated (REQ-miniloan-006, locked in `.aeon/design/context.json`):

- `apps/api` — ASP.NET Core (.NET 8, C#). **Every business rule is enforced here.** Exposes a testable OpenAPI contract.
- `apps/web` — Next.js (React/TypeScript). A thin client only: it calls the API for every decision (eligibility, approval, amounts) and never computes or re-validates business logic itself.
- `db` — PostgreSQL. Money is stored as `numeric`/`decimal`, never floating point.

## Commands

```
./scripts/dev-init.ps1     # dotnet restore + npm install + pull db image
./scripts/dev-test.ps1     # dotnet test + npm test
./scripts/dev-build.ps1    # dotnet build + npm run build
docker compose -f docker/docker-compose.yml up
```

- API: http://localhost:5000
- Web: http://localhost:3000
- Postgres: localhost:5432 (db=miniloan, user=miniloan)

## Conventions (from `.aeon/design/context.json` constraints)

- Money is rounded **round-half-up at the point it occurs** — never store an unrounded value and round only for display.
- Every write that can be retried is guarded by a database unique constraint (idempotent writes), not by client-side dedup.
- No automatic retry queue — a failed API call surfaces immediately and the user re-issues it themselves (REQ-miniloan-006 explicitly puts retry queues out of scope).
- Authentication is a mock/token stand-in this round, not real auth — do not build a real auth system against this codebase.

## Mistakes already made

None yet — Phase 3 (build) has not started. Update this section as `/dev:build` and `/dev:revise` surface real ones; do not leave it as a placeholder once there's something to record.
