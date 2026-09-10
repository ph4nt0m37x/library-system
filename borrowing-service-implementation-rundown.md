# Borrowing Service - Current Implementation Rundown

_Snapshot taken from the repository on 2026-09-10._

## Overall shape

The service is a Kotlin/Spring Boot application organized around Axon commands, events, and JPA-backed aggregates. Axon Server is disabled; commands and events are handled in-process and aggregate state is stored in MySQL. The main domain areas are loans, fees, payments, and member borrowing bans.

## Implemented domain workflows

### Loans

- Create a loan with a configurable due period (currently 14 days).
- Before creation, verify that the member:
  - has an active subscription through `membership-service`;
  - has fewer than 5 active loans;
  - has fewer than 3 unpaid fees;
  - has no active temporary ban or permanent ban.
- Loan creation checks the configured active-loan limit (currently 5) without a per-member database lock.
- Extend an active loan once, before its due date, by a configurable period (currently 14 days).
- Return an active loan, declare it lost, or record permanent damage; each action transitions the loan to its corresponding terminal status.

### Fees and fee calculation

- Automatically create one fee per loan when:
  - a book is returned late (`OVERDUE`);
  - a book is declared lost (`LOST`);
  - permanent damage is recorded (`DAMAGED`).
- Fee IDs are deterministic from the loan ID, and duplicate/conflicting fee creation is guarded.
- Overdue amounts use a configurable daily rate (currently USD 0.50).
- Once lateness reaches the configured threshold (currently 30 days), the overdue charge becomes a replacement-price charge.
- Lost and damaged book fees also use the replacement price supplied by `inventory-service`, multiplied by a configurable factor (currently 1.00).
- Supports two billable-day policies: started calendar days in a configured timezone, or started 24-hour periods.

### Payments

- Record a payment against one or more selected unpaid fees.
- Quote and validate the entire payment before recording it: fee ownership, status, currency, exact total, and duplicate fee selection are checked.
- Generate deterministic payment-allocation IDs and persist individual allocations.
- Settle all allocated fees through follow-up Axon commands.
- Repeating the same payment ID is treated idempotently only when all material payment fields and selected fees match.
- The current model settles whole selected fees; arbitrary partial payment amounts are not supported.

### Borrowing bans

- Count cumulative permanent-damage fees for each member.
- Issue progressively stronger bans at configured damage counts:
  - tier 1 at 10 incidents for 1 month;
  - tier 2 at 20 incidents for 3 months;
  - permanent at 30 incidents.
- Enforce tier order, deterministic IDs, and one issuance per tier.
- Loan eligibility checks both active temporary bans and permanent bans.
- If a new threshold is reached while a temporary ban is active, the next tier is deliberately not issued. The code logs this as an unresolved business decision; there is no deferred activation or scheduler.

## Persistence and integration

- JPA aggregate tables exist for loans, fees, payments, payment allocations, ban records, and ban periods.
- Tracking projection handlers maintain separate MySQL read tables for loans, fees, payments and allocations, and borrowing-ban records and periods. Every borrowing domain event has both an aggregate event-sourcing handler and a projection event handler.
- Axon `GenericJpaRepository` beans are configured for the four aggregates.
- Feign clients are declared for membership subscription status and inventory book price.
- MySQL, Consul discovery, Actuator health, OpenFeign circuit breaking, Kafka properties, Springwolf, and OpenAPI dependencies/configuration are present.
- A multi-stage Dockerfile and a service-level Compose file are present. The Compose file starts MySQL and the borrowing application, while Kafka, Consul, and the external shared network are expected to exist elsewhere.

## REST API

All borrowing endpoints use the `/api/*` prefix and follow the membership service's controller, command-service, and projection-read-service structure.

- Loans (`/api/loans`): list all, get by ID, list all or active loans by member, create, extend, return, declare lost, and record permanent damage.
- Fees (`/api/fees`): list all, get by ID, and list all or unpaid fees by member.
- Payments (`/api/payments`): quote selected fees, record the quoted payment, list all, get by ID, and list by member.
- Borrowing bans (`/api/borrowing-bans`): list all, get by record ID, and get by member.
- Fee creation and settlement and borrowing-ban issuance remain internal policy-driven operations rather than public mutation endpoints.

## What is not currently wired

- Kafka and Springwolf are configured as dependencies/infrastructure, but no borrowing event publishing or consumption code is implemented.
- The service checks book price for replacement fees, but loan creation does not check or update book availability in inventory.
- No fallback implementations are defined for either Feign client.

## Current build and test status

- The main borrowing-service sources compile successfully with tests skipped.
- The application requires explicit values for `BORROWING_FEES_BILLABLE_TIME_ZONE` and `BORROWING_FEES_BILLABLE_DAY_RULE`; their checked-in defaults are blank and configuration validation rejects blanks at startup.
- The active-loan boundary has focused unit coverage. There are no integration tests for event-driven fee/payment behavior, external clients, or ban escalation.
- The Docker build skips test execution.

## Bottom line

Most of the borrowing domain model and in-process command/event workflows have been drafted, including meaningful eligibility, pricing, idempotency, escalation rules, and a REST API. The main sources compile, but startup still requires the explicit billable-time configuration, and Kafka integration is not yet implemented.
