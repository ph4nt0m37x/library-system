# Circulation integration

## Consistency choice

Loan creation uses the minimum synchronous-acceptance design from the investigation:

1. Borrowing asks Membership for current eligibility.
2. Borrowing asks Inventory whether the requested library and Catalog title are active and whether stock is available.
3. Borrowing commits the loan and its external-event outbox row in one local transaction.
4. Inventory consumes `loan.created` and decrements availability.

The availability read is not an atomic reservation. Two concurrent requests can observe the same last copy before either Kafka event is applied. If a hard last-copy invariant is required, replace this with either an idempotent synchronous Inventory reservation plus compensation, or a `PENDING_STOCK` reservation saga. A synchronous reservation must not be combined with a second decrement in the `loan.created` consumer.

## Synchronous contracts

- `GET membership-service/api/members/{memberId}/subscription-status` returns the requested `memberId`, `exists`, `active`, and `currentPeriodEndsAt`. Missing members are `404`, inactive membership rejects the loan with `409 MEMBERSHIP_INACTIVE`, and an unavailable Membership service is `503`.
- `GET inventory-service/api/stock/{libraryId}/{bookId}/availability` returns `libraryActive`, `bookActive`, `availableQuantity`, and `available`. Missing resources are `404`, inactive/no-stock outcomes reject the loan with `409`, and dependency failure is `503`.
- `GET catalog-service/api/books/{bookId}/price` returns `{ "amount": number, "currency": string }`. Missing or retired books are `404`; Catalog failure is `503`.
- Inventory validates manual stock additions and transfers against its local retirement record and a bounded live Catalog check. A retirement event wins immediately; the live check remains a reconciliation guard and fails closed on disagreement or outage.

## External event contracts

Borrowing publishes `loan.created`, `loan.returned`, `loan.marked.lost`, and `loan.marked.damaged`. Catalog publishes `book.deleted`. Runtime DTOs and topic constants also drive the Springwolf documents at each service's `/springwolf/docs` endpoint.

All records are flat JSON objects and contain a stable UUID `eventId`, schema `eventVersion`, monotonic `aggregateVersion`, `occurredAt`, and the routing identifiers needed by the consumer. Loan records are keyed by `loanId`; deletion records are keyed by base `bookId`. Transition-specific timestamps are included for audit. Internal Axon events remain separate and are not exposed as integration payloads.

Membership intentionally publishes no Kafka events: current loan acceptance uses the live eligibility contract, and no consumer requires Membership profile or subscription events.

## Delivery, ordering, and recovery

- Borrowing and Catalog write state and outbox data transactionally. Scheduled publishers use idempotent Kafka producers, bounded retry, pending/dead-letter metrics, and the original aggregate identifier as the Kafka key.
- Inventory records both the external `eventId` and the semantic `(loanId, transition)` identity in the same local transaction as each stock change. Exact and semantic redelivery are acknowledgements without another mutation.
- Inventory accepts a terminal loan transition only after `loan.created`, on the same book/library route, from the borrowed state, and at a greater aggregate version.
- Invalid schema/key/payload and exhausted out-of-order records go to `<original-topic>.DLT`. Spring Kafka headers preserve original topic, partition, offset, key/payload, and exception diagnostics.
- Inventory exposes exhausted recovery counters at `/actuator/metrics/inventory.kafka.dead.letter.recoveries`, tagged by source topic.
- Monitor Borrowing at `/actuator/metrics/borrowing.outbox.pending` and `/actuator/metrics/borrowing.outbox.dead.lettered`; Catalog exposes the equivalent `catalog.outbox.*` metrics. A sustained pending count indicates Kafka delivery failure. Any dead-letter count or DLT record requires operator reconciliation before replay.

The development Kafka broker may retain pre-versioned messages that do not satisfy these contracts. Before introducing a new consumer group with `auto-offset-reset=earliest`, purge those development topics or migrate/re-publish compatible versioned records. Do not replay a DLT record until its contract or ordering cause is corrected.

## Startup order

Start the root Compose project first (Kafka, Kafka UI, Consul), followed by Membership and Catalog, then Inventory, then Borrowing. The service Compose projects share `shared_net` but cannot declare cross-project `depends_on`; synchronous clients fail closed and outbox publication retries while dependencies recover.

Canonical local ports are Membership `8087`, Catalog `8088`, Inventory `8089`, and Borrowing `8090`. Each service database listens on `3306` inside its container; host mappings are service-specific.
