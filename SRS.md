# Software Requirements Specification

## Library Management System

Version 1.0  
Status: As-built baseline  
Date: 13 September 2026  
Format: IEEE 830-inspired

---

## Table of Contents

1. Introduction  
   1.1 Purpose  
   1.2 Scope  
   1.3 Intended Audience  
   1.4 Definitions, Acronyms, and Abbreviations  
   1.5 References  
   1.6 Document Overview  
2. General Description  
   2.1 Product Perspective  
   2.2 Product Functions  
   2.3 User Classes and Characteristics  
   2.4 Operating Environment  
   2.5 Constraints  
   2.6 Assumptions and Dependencies  
   2.7 Out of Scope  
3. Specific Requirements  
   3.1 Requirement Priorities  
   3.2 Functional Requirements  
   3.3 External Interface Requirements  
   3.4 Data Requirements  
   3.5 Non-functional Requirements  
   3.6 Failure and Recovery Requirements  
4. Validation and Acceptance  
   4.1 Validation Approach  
   4.2 Acceptance Criteria  
   4.3 Requirements Traceability  
5. Appendices  
   5.1 Business Rules Summary  
   5.2 Known Limitations and Open Decisions

---

# 1. Introduction

## 1.1 Purpose

This Software Requirements Specification (SRS) defines the functional and non-functional requirements for the Library Management System contained in this repository. It is intended to provide a common, testable description of the product for developers, testers, maintainers, project evaluators, and library stakeholders.

The document describes the current, implemented system baseline. Statements using **shall** are mandatory requirements. Statements using **should** are recommendations. Statements using **may** describe optional behavior. Where the source code contains an unresolved configuration choice or an acknowledged consistency limitation, this document identifies it explicitly.

## 1.2 Scope

The Library Management System is a service-oriented backend for managing:

- a catalog of books and categories;
- library branches and their physical book stock;
- transfers of stock between library branches;
- library members and paid membership subscriptions;
- loans, returns, extensions, lost books, and permanently damaged books;
- overdue, lost-book, and damaged-book fees;
- payments and allocation of payments to fees; and
- temporary and permanent borrowing bans caused by repeated permanent damage.

The system consists of four independently deployable services:

1. **Catalog Service** owns books, categories, catalog availability, and book prices.
2. **Membership Service** owns member profiles, membership numbers, subscriptions, and subscription eligibility.
3. **Inventory Service** owns library branches, per-library book stock, stock transfers, and the inventory effect of loan events.
4. **Borrowing Service** owns loans, fees, payments, and borrowing bans.

The services provide JSON REST APIs. They use synchronous service-to-service HTTP calls for decisions that must be made before accepting a command and Kafka events for asynchronous propagation of catalog retirement and circulation changes. Each service owns a separate MySQL database. Consul provides service discovery. Axon Framework supports command handling, domain events, and read projections.

A React/Vite directory exists in the repository, but it currently contains only starter content. A production library user interface is therefore not part of this SRS baseline.

## 1.3 Intended Audience

- **Library stakeholders** use this document to confirm the supported workflows and business rules.
- **Developers** use it to implement and maintain service behavior and integrations.
- **Testers** use the numbered requirements and acceptance criteria to derive tests.
- **System operators** use the deployment, monitoring, and recovery requirements to operate the services.
- **Project evaluators** use it to compare the intended behavior with the implementation.

Readers are expected to understand basic library operations and HTTP APIs. Knowledge of domain-driven design, CQRS, Axon, Kafka, or container orchestration is helpful but not required.

## 1.4 Definitions, Acronyms, and Abbreviations

- **API** - Application Programming Interface.
- **Aggregate** - A domain object that enforces consistency for a group of related state changes.
- **AsyncAPI** - A machine-readable description of asynchronous messaging interfaces.
- **CQRS** - Command Query Responsibility Segregation; commands change state while queries read projected state.
- **DLT** - Dead-Letter Topic; a Kafka topic containing messages that could not be processed safely.
- **DTO** - Data Transfer Object.
- **Event sourcing** - Recording domain state changes as events and using them to maintain aggregate state and projections.
- **Fee** - A financial obligation caused by an overdue return, loss, or permanent damage.
- **Idempotency** - The property that retrying the same logical operation does not repeat its effect.
- **ISBN** - International Standard Book Number. This system accepts a 13-digit ISBN value.
- **Kafka** - The message broker used for inter-service domain event delivery.
- **Library** - A physical branch that holds stock. This is distinct from the overall Library Management System.
- **Loan** - The circulation record for one book title borrowed by one member from one library.
- **Membership number** - A unique identifier assigned to a registered member.
- **OpenAPI** - A machine-readable description of HTTP APIs.
- **Outbox** - A database-backed queue written in the same transaction as domain state and later published to Kafka.
- **Projection** - A read-optimized representation derived from domain events.
- **REST** - Representational State Transfer.
- **SRS** - Software Requirements Specification.
- **Subscription** - A paid membership period that grants borrowing eligibility while active.
- **Transfer** - Movement of a positive quantity of one book title between two distinct libraries.

## 1.5 References

1. IEEE Std 830-1998, *IEEE Recommended Practice for Software Requirements Specifications*.
2. Repository integration contract, `INTEGRATION.md`.
3. Service source code and configuration under `catalog-service`, `membership-service`, `inventory-service`, and `borrowing-service`.
4. Root and service-level Docker Compose definitions.
5. OpenAPI documents generated by the running services.
6. AsyncAPI documents generated by Springwolf for Kafka-producing services.
7. The supplied `temp/SRS-example.md`, used as the organizational reference for this document.

## 1.6 Document Overview

Section 2 describes the product context, users, environment, constraints, and dependencies. Section 3 contains numbered functional, interface, data, quality, and recovery requirements. Section 4 defines validation and acceptance. Section 5 summarizes business rules and known limitations.

---

# 2. General Description

## 2.1 Product Perspective

The product is a distributed library backend rather than a single monolithic application. Its logical flow is:

```text
API client
   |
   +--> Catalog Service ------> Catalog MySQL
   |
   +--> Membership Service ---> Membership MySQL
   |
   +--> Inventory Service ----> Inventory MySQL
   |          ^
   |          |
   +--> Borrowing Service ----> Borrowing MySQL
              |
              +--> Membership, Inventory, and Catalog via HTTP

Catalog Service ---- book.deleted events -----+
                                                +--> Kafka --> Inventory Service
Borrowing Service -- loan lifecycle events ----+

All services --> Consul registration and health discovery
```

Each service owns its data and must not directly read or write another service's database. The Borrowing Service obtains current membership eligibility, inventory availability, and catalog price through published HTTP contracts. Inventory changes caused by loan transitions are delivered through Kafka.

### 2.1.1 Domain Relationships

- A category may classify zero or more books.
- A book may belong to zero or one category.
- A library holds zero or more book-stock records.
- A book-stock record belongs to one library and references one catalog book.
- A transfer moves a quantity of one catalog book from one source library to one destination library.
- A member has one unique membership number and may have multiple consecutive subscription periods.
- A member may have multiple loans, fees, payments, and one borrowing-ban record.
- A loan references one member, one catalog book, and one lending library.
- A loan may produce at most one fee.
- A payment may settle one or more selected unpaid fees through payment allocations.
- A borrowing-ban record retains the member's current ban and ban history.

## 2.2 Product Functions

The system provides the following major functions:

- create, update, list, retrieve, search, categorize, price, and logically retire books;
- create, update, list, retrieve, and deactivate library branches;
- add and remove physical stock and query stock availability;
- request, accept, reject, cancel, ship, complete, and inspect inter-library transfers;
- register members and update their names and contact details;
- start and renew paid membership subscriptions and report current eligibility;
- create, list, extend, return, and close loans as lost or permanently damaged;
- generate fees based on circulation outcomes;
- quote exact payment amounts and record idempotent payments;
- allocate payments to selected fees and settle those fees;
- escalate borrowing bans at configured permanent-damage thresholds;
- publish and consume versioned integration events reliably; and
- expose health, information, metrics, OpenAPI, and AsyncAPI information where configured.

## 2.3 User Classes and Characteristics

### 2.3.1 Library Member

A library member is the subject of registration, subscription, borrowing, fee, payment, and ban records. A future client may allow members to view these records. The current backend does not authenticate members or distinguish their API permissions.

### 2.3.2 Librarian or Library Administrator

A librarian operates catalog, member, inventory, transfer, circulation, and payment workflows through an API client or a future user interface. The user should understand book identifiers, member identifiers, library identifiers, ISBNs, and basic circulation policies.

### 2.3.3 System Operator

A system operator deploys services, supplies configuration and credentials, observes health and metrics, monitors Kafka and outboxes, diagnoses dependency failures, and reconciles dead-letter records. This user requires experience with Docker, MySQL, Kafka, and Spring Boot services.

### 2.3.4 External Service Client

An external service client invokes published HTTP endpoints or consumes published Kafka records. It must follow JSON schemas, identifier constraints, event versions, Kafka keys, and retry/idempotency rules.

## 2.4 Operating Environment

The supported implementation environment consists of:

- Java 17;
- Kotlin 2.3.x;
- Spring Boot 4.1.x and Spring MVC;
- Axon Framework 4.13 with Axon Server disabled;
- MySQL 8.0, one logical database per service;
- Apache Kafka for integration events;
- Consul 1.18 for service discovery;
- Docker and Docker Compose for the provided deployment definitions; and
- an HTTP client capable of sending and receiving JSON.

Canonical local service ports are:

| Component | Port |
|---|---:|
| Membership Service | 8087 |
| Catalog Service | 8088 |
| Inventory Service | 8089 |
| Borrowing Service | 8090 |
| Kafka client listener | 9092 |
| Kafka UI | 8081 |
| Consul HTTP/UI | 8500 |

## 2.5 Constraints

- **CON-01:** Each service shall own and persist its data in its own MySQL database.
- **CON-02:** Cross-service data access shall occur through published HTTP or Kafka contracts, not shared database tables.
- **CON-03:** JSON shall be the payload format for REST requests, REST responses, and integration events.
- **CON-04:** Service-side time shall be authoritative for normal loan, fee, payment, and renewal processing; deprecated client timestamps shall not override it.
- **CON-05:** Monetary values shall use decimal arithmetic and explicitly configured currency and rounding rules.
- **CON-06:** Kafka integration events shall currently use schema version 1.
- **CON-07:** The development deployment uses a single Kafka broker and is not a high-availability topology.
- **CON-08:** The current codebase does not provide API authentication, authorization, transport-layer configuration, or a production user interface.
- **CON-09:** Hibernate schema mode is configured as `update`; production schema migration and rollback procedures are not supplied by this repository.
- **CON-10:** The Borrowing Service shall not start until a billable time zone and billable-day rule are configured explicitly.

## 2.6 Assumptions and Dependencies

### 2.6.1 Assumptions

- Identifiers supplied by clients refer to records in the appropriate owning service.
- System clocks are synchronized closely enough for subscription, loan, fee, and event timestamps.
- Operators provide valid database credentials and required environment variables.
- Kafka preserves ordering within a partition for records with the same aggregate key.
- Clients tolerate eventual consistency between an accepted borrowing command and the corresponding read projection or inventory update.
- Catalog, membership, and inventory data are administered by trusted callers because access control is not implemented.

### 2.6.2 Runtime Dependencies

- All four services depend on their respective MySQL database.
- All services depend on Consul for the supplied discovery configuration.
- Catalog and Borrowing depend on Kafka to publish external events.
- Inventory depends on Kafka to consume catalog and loan events.
- Borrowing depends synchronously on Membership and Inventory when creating a loan.
- Borrowing depends synchronously on Catalog when a lost or damaged fee, or an escalated overdue fee, requires the current replacement price.
- Inventory depends synchronously on Catalog when validating manual stock additions, transfers, and availability reconciliation.

## 2.7 Out of Scope

The following capabilities are not included in the current product baseline:

- a completed web or mobile user interface;
- user login, identity management, roles, and authorization enforcement;
- reservations, holds, waitlists, and notifications;
- acquisition, supplier, purchase-order, and accounting workflows;
- barcode or RFID device integration;
- full-text search beyond title and author substring queries;
- online payment-provider processing or refunds;
- multi-currency conversion;
- atomic reservation of the final available copy during loan creation; and
- production high availability, backup automation, and disaster-recovery infrastructure.

---

# 3. Specific Requirements

## 3.1 Requirement Priorities

| Priority | Meaning |
|---|---|
| P1 | Essential to the system's core operation or data integrity |
| P2 | Important operational or supporting capability |
| P3 | Desirable enhancement or future capability |

## 3.2 Functional Requirements

### 3.2.1 Catalog Management

- **FR-CAT-001 (P1):** The system shall allow a caller to create a book with ISBN, title, author, positive price, optional description, optional publication year, and optional category.
- **FR-CAT-002 (P1):** The system shall require an ISBN to contain exactly 13 decimal digits.
- **FR-CAT-003 (P1):** The system shall require a non-blank title and author.
- **FR-CAT-004 (P1):** The system shall reject a non-positive book price.
- **FR-CAT-005 (P1):** The system shall reject creation or update when another book uses the same ISBN.
- **FR-CAT-006 (P1):** When a category is supplied for a book, the category identifier shall be positive and shall identify an existing category.
- **FR-CAT-007 (P1):** The system shall allow a caller to update the descriptive, price, and category data of an existing book.
- **FR-CAT-008 (P1):** The system shall logically delete a book rather than physically removing its historical record.
- **FR-CAT-009 (P1):** The system shall reject a repeated deletion of an already deleted book.
- **FR-CAT-010 (P1):** The system shall list all books and separately list books that are not deleted.
- **FR-CAT-011 (P2):** The system shall retrieve a book by identifier and report whether a book is available in the catalog.
- **FR-CAT-012 (P2):** The system shall search books by case-insensitive title substring and author substring. Search results may include logically deleted books; callers that require only active titles shall use the available-books query.
- **FR-CAT-013 (P2):** The system shall filter books by category.
- **FR-CAT-014 (P1):** The system shall return the current amount and configured currency for an existing, non-deleted book.
- **FR-CAT-015 (P1):** Deleting a book shall create a versioned `book.deleted` integration event for Inventory.

### 3.2.2 Category Management

- **FR-CTG-001 (P1):** The system shall create, update, list, and delete book categories.
- **FR-CTG-002 (P1):** A category name shall not be blank.
- **FR-CTG-003 (P1):** Category names shall be unique according to repository matching rules.
- **FR-CTG-004 (P1):** The system shall reject deletion of a category referenced by one or more books.
- **FR-CTG-005 (P1):** Category identifiers accepted from clients shall be positive.

### 3.2.3 Member Management

- **FR-MEM-001 (P1):** The system shall register a member with first name, last name, email address, and phone number.
- **FR-MEM-002 (P1):** First and last names shall be non-blank, trimmed, and no longer than 100 characters each.
- **FR-MEM-003 (P1):** Email addresses shall be normalized to lowercase, shall match a basic local-part/domain format, and shall not exceed 254 characters.
- **FR-MEM-004 (P1):** A member email address shall be unique.
- **FR-MEM-005 (P1):** A phone number shall contain exactly nine digits after whitespace removal and shall begin with `07`.
- **FR-MEM-006 (P2):** The system shall store phone numbers in the normalized form `### ### ###`.
- **FR-MEM-007 (P1):** Registration shall assign a unique member identifier and a unique membership number.
- **FR-MEM-008 (P1):** The system shall record the registration time using the server clock.
- **FR-MEM-009 (P2):** The system shall update a member's name and reject a request that makes no effective change.
- **FR-MEM-010 (P2):** The system shall update a member's email and phone number, preserve email uniqueness, and reject a request that makes no effective change.
- **FR-MEM-011 (P1):** The system shall list all members and retrieve a member by either member identifier or membership number.
- **FR-MEM-012 (P1):** The system shall provide a subscription-eligibility response containing the requested member identifier, existence state, active state, and the current period end time when applicable.

### 3.2.4 Membership Subscription Management

- **FR-SUB-001 (P1):** The system shall support `THREE_MONTHS`, `SIX_MONTHS`, and `TWELVE_MONTHS` subscription tiers.
- **FR-SUB-002 (P1):** The first subscription may be started only when the member has no prior subscription period.
- **FR-SUB-003 (P1):** A subscription may be renewed only after a first subscription has been started.
- **FR-SUB-004 (P1):** The system shall calculate an early renewal from the end of the latest subscription period.
- **FR-SUB-005 (P1):** The system shall calculate an expired subscription renewal from the server's renewal time.
- **FR-SUB-006 (P1):** A subscription end time shall equal its start time plus the configured duration for its tier.
- **FR-SUB-007 (P1):** The amount paid shall exactly equal the configured price for the selected tier.
- **FR-SUB-008 (P1):** The supplied subscription currency shall match the configured membership currency after normalization.
- **FR-SUB-009 (P1):** A payment time shall not be in the future and shall not predate member registration.
- **FR-SUB-010 (P1):** A payment reference shall be non-blank, no longer than 100 characters, and unique across subscription periods.
- **FR-SUB-011 (P1):** Accepted subscription payments shall be stored as settled.
- **FR-SUB-012 (P2):** Tier duration, price, and currency shall be configurable without changing domain source code.

The repository defaults are three months for MKD 900.00, six months for MKD 1500.00, and twelve months for MKD 2400.00.

### 3.2.5 Library and Stock Management

- **FR-INV-001 (P1):** The system shall create a library with a non-blank name of at most 200 characters and a non-blank address of at most 500 characters.
- **FR-INV-002 (P1):** The system shall update an existing library's name and address.
- **FR-INV-003 (P1):** The system shall logically delete a library and distinguish active libraries from deleted libraries.
- **FR-INV-004 (P1):** The system shall list all libraries, list active libraries, and retrieve a library by identifier.
- **FR-INV-005 (P1):** The system shall add a positive quantity of an active catalog book to an active library's stock.
- **FR-INV-006 (P1):** The system shall reject stock addition when Catalog reports the book as retired or when Catalog cannot confirm the book.
- **FR-INV-007 (P1):** The system shall remove stock only when the requested available quantity exists.
- **FR-INV-008 (P1):** Total quantity and available quantity shall never be negative, and available quantity shall never exceed total quantity.
- **FR-INV-009 (P1):** Borrowing a copy shall decrease available quantity without decreasing total quantity.
- **FR-INV-010 (P1):** Returning a copy shall increase available quantity without increasing total quantity.
- **FR-INV-011 (P1):** Marking a borrowed copy lost or permanently damaged shall decrease total quantity and shall not increase available quantity.
- **FR-INV-012 (P1):** The system shall reject a loss or damage operation when there are not enough borrowed copies.
- **FR-INV-013 (P1):** The system shall query stock for one library, one library/book pair, and one book across libraries.
- **FR-INV-014 (P1):** The availability response shall include library-active state, book-active state, available quantity, and a derived `available` decision.
- **FR-INV-015 (P1):** The derived availability decision shall be true only when the library is active, the book is active, and available quantity is greater than zero.
- **FR-INV-016 (P1):** On `book.deleted`, Inventory shall mark the local catalog reference retired so that new stock and loan eligibility fail closed immediately.

### 3.2.6 Inter-library Transfers

- **FR-TRN-001 (P1):** The system shall allow a transfer of a positive stock quantity to be requested between two distinct active libraries.
- **FR-TRN-002 (P1):** A transfer request shall reference an active catalog book and a source library with sufficient available stock.
- **FR-TRN-003 (P1):** A transfer shall begin in `REQUESTED` state.
- **FR-TRN-004 (P1):** Only a requested transfer may be accepted or rejected.
- **FR-TRN-005 (P1):** Accepting a transfer shall remove the requested quantity from the source library's total and available stock while the transfer owns that in-transit quantity.
- **FR-TRN-006 (P1):** Only an accepted transfer may be shipped.
- **FR-TRN-007 (P1):** Shipping shall move an accepted transfer to `SHIPPED`; it shall not apply a second source-stock removal.
- **FR-TRN-008 (P1):** Only a shipped transfer may be completed.
- **FR-TRN-009 (P1):** Completing a transfer shall add the quantity to the destination library.
- **FR-TRN-010 (P1):** Only a requested or accepted transfer may be cancelled.
- **FR-TRN-011 (P1):** Cancelling an accepted transfer shall release its reserved source stock.
- **FR-TRN-012 (P2):** A rejection or cancellation may include a reason of no more than 200 characters.
- **FR-TRN-013 (P2):** The system shall retrieve a transfer by identifier and list transfers optionally filtered by source library, destination library, book, and status.

The permitted state transitions are:

```text
REQUESTED --> ACCEPTED --> SHIPPED --> COMPLETED
    |            |
    |            +------> CANCELLED
    +-------------------> CANCELLED
    +-------------------> REJECTED
```

### 3.2.7 Loan Management

- **FR-LON-001 (P1):** A loan request shall identify one member, one book, and one lending library.
- **FR-LON-002 (P1):** Before creating a loan, Borrowing shall confirm through Membership that the member exists and has an active subscription.
- **FR-LON-003 (P1):** Before creating a loan, Borrowing shall confirm through Inventory that the library and book are active and at least one copy is available.
- **FR-LON-004 (P1):** The system shall reject a loan when the member has reached the configured active-loan limit.
- **FR-LON-005 (P1):** The system shall reject a loan when the member has reached the configured unpaid-fee limit.
- **FR-LON-006 (P1):** The system shall reject a loan while the member has an active temporary ban or a permanent ban.
- **FR-LON-007 (P1):** An accepted loan shall enter `ACTIVE` state and receive a server-generated identifier.
- **FR-LON-008 (P1):** The normal due time shall equal the server borrow time plus the configured regular-loan duration.
- **FR-LON-009 (P1):** Only an active, unextended loan may be extended, and the extension shall be requested before the current due time.
- **FR-LON-010 (P1):** An extension shall add the configured extension duration and may occur no more than once.
- **FR-LON-011 (P1):** Only an active loan may be returned, declared lost, or recorded as permanently damaged.
- **FR-LON-012 (P1):** Returning, losing, or damaging a loan shall place it in `RETURNED`, `LOST`, or `DAMAGED` terminal state respectively.
- **FR-LON-013 (P1):** The server clock shall determine normal borrow, extension, return, loss, and damage times; legacy client timestamp fields shall be ignored.
- **FR-LON-014 (P1):** The system shall list all loans, retrieve a loan by identifier, list a member's loans, and list a member's active loans.
- **FR-LON-015 (P1):** Loan creation shall require an idempotency key that is non-blank after trimming and no longer than 100 characters.
- **FR-LON-016 (P1):** Repeating the same valid loan request with the same idempotency key shall not create a duplicate loan; reuse with different request data shall be rejected.
- **FR-LON-017 (P1):** Loan creation shall publish `loan.created`; terminal transitions shall publish `loan.returned`, `loan.marked.lost`, or `loan.marked.damaged` as appropriate.

The default borrowing policy permits five active loans, permits fewer than three unpaid fees, assigns a 14-day regular period, and assigns one 14-day extension.

### 3.2.8 Fees

- **FR-FEE-001 (P1):** Returning a loan after its due time shall create one overdue fee if the loan does not already have a fee.
- **FR-FEE-002 (P1):** Declaring a loan lost shall create one lost-book fee if the loan does not already have a fee.
- **FR-FEE-003 (P1):** Recording permanent damage shall create one damaged-book fee if the loan does not already have a fee.
- **FR-FEE-004 (P1):** Each fee shall identify its loan, member, currency, reason, state, creation time, and relevant due or incident times.
- **FR-FEE-005 (P1):** A newly created fee shall be `UNPAID`; an allocated successful payment shall change it to `PAID` and record settlement details.
- **FR-FEE-006 (P1):** For an overdue fee below the configured escalation threshold, the amount shall equal billable overdue days multiplied by the configured daily late rate.
- **FR-FEE-007 (P1):** For an overdue fee at or above the escalation threshold, the amount shall equal the current catalog replacement price multiplied by the configured replacement multiplier.
- **FR-FEE-008 (P1):** Lost and damaged fees shall equal the current catalog replacement price multiplied by the configured replacement multiplier.
- **FR-FEE-009 (P1):** Replacement pricing shall fail if the catalog currency differs from the fee currency.
- **FR-FEE-010 (P1):** Money shall be rounded to the configured scale using the configured rounding mode.
- **FR-FEE-011 (P1):** Billable overdue days shall follow the explicitly configured `STARTED_CALENDAR_DAYS` or `STARTED_24_HOUR_PERIODS` rule.
- **FR-FEE-012 (P2):** The system shall list all fees, retrieve a fee by identifier, list a member's fees, and list a member's unpaid fees.

Default fee configuration is USD, USD 0.50 per late day, a 30-day replacement-price threshold, a 1.00 replacement multiplier, scale 2, and `HALF_UP` rounding. The deployment must explicitly choose the billable time zone and day rule.

### 3.2.9 Payments

- **FR-PAY-001 (P1):** The system shall quote a payment for a non-empty selected set of the member's unpaid fees.
- **FR-PAY-002 (P1):** All selected fees shall belong to the supplied member and use the requested currency.
- **FR-PAY-003 (P1):** A quote shall include a generated payment identifier, total amount, server quote time, fee identifiers, deterministic allocation identifiers, and per-fee amounts.
- **FR-PAY-004 (P1):** Recording a payment shall recalculate the quote and require the submitted amount to equal the calculated total exactly.
- **FR-PAY-005 (P1):** Recording a payment shall atomically create the payment, create its allocations, and settle its selected fees within the Borrowing Service's local transaction.
- **FR-PAY-006 (P1):** A payment identifier shall be idempotent when member, amount, currency, and selected fee set match the existing payment.
- **FR-PAY-007 (P1):** Reuse of a payment identifier with different member, amount, currency, or fee selection shall be rejected.
- **FR-PAY-008 (P1):** Normal quote and payment timestamps shall come from the server clock; deprecated client timestamps shall be ignored.
- **FR-PAY-009 (P2):** The system shall list all payments, retrieve a payment by identifier, and list a member's payments.
- **FR-PAY-010 (P1):** The system shall record financial settlement but shall not claim to charge an external payment instrument.

### 3.2.10 Borrowing Bans

- **FR-BAN-001 (P1):** The system shall count a member's permanent-damage fees when evaluating damage-ban escalation.
- **FR-BAN-002 (P1):** The first configured threshold shall create a `TIER_1` temporary ban.
- **FR-BAN-003 (P1):** The second configured threshold shall create a `TIER_2` temporary ban after the first tier has been issued.
- **FR-BAN-004 (P1):** The final configured threshold shall create a `PERMANENT` ban after the second tier has been issued.
- **FR-BAN-005 (P1):** Thresholds shall be positive and strictly increasing; temporary ban durations shall be positive.
- **FR-BAN-006 (P1):** Ban identifiers shall be deterministic per member and tier so event retries do not issue duplicate tiers.
- **FR-BAN-007 (P1):** An active temporary or permanent ban shall make a member ineligible for a new loan.
- **FR-BAN-008 (P2):** The system shall return each ban record's current state and complete period history.
- **FR-BAN-009 (P2):** The system shall list all ban records, retrieve one by identifier, and retrieve a member's ban record.

The default thresholds are 10 permanent-damage fees for a one-month Tier 1 ban, 20 for a three-month Tier 2 ban, and 30 for a permanent ban.

### 3.2.11 Integration Events

- **FR-EVT-001 (P1):** Catalog and Borrowing shall write business state and the corresponding outbox record in one local database transaction.
- **FR-EVT-002 (P1):** Outbox publishers shall use Kafka producers with acknowledgements from all in-sync replicas and idempotent production enabled.
- **FR-EVT-003 (P1):** Loan events shall use the loan identifier as the Kafka key; catalog deletion events shall use the book identifier as the key.
- **FR-EVT-004 (P1):** Every external event shall contain a stable UUID `eventId`, `eventVersion`, non-negative `aggregateVersion`, `occurredAt`, and routing identifiers.
- **FR-EVT-005 (P1):** Inventory shall validate schema version, identifiers, Kafka key, payload, route, transition order, and aggregate version before changing stock.
- **FR-EVT-006 (P1):** Inventory shall record the external event identifier and semantic loan transition in the same local transaction as the stock mutation.
- **FR-EVT-007 (P1):** Exact or semantic redelivery shall be acknowledged without applying another stock mutation.
- **FR-EVT-008 (P1):** A terminal loan event shall be applied only after `loan.created`, for the same book and library route, from the borrowed inventory state, and at a greater aggregate version.
- **FR-EVT-009 (P1):** Invalid or exhausted out-of-order messages shall be routed to `<original-topic>.DLT` with original-message and exception diagnostics retained in headers.
- **FR-EVT-010 (P2):** Runtime integration DTOs and topic definitions shall drive AsyncAPI documentation.
- **FR-EVT-011 (P1):** Membership shall not publish profile or subscription Kafka events while loan admission depends on the live eligibility endpoint.

### 3.2.12 Query Consistency

- **FR-QRY-001 (P1):** State-changing commands shall be handled separately from read queries through the CQRS design.
- **FR-QRY-002 (P1):** Read APIs shall use persisted projections appropriate to their service.
- **FR-QRY-003 (P1):** A successful asynchronous command response shall not imply that every downstream service or tracking projection has already applied the resulting event.
- **FR-QRY-004 (P2):** Clients should retry a read with bounded delay when an immediately preceding accepted command is not yet visible in a projection.

## 3.3 External Interface Requirements

### 3.3.1 REST Interface Conventions

- **IR-REST-001:** REST requests and responses shall use JSON except for endpoints returning an empty body.
- **IR-REST-002:** Successful creation endpoints shall return either a command identifier or the created resource and should use HTTP 201 when implemented by the endpoint.
- **IR-REST-003:** Successful queries shall return HTTP 200.
- **IR-REST-004:** Successful category deletion shall return HTTP 204.
- **IR-REST-005:** Invalid input shall return HTTP 400; missing resources shall return HTTP 404; business-state conflicts shall return HTTP 409; unavailable dependencies shall return HTTP 503.
- **IR-REST-006:** Error responses shall use `ProblemDetail`-compatible JSON and include a stable `code` property where the service maps one.
- **IR-REST-007:** Each service shall expose generated OpenAPI documentation when its documentation dependency is enabled.

### 3.3.2 Catalog Service Endpoints

| Method and path | Purpose |
|---|---|
| `GET /api/books/all` | List all books |
| `GET /api/books/available` | List non-deleted books |
| `GET /api/books/{id}` | Retrieve one book |
| `GET /api/books/{id}/available` | Check catalog availability |
| `GET /api/books/{bookId}/price` | Retrieve price and currency |
| `GET /api/books/search/title?title=...` | Search by title |
| `GET /api/books/search/author?author=...` | Search by author |
| `GET /api/books/filter/category?categoryId=...` | Filter by category |
| `POST /api/books/create` | Create a book |
| `PUT /api/books/update` | Update a book |
| `DELETE /api/books/delete` | Logically delete a book |
| `GET /api/categories/all` | List categories |
| `POST /api/categories/create` | Create a category |
| `PUT /api/categories/update/{id}` | Update a category |
| `DELETE /api/categories/delete/{id}` | Delete an unused category |

### 3.3.3 Membership Service Endpoints

| Method and path | Purpose |
|---|---|
| `GET /api/members/all` | List members |
| `GET /api/members/{id}` | Retrieve by member identifier |
| `GET /api/members/membership-number/{membershipNumber}` | Retrieve by membership number |
| `GET /api/members/{memberId}/subscription-status` | Retrieve loan eligibility |
| `POST /api/members/register` | Register a member |
| `PUT /api/members/{id}/name` | Update name |
| `PUT /api/members/{id}/contact-details` | Update email and phone |
| `POST /api/members/{id}/subscriptions/start` | Start first subscription |
| `POST /api/members/{id}/subscriptions/renew` | Renew subscription |

### 3.3.4 Inventory Service Endpoints

| Method and path | Purpose |
|---|---|
| `GET /api/libraries/all` | List all libraries |
| `GET /api/libraries/available` | List active libraries |
| `GET /api/libraries/{id}` | Retrieve one library |
| `POST /api/libraries/create` | Create a library |
| `PUT /api/libraries/update` | Update a library |
| `DELETE /api/libraries/delete` | Logically delete a library |
| `POST /api/stock` | Add stock |
| `DELETE /api/stock` | Remove available stock |
| `GET /api/stock/{libraryId}` | List a library's stock |
| `GET /api/stock/{libraryId}/{bookId}` | Retrieve one stock record |
| `GET /api/stock/{libraryId}/{bookId}/availability` | Obtain loan availability decision |
| `GET /api/stock/book/{bookId}` | List stock for a book across libraries |
| `GET /api/transfers/{id}` | Retrieve one transfer |
| `GET /api/transfers` | List or filter transfers |
| `POST /api/transfers/request` | Request a transfer |
| `POST /api/transfers/accept` | Accept a requested transfer |
| `POST /api/transfers/reject` | Reject a requested transfer |
| `POST /api/transfers/cancel` | Cancel a requested or accepted transfer |
| `POST /api/transfers/ship` | Ship an accepted transfer |
| `POST /api/transfers/complete` | Complete a shipped transfer |

### 3.3.5 Borrowing Service Endpoints

| Method and path | Purpose |
|---|---|
| `GET /api/loans/all` | List loans |
| `GET /api/loans/{id}` | Retrieve one loan |
| `GET /api/loans/member/{memberId}` | List member loans |
| `GET /api/loans/member/{memberId}/active` | List active member loans |
| `POST /api/loans/create` | Create a loan |
| `POST /api/loans/{id}/extend` | Extend a loan |
| `POST /api/loans/{id}/return` | Return a loan |
| `POST /api/loans/{id}/lost` | Declare a book lost |
| `POST /api/loans/{id}/damage` | Record permanent damage |
| `GET /api/fees/all` | List fees |
| `GET /api/fees/{id}` | Retrieve one fee |
| `GET /api/fees/member/{memberId}` | List member fees |
| `GET /api/fees/member/{memberId}/unpaid` | List unpaid member fees |
| `GET /api/payments/all` | List payments |
| `GET /api/payments/{id}` | Retrieve one payment |
| `GET /api/payments/member/{memberId}` | List member payments |
| `POST /api/payments/quote` | Calculate an exact fee payment |
| `POST /api/payments/record` | Record and allocate a payment |
| `GET /api/borrowing-bans/all` | List ban records |
| `GET /api/borrowing-bans/{id}` | Retrieve one ban record |
| `GET /api/borrowing-bans/member/{memberId}` | Retrieve a member's ban record |

### 3.3.6 Synchronous Service Contracts

- **IR-SVC-001:** Membership eligibility shall return `memberId`, `exists`, `active`, and `currentPeriodEndsAt`.
- **IR-SVC-002:** Inventory availability shall return `libraryActive`, `bookActive`, `availableQuantity`, and `available`.
- **IR-SVC-003:** Catalog price shall return decimal `amount` and three-letter `currency`.
- **IR-SVC-004:** Missing resources in these contracts shall map to 404; an unavailable provider shall map to a dependency failure and cause the caller to fail closed.
- **IR-SVC-005:** Configured HTTP clients shall use bounded connection and read timeouts and circuit-breaker integration.

### 3.3.7 Kafka Interface

The published topic contracts are:

| Producer | Topic | Consumer | Business effect |
|---|---|---|---|
| Catalog | `book.deleted` | Inventory | Retire local catalog reference |
| Borrowing | `loan.created` | Inventory | Decrease available stock by one |
| Borrowing | `loan.returned` | Inventory | Increase available stock by one |
| Borrowing | `loan.marked.lost` | Inventory | Decrease total stock by one |
| Borrowing | `loan.marked.damaged` | Inventory | Decrease total stock by one |

All event records shall be flat JSON. Loan event payloads shall contain `eventId`, `loanId`, `eventVersion`, `aggregateVersion`, `occurredAt`, `bookId`, `libraryId`, optional `idempotencyKey`, and a transition-specific timestamp. A book deletion payload shall contain `eventId`, `bookId`, `eventVersion`, `aggregateVersion`, and `occurredAt`.

### 3.3.8 Operational Interfaces

- **IR-OPS-001:** Services shall expose `/actuator/health`; configured services shall also expose `/actuator/info` and metrics.
- **IR-OPS-002:** Catalog and Borrowing shall expose pending and dead-lettered outbox metrics under their service-specific `*.outbox.*` names.
- **IR-OPS-003:** Inventory shall expose exhausted Kafka recovery counts at `/actuator/metrics/inventory.kafka.dead.letter.recoveries`, tagged by source topic.
- **IR-OPS-004:** Kafka-producing services shall expose Springwolf AsyncAPI documentation at `/springwolf/docs` when enabled.
- **IR-OPS-005:** Services shall register health information in Consul at the configured interval.

## 3.4 Data Requirements

### 3.4.1 Data Ownership

- **DR-001:** Catalog shall be the source of truth for books, categories, catalog retirement, and current book price.
- **DR-002:** Membership shall be the source of truth for member profiles and subscription eligibility.
- **DR-003:** Inventory shall be the source of truth for libraries, stock counts, transfers, and applied circulation inventory state.
- **DR-004:** Borrowing shall be the source of truth for loans, fees, payments, allocations, and bans.
- **DR-005:** A service shall not use another service's database as an integration interface.

### 3.4.2 Integrity and History

- **DR-006:** Public domain identifiers and integration event identifiers shall remain stable after creation.
- **DR-007:** Book and library deletion shall preserve historical records through logical-deletion state.
- **DR-008:** Historical subscription start and end times shall be stored and shall not be recalculated when tier configuration changes.
- **DR-009:** Loan terminal state shall preserve its relevant event timestamp and original routing identifiers.
- **DR-010:** Payment allocations and fee settlement details shall be retained for audit.
- **DR-011:** Borrowing bans shall retain the full issued-ban history.
- **DR-012:** Integration events and processing records shall retain enough version and identity data to detect duplicates and invalid ordering.

### 3.4.3 Time and Money

- **DR-013:** API timestamps shall include an offset or zone where represented as `ZonedDateTime`; integration timestamps shall represent instants.
- **DR-014:** All money shall be represented as decimal values, never binary floating-point values.
- **DR-015:** Currency codes shall be normalized uppercase three-letter codes where validated.
- **DR-016:** Operators shall configure a stable billable time zone; changing it may change calendar-day fee results and therefore requires a business-approved migration decision.

### 3.4.4 Retention and Backup

- **DR-017 (P2):** Production operators should back up each service database independently and verify restoration procedures.
- **DR-018 (P2):** Kafka retention shall be long enough to recover consumers without losing unpublished business transitions.
- **DR-019 (P2):** Dead-letter records and outbox failure details shall be retained until reconciliation is complete.
- **DR-020 (P3):** A production retention policy for member personal data, financial records, events, and operational logs should be defined before deployment with real users.

## 3.5 Non-functional Requirements

### 3.5.1 Performance

- **NFR-PERF-001 (P2):** Under nominal local conditions, a read request that does not invoke a downstream service should complete within 500 ms at the 95th percentile.
- **NFR-PERF-002 (P2):** Under nominal local conditions, a command that does not invoke a downstream service should be accepted or rejected within 1 second at the 95th percentile.
- **NFR-PERF-003 (P2):** A synchronous cross-service operation should complete within 5 seconds or fail with a dependency error; configured downstream connect/read timeouts shall remain bounded.
- **NFR-PERF-004 (P2):** Outbox publishers should attempt pending event publication within 2 seconds under healthy broker conditions; the configured scheduling delay is 1 second.
- **NFR-PERF-005 (P2):** Pagination shall be added before unbounded collection endpoints are used with production-scale data.

The repository contains no load-test evidence. The performance values above are acceptance targets, not measured guarantees.

### 3.5.2 Reliability and Consistency

- **NFR-REL-001 (P1):** A local business-state change and its outbox record shall commit atomically.
- **NFR-REL-002 (P1):** Consumer duplicate detection and its inventory mutation shall commit atomically.
- **NFR-REL-003 (P1):** Dependency uncertainty shall fail closed for loan admission, catalog validation, and replacement pricing.
- **NFR-REL-004 (P1):** Event publication and consumption shall tolerate retries without repeating the logical business effect.
- **NFR-REL-005 (P1):** A sustained pending-outbox count or any dead-letter count shall be treated as an operational incident.
- **NFR-REL-006 (P2):** Services should recover automatically when MySQL, Kafka, Consul, or a synchronous dependency returns, subject to retry bounds.

### 3.5.3 Security and Privacy

- **NFR-SEC-001 (P1, deployment prerequisite):** A production deployment shall add authentication and authorization before exposing any API outside a trusted network.
- **NFR-SEC-002 (P1, deployment prerequisite):** Administrative commands and unrestricted list endpoints shall be limited to authorized staff roles.
- **NFR-SEC-003 (P1, deployment prerequisite):** Member-facing access shall be limited to records belonging to the authenticated member unless staff privileges apply.
- **NFR-SEC-004 (P1, deployment prerequisite):** External HTTP traffic and sensitive internal traffic shall use TLS.
- **NFR-SEC-005 (P1):** Database, Kafka, and service credentials shall be supplied through environment or secret management and shall not be committed to source control.
- **NFR-SEC-006 (P1):** Error responses and logs shall not disclose passwords, secrets, connection strings, or unnecessary personal data.
- **NFR-SEC-007 (P2):** Production logs should provide an audit trail for catalog retirement, stock changes, transfers, subscription payments, loan transitions, fee settlements, and bans.
- **NFR-SEC-008 (P2):** Personal-data retention and deletion rules shall be defined according to applicable law before production use.

The first four requirements in this subsection are required for production acceptance but are not implemented by the current repository.

### 3.5.4 Maintainability and Testability

- **NFR-MNT-001 (P2):** Requirements shall be traceable to stable identifiers in this document.
- **NFR-MNT-002 (P2):** Business policy values shall be externalized where the implementation already supports configuration.
- **NFR-MNT-003 (P1):** Service-provider and consumer contracts shall be covered by Pact or equivalent contract tests.
- **NFR-MNT-004 (P1):** Domain invariants and state transitions shall be covered by automated unit or integration tests.
- **NFR-MNT-005 (P2):** REST and event contracts shall remain documented through OpenAPI and AsyncAPI.
- **NFR-MNT-006 (P1):** An incompatible external event change shall use a new schema version and a coordinated consumer migration.

### 3.5.5 Usability and Accessibility

- **NFR-USA-001 (P2):** API field names, status values, and error codes shall be stable and meaningful to client developers.
- **NFR-USA-002 (P2):** Validation errors shall identify the rejected field or business rule sufficiently for correction.
- **NFR-USA-003 (P3):** Any future browser interface should conform to WCAG 2.2 Level AA and support keyboard-only operation.
- **NFR-USA-004 (P3):** Any future browser interface should clearly distinguish active, returned, lost, damaged, paid, unpaid, retired, and transfer workflow states without relying on color alone.

### 3.5.6 Portability and Deployment

- **NFR-DEP-001 (P2):** The services shall be buildable as Java 17 applications and deployable using the supplied Docker definitions.
- **NFR-DEP-002 (P1):** Runtime-specific ports, data-source settings, Kafka bootstrap servers, Consul address, credentials, and required fee policy decisions shall be externally configurable.
- **NFR-DEP-003 (P2):** The root infrastructure shall be started before application services; Membership and Catalog should start before Inventory, and Inventory should start before Borrowing.
- **NFR-DEP-004 (P2):** The service Compose projects shall join the shared Docker network created by the root Compose project.

## 3.6 Failure and Recovery Requirements

- **FRR-001:** If Membership is unavailable, Borrowing shall reject new loan processing with a dependency-unavailable response rather than assume eligibility.
- **FRR-002:** If Inventory is unavailable, Borrowing shall reject new loan processing rather than assume stock availability.
- **FRR-003:** If Catalog is unavailable during stock validation or replacement pricing, the requesting operation shall fail closed.
- **FRR-004:** If Kafka is unavailable after a local transaction commits, the outbox record shall remain pending and be retried up to the configured maximum.
- **FRR-005:** When outbox retries are exhausted, the record shall be marked dead-lettered and exposed through metrics for operator action.
- **FRR-006:** If Inventory receives an invalid, incompatible, mis-keyed, or exhausted out-of-order message, it shall preserve the message in the related DLT and shall not apply an unsafe stock mutation.
- **FRR-007:** Operators shall correct the contract, ordering, or data cause before replaying a DLT record.
- **FRR-008:** Before a new consumer group with `auto-offset-reset=earliest` reads development topics, incompatible pre-versioned records shall be purged or migrated.
- **FRR-009:** Database unavailability shall cause the affected service health check to fail and state-changing operations shall not report success.
- **FRR-010:** After restoration, operators shall reconcile service state, pending outboxes, DLT records, and inventory projections before declaring the incident resolved.

### 3.6.1 Known Concurrency Limitation

Loan creation currently uses synchronous availability checking followed by local Borrowing persistence and later asynchronous Inventory decrement. The check is not an atomic stock reservation. Two concurrent requests can observe the same final copy before either `loan.created` event changes Inventory.

- **FRR-011 (P3):** If a strict last-copy invariant becomes mandatory, the system shall replace the current approach with either an idempotent synchronous Inventory reservation with compensation or a pending-stock reservation saga.
- **FRR-012 (P1):** A future synchronous reservation shall not be combined with a second decrement by the `loan.created` consumer.

---

# 4. Validation and Acceptance

## 4.1 Validation Approach

Requirements shall be validated using a combination of:

- unit tests for value-object validation, pricing, dates, and aggregate state transitions;
- service integration tests for persistence, command handling, projections, and HTTP error mapping;
- provider and consumer contract tests for Membership eligibility, Inventory availability, Catalog pricing/availability, and Kafka payloads;
- end-to-end tests with all four services, MySQL, Kafka, and Consul;
- resilience tests that interrupt downstream services and Kafka;
- duplicate and out-of-order event tests;
- API schema review using generated OpenAPI and AsyncAPI documents; and
- operator review of health, outbox, and dead-letter metrics.

Test data shall include valid cases, boundaries, missing resources, duplicate identifiers, wrong states, dependency failures, retries, and concurrent requests.

## 4.2 Acceptance Criteria

The current backend baseline is accepted when all of the following are true:

1. Each service builds and its automated test suite passes.
2. The four services start with valid explicit configuration and register healthy in Consul.
3. Catalog book/category workflows satisfy FR-CAT and FR-CTG requirements.
4. Member registration and subscription workflows satisfy FR-MEM and FR-SUB requirements.
5. Library stock and transfer workflows satisfy FR-INV and FR-TRN requirements.
6. Loan, fee, payment, and ban workflows satisfy FR-LON, FR-FEE, FR-PAY, and FR-BAN requirements.
7. HTTP provider/consumer and Kafka provider/consumer contracts pass.
8. Replayed integration events do not repeat stock mutations.
9. Invalid and irrecoverably out-of-order records reach a DLT without corrupting inventory.
10. Health and required operational metrics are available.
11. No images or binary diagrams are required to interpret this specification.

A public production deployment is not accepted until NFR-SEC-001 through NFR-SEC-004 are implemented and verified.

## 4.3 Requirements Traceability

| Requirement group | Primary implementation area | Primary validation |
|---|---|---|
| FR-CAT, FR-CTG | `catalog-service` | Catalog API/domain tests |
| FR-MEM, FR-SUB | `membership-service` | Membership domain and provider contract tests |
| FR-INV, FR-TRN | `inventory-service` | Inventory domain, HTTP provider, and Kafka consumer tests |
| FR-LON | `borrowing-service` loan handlers and API | Loan handler/API and dependency contract tests |
| FR-FEE, FR-PAY, FR-BAN | `borrowing-service` policies, handlers, and API | Policy, calculation, aggregate, and API tests |
| FR-EVT, FRR | Catalog/Borrowing outboxes and Inventory inbox/listeners | Kafka provider/consumer, replay, and failure tests |
| IR-SVC | Feign clients and provider APIs | Pact HTTP consumer/provider tests |
| IR-OPS, NFR-REL | Actuator, metrics, Kafka configuration | Operational integration tests |
| NFR-SEC | Deployment/API security layer | Security tests required before production |

---

# 5. Appendices

## 5.1 Business Rules Summary

| Rule | Default baseline |
|---|---|
| ISBN format | Exactly 13 digits |
| Member phone format | Nine digits beginning with `07`, stored with spaces |
| Membership tiers | 3, 6, or 12 months |
| Membership tier prices | MKD 900.00, MKD 1500.00, MKD 2400.00 |
| Maximum active loans | 5 |
| Unpaid-fee rejection threshold | 3 |
| Regular loan period | 14 days |
| Extension | One extension of 14 days |
| Fee currency | USD |
| Daily late fee | USD 0.50 |
| Replacement-price threshold | 30 billable late days |
| Replacement multiplier | 1.00 |
| Money rounding | Scale 2, `HALF_UP` |
| Tier 1 damage ban | 10 damaged-book fees; 1 month |
| Tier 2 damage ban | 20 damaged-book fees; 3 months |
| Permanent damage ban | 30 damaged-book fees |
| Event schema version | 1 |
| Outbox publish delay | 1 second |
| Outbox maximum attempts | 20 |

Configuration values are part of the current baseline, not immutable universal library rules. A deployment may change them if it preserves the validation constraints and records the approved policy.

## 5.2 Known Limitations and Open Decisions

1. **Authentication and authorization:** No security layer currently protects the APIs. This must be designed before production exposure.
2. **User interface:** The React/Vite project is starter content and does not implement the library workflows.
3. **Final-copy race:** Availability checking does not reserve stock atomically. A strict last-copy guarantee requires the design described in FRR-011.
4. **Billable time policy:** `BORROWING_FEES_BILLABLE_TIME_ZONE` and `BORROWING_FEES_BILLABLE_DAY_RULE` have no safe business default and must be chosen explicitly.
5. **Ban escalation during an active temporary ban:** When the next damage threshold is reached while a temporary ban is still active, the implementation records a warning and does not issue the next tier. The business must decide whether escalation should be immediate, queued, or applied after expiry.
6. **Payment processing:** Payments are recorded as business facts; no external payment gateway verifies or transfers funds.
7. **Pagination:** Collection endpoints return unpaged lists and require pagination before large-scale production use.
8. **Database migrations:** Automatic Hibernate update is used; versioned production migrations are not present.
9. **Availability objectives:** No measured throughput, capacity, uptime, recovery-time, or recovery-point objective is supplied. Production service-level objectives require stakeholder approval and load/resilience evidence.
10. **Personal-data governance:** Legal basis, retention schedule, subject-access procedure, and deletion/anonymization rules remain to be defined for real member data.

---

End of Software Requirements Specification.
