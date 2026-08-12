# TicketFlow — Work Log

A running record of what shipped each phase, what broke, what I learned, and what's next.
One entry per phase (and optionally per week). This is the paper trail: it turns into
the story I tell in interviews, and it's how I revise what I've actually learned.

How to use this file:
- At the end of each phase, copy the TEMPLATE block at the bottom to the top of the log.
- Fill it in honestly — the "what broke / what I learned" parts matter most.
- Keep it short. Bullet points are fine. Future-me just needs the gist.

---

## Phase 0 — Foundations
**Dates:** started ~2026-07-27, completed 2026-08-11
**Branch tag:** v0.1-phase0

### What shipped
- Public GitHub repo with full engineering docs pack committed to `/docs` before any code.
- Branch protection on `main`: pull request required + CI `build` check required to merge.
- Spring Boot 4.0.7 app (Java 21), runs locally, serves `/hello` and `/actuator/health`.
- Module packages (booking, payment, notification, ai, common) as future service boundaries.
- PostgreSQL 16 + Redis 7 running via Docker Compose (Postgres mapped to host port 5433).
- Secrets externalized: gitignored `.env` for real values, committed `.env.example` as template.
  Both `application.properties` and `docker-compose.yml` read from `.env` — no secrets in git.
- ArchUnit module-boundary test that fails if one module imports another's internals.
- CI pipeline (GitHub Actions): checkout → JDK 21 → `./mvnw verify` on every push and PR.
- 5 issues opened, tracked on a project board, each closed with a note. All Phase 0 exit criteria met.

### What broke (and how I fixed it)
- Spring Boot 4 moved auto-config classes to new packages → the old `exclude` lines silently did
  nothing. Sidestepped by using an in-memory H2 DB for the first run, later removed for real Postgres.
- App failed to start: needed a database. Learned to read Spring's failure-analysis block (Description/Action).
- Postgres "password authentication failed" even with matching creds → root cause was a persistent
  Docker volume holding stale credentials. `docker compose down -v` wipes the volume for a clean re-init.
- Real culprit found via isolation: two Postgres instances were fighting over port 5432 (a native
  Windows Postgres + the container). Diagnosed with `docker exec ... psql` (direct DB test) and
  `netstat -ano | findstr :5432` (two PIDs on the port). Fixed by mapping the container to host port 5433.
- Port 8080 "already in use" = a previous app instance still running. Stop it in the IDE before re-running.
- EnvFile plugin: `.env` was marked "Executable", so the IDE tried to *run* it → "not a valid Win32
  application". Unticking Executable (leaving Enabled) fixed it — `.env` is read, not executed.
- CI failed instantly with `./mvnw: Permission denied` (exit 126). Windows doesn't set the Linux
  executable bit; fixed with `git update-index --chmod=+x mvnw` so Linux CI can run the wrapper.

### What I learned
- How to read a long Java/Spring stack trace: go to the bottom first, find the deepest `Caused by:`,
  ignore the downstream noise. The real error is usually one plain-English line.
- Docker Compose basics: services, images (pinned versions), ports (`host:container`), and volumes
  (persistence — and how persistence can trap stale state).
- The full professional git loop: branch → commit (Conventional Commits) → push → PR → self-review → merge,
  with branch protection making it mandatory.
- Why secrets belong in a gitignored `.env` with a committed `.env.example` template — and to *verify*
  the ignore rule works (`git status` shows no `.env`) rather than assume it.
- What CI actually is: a robot that builds and tests my code on a clean machine on every change, and
  how a required status check physically blocks broken code from reaching `main`.
- Architecture-as-tests: ArchUnit can enforce module boundaries, and a test is only trustworthy once
  I've watched it fail for the right reason ("prove it once, then fix").

### Decisions worth remembering (see also /docs/adr)
- Modular monolith first, split into services at Kafka introduction (ADR-001).
- Single source of truth for DB creds is `.env`; Postgres is source of truth, Redis is cache (ADR-003).
- Used Spring Boot 4.x because 3.x was no longer offered; accepted slightly thinner tutorial coverage.

### Deferred (with where it goes)
- Containerizing the Spring app itself (Dockerfile + app as a Compose service) → Phase 7 (deployment).
- Proper app-context integration test with a real DB via Testcontainers → Phase 1.
- Local dev DB password is simple/plain in `.env`; move to a real secret store before any deploy (risk R15).

### Next
- Phase 1 — Core domain: events, venues, seats, orders as JPA entities; order state machine;
  Flyway migrations replacing `ddl-auto=update`; unit + Testcontainers integration tests.

---

## TEMPLATE — copy this block up top for each new phase

## Phase N — <name>
**Dates:** started YYYY-MM-DD, completed YYYY-MM-DD
**Branch tag:** vX.Y-phaseN

### What shipped
- (the concrete things that now exist / work that didn't before)

### What broke (and how I fixed it)
- (each real bug: the symptom, the root cause, the fix — this is the most valuable section)

### What I learned
- (concepts and skills, in my own words — this is what I revise before interviews)

### Decisions worth remembering (see also /docs/adr)
- (any choice that would be painful to reverse; link the ADR if one was written)

### Deferred (with where it goes)
- (anything intentionally postponed, and which phase it moves to — so it's never silently forgotten)

### Next
- (the immediate next phase's focus)