# Roadmap — TicketFlow

**Total: 14 weeks to public MVP.** Each phase ends with a hard exit-criteria checklist. A phase is not done until every box is checked — this is the mechanism that prevents "forgot this, redo later."

Timeline assumes ~25–35 focused hours/week. Learning time is included: each phase starts with a bounded learning spike (max 2 days) — read/watch, build a throwaway hello-world of the new tool, THEN build the real thing. Never learn a tool inside production code.

## Phase 0 — Foundations (Week 1)
Learn/refresh: Git workflow, Docker, Spring Boot 3.x project structure, this doc pack.
Build: repo + this docs pack committed · modular monolith skeleton (booking/payment/notification/ai packages with enforced boundaries via ArchUnit test) · Docker Compose with Postgres + Redis · CI pipeline runs tests on every push · WORKLOG.md started.
**Exit criteria:**
- [ ] `docker compose up` gives a healthy app + Postgres + Redis from a fresh clone
- [ ] CI green on main; ArchUnit boundary test fails if booking imports payment internals (prove it once, then fix)
- [ ] ADR-001..003 written

## Phase 1 — Core domain (Weeks 2–3)
Learn: JPA pitfalls (N+1, lazy loading), Flyway migrations, Testcontainers.
Build: events/venues/seats/orders CRUD · seat map model (reserved + GA) · order state machine (CREATED→PENDING_PAYMENT→PAID→TICKETED / CANCELLED) · mock payment · Flyway from day 1 · unit + Testcontainers integration tests.
**Exit criteria:**
- [ ] Full happy path via Postman: create event → create order → mock-pay → ticket row exists
- [ ] Order state machine rejects every illegal transition (tested exhaustively)
- [ ] ≥80% coverage on domain logic; zero N+1 on list endpoints (verified with datasource-proxy logging)
- [ ] All schema changes are Flyway migrations; `flyway clean && migrate` rebuilds from scratch

## Phase 2 — Seat locking + real payments (Weeks 4–5) ⚠ hardest phase
Learn: Redis distributed locks (and their limits), optimistic vs pessimistic locking, Stripe PaymentIntents + webhooks, idempotency.
Build: Redis seat hold with TTL · Postgres partial-unique + version column as source of truth · Stripe sandbox integration behind a `PaymentGateway` interface · webhook endpoint with signature verification · idempotency keys on order + capture · double-entry ledger tables · reconciliation job (nightly: Stripe charges vs internal payments).
**Exit criteria:**
- [ ] Concurrency test: 100 threads race for 1 seat → exactly 1 wins, 99 get clean rejection (JUnit + Testcontainers, runs in CI)
- [ ] Kill the app between Stripe capture and DB commit (chaos test) → recovery via webhook/reconciliation, no lost payment, no double charge
- [ ] Replayed webhook processed exactly once; expired seat hold releases automatically and is re-buyable
- [ ] Refund flow works end to end; ledger always balances (sum of entries per payment = 0)
- [ ] ADR-004 (single gateway first), ADR-005 (locking strategy) written

## Phase 3 — Kafka + service extraction (Weeks 6–7)
Learn: Kafka fundamentals (partitions, consumer groups, offsets, rebalancing), outbox pattern, sagas.
Build: Kafka in Compose · outbox publisher · extract payment-service + notification-service into separate Spring Boot apps · saga: payment.failed → seat.released · idempotent consumers + DLQs · email notifications.
**Exit criteria:**
- [ ] Stop Kafka for 5 minutes during orders → zero lost events after restart (outbox proof)
- [ ] Poison message lands in DLQ with error context; consumer keeps processing others
- [ ] Duplicate event delivery (replay a partition) causes zero duplicate side effects
- [ ] payment-service killed mid-saga → order recovers to a consistent terminal state
- [ ] Each service starts, tests, and deploys independently

## Phase 4 — Frontend + WebSockets (Weeks 8–9)
Learn: Next.js App Router, TypeScript, TanStack Query, STOMP.js, Tailwind.
Build: attendee flow (browse/search/seat map/checkout/order status) · organizer flow (create event, dashboard) · live seat map + order status + sales feed over WebSocket · Redis pub/sub fan-out across 2 app instances.
**Exit criteria:**
- [ ] Two browsers: buyer A locks a seat → buyer B sees it grey out < 1 s, across DIFFERENT backend instances
- [ ] WebSocket drops → client reconnects and resyncs state (kill server test)
- [ ] Full purchase possible on mobile viewport; Lighthouse ≥ 90 accessibility
- [ ] Auth: Spring Security JWT, organizer routes protected, tested

## Phase 5 — Scale + resilience (Weeks 10–11)
Learn: k6, queueing basics, Resilience4j, virtual threads internals.
Build: flash-sale k6 scenario · virtual waiting room (Redis sorted set + WebSocket position updates) · rate limiting · circuit breakers on Stripe + LLM · load shedding · Grafana dashboards · virtual vs platform thread benchmark.
**Exit criteria:**
- [ ] Flash sale test: 100k VUs, 500 tickets → 0 double-sells, 0 5xx storms, fair queue admission, system recovers to idle
- [ ] Stripe outage simulated (breaker open) → orders park as PAYMENT_PENDING and complete when it closes
- [ ] Benchmark published in load-tests/RESULTS.md with numbers for the resume
- [ ] Every Grafana panel + alert rule committed as code

## Phase 6 — AI layer (Weeks 12–13)
Learn: embeddings, chunking, RAG failure modes, Spring AI, prompt evals.
Build: pgvector + embedding pipeline (event ingest via Kafka) · semantic search endpoint + UI · RAG assistant with citations + refusal on empty retrieval · organizer daily summaries · fraud rule engine + LLM triage notes.
**Exit criteria:**
- [ ] 50-question groundedness eval: zero invented policies/prices; refuses when answer not in context
- [ ] Semantic search beats keyword search on a 20-query eval sheet (documented)
- [ ] LLM API breaker open → search/chat degrade gracefully (keyword fallback, "assistant unavailable")
- [ ] Cost guardrail: per-session token cap + daily budget alarm

## Phase 7 — Deploy + polish (Week 14)
Build: ECS Fargate + RDS + ElastiCache + S3 deploy via GitHub Actions · staging + prod · runbooks · README with architecture diagrams, demo video, load-test results.
**Exit criteria:**
- [ ] Push to main → staging deploy automatically; manual approval → prod
- [ ] Rollback rehearsed once (deliberately deploy a bad build, roll back < 5 min)
- [ ] A stranger can sign up, create an event, and sell a ticket with no help
- [ ] README tells the whole story: problem, architecture, numbers, decisions

## Buffer policy
Phases 2, 3, 5 are the likely overruns. If a phase slips > 1 week, cut MVP scope (consult PRD non-goals — move something IN there), never cut exit criteria. Exit criteria are the product.
