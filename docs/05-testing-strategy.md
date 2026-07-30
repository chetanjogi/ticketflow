# Testing Strategy — TicketFlow

Principle: the tests that matter most here are not the happy-path ones. Ticketing + payments is a concurrency-and-failure product; the test suite must attack it the way production traffic will.

## The pyramid (per service)

| Layer | Tool | Scope | Target |
|-------|------|-------|--------|
| Unit | JUnit 5 + Mockito | domain logic, state machines, calculators | fast (<5 s total), the bulk of tests |
| Integration | Testcontainers (Postgres, Redis, Kafka) | repositories, outbox, consumers, locking | real infra, no mocks of infra |
| Contract | Spring Cloud Contract (Phase 3+) | REST + event schemas between services | producer/consumer pairs |
| E2E | Playwright (UI) + REST-assured (API) | 5–8 critical journeys only | purchase, refund, drop, dashboard |
| Load | k6 | flash sale, browse, checkout | Phase 5 protocol below |
| Chaos (manual, scripted) | bash + docker kill | kill services mid-flow | scenarios listed below |

Coverage gates in CI (JaCoCo): 80% line overall; **100% branch on**: order state machine, seat allocation, payment capture/refund, ledger postings, idempotency handling. Coverage is a floor, not a goal — the named critical paths are the goal.

## Non-negotiable test scenarios (the "real world problems" list)

These are written as automated tests BEFORE the feature is called done. Each maps to a risk in `07-risk-register.md`.

**Inventory / concurrency**
- T1: 100 concurrent threads buy the last seat → exactly 1 success. (CI, every build)
- T2: Seat hold expires mid-checkout → payment attempt rejected cleanly, seat re-buyable.
- T3: Same user double-clicks Buy → one order (idempotency key).
- T4: Redis flushed (crash) while holds active → Postgres constraint still prevents double-sell; UX degrades, invariant holds.

**Payments**
- T5: App killed after Stripe capture, before DB commit → webhook/reconciliation heals; money and order agree.
- T6: Stripe webhook delivered twice / out of order → processed once, correct final state.
- T7: Webhook with invalid signature → 401, logged, alerted.
- T8: Capture timeout (Stripe slow) → breaker/retry policy engages; user sees pending, not error; no double charge on retry.
- T9: Refund of already-refunded payment → rejected, ledger unchanged.
- T10: Nightly reconciliation detects an orphan Stripe charge (created by test harness) → flagged on ops report.

**Events / Kafka**
- T11: Kafka down 5 min under order load → outbox drains fully on recovery, zero loss, order of events per orderId preserved.
- T12: Poison message → DLQ with payload + error, consumer lag recovers.
- T13: Consumer rebalance mid-batch → no duplicate side effects (idempotency proof).

**WebSocket**
- T14: 2 backend instances; action on instance A visible to subscriber on instance B < 1 s.
- T15: Client offline 30 s → reconnect delivers current state (no stale seat map).

**AI**
- T16: 50-question groundedness eval, includes 10 trap questions whose answers are NOT in context → must refuse all 10.
- T17: LLM API down → search falls back to keyword, chat says unavailable; no user-facing 500.

## Load testing protocol (Phase 5)

1. Baseline: browse+search steady state, find max RPS at p99 < 500 ms.
2. Flash sale: 100k VUs ramp in 60 s against a 500-ticket drop with waiting room ON. Pass = 0 double-sells (SQL invariant check post-run), 0 sustained 5xx, queue positions monotonic, full recovery to idle.
3. Same test, waiting room OFF → document the failure mode honestly (this is a great README section).
4. Virtual vs platform threads: identical scenario, both configs, record throughput + p99 + memory. Publish in `load-tests/RESULTS.md`.
Every run: commit the k6 script, the Grafana snapshot, and a 5-line conclusion. Unreproducible numbers don't exist.

## Test data & environments

- Deterministic seed data via Flyway repeatable migrations for dev; factory builders (no fixtures soup) in tests.
- Testcontainers = same Postgres/Redis/Kafka versions as Compose = same as prod. One version pin file (`versions.env`) referenced everywhere.
- Stripe: sandbox + stripe-cli for local webhook forwarding; test clocks for TTL-dependent flows.

## When a bug escapes

Every post-merge bug gets: an issue, a failing regression test FIRST, then the fix, then one line in `07-risk-register.md` if it revealed a new risk class. No silent fixes.
