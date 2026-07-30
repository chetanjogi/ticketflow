# PRD — TicketFlow

**Status:** Draft v1 · **Owner:** (you) · **Last updated:** 2026-07-27

## 1. Vision

A real-time ticketing platform where small organizers (meetups, college fests, workshops, local shows) create events and sell tickets through high-demand drops, with a production-grade payment subsystem, live seat availability, and an AI assistant grounded in each event's real details.

**Secondary (honest) goal:** demonstrate end-to-end senior-engineer skills — distributed systems, event-driven design, payments, resilience at scale, and applied AI — with measured numbers, for job applications.

## 2. Users and personas

- **Attendee** — browses/searches events, buys tickets during drops, gets live order status, asks the AI assistant questions ("what's the refund policy?", "is parking included?").
- **Organizer** — creates events and seat maps, schedules drops, watches sales live, reads AI-generated sales summaries, issues refunds.
- **Platform operator (you)** — monitors system health, investigates payment failures, manages incidents.

## 3. Core user stories (MVP)

### Attendee
- A1: Search events by keyword AND by meaning ("chill outdoor music this weekend").
- A2: View live seat map; seats held/sold by others update in real time.
- A3: Select seats → seats locked for 5 minutes → pay via card (Stripe sandbox) → receive ticket with QR code.
- A4: See live order status (processing → paid → ticket issued) without refreshing.
- A5: Ask the event assistant questions; answers grounded in event data, never invented.
- A6: Request a refund per the event's refund policy.

### Organizer
- O1: Create event with venue, seat map (general admission or reserved seating), pricing tiers.
- O2: Schedule a timed drop (e.g., 500 tickets at Friday 10:00 AM).
- O3: Watch a live dashboard: sales/minute, revenue, seat map fill, payment failures.
- O4: Receive a daily AI-written sales summary.
- O5: Cancel an event → automatic refunds to all buyers.

### Platform
- P1: Survive a flash sale: 100k+ concurrent users competing for 500 tickets, via a fair virtual waiting room.
- P2: Zero double-sold seats. Ever. (This is the invariant the whole system is designed around.)
- P3: Zero lost payments: every gateway charge is reconciled with an internal order.
- P4: p99 API latency < 500 ms under normal load; graceful degradation (not collapse) under overload.

## 4. MVP scope

**In:** reserved + general-admission seating, Stripe sandbox (single gateway first — see ADR-004), timed drops with waiting room, live seat map + dashboards (WebSocket), refunds, semantic search, RAG event assistant, organizer AI summaries, email notifications.

**Out (non-goals for MVP) — write these down so scope can't creep silently:**
- Multiple payment gateways (Razorpay/PayPal are post-MVP; the gateway interface is designed for them from day 1, but only Stripe is implemented)
- Mobile apps (responsive web only)
- Secondary market / ticket resale
- Multi-currency, taxes beyond a flat configurable rate
- Organizer payouts (money-out is a whole product; MVP tracks balances only)
- SSO / social login (email + password with Spring Security first, OAuth later)
- Real fraud ML models (rule-based flags + LLM triage only)

## 5. Success metrics (measured, not guessed)

| Metric | Target | How measured |
|--------|--------|--------------|
| Double-sold seats under load | 0 | k6 flash-sale test + DB invariant check |
| Flash sale survived | 100k VUs, 500 tickets | k6 + Grafana capture |
| p99 latency (browse/search) | < 500 ms | Prometheus histogram |
| Order → ticket end-to-end | < 10 s p95 | tracing (order ID timestamps) |
| Payment reconciliation mismatches | 0 | nightly reconciliation job report |
| Test coverage (services) | ≥ 80% line, critical paths 100% | JaCoCo in CI |
| RAG assistant groundedness | No hallucinated policies in eval set of 50 Qs | manual eval sheet |

## 6. Constraints

- Solo developer, part-time-to-full-time; realistic budget ≈ $0–30/month AWS (free tier + minimal Fargate; local Docker Compose is the primary dev environment).
- Timeline: ~14 weeks to public MVP (see roadmap). Learning time is budgeted in, not squeezed out.

## 7. Open questions (resolve before the relevant phase, log answer as ADR or PRD update)

- Q1: Waiting room — Redis sorted-set queue vs. token-bucket entry? (resolve in Phase 5, spike first)
- Q2: Seat map rendering — SVG seat map vs. canvas for 1000+ seat venues? (resolve in Phase 4)
- Q3: Notification email provider — SES vs. Resend free tier? (resolve in Phase 3)
