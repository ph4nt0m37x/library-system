# Membership Service Implementation Roadmap

## Goal

Build the Membership Service using the same overall CQRS, Axon, JPA, REST, MySQL, Kafka/AsyncAPI, Consul, and Docker approach used by the Catalog Service, adapted to the `Member` aggregate and its owned `SubscriptionPeriod` entities.

This document is a plan only. Items below should be implemented one phase at a time after their domain details are confirmed.

## Current state

Already present in the Membership Service:

- Spring Boot/Kotlin application and Maven configuration.
- Dockerfile, Docker Compose, `.env`, and `.env.example`.
- Source package folder structure based on the Catalog Service.
- `Member` aggregate with member profile fields and an owned subscription list.
- `SubscriptionPeriod` child entity with payment and date fields.
- `Tier` enum with `THREE_MONTHS(3)`, `SIX_MONTHS(6)`, and `TWELVE_MONTHS(12)`.

## Catalog Service findings

The Catalog Service implements the following vertical path:

1. Typed aggregate identifier and shared entity abstractions.
2. Create, update, and delete commands.
3. Domain events corresponding to those commands.
4. Command handlers and event-sourcing handlers inside the aggregate itself.
5. A custom Axon `GenericJpaRepository` bean for the aggregate.
6. Spring Data JPA repositories for the aggregate, related entities, and read view.
7. A separate JPA read-view type and read service.
8. A command service backed by Axon's `CommandGateway`.
9. A REST controller that converts DTOs into commands.
10. MySQL, Consul, Kafka/Springwolf configuration and Docker support.

Important limitations in the Catalog reference:

- The `handlers` packages are empty; handlers are located in the aggregate.
- Kafka and AsyncAPI dependencies/configuration exist, but no explicit Kafka event publisher is implemented.
- The read view maps directly to the aggregate's table; there is no event-driven projection handler.
- Most tests consist only of a Spring context-load test.
- Some Catalog DTO fields and imports are unused, so code should be adapted deliberately rather than copied blindly.

## Phase 1: Confirm domain behavior

Before creating commands and events, decide the rules they must enforce:

- How `memberId` is generated and whether it should use a Catalog-style typed `MemberId`.
- How `subscriptionId` is generated and whether it should use a typed `SubscriptionId`.
- How `membershipNumber` is generated, formatted, and guaranteed unique.
- Whether email must be unique and whether email and phone require normalization or validation.
- Whether members can be updated, deactivated, or deleted, and whether deletion is soft or physical.
- Whether a member can have overlapping subscription periods.
- Whether future-dated subscriptions are allowed.
- Whether `endsAt` is supplied or calculated from `startsAt` plus `tier.months`.
- Whether `createdAt`, `registeredAt`, and `paidAt` are supplied by clients or assigned by the service.
- Whether `paidAt` may be null for unpaid or pending subscriptions.
- Allowed currencies and whether `currency` must be an ISO 4217 three-letter code.
- Whether `amountPaid` must be positive and how scale/rounding are handled.
- Whether subscription correction, cancellation, or refund operations are required.

## Phase 2: Align the domain foundation with Catalog

Implement the reusable foundation needed by commands, events, repositories, and views:

- `model/common/Identifier.kt`, adapted from Catalog.
- `model/common/LabeledEntity.kt`, adapted for member labels and creation dates.
- `model/valueObject/MemberId.kt` as an embeddable typed identifier, if typed IDs are selected.
- `model/valueObject/SubscriptionId.kt`, if subscriptions also use typed IDs.

Then revise the existing domain mappings:

- Change `Member.memberId` and `SubscriptionPeriod.memberId` to the decided identifier representation.
- Give the aggregate the custom Axon repository bean name once that repository exists.
- Confirm that `subscriptions` is both an Axon-owned member collection and a correct JPA one-to-many relationship.
- Confirm cascade, orphan-removal, fetch strategy, and ownership of the `member_id` column.
- Keep the tier persisted as `EnumType.STRING`; the integer `months` value is behavior, not the database ordinal.
- Add aggregate methods that preserve invariants instead of allowing unrestricted external mutation.
- Decide whether direct JPA persistence of the aggregate and Axon state changes share one model, as in Catalog.

## Phase 3: Define the command model

Create commands only for approved use cases. The likely baseline is:

- `RegisterMemberCommand`
  - Profile data required to register a member.
  - No target aggregate identifier because it creates the aggregate.
- `UpdateMemberCommand` or a narrower `UpdateMemberContactDetailsCommand`
  - `@TargetAggregateIdentifier memberId`.
  - Only fields the business permits changing.
- `AddSubscriptionPeriodCommand` or `RenewMembershipCommand`
  - `@TargetAggregateIdentifier memberId`.
  - Tier, start/payment data, and any other confirmed inputs.
- Optional `DeactivateMemberCommand` or `DeleteMemberCommand`
  - Add only if member lifecycle requirements call for it.
- Optional subscription correction/cancellation/refund commands
  - Add only after those behaviors are defined.

Each command should carry intent and client-supplied facts only. Generated identifiers, calculated end dates, and server timestamps should be produced in the domain layer.

## Phase 4: Define domain events

Adapt Catalog's event hierarchy:

- `AbstractEvent` for event type/topic metadata.
- `MemberEvent` as the base event keyed by `MemberId`.
- `MemberRegisteredEvent`.
- `MemberUpdatedEvent` or a narrower profile/contact event.
- `SubscriptionPeriodAddedEvent` or `MembershipRenewedEvent`.
- Optional member deactivation/deletion event.
- Optional subscription correction/cancellation/refund events.

Events must include all state required to rebuild the aggregate. Subscription events therefore need the generated subscription ID, member ID, tier, calculated dates, payment amount, currency, and payment timestamp.

## Phase 5: Implement aggregate behavior

Following the Catalog style, place command handlers and event-sourcing handlers in `Member` unless we intentionally choose separate handler classes.

Required work:

- Add a command-handler constructor for member registration.
- Add command handlers for each approved update and subscription operation.
- Validate business invariants before applying events.
- Apply one event per accepted state transition, or a deliberate sequence when needed.
- Add event-sourcing handlers that are the single source of state mutation.
- Create and append `SubscriptionPeriod` children when subscription events are applied.
- Ensure child identity is marked for Axon command routing when child-targeted commands are introduced.
- Add member label, creation date, and lifecycle state methods if `LabeledEntity` is used.
- Avoid manually invoking an event-sourcing handler before `AggregateLifecycle.apply`; Axon invokes the handler when the event is applied.

## Phase 6: Add persistence and Axon repositories

Create repository configuration equivalent to Catalog:

- A Spring configuration class that exposes `axonMemberRepository` using `GenericJpaRepository<Member>`.
- An identifier converter that converts command-bus string identifiers into `MemberId`.
- `MemberRepository : JpaRepository<Member, MemberId>`.
- A repository for `SubscriptionPeriod` only if subscriptions need independent reads or maintenance.
- A repository for the read model introduced in the next phase.

Persistence checks:

- Verify table and column names for `members` and `subscription_periods`.
- Add unique constraints for membership number and any confirmed unique contact fields.
- Verify `ZonedDateTime`, enum, decimal precision/scale, and child foreign-key mappings with MySQL.
- Decide whether Hibernate `ddl-auto=update` remains acceptable or whether schema migrations are needed.

## Phase 7: Build the read model

Catalog separates command and read types even though both map to the same table. For Membership, decide between:

### Minimal Catalog-parity approach

- Create `MemberView` as a read-only JPA representation.
- Include member details, lifecycle state, and the subscription history required by API responses.
- Map the view to the same tables as the aggregate.
- Create `MemberViewRepository`.

### Full projection approach

- Create dedicated member and subscription projection tables.
- Add event handlers that update those projections from member events.
- Keep read concerns independent from aggregate persistence.

Whichever approach is selected, define common queries:

- Find all members.
- Find a member by ID.
- Find by membership number.
- Optionally find by email.
- Return subscription history.
- Derive the current/active subscription from `startsAt` and `endsAt`.
- Optionally list active, expired, or expiring memberships.

## Phase 8: Implement services

Create interfaces and implementations matching the Catalog layering:

- `MemberService`
  - Sends registration, update, subscription, and lifecycle commands.
  - Returns `CompletableFuture` results from `CommandGateway`.
- `MemberServiceImpl`
  - Implements the command methods with Axon's `CommandGateway`.
- `MemberViewReadService`
  - Defines approved member queries.
- `MemberViewReadServiceImpl`
  - Uses the read repository and provides not-found behavior consistently.

Keep business decisions inside the aggregate rather than duplicating them in controllers or services.

## Phase 9: Define API DTOs and REST endpoints

Create DTOs under `model/valueObject/dto` after commands are finalized:

- `RegisterMemberDTO`.
- `UpdateMemberDTO` or a narrower contact-details DTO.
- `AddSubscriptionPeriodDTO` or `RenewMembershipDTO`.
- Optional deactivation/deletion and subscription-maintenance DTOs.
- Response DTOs if exposing JPA view objects directly is not desired.

Create `MemberRestApi` with Catalog-style OpenAPI annotations. A likely API shape is:

- `GET /api/members/all`
- `GET /api/members/{id}`
- `GET /api/members/membership-number/{membershipNumber}`
- `POST /api/members/register`
- `PUT /api/members/update`
- `POST /api/members/{id}/subscriptions`
- Optional member lifecycle and subscription-maintenance endpoints.

API work must also define:

- HTTP status codes (`201` for registration, `200`/`202` for commands, `404` for missing members).
- Validation errors and malformed ID handling.
- Asynchronous command result serialization; avoid returning a raw future without confirming the desired contract.
- Consistent error response bodies through a controller advice.

## Phase 10: Kafka events and AsyncAPI

The Catalog Service stops at Kafka/Springwolf configuration. Decide whether Membership should only match that level or complete event publication.

If external events are required:

- Define stable external event payloads separate from internal event-sourcing events.
- Implement `toExternalEvent` mappings or an equivalent mapper.
- Publish approved member/subscription events to Kafka.
- Define topic names and keys, preferably keyed by member ID to preserve order per member.
- Add Springwolf operation annotations so AsyncAPI documents actual producers.
- Define serialization, versioning, retry, and failure behavior.
- Consider an outbox approach if database/event delivery consistency is required.

Potential consumers in other services should depend only on external event contracts, not JPA entities.

## Phase 11: Configuration and container alignment

Review the current operational files as part of implementation:

- Change the local default server port in `application.properties` from `8088` to `8089` so it matches the Membership Docker configuration.
- Keep the MySQL container port at `3306` internally and host mapping at `3307:3306`.
- Verify the local datasource URL versus the Docker datasource URL.
- Use environment variables for database credentials and Kafka bootstrap servers.
- Confirm the Membership Kafka consumer group and Springwolf base package.
- Verify Consul registration on port `8089` and the actuator health endpoint.
- Confirm that the external `shared_net` network is created by the root Compose stack.
- Add an application health check to Compose if service-level startup ordering requires it.

## Phase 12: Validation and tests

Add tests beyond Catalog's context-load baseline:

- Aggregate tests with Axon's aggregate test fixture:
  - Member registration.
  - Member update.
  - Subscription addition/renewal.
  - End-date calculation for each tier.
  - Rejection of invalid payment, currency, date, and overlap cases.
- JPA mapping tests for the member/subscription relationship and typed identifiers.
- Repository tests for membership number, email, and active-subscription queries.
- Service tests verifying commands sent through `CommandGateway`.
- MVC/API tests for success, validation failure, not found, and malformed IDs.
- Kafka publication tests if external events are implemented.
- A Docker smoke test covering MySQL startup, application health, and Consul registration.

The Maven wrapper must also be repaired or regenerated before command-line verification; it currently cannot start, and a global Maven executable is not available in the present environment.

## Recommended implementation order

1. Resolve the Phase 1 domain decisions.
2. Implement identifiers and common types.
3. Correct and finalize aggregate/entity persistence mappings.
4. Implement registration end to end: command, event, aggregate handler, repository, service, DTO, API, and tests.
5. Implement member updates end to end.
6. Implement subscription addition/renewal end to end, including tier-based date calculation.
7. Implement the selected read-model approach and query endpoints.
8. Add lifecycle and correction operations only if approved.
9. Complete Kafka publication if required.
10. Align configuration and run unit, persistence, API, and Docker validation.

## Definition of done

The Membership Service is complete when:

- Approved member and subscription commands enforce the agreed domain rules.
- Aggregate state can be rebuilt correctly from its events.
- Member and subscription data persist with valid constraints and mappings.
- Required queries return stable response contracts.
- REST endpoints have validation, status codes, error handling, and OpenAPI documentation.
- Required external events are documented and reliably published, if Kafka publication is in scope.
- The service starts locally and in Docker on port `8089`, connects to MySQL/Kafka/Consul, and reports healthy.
- Automated tests cover the main success paths and domain failures.
