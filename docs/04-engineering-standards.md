# Engineering Standards — TicketFlow

Solo doesn't mean sloppy. These rules simulate the guardrails a team gives you.

## Git workflow

- Trunk-based with short-lived branches: `feat/seat-locking`, `fix/webhook-dedupe`, `docs/adr-005`.
- Every branch merges via a Pull Request — yes, to yourself. The PR is where the self-review checklist runs. Merge = squash, message follows Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `perf:`, `chore:`).
- `main` is always deployable. CI must be green to merge (enforced via branch protection).
- Tag at every phase exit: `v0.1-phase1`, etc. If something breaks later, you can diff against a known-good phase.

## Solo code review — the PR checklist

Before merging any PR, review your own diff in the GitHub UI (not your editor — fresh eyes need a fresh surface) and answer in the PR description:

- [ ] What does this change do, in one sentence? (If you can't: split the PR.)
- [ ] What breaks if this is wrong? What's the blast radius?
- [ ] Are the failure paths handled (timeout, retry, partial failure, duplicate delivery)?
- [ ] Are there tests for the failure paths, not just the happy path?
- [ ] Any new config/secret documented in `.env.example` + README?
- [ ] Does this touch money or seat inventory? If yes → re-read ADR-005, run the concurrency test suite locally.
- [ ] Wait 30 minutes (or overnight for money/inventory code), re-read once, then merge.

## Code standards

- Java 21, Spring Boot 3.x. Format: google-java-format via Spotless, enforced in CI — zero style debates with yourself.
- Static analysis: Error Prone + SpotBugs in the build. Warnings are errors.
- Package boundaries enforced by ArchUnit tests (modules may only talk via their `api` package).
- No TODO without an issue number. `// TODO(#42): handle partial refund` or it doesn't merge.
- Money: `NUMERIC(12,2)` in DB, `BigDecimal` in Java, minor units (cents) toward Stripe. Never float/double. This is checked in review every time.
- DTOs at boundaries; entities never leave the service layer.
- Every external call has: timeout, defined retry policy (or explicit "no retry because not idempotent"), and a metric.

## Issue tracking

GitHub Issues + a single Project board (Backlog → This Week → In Progress → Done). Every work item is an issue BEFORE work starts, labeled by phase and module. "I'll just quickly add X" without an issue is how scope creep starts — the 2-minute issue write-up is the speed bump that forces the question "is this in the PRD?"

## Documentation duties (per merge)

- Behavior change → update relevant doc in same PR.
- New decision → ADR in same PR.
- New env var / port / queue → `.env.example` + architecture doc.
- Weekly: 3-line WORKLOG entry. This becomes your interview narrative.

## Secrets & config

- Never in git. `.env` locally (gitignored), AWS SSM Parameter Store in cloud.
- `.env.example` always current — a fresh clone must know every variable it needs.
- Stripe/LLM keys: sandbox/dev keys only until Phase 7 review.

## Dependency policy

- Add a dependency only if it saves > a day of work AND is actively maintained. Note non-obvious ones in the PR.
- Dependabot on; upgrades merged weekly with green CI.
