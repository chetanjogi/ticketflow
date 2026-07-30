# Architecture Decision Records

An ADR is a short (half-page) record of a decision that would be expensive to reverse. Written when the decision is made, never edited afterward — superseded by a new ADR instead.

## Index

| # | Title | Status |
|---|-------|--------|
| 001 | Modular monolith first, extract services in Phase 3 | Accepted |
| 002 | Kafka for inter-service events, with transactional outbox | Accepted |
| 003 | PostgreSQL as source of truth; Redis for holds/cache only | Accepted |
| 004 | Single payment gateway (Stripe) for MVP behind a gateway interface | Accepted |
| 005 | Seat inventory: Redis TTL hold + Postgres partial unique index + optimistic lock | Accepted |
| 006 | pgvector over dedicated vector DB | Accepted |
| 007 | (Phase 4) Seat map rendering approach | Proposed |
| 008 | (Phase 6/7) MSK vs self-managed Kafka container in cloud | Proposed |

## Template

```markdown
# ADR-NNN: Title

Date: YYYY-MM-DD · Status: Proposed | Accepted | Superseded by ADR-XXX

## Context
What problem forces a decision? What constraints apply (cost, solo dev, timeline, learning goals)?

## Options considered
1. Option A — pros / cons
2. Option B — pros / cons

## Decision
What we chose, in one sentence.

## Consequences
What becomes easier, what becomes harder, what we're betting on, what would make us revisit.
```

---

# ADR-001: Modular monolith first, extract services in Phase 3

Date: 2026-07-27 · Status: Accepted

## Context
Solo developer, 14-week timeline, learning goals include microservices AND shipping. Starting with 4+ deployables means weeks of plumbing (service discovery, shared auth, N pipelines) before any feature works.

## Options considered
1. Microservices from day 1 — matches target architecture; slow start, painful local dev, high early failure surface.
2. Modular monolith with ArchUnit-enforced boundaries, extract at Kafka introduction — fast start; risk of boundary erosion (mitigated by CI-enforced rules); extraction is a real, valuable exercise in itself.
3. Monolith forever — simplest; fails the learning goals.

## Decision
Option 2. Package-per-module with enforced boundaries; payment + notification extracted in Phase 3.

## Consequences
Faster Phases 0–2; extraction becomes a documented milestone (great interview story). Bet: ArchUnit rules keep boundaries clean enough that extraction is ~1 week. Revisit if boundaries erode despite CI.

---

# ADR-002: Kafka with transactional outbox

Date: 2026-07-27 · Status: Accepted

## Context
Services must exchange state changes reliably; learning Kafka is an explicit goal. Naive publish-after-commit (dual write) loses events on crash.

## Options considered
1. Direct REST between services — simple; tight coupling, no replay, fails the learning goal.
2. RabbitMQ — great tool; industry demand and learning goal point to Kafka.
3. Kafka, publish directly in transaction path — dual-write inconsistency.
4. Kafka + outbox pattern — event row committed atomically with state; poller publishes; at-least-once + idempotent consumers.

## Decision
Option 4. Topics keyed by aggregate ID; every consumer idempotent; DLQ per topic.

## Consequences
Slightly more moving parts (outbox poller); zero lost events by construction; replayability. Bet: at-least-once + idempotency is simpler to operate solo than exactly-once transactions. Revisit only if outbox lag becomes a measured problem.

---

# ADR-005: Seat inventory strategy

Date: 2026-07-27 · Status: Accepted

## Context
The system invariant: a seat is never sold twice. Flash sales create extreme contention. Redis is fast but not the system of record; Postgres is durable but slower under hot contention.

## Options considered
1. Postgres pessimistic locks (SELECT FOR UPDATE) only — correct; connection pile-ups under flash-sale contention.
2. Redis locks only — fast; a Redis failure or TTL edge can violate the invariant. Unacceptable.
3. Layered: Redis SET NX EX for the 5-minute UX hold (absorbs contention, gives countdown UX) + Postgres as truth: partial unique index `(event_id, seat_id) WHERE status='SOLD'` + version column optimistic lock at purchase commit.

## Decision
Option 3. Redis makes it fast; Postgres makes it correct; correctness never depends on Redis.

## Consequences
Two systems to reason about; clear rule resolves all conflicts (Postgres wins). T1/T2/T4 tests enforce it forever. Revisit only with measured evidence Postgres commit contention is the bottleneck after tuning.
