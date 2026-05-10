# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build all modules
./gradlew build

# Run a specific service
./gradlew :account-service:bootRun
./gradlew :product-service:bootRun
./gradlew :order-service:bootRun
./gradlew :notification-service:bootRun

# Run all tests
./gradlew test

# Run tests for a specific service
./gradlew :order-service:test

# Run a single test class
./gradlew :order-service:test --tests "dn.orderservice.OrderServiceIT"
```

## Project Structure

Multi-module Gradle (Kotlin DSL) project. Java 25, Spring Boot 3.5, virtual threads.

```
shared-events/         # Shared library: events, outbox, idempotency infrastructure
account-service/       # User profiles, bans, addresses
product-service/       # Product catalog, inventory management
order-service/         # Order orchestration, saga coordinator
notification-service/  # Notifications via Kafka DLT + scheduled retry
```

All services depend on `shared-events` but never directly on each other.

## Architecture: Key Patterns

### Transactional Outbox (`shared-events`)
Events are written to the `marketplace.outbox` table within the same transaction as the business entity. `OutboxRelay` (scheduled every 1s) polls with `FOR UPDATE SKIP LOCKED` to send to Kafka without duplicate processing. Never publish to Kafka directly — always go through outbox.

Key rules:
- `OutboxEntity.id` must be set to `outboxEvent.eventId()` (write-side idempotency — duplicate key prevents double-publish)
- All `createOutbox` methods must be annotated `@Transactional(propagation = MANDATORY)` — they must always run within an existing transaction
- `ExecutorService` bean and `ConcurrencyConfig` live only in `shared-events`, not in individual services

### Idempotency (`shared-events`)
`EventProcessor.processEvent(UUID eventId)` inserts into `processed_events` table with `@Transactional(propagation = REQUIRES_NEW)`. Saga listeners call it first; on `DataIntegrityViolationException` (duplicate key) — the event was already processed, skip silently. `REQUIRES_NEW` is critical: prevents the duplicate-key exception from tainting the outer transaction.

### Choreography-Based Saga (order ↔ product)
Order lifecycle is driven by Kafka events across services:
- `order.created` → ProductSagaListener reserves inventory → publishes `item.reserved` or `item.reserved.failed`
- `item.reserved` → OrderSagaListener confirms order
- `order.cancelled` → ProductSagaListener restores inventory

Each saga listener uses `EventProcessor` (idempotency via `processed_events` table) to safely handle redelivery.

### HTTP Sync Call
Order-service calls product-service via HTTP for batch product queries (`ProductClient`). Connect and read timeouts configured via `app.http.client.connect-timeout` / `app.http.client.read-timeout`.

## Environment Variables

All services share this pattern (configured in `application.yml` via env vars):

| Variable | Purpose |
|----------|---------|
| `SERVER_PORT` | HTTP port |
| `POSTGRES_URL` | JDBC URL (each service has its own DB) |
| `POSTGRES_USERNAME / PASSWORD` | DB credentials |
| `REDIS_HOST / PORT / REDIS_CACHE_TTL_SECONDS` | Redis cache |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker |
| `LIQUIBASE_CHANGELOG_PATH` | Path to `db.changelog-master.yaml` |
| `APPLICATION_NAME` | Spring app name |
| `HTTP_CLIENT_PRODUCT_SERVICE_URL` | (order-service only) product-service base URL |
| `APP_HTTP_CLIENT_CONNECT_TIMEOUT` | (order-service only) HTTP connect timeout ms |
| `APP_HTTP_CLIENT_READ_TIMEOUT` | (order-service only) HTTP read timeout ms |

## Database Conventions

- Schema: `marketplace` (all tables, including outbox and processed_events)
- Migrations: Liquibase; `hibernate.ddl-auto: validate` — schema must exist before startup
- Pessimistic locking used where needed: `@Lock(PESSIMISTIC_WRITE)` on JPA queries or native `FOR UPDATE SKIP LOCKED`

## Kafka Topics

| Topic | Producer | Consumer |
|-------|----------|----------|
| `order.created` | order-service | product-service, notification-service |
| `order.confirmed` / `order.paid` / `order.cancelled` | order-service | — |
| `item.reserved` / `item.reserved.failed` | product-service | order-service |
| `payment.success` / `payment.failed` | (future) | order-service |
| `account.created` / `account.banned` / etc. | account-service | notification-service |

Dead Letter Topics (DLT) are configured for each consumer group.

## Testing

- Unit tests: plain JUnit 5 + AssertJ + Mockito
- Integration tests: TestContainers (real PostgreSQL), named with `IT` suffix
- Async assertions: use `Awaitility` (already a dependency in order-service)
- Base class pattern: `AbstractProductIT` sets up shared TestContainers context

## CodeGraph

`.codegraph/` exists — use `codegraph_explore` (via Explore agent) for codebase questions. Use `codegraph_search` / `codegraph_callers` / `codegraph_impact` directly in main session for targeted symbol lookups before edits.
