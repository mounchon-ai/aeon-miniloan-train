# miniloan

Personal loan intake, assessment, approval, disbursement, repayment and closure — built through the `aeon` req → design → mock → dev pipeline. Requirement source of truth: `.aeon/req/requirements.json`. Design source of truth: `.aeon/design/`.

## Architecture

Two apps, strictly separated (REQ-miniloan-006, locked in `.aeon/design/context.json` — separation is the requirement; the frameworks below are architecture decisions, not the requirement itself):

- `apps/api` — Java (Spring Boot 3.x, Java 21, Maven), root package `com.miniloan`. Single module, package-based layering (`controller` / `service` / `repository` / `domain`) per `DEC-001`. **Every business rule is enforced here.** Exposes a testable OpenAPI contract via springdoc-openapi.
- `apps/web` — Angular, project name `miniloan-web`, structured as `core` / `shared` / `features` per `DEC-003`. A thin client only: it calls the API for every decision (eligibility, approval, amounts) and never computes or re-validates business logic itself.
- `db` — PostgreSQL. Money is stored as `numeric`/`decimal`, never floating point.
- No shared code between `apps/api` and `apps/web` — the two are deliberately decoupled (matches REQ-miniloan-006's web/API separation).

**Stack history**: this project originally ran ASP.NET Core (.NET 8) + Next.js. The API moved to Java Spring Boot per `DEC-001`/`ADR-001` (2026-09-04), and the web app moved to Angular per `DEC-003`/`ADR-002` (2026-09-04) — both are the project owner's own architecture decisions (not driven by a new requirement; `NFR-miniloan-006` explicitly leaves framework choice open). See `.aeon/design/context.json` `decisions[]` for the full record.

**Angular + Docker Compose gotcha**: unlike Next.js, Angular bakes its API base URL in at **build time** (`environment.ts` / the `API_URL` build arg in `apps/web/Dockerfile`), not at container runtime. Changing `docker-compose.yml`'s `web.build.args.API_URL` requires rebuilding the `web` image — setting a runtime environment variable on the running container has no effect on an already-built Angular bundle. Do not port the Next.js `NEXT_PUBLIC_*`-at-runtime pattern here.

## Commands

```
./scripts/dev-init.ps1     # mvn dependency:resolve + npm install + pull db image
./scripts/dev-test.ps1     # mvn test + npm test
./scripts/dev-build.ps1    # mvn package + npm run build
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
