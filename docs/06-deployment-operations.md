# Deployment & Operations — TicketFlow

## Environments

| Env | Where | Purpose | Data |
|-----|-------|---------|------|
| local | Docker Compose | daily dev; full stack incl. Kafka, Prometheus, Grafana | seeded, disposable |
| staging | ECS Fargate (min-size tasks) | pre-prod verification, load tests | synthetic |
| prod | ECS Fargate | public MVP | real |

Rule: if it isn't in Compose, it doesn't exist. Anyone (including future-you) must get the full system with `docker compose up` — no snowflake local setups.

## CI/CD (GitHub Actions)

Pipeline on every PR: `spotless check → build → unit tests → Testcontainers integration tests → JaCoCo gate → docker build`.
On merge to main: all of the above → push image to ECR (tagged with git SHA) → deploy to **staging** automatically → smoke test (scripted purchase via API) → **manual approval gate** → deploy to prod.

- Images are immutable; the SHA that passed staging is the SHA that ships. No rebuilds between environments.
- DB migrations run via Flyway on app start; every migration must be backward-compatible with the previous app version (expand → migrate → contract pattern) so rollback is always safe.

## Rollback

- App: redeploy previous task definition (previous SHA). Rehearsed in Phase 7; target < 5 minutes.
- DB: never roll back migrations in prod — roll forward with a fix. This is why expand/contract is mandatory.
- Feature flags (simple: config-driven) for risky features (waiting room, AI assistant) → disable without deploying.

## Infrastructure

- IaC from Phase 7: Terraform (or CDK) in `infra/` — no console-clicked resources; the diagram in the README must match `terraform plan` reality.
- Cost guardrails: AWS Budget alarm at $25/month; Fargate Spot for staging; staging scaled to zero when idle (script); MSK decision per ADR-008 (a single Kafka container on ECS is acceptable for MVP cost).
- Networking: ALB → services in private subnets; only ALB + NAT public; security groups least-privilege; secrets in SSM.

## Monitoring & alerting

- Prometheus scrapes every service; Grafana dashboards as JSON in repo.
- Golden signals per service + business metrics: orders/min, payment success %, seat-lock contention, Kafka consumer lag, WebSocket connections, LLM cost/day.
- Alerts (email/Discord webhook): payment success < 95% (5 min), consumer lag > 1000, p99 > 1 s (10 min), reconciliation mismatch > 0, budget alarm.
- Uptime: external ping (UptimeRobot free) on /health of prod.

## Runbooks (write during the phase that builds the feature, not after)

`docs/runbooks/` one page each, format: symptoms → diagnosis queries → remediation → prevention follow-up.
Required set: payment-stuck-pending · kafka-consumer-lag · seat-oversell-suspected (incl. the SQL invariant query) · websocket-storm · stripe-webhook-failures · llm-cost-spike · rollback-procedure.

## Incident habit (even solo)

Any prod issue > 15 min: write a 10-line postmortem (what happened, impact, timeline, root cause, actions) in `docs/postmortems/`. Blameless is easy when it's just you — the point is the written actions, and interviewers love these.

## Release notes

Every prod deploy appends to `CHANGELOG.md` (auto-generated from Conventional Commits). Public MVP gets a version: `1.0.0`.
