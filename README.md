# TicketFlow — Engineering Documentation

Real-time event ticketing platform with a full payment orchestration subsystem.
Built solo, run like a startup: documentation-first, decision records, exit criteria, no silent rework.

## How this documentation system works

**Rule 1 — Docs before code.** No module starts until its section in the PRD and architecture doc exists and the relevant ADR is written.

**Rule 2 — Decisions are written, once.** Any choice that would be painful to reverse (database, message broker, locking strategy, deployment target) gets an ADR in `adr/`. Reversing a decision later is allowed — but only by writing a new ADR that supersedes the old one. This kills the "why did I do this?" problem permanently.

**Rule 3 — Phases have exit criteria.** A phase is done when every item in its exit checklist (see `03-roadmap.md`) is checked. Not before. Moving on with unchecked items is how projects rot.

**Rule 4 — Every module passes the same Definition of Done.** See `08-module-checklist.md`. Design, build, test, observe, deploy, recheck — every time, no exceptions, even for "small" modules.

**Rule 5 — Weekly review (solo standup).** Every week, 30 minutes: update the risk register, check the roadmap, write 3 lines in `WORKLOG.md` (what shipped, what's blocked, what's next). This is your paper trail for interviews.

## Document index

| File | Purpose |
|------|---------|
| `01-prd.md` | Product requirements: vision, users, MVP scope, non-goals |
| `02-architecture.md` | System design: services, data flow, tech stack with rationale |
| `03-roadmap.md` | Phases, timeline, milestones, exit criteria |
| `04-engineering-standards.md` | Git workflow, code standards, solo code review, Definition of Done |
| `05-testing-strategy.md` | Test pyramid, coverage targets, load testing plan |
| `06-deployment-operations.md` | Environments, CI/CD, monitoring, runbooks, rollback |
| `07-risk-register.md` | Known risks, bottlenecks, real-world failure scenarios + mitigations |
| `08-module-checklist.md` | Reusable Definition of Done for every module |
| `adr/` | Architecture Decision Records (template + decisions to date) |

## Repository layout (target)

```
ticketflow/
├── docs/                  ← this pack
├── services/
│   ├── booking-service/
│   ├── payment-service/
│   ├── notification-service/
│   └── ai-service/
├── frontend/              ← Next.js app
├── infra/                 ← Docker Compose, ECS task defs, IaC
├── load-tests/            ← k6 scripts + results
└── .github/workflows/     ← CI/CD
```

## Reading order for a newcomer (or future you)

1. `01-prd.md` — what and why
2. `02-architecture.md` — how
3. `adr/` — why *this* how and not another
4. `03-roadmap.md` — when
