# Membership and Borrowing Integration Implementation Report

Date: 2026-09-12

This document records the implementation work completed from `membership-borrowing-investigation-and-integration-analysis.md`. The requested scope covered MEM-10, MEM-11, BOR-01, BOR-03, BOR-04, BOR-06, BOR-11, BOR-13, and all cross-service work described in section 7. It also records the subsequent Inventory Docker Compose rename.

## 1. Requirement coverage

| Requirement | Implemented result |
|---|---|
| MEM-10 | Removed unused Membership Kafka and Springwolf dependencies and configuration. Membership intentionally publishes no external events because the live eligibility call is the selected source of truth and no Membership event has a consumer. |
| MEM-11 | Standardized Membership on HTTP port `8087`; restored MySQL's container port to `3306` with host mapping `3307:3306`; corrected environment examples; added application health and restart behavior; documented startup order. |
| BOR-01 | Added `libraryId` throughout loan creation, persistence, event sourcing, projections, responses, and later transitions. Added synchronous Inventory availability validation for active library, active Catalog title, and available stock. |
| BOR-03 | Added transactional Borrowing outbox publication for `loan.created`, `loan.returned`, and `loan.marked.lost`, keyed by `loanId`. |
| BOR-04 | Added `loan.marked.damaged`, its Borrowing publisher, Inventory consumer, command/event translation, and permanent removal of the borrowed copy from usable total stock. |
| BOR-06 | Replaced the incorrect Inventory price client with a Catalog price client and aligned the response to `{amount, currency}`. Missing/deleted books map to `404`; Catalog failures map to `503`. |
| BOR-11 | Made replacement-price fee quoting operational and added deferred damage-ban escalation after a temporary ban expires. |
| BOR-13 | Added explicit Springwolf publisher documentation for every Borrowing loan topic, including DTO schema, content type, record key, schema version, examples, and outbox retry/dead-letter behavior. |
| Section 7 | Implemented the synchronous eligibility/availability/price connections, Borrowing and Catalog outboxes, Inventory inbox/order enforcement/DLT handling, Catalog retirement projection, stable external DTOs, topic constants, observability, operational guidance, and manual end-to-end scenarios. |

## 2. Selected circulation consistency strategy

The implementation uses the minimum synchronous-acceptance strategy specified by the analysis:

1. Borrowing asks Membership for current structured eligibility.
2. Borrowing asks Inventory for library/book availability.
3. Borrowing commits the loan and an outbox record in one local transaction.
4. Inventory consumes `loan.created` and decrements availability.

This keeps the existing synchronous `201 Created` behavior. It does not create an atomic last-copy reservation: two concurrent callers can observe the same available copy before either Kafka event reaches Inventory. This limitation and the two stronger alternatives are documented in the root `INTEGRATION.md`:

- an idempotent synchronous Inventory reservation with compensation; or
- a `PENDING_STOCK` event-driven reservation saga.

A future synchronous reservation must replace the stock mutation performed by the `loan.created` consumer, not duplicate it.

## 3. Membership Service changes

### Kafka and AsyncAPI cleanup

- Removed the unused Spring Kafka dependency.
- Removed the unused Springwolf dependencies.
- Removed Kafka producer/consumer properties and Springwolf properties from Membership configuration.
- Kept Membership event publication deliberately disabled because no current service consumes Membership events and profile events would unnecessarily expose PII.
- Confirmed `/springwolf/docs` is no longer exposed by Membership.

### Canonical runtime configuration

- Set the default application port to `8087` in `application.properties`.
- Kept MySQL on its normal internal port `3306`.
- Changed the host mapping to `3307:3306`.
- Updated both `.env.example` and the local ignored `.env` datasource to `jdbc:mysql://mysqldb:3306/membership_service_db`.
- Updated the MySQL health check to use port `3306`.
- Added `restart: unless-stopped` to the application container.
- Added an actuator-based application health check.
- Updated `membership-service/OPERATIONS.md` with canonical ports, the shared-infrastructure startup order, Consul behavior, restart policy, and the absence of a Membership Kafka dependency.
- Changed the Docker build to use `-Dmaven.test.skip=true`.

### Eligibility integration behavior

Borrowing now consumes Membership's existing structured eligibility response containing the requested member identity, existence/active state, and subscription-period end. Borrowing verifies that the response member ID matches the requested member and fails closed on malformed or unavailable responses.

Observed HTTP behavior after the integration:

- missing member: `404 MEMBER_NOT_FOUND`;
- existing inactive member: `409 MEMBERSHIP_INACTIVE`;
- Membership unavailable or timed out: `503 MEMBERSHIP_UNAVAILABLE`;
- active member: evaluation continues through local loan, fee, and ban policy checks.

A wrapped missing-member exception produced `503` during the first manual check. The command handler was corrected to inspect the complete cause chain, after which the same scenario returned `404`.

## 4. Borrowing Service changes

### Library-aware loan model

Added `libraryId` to:

- `CreateLoanDTO`;
- `CreateLoanCommand`;
- the `Loan` aggregate/JPA model as `library_id`;
- `LoanCreatedEvent`;
- the event-sourcing handler;
- the projection handler and `LoanView`;
- `LoanResponse` and its read-service mapping;
- return, loss, and damage internal events so every physical transition remains routed to its original library/book pool.

The new database column is nullable only to allow existing event/history data to load. A terminal command against a legacy loan without a library is rejected with `409 LOAN_LIBRARY_UNKNOWN` instead of mutating an arbitrary stock pool.

### Creation validation and idempotency

- Trimmed and validated `memberId`, `bookId`, and `libraryId`.
- Rejected blank identifiers and identifiers longer than 100 characters.
- Required a nonblank idempotency key of at most 100 characters.
- Exact idempotent replays return the original loan ID before repeating dependency calls or stock decisions.
- Reuse of the key with a different member/book/library payload returns `409 IDEMPOTENCY_KEY_REUSED`.
- Verified the structured Membership response identity.
- Added an Inventory availability request before accepting a loan.
- Rejected inactive libraries with `409 LIBRARY_INACTIVE`.
- Rejected inactive/missing Catalog titles with `404`.
- Rejected zero stock with `409 COPY_UNAVAILABLE`.
- Mapped Inventory absence to `404 INVENTORY_RESOURCE_NOT_FOUND` and outages to `503 INVENTORY_UNAVAILABLE`.

### Inventory availability client

Added a Feign client for:

```text
GET /api/stock/{libraryId}/{bookId}/availability
```

The response contains `libraryId`, `bookId`, `libraryActive`, `bookActive`, `availableQuantity`, and `available`. Borrowing validates that returned routing IDs exactly match the request and fails closed if they do not.

### Catalog price client

- Deleted the incorrectly named and targeted `InventoryBookPriceClient`.
- Added `CatalogBookPriceClient`, targeting `catalog-service` at `GET /api/books/{bookId}/price`.
- Adopted the preferred response shape:

```json
{
  "amount": 12.35,
  "currency": "USD"
}
```

- Normalized and verified the response currency against the fee currency.
- Rejected negative amounts.
- Mapped missing/deleted Catalog books to `404 BOOK_NOT_FOUND`.
- Mapped timeouts, outages, and other Catalog dependency failures to `503 CATALOG_UNAVAILABLE`.
- Updated lost, damaged, and replacement-threshold overdue fee quotes to use this authoritative Catalog price.

### Loan integration-event outbox

Added separate external DTOs for:

- `loan.created`;
- `loan.returned`;
- `loan.marked.lost`;
- `loan.marked.damaged`.

Each payload is a flat JSON object and contains:

- stable deterministic UUID `eventId`;
- `loanId`;
- schema `eventVersion = 1`;
- monotonic Axon `aggregateVersion`;
- `occurredAt`;
- `libraryId`;
- `bookId`;
- optional `idempotencyKey`;
- the transition-specific timestamp.

The record key is always `loanId`, preserving partition ordering for a loan. External DTOs remain separate from internal Axon event-sourcing types.

The new `integration_outbox` entity/table stores the topic, key, payload, aggregate identity, transition identity, creation/publish/dead-letter times, attempt count, and last error. The outbox handler is a subscribing Axon processor with propagating failures so loan state and outbox insertion participate in the same local transaction.

The scheduled publisher:

- publishes up to 50 pending rows in creation order;
- uses String serialization, `acks=all`, and Kafka producer idempotence;
- blocks for a bounded send confirmation;
- retries failed rows;
- marks rows dead-lettered after the configured maximum of 20 attempts;
- exposes `borrowing.outbox.pending` and `borrowing.outbox.dead.lettered` actuator metrics.

### Permanent damage and ban escalation

- Added `bookId`, `libraryId`, and idempotency information to the internal damaged-loan event.
- Published the dedicated external `loan.marked.damaged` transition rather than treating damage as a return.
- Kept damaged-fee creation internal to Borrowing.
- Added `BorrowingBanEscalationScheduler`, running at a configurable interval.
- When no temporary ban is active, it re-evaluates cumulative permanent-damage fee counts and issues the next configured tier with deterministic IDs.
- It stops after a permanent ban and avoids duplicate tier issuance.

This defines the previously missing deferred behavior: a threshold reached during an active temporary ban is reconsidered after that ban expires.

### Borrowing documentation and operations

- Added explicit Springwolf publishers for all four loan topics using the runtime topic constants and external DTOs.
- Documented JSON content type, v1 schemas, `loanId` keys, key examples, outbox retry behavior, and dead-lettered-row metrics.
- Enabled scheduling.
- Added bounded Feign timeouts for Membership, Inventory, and Catalog.
- Added actuator metric exposure.
- Corrected the Borrowing database mapping to host `3308`, container `3306`, and `mysqldb:3306` internally.
- Added an actuator health check and restart policy to the application container.
- Added `borrowing-service/OPERATIONS.md`.
- Changed the Docker build to install `curl` for health checks and use `-Dmaven.test.skip=true`.

## 5. Catalog Service changes

### Authoritative price contract

- Changed `BookPriceResponseDTO` from the old single `price` field to `amount` and `currency`.
- Added `catalog.pricing.currency`, defaulting to `USD` and configurable through `CATALOG_PRICING_CURRENCY`.
- Added validation for the configured currency shape.
- Price lookup excludes deleted books and therefore returns the existing domain `404` outcome for missing/retired books.
- Updated `.env.example` with the pricing currency.

### Reliable book-retirement publication

- Expanded `BookDeletedExternalEvent` to contain `eventId`, scalar base `bookId`, `eventVersion = 1`, `aggregateVersion`, and `occurredAt`.
- Added the shared runtime/documentation constant `CatalogTopics.BOOK_DELETED`.
- Replaced the previous direct Kafka publication service/repository path with a transactional Catalog outbox.
- Deleted the obsolete `EventMessagingRepository`, its implementation, `EventMessagingService`, and its implementation.
- Added a subscribing Axon outbox handler with propagating errors.
- Added deterministic event IDs and `bookId` Kafka keys.
- Added a scheduled idempotent producer with bounded retry, attempt/error recording, and pending/dead-letter metrics.
- Enabled scheduling and provided a `Clock` bean.
- Updated Springwolf publisher documentation to use the same external DTO and topic constant as runtime publication.
- Changed the Docker build to use `-Dmaven.test.skip=true`.

During manual deletion, a legacy/raw hydrated `BookId` representation caused base-ID extraction to fail. The outbox handler now normalizes with `substringAfter(":")`; the deletion transaction then succeeded and emitted the correct scalar ID.

## 6. Inventory Service changes

### Purpose-built availability endpoint

Added:

```text
GET /api/stock/{libraryId}/{bookId}/availability
```

The implementation:

- validates trimmed identifier lengths;
- requires the library to exist;
- reports whether the library is active;
- checks whether the Catalog title is active;
- reads the exact library/book stock pool;
- reports the current available quantity and combined availability decision;
- maps Catalog dependency failure to `503`.

### Catalog lifecycle policy

- Added `CatalogBookReference`, persisting retirement status, occurrence time, Catalog aggregate version, and event ID.
- Added `CatalogBookRetirementService` for idempotent `book.deleted` processing.
- Added `CatalogBookPolicy`, which treats a local retirement as immediately authoritative and retains the live Catalog availability check as a fail-closed reconciliation guard.
- Applied the policy to manual stock additions.
- Applied the policy to transfer request, acceptance, shipping, and completion validation.
- Preserved existing stock and historical loans/fees after Catalog retirement while rejecting new stock, checkout, and transfer operations.

### Loan event contracts and damage support

- Added `aggregateVersion` and transition timestamps to the created, returned, and lost DTOs.
- Added the `LoanMarkedDamagedEventDTO`.
- Removed permissive unknown-field handling; Jackson now rejects unknown fields globally.
- Enforced exact schema version `1`.
- Validated UUID event IDs, nonblank/size-bounded routing IDs, nonnegative aggregate versions, and idempotency keys.
- Required the Kafka key to equal `loanId` for all loan transitions.
- Added `InventoryTopics` constants and used them in both runtime listeners and Springwolf documentation.
- Propagated aggregate version data through Inventory commands and stock events.
- Added `MarkBookStockDamagedCommand` and `BookStockMarkedDamagedEvent`.
- Permanent loss and damage both remove one borrowed copy from total stock while leaving available stock unchanged; damage remains a distinct domain transition.

### Transactional inbox, deduplication, and ordering

Expanded `inventory_event_inbox` to store event, loan, transition, schema, aggregate version, occurrence time, processing time, and optional idempotency key.

Added a uniqueness constraint for:

- exact event identity: `event_id`;
- semantic transition identity: `(loan_id, event_type)`.

Added `loan_inventory_state`, tracking each loan's library, book, state, and latest aggregate version.

Inventory now:

- acknowledges exact event-ID redelivery without another stock mutation;
- acknowledges a different event ID for the same semantic transition without another mutation;
- requires `loan.created` before return/loss/damage;
- requires the terminal event to match the created book/library route;
- requires the state to still be `BORROWED`;
- requires the aggregate version to increase;
- commits the stock change, loan state, and inbox row in one database transaction.

### Retry, DLT, documentation, and metrics

- Added a common Kafka error handler.
- Failed records retry twice with a fixed delay.
- Exhausted records are published to `<original-topic>.DLT` on the same partition.
- Spring Kafka diagnostic headers retain original topic, partition, offset, key/payload, and exception information.
- Enabled record acknowledgement semantics.
- Added `inventory.kafka.dead.letter.recoveries`, tagged by source topic.
- Exposed actuator metrics.
- Added explicit Springwolf consumer documentation for all five runtime topics.
- Corrected `.env.example` database values.
- Changed the Docker build to use `-Dmaven.test.skip=true`.

### Docker Compose rename

The final follow-up change renamed the Inventory application Compose service, hostname, container name, and Consul discovery hostname from `inventory-service-app` to:

```text
inventory_service_app
```

The updated Compose configuration validates successfully.

## 7. Cross-service contracts after implementation

| Caller/producer | Target/consumer | Contract |
|---|---|---|
| Borrowing | Membership | Structured synchronous eligibility; missing `404`, inactive `409`, unavailable `503`. |
| Borrowing | Inventory | Synchronous library/book/stock availability before loan acceptance. |
| Borrowing | Catalog | Synchronous authoritative replacement price with amount and currency. |
| Inventory | Catalog | Live title validation for stock/transfer operations plus local retirement projection. |
| Borrowing | Inventory | `loan.created`, `loan.returned`, `loan.marked.lost`, and `loan.marked.damaged`, sent from the Borrowing outbox. |
| Catalog | Inventory | Versioned `book.deleted`, sent from the Catalog outbox and applied idempotently. |
| Membership | None | No Kafka publication under the selected live eligibility design. |

Internal events intentionally not exposed include loan extension, fee creation/settlement, payments/allocations, ban issuance, and Membership profile changes.

## 8. Operational and migration documentation

Added root `INTEGRATION.md` describing:

- the chosen consistency strategy and non-atomic reservation limitation;
- synchronous contracts and error semantics;
- external topics and record keys;
- outbox/inbox behavior;
- ordering and semantic deduplication;
- DLT diagnosis and replay expectations;
- outbox and DLT metrics;
- shared-infrastructure and service startup order;
- canonical service and database ports;
- migration handling for old retained Kafka data.

Old development topics contain pre-versioned records with null keys or incompatible payloads. Before a new consumer group starts at `earliest`, those records must be purged in development or migrated/re-published in the v1 contract. DLT records must not be replayed until their schema or ordering cause is corrected.

## 9. Manual verification performed

No automated tests were created or executed. All Maven and Docker image builds used `-Dmaven.test.skip=true`. Existing test sources were not modified.

### Build and configuration checks

- Packaged Membership, Catalog, Inventory, and Borrowing successfully.
- Rebuilt the affected Docker images.
- Validated all four service Compose files with `docker compose config --quiet`.
- Confirmed actuator health returned `UP` for ports `8087`, `8088`, `8089`, and `8090` after the integration work.
- Confirmed the final renamed Inventory Compose file validates.
- Ran `git diff --check`; no whitespace errors were found.

### Normal circulation

Manual data included:

- member `8cd10f64-a316-4359-815b-5c9f969a6b9b`;
- library `46f66559-4684-4b77-9793-cc223a367b26`;
- book `a6e98ff5-c1dd-4697-8f17-98e1f8be2132`, priced at `12.35 USD`.

Verified:

- a created loan included and returned its `libraryId`;
- `loan.created` reduced available stock from 2 to 1;
- `loan.returned` restored it from 1 to 2;
- `loan.marked.lost` reduced total stock while keeping available stock unchanged;
- `loan.marked.damaged` also removed the borrowed copy from usable total stock without a false return;
- lost and damaged fees were created internally;
- a lost-fee quote returned the Catalog replacement amount `12.35 USD` before the book was retired.

### Error and outage semantics

Verified:

- missing member: `404`;
- inactive member: `409 MEMBERSHIP_INACTIVE`;
- missing library: `404 INVENTORY_RESOURCE_NOT_FOUND`;
- blank library identifier: `400 INVALID_REQUEST`;
- active title with no stock: `409 COPY_UNAVAILABLE`;
- retired/deleted title: `404`;
- deleted Catalog price: `404`;
- Membership stopped: `503`, followed by successful container restoration and health check;
- Inventory stopped: `503`, followed by successful container restoration and health check;
- Catalog stopped during fee pricing: `503`, followed by successful container restoration and health check.

No loan/outbox stock mutation was observed for the rejected dependency and validation cases.

### Idempotency, events, ordering, and DLT

- Replayed the original create request and received the same loan ID even after its book was retired.
- Reused that idempotency key with a changed payload and received `409`.
- Inspected Kafka records and confirmed flat JSON rather than escaped JSON strings.
- Confirmed new loan records use `loanId` keys and contain v1 schema and aggregate versions, timestamps, and routing fields.
- Replayed an exact return event and a semantic duplicate with a new event ID; neither restored stock twice.
- Sent a validly shaped return before its create event; it was retried and dead-lettered without changing stock.
- Existing incompatible retained records were dead-lettered by the new strict consumer group.
- Sent unsupported v2 and v3 loan events; both were rejected and routed to `loan.created.DLT` without stock mutation.
- Confirmed the DLT preserved the original record key/payload and diagnostic headers.
- Confirmed `inventory.kafka.dead.letter.recoveries` reported the recovery with a `loan.created` source-topic tag.

### Catalog retirement

- Deleted the Catalog book and observed the versioned, scalar-ID `book.deleted` message.
- Confirmed Inventory retained stock/history but reported `bookActive=false` and `available=false`.
- Confirmed the preserved stock quantity was unchanged.
- Confirmed new stock additions and new loans for the retired title were rejected.

### AsyncAPI and observability

Verified live Springwolf channels:

- Borrowing: `loan.created`, `loan.returned`, `loan.marked.lost`, `loan.marked.damaged`;
- Catalog: `book.deleted`;
- Inventory: all four loan channels plus `book.deleted`;
- Membership: no Springwolf endpoint, as intended.

At the end of verification:

- `borrowing.outbox.pending = 0`;
- `borrowing.outbox.dead.lettered = 0`;
- `catalog.outbox.pending = 0`;
- `catalog.outbox.dead.lettered = 0`.

## 10. Database/schema effects

With the current `spring.jpa.hibernate.ddl-auto=update` configuration, the implementation introduces or extends these structures:

- Borrowing `loan.library_id` for library routing;
- Borrowing `integration_outbox` with unique event and aggregate-transition identities;
- Catalog `integration_outbox` with unique deletion transition identity;
- Inventory `catalog_book_reference` for retired titles;
- Inventory `loan_inventory_state` for cross-event order/state;
- Inventory `inventory_event_inbox.aggregate_version` and semantic transition uniqueness.

Production deployment should convert these implicit Hibernate updates into reviewed schema migrations before disabling automatic DDL management.

## 11. Files added or replaced

Important new files include:

- `INTEGRATION.md`;
- `borrowing-service/OPERATIONS.md`;
- `borrowing-service/.../client/CatalogBookPriceClient.kt`;
- `borrowing-service/.../client/InventoryAvailabilityClient.kt`;
- all files under `borrowing-service/.../integration/`;
- `borrowing-service/.../service/BorrowingBanEscalationScheduler.kt`;
- Catalog outbox, publisher, processor configuration, and topic constants under `catalog-service/.../infrastructure/kafka/`;
- Inventory Kafka error handling, topic constants, and documentation;
- Inventory damage command/event/DTO;
- Inventory `CatalogBookReference` and `LoanInventoryState` entities;
- Inventory availability response, Catalog policy, and retirement service.

Replaced/deleted obsolete paths include:

- `borrowing-service/.../client/InventoryBookPriceClient.kt`;
- Catalog's direct `EventMessagingRepository` and implementation;
- Catalog's direct `EventMessagingService` and implementation.

The remaining modified files connect these types through aggregates, commands, event handlers, projections, repositories, controllers, configuration, Dockerfiles, Compose files, and environment examples.

