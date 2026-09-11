# Catalog and Inventory Service Investigation, Manual Test Findings, and Required Fixes

Date: 11 September 2026  
Project: Library System  
Scope: Catalog Service and Inventory Service only

## 1. Scope and method

This report documents the source review, Docker deployment verification, Consul verification, manual HTTP testing, Kafka event testing, database inspection, and restart/persistence testing performed against the Catalog and Inventory services.

Membership and Borrowing were started and health-checked because the requested deployment precaution required all four microservices to be running. No Membership or Borrowing business API was tested.

No source code or automated tests were added. Before business testing started, two deployment-only changes were necessary:

1. `inventory-service/.env` was missing, so a local development file was created with the Inventory database URL and Kafka address `broker:29092`.
2. Membership and Inventory both attempted to bind port `8089`. Membership's Docker port and `SERVER_PORT` were changed to `8087` so all services could run simultaneously.

Once the deployment was healthy, code and configuration were frozen and all remaining actions were requests, event publication, read-only log inspection, database inspection, and container restarts.

The final running endpoints were:

| Component | Address | Final result |
|---|---:|---|
| Catalog | `http://localhost:8088` | Actuator `200`; Consul checks passing |
| Inventory | `http://localhost:8089` | Actuator `200`; Consul checks passing |
| Membership | `http://localhost:8087` | Actuator `200`; Consul checks passing |
| Borrowing | `http://localhost:8090` | Actuator `200`; Docker health check passing; Consul checks passing |
| Consul | `http://localhost:8500` | Healthy |
| Kafka | `localhost:9092` externally, `broker:29092` internally | Running |
| Kafka UI | `http://localhost:8081` | Running |

Catalog and Inventory were restarted after the tests. Both recovered, retained their MySQL state, resumed their Kafka positions, and re-registered as one passing Consul instance each.

---

# 2. Catalog Service

## 2.1 Current design

Catalog stores book metadata and categories in MySQL. Axon Server is disabled. Books are JPA-backed Axon aggregates; categories use ordinary Spring Data JPA CRUD.

The write-side book commands are:

- `CreateBookCommand`
- `UpdateBookCommand`
- `DeleteBookCommand`

The internal book events are:

- `BookCreatedEvent`
- `BookUpdatedEvent`
- `BookDeletedEvent`

`BookCreatedEvent` and `BookUpdatedEvent` do not produce external events. `BookDeletedEvent` is converted to `BookDeletedExternalEvent` and sent to a Kafka topic derived from the class name: `book.deleted`.

Although the code is structured into command and read services, `Book` and `BookView` both map to the same `book` table. This is a CQRS-shaped code separation, not a separate read model or independently projected read store.

Axon event data is also persisted in the Catalog MySQL database through tables such as `domain_event_entry` and `token_entry`.

## 2.2 Exposed Catalog API

### Categories

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/categories/create` | Create category |
| `PUT` | `/api/categories/update/{id}` | Rename category |
| `DELETE` | `/api/categories/delete/{id}` | Physically delete category |
| `GET` | `/api/categories/all` | List categories |

### Books

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/books/create` | Create book aggregate |
| `PUT` | `/api/books/update` | Replace mutable book data |
| `DELETE` | `/api/books/delete` | Set the book's `deleted` flag |
| `GET` | `/api/books/all` | List every book row |
| `GET` | `/api/books/{id}` | Find book by ID |
| `GET` | `/api/books/{bookId}/price` | Return book price |
| `GET` | `/api/books/search/title?title=...` | Case-insensitive title substring search |
| `GET` | `/api/books/search/author?author=...` | Case-insensitive author substring search |
| `GET` | `/api/books/filter/category?categoryId=...` | Filter by category ID |

## 2.3 Catalog behavior that worked

The following behavior worked in the deployed service:

- Category creation, update, listing, and deletion of an unused category.
- Book creation with and without a category.
- Book retrieval by ID and listing.
- Book update and category reassignment.
- Case-insensitive title and author substring search.
- Category filtering.
- Existing-book price lookup.
- `404` for a missing book read.
- `404` for update/delete of a missing category.
- `400` for malformed JSON and missing non-null DTO fields.
- `415` for an unsupported request content type.
- MySQL state and Axon event persistence across a container restart.
- Kafka publication of book deletion events.
- Consul registration, health checking, deregistration during restart, and clean re-registration.

## 2.4 Catalog findings and required fixes

### CAT-01 — High: soft deletion is not enforced by reads or commands

Observed:

- Deleting a book sets `deleted=true` but does not hide the record.
- The deleted book remains visible through `GET /api/books/all` and `GET /api/books/{id}`.
- Title/author searches and category filtering include deleted books.
- Price lookup continues to return a price for a deleted book.
- Deleting the same book repeatedly returns success and emits another deletion event.
- Updating a deleted book succeeds and changes its stored data while leaving `deleted=true`.

Cause:

- Read repositories use unrestricted `findAll`, `findById`, title, author, and category queries.
- Update and delete command handlers do not check `deleted`.
- The deleted flag has no database filter or repository predicate associated with it.

Required fix:

1. Decide the deletion contract explicitly:
   - soft-deleted records are inaccessible to ordinary clients; or
   - archived records are intentionally visible through separate administrative endpoints.
2. Add `deleted=false` to every normal book query.
3. Make `GET /api/books/{id}` and price lookup return `404` or `410` for deleted books.
4. Reject update and repeated-delete commands once a book is deleted.
5. Add a separate restore command only if restoration is a real business requirement.
6. Ensure deletion events are emitted once per actual state transition, not once per repeated request.

### CAT-02 — High: ISBN uniqueness is not enforced

Observed:

- Two books with ISBN `978-E2E-001` were created successfully.

Cause:

- `isbn` is a plain field without a unique database constraint.
- The create command does not check an ISBN index or repository.

Required fix:

1. Define whether ISBN is globally unique or unique per edition/format.
2. Normalize ISBN input before comparison by removing permitted separators and validating ISBN-10/ISBN-13 checksums.
3. Add the corresponding database unique constraint.
4. Perform an application-level conflict check for a useful response, while retaining the database constraint for race safety.
5. Return `409 Conflict` for a duplicate ISBN.

### CAT-03 — High: important book/category input validation is absent

Observed:

- Empty ISBN, title, and author strings were accepted.
- A publication year of `-500` was accepted.
- An empty category name was accepted.
- A nonexistent category reference reached the database and returned `500`.
- Zero and negative prices were rejected by `Money`, but surfaced as `500` rather than a client error.

Required fix:

1. Add Jakarta Bean Validation to DTOs and `@Valid` to controller request bodies.
2. Validate:
   - nonblank ISBN, title, author, and category name;
   - normalized maximum lengths compatible with database columns;
   - a publication-year range agreed by the domain;
   - price greater than zero with supported scale and precision;
   - category existence before dispatching the book command.
3. Reject invalid input with field-level `400 Bad Request` responses.
4. Add database `NOT NULL`, length, unique, and check constraints as a second line of defense.

### CAT-04 — Medium: price precision is silently rounded

Observed:

- Price `12.3456` was accepted and later returned as `12.35`.

Cause:

- The API does not validate scale explicitly, so database/JPA decimal mapping performs rounding.

Required fix:

1. Define the accepted currency scale, normally two decimal places for the current model.
2. Either reject excessive precision with `400`, or round explicitly using a documented rounding rule before command creation.
3. Declare explicit JPA precision/scale for `Money.amount`.
4. Consider adding a currency field if the system can operate in more than one currency.

### CAT-05 — Medium: `publisher` is accepted and then discarded

Observed:

- `CreateBookDTO` and `UpdateBookDTO` accept `publisher`.
- The REST controller does not copy it into a command.
- The aggregate, events, and view do not contain publisher.
- Responses therefore omit data clients were allowed to submit.

Required fix:

- If publisher is part of the domain, add it consistently to the command, events, aggregate, database schema, and read view.
- If publisher is not part of the domain, remove it from both DTOs and the OpenAPI contract.

### CAT-06 — High: expected business failures are exposed as generic `500`

Observed `500` cases included:

- Missing book price.
- Update/delete of a nonexistent book aggregate.
- Duplicate category name.
- Deleting a category referenced by a book.
- Nonexistent category supplied during book creation.
- Zero or negative price.

Cause:

- Catalog has no service-specific REST exception mapping for domain validation, aggregate-not-found, conflict, or foreign-key exceptions.

Required fix:

1. Add a `@RestControllerAdvice`.
2. Map errors consistently:
   - malformed/invalid input: `400`;
   - missing resource or aggregate: `404`;
   - duplicate ISBN/category and illegal state transition: `409`;
   - referenced category deletion: `409` with an explanatory problem detail;
   - deleted resource: `404` or `410`, according to the chosen contract.
3. Return RFC 9457 `ProblemDetail` bodies containing a stable error code, message, path, timestamp, and field violations where relevant.
4. Avoid leaking persistence or Axon implementation exceptions.

### CAT-07 — High: category deletion conflicts with soft-deleted books

Observed:

- Deleting a category referenced by books returned `500`.
- Soft-deleting a book does not release its category foreign key, so the category can remain undeletable indefinitely.

Required fix:

1. Define category lifecycle policy:
   - prevent deletion while any active or archived book references it;
   - soft-delete categories too; or
   - reassign books to an explicit uncategorized category.
2. Check the policy before calling `deleteById`.
3. Return `409 Conflict` rather than relying on a database exception.

### CAT-08 — High: Kafka deletion payload is double encoded

Observed runtime record shape:

```text
book-id-key | "{\"bookId\":{\"value\":\"...\",\"entityClass\":null}}"
```

The Kafka value is a JSON string containing escaped JSON instead of a JSON object.

Cause:

- `EventMessagingEventHandler` manually converts the event to a JSON string.
- That string is sent through `KafkaTemplate<String, String>` while the producer uses Spring Kafka's `JsonSerializer`, which serializes the string a second time.

Required fix — choose one consistent approach:

1. Preferred: use `KafkaTemplate<String, BookDeletedExternalEvent>` and send the object directly through `JsonSerializer`; or
2. Keep a string template and configure `StringSerializer` for values after manual serialization.

Add a consumer-contract test that asserts the Kafka value is a JSON object with a documented schema.

### CAT-09 — Medium: AsyncAPI documentation does not match runtime messaging

Observed:

- Runtime topic: `book.deleted`.
- Catalog AsyncAPI channel: `catalog-events`.
- Documentation declares `BookDeletedEvent`.
- Runtime conversion sends `BookDeletedExternalEvent`.

Required fix:

1. Document the actual `book.deleted` channel.
2. Declare `BookDeletedExternalEvent` as the payload type.
3. Include the message key, content type, version, and example payload.
4. Generate the documentation from the same constants/types used by the publisher to prevent drift.

### CAT-10 — High: deletion publication is not idempotent and has no delivery guarantees

Observed:

- Two delete requests for the same already-deleted book produced two `book.deleted` records.
- Kafka had three deletion records for three delete commands, including the duplicate state transition.

Additional risk:

- Database commit and Kafka send are separate operations. A failure between them can leave Catalog deleted without notifying downstream consumers, or can produce a duplicate after retry.

Required fix:

1. Reject repeat deletion at aggregate level.
2. Include a unique `eventId`, aggregate ID, aggregate version, event version, and occurrence timestamp in the external event.
3. Use a transactional outbox or another durable publication mechanism.
4. Require consumers to deduplicate by `eventId`.
5. Add retry and dead-letter handling with monitoring.

### CAT-11 — Medium: aggregate handlers manually apply state before publishing the event

Observed source pattern:

```kotlin
this.on(event)
AggregateLifecycle.apply(event)
```

This appears in create, update, and delete handlers.

Impact:

- Axon invokes the event-sourcing handler when `AggregateLifecycle.apply` is used, so manually invoking it applies state twice.
- Current Catalog handlers mostly assign values, which makes the duplicate mutation appear harmless, but it is unsafe and is the direct cause of catastrophic arithmetic defects in Inventory.

Required fix:

- Remove every direct `this.on(event)` call from command handlers.
- Use only `AggregateLifecycle.apply(event)` and let the event-sourcing handler mutate state.
- Add Axon aggregate fixture tests asserting one state transition and one stored event per command.

### CAT-12 — Medium: command response contracts are inconsistent

Observed:

- Create returns `200` with a `BookId` containing internal `entityClass` metadata.
- Update and delete return `200` with an empty body even though service methods are typed as `CompletableFuture<BookId>`.
- Create uses `200` rather than `201 Created`.

Required fix:

1. Introduce explicit public response DTOs without framework/domain serialization metadata.
2. Return `201 Created` plus a `Location` header for creation.
3. Return `204 No Content` for successful update/delete if no representation is returned, or return a documented updated representation.
4. Correct command gateway generic return types so they match actual handler results.

### CAT-13 — Low: request DTOs expose persistence entities

Observed:

- Book create/update request bodies accept an embedded `BookCategory` object.

Impact:

- The public API is coupled to the JPA entity.
- Clients must send an object where only an ID is required.
- A client can submit an arbitrary category name alongside an ID, creating ambiguous semantics.

Required fix:

- Replace `BookCategory?` in public DTOs/commands/events with `categoryId: Long?` or a dedicated category identifier value object.
- Resolve the category in the application layer.

### CAT-14 — Low: advertised audit fields are permanently empty/defaulted

Observed:

- Every book response contains `dateCreated: null` and `archived: false` from default interface methods.
- The service separately exposes a `deleted` field.

Required fix:

- Persist and populate audit fields if they belong in the API, or remove them.
- Consolidate `archived` and `deleted` into one clearly defined lifecycle model.

### CAT-15 — Security requirement is absent

Observed:

- All create, update, and delete operations were callable without authentication or authorization.

Required fix if the system is not intentionally public:

- Add authentication and role/permission checks.
- Restrict category/book mutations to authorized staff.
- Keep read access policy explicit.
- Include audit identity in commands/events.

### CAT-16 — Deployment robustness gaps

Observed:

- The service compose file depends on MySQL but not on Kafka or Consul readiness.
- The application compose definition has no Docker health check and no restart policy.
- Consul registration failure can abort application startup, as seen elsewhere in the same deployment.

Required fix:

1. Add a Docker actuator health check and a suitable restart policy.
2. Provide an orchestrated startup procedure for shared Kafka/Consul before service stacks.
3. Add bounded retry/backoff for Consul registration and Kafka publication.
4. Add `.env.example` documentation and use Docker secrets or externally managed secrets outside local development.

---

# 3. Inventory Service

## 3.1 Current design

Inventory stores libraries, book stock, and transfers in MySQL. Axon Server is disabled. `Library` and `Transfer` are JPA-backed Axon aggregates.

Library commands:

- `CreateLibraryCommand`
- `UpdateLibraryCommand`
- `DeleteLibraryCommand`
- `AddBookStockCommand`
- `RemoveBookStockCommand`
- `BorrowBookStockCommand`
- `ReturnBookStockCommand`
- `MarkBookStockLostCommand`

Transfer commands:

- `RequestTransferCommand`
- `AcceptTransferCommand`
- `RejectTransferCommand`
- `CancelTransferCommand`
- `ShipTransferCommand`
- `CompleteTransferCommand`

Library/stock events:

- `LibraryCreatedEvent`
- `LibraryUpdatedEvent`
- `LibraryDeletedEvent`
- `BookStockIncreasedEvent`
- `BookStockDecreasedEvent`
- `BookStockBorrowedEvent`
- `BookStockReturnedEvent`
- `BookStockMarkedLostEvent`

Transfer events:

- `TransferRequestedEvent`
- `TransferAcceptedEvent`
- `TransferRejectedEvent`
- `TransferCancelledEvent`
- `TransferShippedEvent`
- `TransferCompletedEvent`

Kafka consumers translate three external loan messages into stock commands:

| Topic | Inventory command | Quantity |
|---|---|---:|
| `loan.created` | `BorrowBookStockCommand` | 1 |
| `loan.returned` | `ReturnBookStockCommand` | 1 |
| `loan.marked.lost` | `MarkBookStockLostCommand` | 1 |

The listener annotations hardcode consumer group `inventory-service`, overriding the configured `inventory-service-group` value for these listeners.

Transfer shipped/completed events are processed asynchronously. Shipping sends `RemoveBookStockCommand` to the source library; completion sends `AddBookStockCommand` to the destination library.

## 3.2 Exposed Inventory API

### Libraries

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/libraries/create` | Create library |
| `PUT` | `/api/libraries/update` | Update library |
| `DELETE` | `/api/libraries/delete` | Set `deleted=true` |
| `GET` | `/api/libraries/all` | List libraries |
| `GET` | `/api/libraries/{id}` | Find library by ID |

### Stock

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/stock` | Add physical stock |
| `DELETE` | `/api/stock` | Remove physical stock |
| `GET` | `/api/stock/{libraryId}` | List a library's stock |
| `GET` | `/api/stock/{libraryId}/{bookId}` | Find one library/book stock tuple |
| `GET` | `/api/stock/book/{bookId}` | List stock across libraries |

### Transfers

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/transfers/request` | Create a requested transfer |
| `POST` | `/api/transfers/accept` | `REQUESTED -> ACCEPTED` |
| `POST` | `/api/transfers/reject` | `REQUESTED -> REJECTED` |
| `POST` | `/api/transfers/cancel` | `REQUESTED/ACCEPTED -> CANCELLED` |
| `POST` | `/api/transfers/ship` | `ACCEPTED -> SHIPPED` |
| `POST` | `/api/transfers/complete` | `SHIPPED -> COMPLETED` |

No endpoint exists to retrieve a transfer or list transfers/statuses.

## 3.3 Inventory behavior that worked

The following behavior worked in isolation:

- Library create, update, list, and get-by-ID.
- Stock collection and tuple queries.
- Positive-quantity checks execute before stock command event creation.
- Removing absent or insufficient available stock is rejected internally.
- Return prevents returning more than total stock before event application.
- Mark-lost requires borrowed copies before event application.
- Nominal transfer state restrictions are enforced inside the `Transfer` aggregate.
- Requested transfers could be accepted or rejected.
- Requested/accepted transfers could be cancelled.
- Only accepted transfers could be shipped.
- Only shipped transfers could be completed.
- Repeated and invalid transitions failed.
- Kafka consumers received all three loan topics and ignored unknown JSON fields.
- Consumer offsets persisted across restart.
- Database state persisted across restart.
- `400`, `404`, `405`, and `415` were correctly returned for basic HTTP binding/routing failures.

These checks only show that the individual guards execute. Several invariants are still broken by the event application and asynchronous transfer design described below.

## 3.4 Inventory findings and required fixes

### INV-01 — Critical: all stock-changing events mutate stock twice

Observed:

| Action | Requested quantity | Actual change |
|---|---:|---:|
| Add stock | 5 | +10 |
| Add more stock | 2 | +4 |
| Remove stock | 2 | -4 |
| `loan.created` | 1 | available -2 |
| `loan.returned` | 1 | available +2 |
| `loan.marked.lost` | 1 | total -2 |
| Transfer ship | 2 | source total/available -4 |
| Transfer complete | 2 | destination total/available +4 |

Cause:

Every command handler first calls its event-sourcing handler directly and then calls `AggregateLifecycle.apply(event)`. Axon invokes the event-sourcing handler again during `apply`.

Required immediate fix:

1. Remove all `this.on(event)` calls from `Library` command handlers.
2. Keep only `AggregateLifecycle.apply(event)`.
3. Apply the same correction to `Transfer` and Catalog's `Book` aggregate for consistency and safety.
4. Add aggregate fixture tests for every command, verifying:
   - exactly one event;
   - exact expected state change;
   - invariant preservation after the change.
5. Repair existing corrupted stock rows before using the system with real data. Existing totals cannot safely be divided by two because some rows have experienced different mixtures of operations; reconstruct them from trusted stock history or perform a physical reconciliation.

### INV-02 — Critical: stock can become negative or overflow despite command guards

Observed:

- A stock row reported total/available `2`. Removing quantity `2` passed the guard, but double application produced total/available `-2`.
- Adding `2147483647` passed validation and double application overflowed both integer fields to `-2`.

Cause:

- Guards validate the requested change once, while state changes twice.
- Quantities use signed 32-bit `Int` without checked arithmetic or an upper domain limit.
- The database has no nonnegative check constraints.

Required fix:

1. Fix double application first.
2. Use checked arithmetic (`Math.addExact`/`subtractExact`) or a bounded quantity value object.
3. Define a realistic maximum stock quantity.
4. Enforce aggregate invariants after computing the proposed new values:
   - `totalQuantity >= 0`;
   - `availableQuantity >= 0`;
   - `availableQuantity <= totalQuantity`.
5. Add matching database check constraints.
6. Reject overflow/out-of-range requests with `400` or `422`.

### INV-03 — Critical: transfer state succeeds independently of stock movement

Observed happy-path timing:

1. Transfer quantity `2` was accepted and shipped with `200`.
2. An immediate stock read still showed unchanged source/destination stock.
3. A later read showed that the asynchronous handlers had moved `4` copies.

Observed failure path:

1. The source had no stock for a valid Catalog book.
2. Request returned `200`.
3. Accept returned `200`.
4. Ship returned `200`; asynchronous source removal then failed.
5. Complete still returned `200`.
6. The destination later contained `6` copies for the requested quantity `3`.
7. MySQL recorded the transfer as `COMPLETED`.

This creates inventory from nothing and records a false completed transfer.

Required fix:

1. Treat a transfer as a multi-aggregate process, not two uncoordinated follow-up listeners.
2. At request/accept time, validate both libraries, the Catalog book, and source stock.
3. Reserve source copies before a transfer can be accepted or shipped.
4. Introduce explicit intermediate/failure states such as `RESERVED`, `SHIPPING`, `RECEIVING`, and `FAILED`.
5. Use an Axon saga/process manager or another durable workflow:
   - send source reserve/remove command;
   - wait for success/failure event;
   - only then mark shipped;
   - send destination receive command;
   - only then mark completed;
   - compensate/release reservations on failure or cancellation.
6. Make every workflow command/event idempotent.
7. Return `202 Accepted` plus a status URL if completion is asynchronous.
8. Never report `SHIPPED` or `COMPLETED` before the corresponding stock action succeeds.

### INV-04 — Critical: transfers accept nonexistent libraries and unavailable books

Observed:

- A transfer between two nonexistent library IDs was requested, accepted, shipped, completed, and stored as `COMPLETED`.
- Source and destination stock handlers failed asynchronously.
- A transfer for an unstocked book was also accepted and completed.

Cause:

- `RequestTransferCommand` validates only `quantity > 0` and source != destination.
- Transfer stores raw references without verifying the related aggregates or Catalog.

Required fix:

- Validate source library, destination library, active status, Catalog book existence, and source stock/reservability before creating or accepting the transfer.
- Enforce these checks again when acting, because state may change after request.
- Emit a rejected/failed event rather than allowing an impossible transfer to progress.

### INV-05 — High: Inventory does not validate book IDs against Catalog

Observed:

- Adding stock for `ghost-book-not-in-catalog` returned `200` and created two copies.
- Blank book IDs were accepted and stored.
- Transfer requests can contain arbitrary book IDs.

Required fix:

1. Use a validated `BookId` value object rather than an unrestricted string.
2. Reject blank/malformed identifiers.
3. Validate that the book exists and is active in Catalog before initial stock creation and transfer request.
4. For service independence, maintain an Inventory-side book-reference projection from Catalog events rather than requiring a synchronous Catalog call for every stock mutation.
5. Still handle races where a book is deleted after validation.

### INV-06 — High: Catalog deletion is not consumed by Inventory

Observed:

1. Inventory contained six copies of a Catalog book.
2. Catalog deleted the book and published `book.deleted`.
3. Inventory's consumer group had assignments only for the three loan topics.
4. Inventory continued returning all six copies.

Required fix:

1. Add a versioned `book.deleted` consumer.
2. Decide what deletion means when stock or active loans exist:
   - mark the title unavailable/discontinued while preserving historical stock;
   - reject Catalog deletion until dependencies are resolved; or
   - run a coordinated retirement workflow.
3. Do not blindly delete stock that may be associated with active loans.
4. Add a local book-reference status (`ACTIVE`, `RETIRED`, etc.) and exclude retired books from new stock/loan/transfer operations.
5. Make handling idempotent by external `eventId`.

### INV-07 — High: soft-deleted libraries remain visible and mutable

Observed:

- Deleted libraries remain in list and get-by-ID responses.
- Repeated deletion succeeds.
- Updating a deleted library succeeds.
- Adding and reading stock on a deleted library succeeds.
- Transfers do not check deleted state.

Required fix:

1. Add `deleted=false` to ordinary read queries.
2. Reject every mutating library/stock/transfer command targeting a deleted library.
3. Reject repeated delete.
4. Define a safe closure workflow that requires loans/transfers/stock to be resolved first.
5. Add separate administrative endpoints if archived library visibility is needed.

### INV-08 — High: Kafka messages have no idempotency mechanism

Observed:

- Publishing identical `loan.created` payloads twice applied the stock change twice.
- Payload DTOs include only `bookId` and `libraryId`; no loan ID or event ID is retained.

Required fix:

1. Require `eventId`, `loanId`, event version, and occurrence timestamp.
2. Store processed event IDs in an inbox table with a unique constraint.
3. Treat redelivery as success without repeating the state change.
4. Partition loan events consistently by loan or library ID to preserve required ordering.

### INV-09 — High: invalid Kafka records are retried and then silently skipped

Observed:

- An invalid `loan.returned` event was retried ten times.
- The default error handler reported exhausted retries.
- The consumer offset then advanced to the log end with zero lag.
- There was no dead-letter topic.
- The actuator remained `UP` throughout.

Required fix:

1. Configure a `DefaultErrorHandler` explicitly.
2. Distinguish non-retryable schema/domain failures from transient infrastructure failures.
3. Publish exhausted records to a dead-letter topic with original topic, partition, offset, exception, and correlation metadata.
4. Add alerts and metrics for retry and dead-letter counts.
5. Expose degraded readiness when consumers are unable to process required records.
6. Provide an audited replay/recovery procedure.

### INV-10 — Medium: configured Kafka group ID is not the group actually used

Observed:

- Configuration specifies `spring.kafka.consumer.group-id=inventory-service-group`.
- Each `@KafkaListener` hardcodes `groupId="inventory-service"`.
- Kafka confirmed the active group is `inventory-service`.

Required fix:

- Remove the hardcoded annotation values and use the configured group ID, or use one shared property placeholder consistently.
- Document the group name because changing it causes replay behavior under `auto-offset-reset=earliest`.

### INV-11 — High: `LibraryUpdatedEvent` contains the wrong library ID

Observed source defect:

```kotlin
constructor(command: UpdateLibraryCommand) : this(
    id = LibraryId(),
    name = command.name,
    address = command.address
)
```

The event payload generates a new random ID rather than using `command.id`.

Impact:

- The Axon event envelope is associated with the target aggregate, but the event payload claims a different identifier.
- Projections or integrations that trust the payload can update the wrong/nonexistent library.

Required fix:

- Replace `LibraryId()` with `command.id`.
- Add an aggregate test asserting event ID equals command target ID.
- Audit already stored update events before replaying projections.

### INV-12 — High: `TransferId.equals` casts to the wrong class

Observed source defect:

```kotlin
return this.value == (other as LibraryId).value
```

After confirming the other object has `TransferId`'s class, the method casts it to `LibraryId`, which can throw `ClassCastException` when comparing two distinct `TransferId` instances.

Required fix:

- Cast to `TransferId`, not `LibraryId`.
- Prefer a common tested identifier implementation to eliminate copy/paste errors.
- Add equality contract tests for same value, different value, null, other type, symmetry, and hash consistency.

### INV-13 — High: timestamps are generated inside event-sourcing handlers

Observed:

- `requestedAt`, `reviewedAt`, and `completedAt` are set with `LocalDateTime.now()` in `@EventSourcingHandler` methods rather than carried by the events.

Impact:

- Event replay can produce different timestamps each time.
- Aggregate state is nondeterministic and cannot be reliably reconstructed from its events.
- Time zone/clock behavior cannot be controlled in tests.

Required fix:

1. Generate timestamps before event publication using an injected `Clock`.
2. Put the timestamp in each transfer event.
3. Have event-sourcing handlers assign the timestamp from the event only.
4. Prefer `Instant` or an offset-aware type for persisted integration timestamps.

### INV-14 — High: expected domain failures become generic `500`

Observed `500` cases included:

- Zero/negative stock quantity.
- Insufficient available copies.
- Missing book stock.
- Missing library aggregate.
- Missing transfer aggregate.
- Invalid transfer state transition.
- Same source and destination.
- Zero/negative transfer quantity.

Required fix:

- Add `@RestControllerAdvice` and stable problem responses.
- Map validation to `400`/`422`, missing resources to `404`, and illegal state transitions/conflicts to `409`.
- Do not expose generic servlet errors for normal business decisions.

### INV-15 — High: library and stock fields lack basic validation

Observed:

- Empty library name and address were accepted.
- Duplicate name/address combinations were accepted.
- Blank book IDs were accepted.
- Very large integer quantities reached domain arithmetic.
- Empty `requestedBy` and `reviewedBy` were accepted.

Required fix:

1. Add Bean Validation and domain value objects.
2. Enforce nonblank names, addresses, book IDs, requester, and reviewer.
3. Define whether library name/address must be unique and add the correct constraint.
4. Bound all quantities.
5. Add database constraints matching aggregate rules.

### INV-16 — Medium: no transfer read/status API exists

Observed:

- OpenAPI exposes six transfer commands but no get/list endpoint.
- Database inspection was required to confirm `REJECTED`, `CANCELLED`, and `COMPLETED` states.

Required fix:

- Add a transfer read model and endpoints for get-by-ID and filtered listing.
- Include status, source/destination, title ID, quantity, requester/reviewer, timestamps, failure reason, and workflow correlation identifiers.
- This is required if transfer commands become asynchronous and return `202`.

### INV-17 — Medium: response contracts do not match command service types

Observed:

- Create returns a serialized identifier with internal `entityClass`.
- Update, delete, add/remove stock, and transfer transitions generally return `200` with empty bodies.
- Service methods are typed as futures of identifiers although non-create handlers return no identifier.

Required fix:

- Use explicit public response DTOs.
- Correct command gateway result types.
- Use `201` for creation, `202` for asynchronous workflows, and `204` for synchronous success without a body.

### INV-18 — Medium: stock persistence has two ownership representations

Observed design:

- `Library` owns a `@OneToMany` stock list without `mappedBy`, producing a `library_stock` join table.
- `BookStock` separately stores a raw `libraryId` column.

Impact:

- Library ownership is represented both by the join table and the `BookStock.library_id` string.
- These can diverge if either is written independently.

Required fix:

- Model one source of truth:
  - use `BookStock.library: Library` with `@ManyToOne` and `Library.stock(mappedBy="library")`; or
  - model stock as an embeddable/value collection owned solely by `Library` if independent querying requirements permit it.
- Retain the unique `(library_id, book_id)` database constraint.

### INV-19 — Medium: asynchronous side effects are reported as synchronous success

Observed:

- `ship` and `complete` returned `200` before their stock effects appeared.
- Later handler failures did not alter the already successful response.

Required fix:

- Align HTTP semantics with processing semantics.
- If work is asynchronous, return `202` and expose operation/transfer status.
- If the endpoint promises synchronous success, wait for and atomically verify the stock result before returning.

### INV-20 — Medium: internal Kafka fallback address is wrong for this Docker topology

Observed:

- `inventory-service/src/main/resources/application.properties` defaults to `broker:9092`.
- The Docker broker advertises its internal listener at `broker:29092`; port `9092` advertises `localhost:9092` for host clients.
- The service worked only because the newly created `.env` overrides the property with `broker:29092`.

Required fix:

- Change the Docker/default service setting to `broker:29092`.
- Use profiles for local JVM execution versus container execution.
- Commit a safe `.env.example`; do not commit real credentials.

### INV-21 — Low: identifier request representation is unnecessarily verbose

Observed:

- Stock and transfer request bodies require nested identifiers such as:

```json
{
  "libraryId": { "value": "..." },
  "id": { "value": "..." }
}
```

- A scalar string identifier produces `400`.

Required fix:

- Use API DTOs with scalar string IDs and translate them to domain value objects in the application layer.
- Keep persistence/framework metadata out of the public schema.

### INV-22 — Low: aggregate creation routing annotations are misleading

Observed:

- Creation commands mark fields such as library name/source library ID as `@TargetAggregateIdentifier`, even though the aggregate generates a new UUID in its creation event.

Required fix:

- Remove target aggregate annotations from creation commands unless Axon's creation routing genuinely requires them.
- Put the generated aggregate ID in the command when deterministic/request-level idempotent creation is desired.

### INV-23 — Security requirement is absent

Observed:

- Library deletion, stock mutation, transfer review, shipment, and completion required no authentication or authorization.
- `requestedBy` and `reviewedBy` are trusted arbitrary client strings.

Required fix if the system is not intentionally public:

- Authenticate callers.
- Derive requester/reviewer identity from the authenticated principal rather than request JSON.
- Authorize library staff by location and operation.
- Audit all stock and transfer mutations.

### INV-24 — Deployment robustness gaps

Observed:

- The required `.env` file was absent.
- The application default Kafka listener address is unsuitable for Docker.
- The compose service has no application health check or restart policy.
- It depends only on MySQL, not shared Kafka/Consul readiness.

Required fix:

1. Add `.env.example` with documented local defaults.
2. Add an actuator Docker health check and restart policy.
3. Add a documented root-infrastructure startup step or a unified compose deployment.
4. Add bounded retry/backoff for Consul/Kafka availability.
5. Use managed secrets for non-development environments.

---

# 4. Cross-service findings

## 4.1 Catalog-to-Inventory book lifecycle is incomplete

Catalog is the source of truth for books, while Inventory stores only arbitrary string book IDs. The only Catalog external event is deletion, and Inventory does not consume it. Catalog create/update events do not leave Catalog at all.

Required target design:

1. Publish versioned `book.created`, `book.updated`, and `book.deleted` or `book.retired` external events through an outbox.
2. Maintain a small Inventory book-reference projection containing at least ID and lifecycle status.
3. Reject new stock, loans, and transfers for unknown/retired books.
4. Do not delete historical references needed by loans/transfers.
5. Use event IDs and an inbox for deduplication.

## 4.2 Event contracts need a common envelope

Current events lack consistent correlation and idempotency information.

A common external envelope should contain:

```json
{
  "eventId": "uuid",
  "eventType": "book.deleted",
  "eventVersion": 1,
  "aggregateId": "uuid",
  "aggregateVersion": 7,
  "occurredAt": "2026-09-11T18:00:00Z",
  "correlationId": "uuid",
  "causationId": "uuid",
  "payload": {}
}
```

Schemas should be versioned and validated by producer/consumer contract tests.

## 4.3 Service health is too shallow

Both services reported `UP` while asynchronous business processing could fail or discard messages.

Required fix:

- Separate liveness from readiness.
- Include database, Consul, Kafka connectivity, consumer assignment, and event-publication backlog in readiness/metrics.
- Do not make transient downstream failure kill liveness, but do expose degraded readiness and alerting.

---

# 5. Required fix order

## Priority 0 — must be fixed before trustworthy inventory use

1. Remove manual `this.on(event)` calls and stop double stock mutation.
2. Reconcile already corrupted stock data.
3. Redesign transfer stock coordination so a transfer cannot ship/complete after a stock failure.
4. Add nonnegative/overflow-safe stock invariants and database constraints.
5. Block nonexistent/deleted libraries and nonexistent/deleted books.

## Priority 1 — required for reliable integration

1. Add Catalog lifecycle events and Inventory book-reference handling.
2. Fix Catalog Kafka double encoding and AsyncAPI mismatch.
3. Add outbox/inbox idempotency and dead-letter handling.
4. Fix `LibraryUpdatedEvent` ID and `TransferId.equals`.
5. Move timestamps into transfer events.
6. Enforce soft deletion in reads and mutations.
7. Add domain-aware REST exception handling.

## Priority 2 — API and data quality

1. Add complete DTO/domain/database validation.
2. Enforce ISBN and agreed library uniqueness.
3. Correct HTTP statuses and public response DTOs.
4. Remove or implement `publisher` and audit fields.
5. Replace embedded entity/identifier request shapes with scalar IDs.
6. Add transfer query/status endpoints.
7. Consolidate the stock persistence relationship.

## Priority 3 — operations and security

1. Add authentication, authorization, and trusted audit identity.
2. Add Docker health checks, restart policies, startup documentation, and `.env.example` files.
3. Add Kafka/Consul readiness, retry, metrics, and alerting.
4. Keep secrets out of source-controlled environment files.

---

# 6. Test evidence retained in the environment

The service databases intentionally retain test evidence. Examples include:

- Duplicate Catalog ISBNs.
- Blank Catalog fields and a negative publication year.
- Soft-deleted books that remain readable/mutable.
- Duplicate and blank Inventory library records.
- A deleted library that was updated and received stock.
- Stock rows with negative quantities caused by double application and integer overflow.
- Completed transfers whose source removal failed.
- Destination stock created without source stock.
- Axon domain event and token records.
- Three `book.deleted` Kafka records, including a repeated-delete event.

These records should not be treated as valid seed data. After fixes are implemented, use new disposable databases for regression testing or explicitly remove/rebuild the test volumes after preserving any evidence that is still needed.

## Final assessment

Catalog's core CRUD and search behavior is operational, but lifecycle handling, validation, error contracts, and Kafka publication require correction.

Inventory starts and exposes all intended commands, but its stock arithmetic and transfer workflow are not currently safe. The double event application and uncoordinated asynchronous transfer side effects can produce negative quantities, overflowed quantities, doubled movements, and stock created from nothing. Those two issues are release-blocking and should be resolved before further feature work or production-like testing.
