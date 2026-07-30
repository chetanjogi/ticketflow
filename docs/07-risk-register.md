# Risk Register — TicketFlow

Reviewed weekly. Each risk: likelihood (L/M/H) × impact (L/M/H), mitigation, and the test or mechanism that proves the mitigation works. New risks discovered during building get added the same day.

## Technical risks

| # | Risk | L×I | Mitigation | Proven by |
|---|------|-----|-----------|-----------|
| R1 | Double-sold seat under race conditions | M×H | Redis hold + Postgres partial unique index + optimistic version; DB is source of truth | T1, T4, flash-sale invariant check |
| R2 | Payment captured but order lost (crash between gateway and DB) | M×H | Webhooks as safety net + nightly reconciliation + ledger | T5, T10 |
| R3 | Double charge on retry | M×H | Idempotency keys on every capture; retries only on idempotent ops | T3, T8 |
| R4 | Event loss when Kafka is down | M×H | Transactional outbox; no dual-writes anywhere | T11 |
| R5 | Duplicate event side effects (Kafka at-least-once) | H×M | Idempotent consumers keyed on event ID | T13, T6 |
| R6 | Saga stuck half-done (payment ok, ticket never issued) | M×H | Order state machine + timeout sweeper job that re-drives stuck orders; alert on stuck > 10 min | chaos test in Phase 3 exit |
| R7 | Redis outage kills seat locking | M×M | Postgres constraint holds the invariant regardless; degraded UX only | T4 |
| R8 | WebSocket fan-out breaks with >1 instance | H×M | Redis pub/sub backplane from day one of Phase 4 (not retrofitted) | T14 |
| R9 | Flash sale melts the system | H×H | Waiting room, rate limits, load shedding, breakers; capacity tested before launch | Phase 5 flash-sale protocol |
| R10 | Virtual thread pinning under load (synchronized + I/O) | M×M | No synchronized around I/O; pinned-thread tracing in load tests | benchmark runs |
| R11 | RAG assistant invents refund policies → real user harm | M×H | Answer only from retrieved context, citations required, refusal on empty retrieval; trap-question eval | T16 |
| R12 | LLM cost runaway | M×M | Token caps per session, daily budget alarm, cache embeddings | cost dashboard + alarm test |
| R13 | N+1 queries make browse slow at scale | H×M | datasource-proxy query counting in tests; explicit fetch joins | Phase 1 exit check |
| R14 | Schema change breaks rollback | M×H | Expand→migrate→contract policy; migration review item in PR checklist | rollback rehearsal |
| R15 | Secrets leaked to git | L×H | gitignore + pre-commit secret scan (gitleaks) in CI | CI check |

## Product / project risks (solo-specific)

| # | Risk | L×I | Mitigation |
|---|------|-----|-----------|
| P1 | Scope creep ("just one more feature") | H×H | PRD non-goals list; every work item needs an issue; weekly review asks "is this in the PRD?" |
| P2 | Phase overrun cascades the timeline | H×M | Buffer policy in roadmap: cut scope into non-goals, never cut exit criteria |
| P3 | Learning rabbit holes (3 days reading Kafka internals) | H×M | Bounded 2-day learning spikes with a throwaway artifact, then build |
| P4 | Motivation dip mid-project (weeks 6–9 classically) | H×H | Phase tags = visible wins; WORKLOG streak; demo something visual every phase |
| P5 | Burnout from "professional process" overhead | M×M | Process budget: docs/process ≤ 15% of weekly hours; if exceeded, simplify the process, not the testing |
| P6 | Building in private forever, never launching | M×H | Phase 7 has a date; "a stranger sells a ticket" is an exit criterion, not a wish |

## Real-world questions to answer BEFORE each phase (pre-mortems)

Phase 2: What happens to a hold when the user closes the tab? When their card needs 3DS and takes 4 minutes? When Stripe is up but slow (2 s per call)?
Phase 3: What if the outbox poller crashes — who notices? What's our max acceptable consumer lag and what happens past it?
Phase 4: What does the seat map show during a Redis failover? What do 10k idle WebSocket connections cost in memory?
Phase 5: What is the fair-ness definition for the waiting room (FIFO? random draw?) — and is it documented for users?
Phase 6: What data must NEVER reach the LLM API (emails, payment details)? Write the redaction list first.
Phase 7: If the single Fargate task for payments dies at 9:59 AM before a 10:00 drop — what happens? (Min 2 tasks for payment + booking on drop days.)
