# Module Definition of Done — TicketFlow

Copy this checklist into a GitHub issue for every module/feature. All boxes checked = done. Any unchecked box = not done, regardless of whether "it works."

## 1. Design (before code)
- [ ] One-pager written: what, why, API sketch, data model sketch (can live in the issue)
- [ ] Failure modes listed: what breaks, what's the blast radius, what's the recovery
- [ ] Any irreversible choice → ADR written
- [ ] Pre-mortem question from risk register answered (if phase has one)
- [ ] Scope check: is this in the PRD? If not → PRD update or drop it

## 2. Build
- [ ] Feature branch + issue linked
- [ ] Timeouts, retry policy (or explicit no-retry rationale), and metrics on every external call
- [ ] Idempotency considered for every write path exposed to retries/events
- [ ] Money/inventory code follows ADR-005 patterns exactly
- [ ] Config in `.env.example`; no secrets in code
- [ ] Structured logs with correlation ID at every state change

## 3. Test
- [ ] Happy path: unit + integration (Testcontainers)
- [ ] Failure paths from the design one-pager each have a test
- [ ] Relevant scenarios from `05-testing-strategy.md` T-list implemented
- [ ] Coverage gate passes; critical-path branches 100%
- [ ] Concurrency test if the module touches seats, orders, or payments

## 4. Observe
- [ ] RED metrics visible in Grafana for new endpoints/consumers
- [ ] Business metric added if applicable (e.g., locks/min, refunds/day)
- [ ] Alert rule if the module can fail silently
- [ ] Log sampling checked: can you trace one order end-to-end by ID?

## 5. Document
- [ ] Architecture doc updated if behavior/topology changed
- [ ] Runbook written/updated if the module can page you
- [ ] API documented (springdoc-openapi annotations current)
- [ ] WORKLOG line written

## 6. Deploy
- [ ] Migration is expand/contract-safe
- [ ] Deployed to staging via pipeline; smoke test passes
- [ ] Feature flag if risky
- [ ] Rollback plan stated in PR (usually: previous SHA; anything special, write it)

## 7. Recheck (the step everyone skips)
- [ ] 24–48 h after merge: check Grafana for the new module — errors, latency, lag anomalies?
- [ ] Re-run the module's concurrency/chaos test against staging once
- [ ] Ask: what did I learn that changes the risk register or a future phase? Write it down.
- [ ] Close the issue with a 2-line summary (future-you will thank present-you)
