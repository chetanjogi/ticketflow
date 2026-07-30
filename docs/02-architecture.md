# Architecture — TicketFlow

**Status:** v1 · Changes to anything in this doc require an ADR.

## 1. Evolution strategy (important)

We start as a **modular monolith** (one Spring Boot app, strict package boundaries per module) and extract services when Kafka is introduced in Phase 3. This is deliberate (ADR-001): extraction with clean module boundaries is a one-week refactor; premature microservices cost a month of plumbing before any feature exists. The module boundaries below are service boundaries from day one — code in `booking` may never import from `payment` internals, only via its public API.

## 2. Services (target state)

| Service | Responsibility | Owns (data) | Publishes | Consumes |
|---------|---------------|-------------|-----------|----------|
| **booking-service** | Events catalog, venues, seat maps, seat locking, orders, drops, waiting room | events, venues, seats, orders (Postgres); seat locks, queue (Redis) | order.created, order.cancelled, seat.locked, seat.released | payment.captured, payment.failed |
| **payment-service** | The entire Idea-1 platform: gateway abstraction, charges, refunds, webhooks, ledger, reconciliation, idempotency | payments, ledger_entries, gateway_events (Postgres); idempotency keys (Redis) | payment.captured, payment.failed, refund.completed | order.created, order.cancelled |
| **notification-service** | Email, WebSocket fan-out to browsers | notification log (Postgres); pub/sub channels (Redis) | notification.sent | everything relevant (order.*, payment.*, ticket.*) |
| **ai-service** | Embeddings, semantic search, RAG assistant, sales summaries, fraud triage | embeddings (pgvector), chat sessions | ai.summary.ready, fraud.flagged | order.created, payment.*, event.updated |

Ticket issuance (QR generation → S3, `ticket.issued` event) lives in booking-service for MVP; extract only if it grows.

## 3. Communication rules

- **Synchronous (REST):** only for user-facing request/response (browse, search, create order). Services never call each other synchronously except booking → payment status query as a fallback.
- **Asynchronous (Kafka):** all state changes between services. Topics: `orders`, `payments`, `tickets`, `notifications`, `analytics`. Keyed by aggregate ID (orderId) for ordering. Every consumer is idempotent (dedupe on event ID) and has a DLQ (`<topic>.dlq`).
- **Reliability pattern:** transactional outbox on every publisher (event row committed in the same DB transaction as the state change; a poller publishes to Kafka). No dual-write, ever.
- **Saga (order flow):** order.created → payment attempts → payment.captured → ticket issued, OR payment.failed → seat.released (compensating action). Orchestration lives in booking-service state machine on the order row.
- **WebSocket:** STOMP over WebSocket (Spring). Browser subscriptions: `/topic/events/{id}/seats` (seat map), `/user/queue/orders` (own order status), `/topic/organizer/{id}/sales` (dashboard). Fan-out across instances via Redis pub/sub.

## 4. Data

- **PostgreSQL** (one instance, schema-per-service to mimic service ownership; RDS later): orders, seats, payments, ledger. Money is `NUMERIC(12,2)`, never float. Ledger is double-entry, append-only.
- **Seat inventory invariant:** a seat's sold state is guarded by BOTH a Redis lock (fast UX-level hold, 5-min TTL) AND a database constraint (`UNIQUE(event_id, seat_id) WHERE status='SOLD'` via partial index) with optimistic locking on the seat row. Redis prevents contention; Postgres is the source of truth. If they disagree, Postgres wins.
- **Redis (ElastiCache later):** seat locks, idempotency keys, waiting-room queue (sorted set), hot event cache, WebSocket pub/sub.
- **S3:** ticket PDFs/QR codes, event images, exported reports.
- **pgvector:** event embeddings for semantic search + RAG chunks. Chosen over a dedicated vector DB to keep one database technology (ADR-006).

## 5. AI layer

- **Framework:** Spring AI. **Models:** any OpenAI-compatible or Anthropic API; abstracted behind Spring AI so provider is swappable.
- **RAG pipeline:** event details/policies/FAQ → chunk (~300 tokens, overlap 50) → embed → pgvector. Assistant retrieves top-k by cosine similarity, answers ONLY from retrieved context, must cite which chunk, must say "I don't know" on empty retrieval. Groundedness eval set lives in `ai-service/src/test/resources/eval/`.
- **Summaries:** nightly job aggregates the day's `analytics` events per organizer → LLM writes summary → stored + emailed.
- **Fraud triage:** rule engine flags (velocity, mismatched country, repeated failures) → LLM writes human-readable triage note → surfaces on operator dashboard. Rules decide; LLM only explains.

## 6. Concurrency model

Java 21 virtual threads for all I/O-bound request handling (`spring.threads.virtual.enabled=true`). Benchmark protocol: identical k6 load vs. platform-thread config, record throughput + p99, publish results in `load-tests/RESULTS.md`. Pinning watch: no `synchronized` around I/O (use ReentrantLock); check with `-Djdk.tracePinnedThreads`.

## 7. Resilience (Resilience4j)

- Circuit breaker on every external call (Stripe, LLM API). Payment breaker open → orders queue with status PAYMENT_PENDING, user informed, retried on close.
- Retries: exponential backoff + jitter, only on idempotent operations, max 3.
- Rate limiting: token bucket at gateway (per-IP) and per-user on order creation.
- Load shedding: waiting room admits N users/sec into checkout; everyone else holds a queue position (live via WebSocket).
- Timeouts on everything. Default 3 s HTTP client, 10 s payment capture.

## 8. Deployment (target)

Local: Docker Compose (all services + Postgres + Redis + Kafka(KRaft) + Prometheus + Grafana). 
Cloud: ECS Fargate (one task per service), RDS Postgres, ElastiCache, MSK-serverless OR self-managed Kafka container (cost call — ADR-008, decide Phase 6), ALB, S3, ECR. CI/CD: GitHub Actions → test → build image → push ECR → deploy.

## 9. Observability

- Prometheus metrics from every service (Micrometer): RED metrics (rate, errors, duration) per endpoint + business metrics (orders/min, lock contention rate, payment success rate, Kafka consumer lag).
- Grafana dashboards committed as JSON in `infra/grafana/`.
- Structured JSON logs with orderId/paymentId correlation ID propagated via Kafka headers.
- Alert rules (even solo): payment success rate < 95%, consumer lag > 1000, p99 > 1s.
