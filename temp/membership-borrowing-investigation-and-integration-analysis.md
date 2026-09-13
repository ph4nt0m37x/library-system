# Membership and Borrowing Service Investigation, Manual Test Findings, and Integration Analysis

Date: 12 September 2026  
Project: Library System  
Primary scope: Membership Service and Borrowing Service only

## 1. Scope and method

This report documents source review, Docker deployment verification, Consul verification, manual HTTP testing, Kafka inspection, read-only MySQL inspection, dependency-failure testing, concurrency testing, and restart/persistence testing for the Membership and Borrowing services.

Catalog and Inventory were reviewed only where necessary to establish the contracts that Membership or Borrowing depend on. Their business behavior was not reinvestigated. The already implemented Catalog and Inventory fixes were treated as the current baseline.

No application source code or automated tests were added or changed. Every business state transition in this investigation was created through a real HTTP API. MySQL was queried only after the requests to verify persistence. No H2 tests and no Maven test suites were run. The Dockerfiles necessarily execute `mvn package` with `-DskipTests` while building application images; this was packaging, not test execution.

The first Docker start reused at least one stale application image: Inventory registered the old `inventory-service` Kafka group although the checked-out source uses `inventory-service-group`. All four application images were therefore rebuilt from the checked-out source with Docker Compose before results were accepted as final. The decisive Membership and Borrowing scenarios were repeated against the rebuilt images.

The databases already contained evidence from earlier investigations. They were deliberately not reset. New records used unique identifiers and email addresses, and the report identifies representative records from this run.

### Final running environment

| Component | Address | Final result |
|---|---:|---|
| Catalog | `http://localhost:8088` | Actuator `UP`; rebuilt from current source |
| Inventory | `http://localhost:8089` | Actuator `UP`; rebuilt from current source; `inventory-service-group` connected |
| Membership | `http://localhost:8087` | Actuator `UP`; rebuilt; restart persistence verified |
| Borrowing | `http://localhost:8090` | Actuator `UP`; Docker `healthy`; rebuilt; restart persistence verified |
| Consul | `http://localhost:8500` | Healthy; service health checks passing |
| Kafka | `localhost:9092` externally, `broker:29092` internally | Running |
| Kafka UI | `http://localhost:8081` | Running |

The final Membership and Borrowing reads still returned the API-created member, subscription history, incoherent-date test loan, fee, payment, and allocation after both application containers were restarted.

---

# 2. Membership Service

## 2.1 Current design

Membership stores member profiles and subscription periods in MySQL. Axon Server is disabled. `Member` is a JPA-backed Axon aggregate and `SubscriptionPeriod` is an aggregate-owned entity. A separate tracking projection writes `MemberView` and `SubscriptionPeriodView` rows for reads.

The write-side commands are:

- `RegisterMemberCommand`
- `UpdateMemberNameCommand`
- `UpdateMemberContactDetailsCommand`
- `StartSubscriptionCommand`
- `RenewSubscriptionCommand`

The internal events are:

- `MemberRegisteredEvent`
- `MemberNameUpdatedEvent`
- `MemberContactDetailsUpdatedEvent`
- `SubscriptionStartedEvent`
- `SubscriptionRenewedEvent`

Tier durations are configured as 3, 6, and 12 calendar months. Starting a subscription uses the client-supplied `startsAt`; renewing starts at the later of the server's renewal time and the previous period's end. Active status is derived at read time from projected periods.

Kafka and Springwolf dependencies/properties exist, and the event base class can derive topic names, but Membership has no Kafka producer, external-event DTO, or consumer. Its live AsyncAPI document contained no channels.

## 2.2 Exposed Membership API

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/members/all` | List all members and complete subscription histories |
| `GET` | `/api/members/{id}` | Find a member by ID |
| `GET` | `/api/members/membership-number/{membershipNumber}` | Find by membership number |
| `GET` | `/api/members/{memberId}/subscription-status` | Return a Boolean active-subscription result |
| `POST` | `/api/members/register` | Register a member |
| `PUT` | `/api/members/{id}/name` | Update the member's name |
| `PUT` | `/api/members/{id}/contact-details` | Update email and phone |
| `POST` | `/api/members/{id}/subscriptions/start` | Start the first subscription |
| `POST` | `/api/members/{id}/subscriptions/renew` | Append a renewal period |

## 2.3 Membership behavior that worked

The following behavior worked in the rebuilt Docker deployment:

- Registration returned `201 Created`, a scalar public ID, and a `Location` header.
- Names and email were trimmed; email was lowercased; the accepted local phone format was normalized to `07X XXX XXX`.
- Member reads by ID and membership number returned the expected projection.
- Sequential duplicate email registration was rejected.
- Blank names, malformed JSON, missing required JSON fields, invalid enum values, invalid email shapes caught by the current regex, invalid local phone shapes, and negative subscription amounts were rejected.
- Name/contact updates changed the read projection; no-op updates were rejected.
- A first subscription was created and a renewal was chained from the preceding end date without overlap.
- A current subscription made the member active; a future-only subscription remained inactive.
- An attempted renewal before the first subscription and an attempted second start were rejected.
- The separate aggregate and read-model data remained readable after the application container restarted.

Representative rebuilt-image member:

```text
memberId: c4ded25f-6ef3-414c-bc4c-9266a8db393e
registered email: rebuilt.1789233600196@example.com
first period: 2026-09-01 through 2026-12-01, THREE_MONTHS
```

## 2.4 Membership findings and required fixes

### MEM-01 - Critical: an active borrowing entitlement can be created without a valid payment

Observed:

- `amountPaid = 0.00` was accepted for a six-month renewal.
- Currency `ZZZ` was accepted because validation checks only three uppercase letters, not an allowed ISO 4217 currency.
- A future `paidAt` in 2099 did not prevent a subscription from being active in 2026.
- A payment timestamp in 1900 was accepted for a period created in 2026.
- There is no tier price, quote, payment authorization/reference, payment status, or verification against another source.
- Borrowing trusts only the resulting Boolean active status, so this Membership request is enough to grant borrowing eligibility.

Cause:

- `validatePayment` permits zero with `amountPaid >= 0`.
- It validates currency with `^[A-Z]{3}$` only.
- Active status depends only on `startsAt` and `endsAt`; payment fields do not participate.

Required fix:

1. Decide whether a subscription can be complimentary. If yes, model an explicit complimentary/waived reason and authorized actor rather than treating zero as an ordinary payment.
2. Otherwise require a strictly positive amount.
3. Configure or retrieve the authoritative price and currency for each tier and require the paid amount to match it.
4. Validate against an explicit supported-currency set, not only a three-letter shape.
5. Store an idempotent payment/transaction reference and an authorization or settlement status.
6. Do not make the subscription eligible until the payment condition required by the business is satisfied.

### MEM-02 - High: subscription renewal has no request idempotency

Observed:

- The same renewal JSON was submitted twice after a successful renewal.
- Both calls returned `200` with different subscription IDs.
- Each retry appended another six-month period. The tested member's history grew from two periods to four:

```text
2026-09-01 -> 2026-12-01
2026-12-01 -> 2027-06-01
2027-06-01 -> 2027-12-01   identical retry 1
2027-12-01 -> 2028-06-01   identical retry 2
```

Impact:

- A client/network retry can add repeated entitlement and, once real payments are connected, can duplicate financial processing.

Required fix:

1. Require an `idempotencyKey` or payment transaction ID on start and renewal commands.
2. Persist it with a unique constraint.
3. On an exact retry, return the original subscription ID and response without adding an event or period.
4. Reject reuse of the same key with different material fields as `409 Conflict`.

### MEM-03 - High: payment and subscription chronology is not validated

Observed:

- A subscription starting in 2099 with `paidAt = 1900` was accepted.
- A currently active subscription with `paidAt = 2099` was accepted.
- Renewal payment timestamps can predate registration or occur far in the future.
- `startsAt` can be arbitrarily old or far in the future without an explicit scheduling/correction mode.

Required fix:

1. Generate receipt/processing time on the server with the injected `Clock`.
2. If the upstream payment provider supplies a payment time, retain it separately and validate it against reasonable bounds.
3. Define whether scheduled future starts and historical corrections are allowed.
4. Restrict exceptional backdating/future dating to explicit privileged workflows with audit data.
5. Reject impossible relationships, such as a payment that occurs after eligibility has already started when prepayment is required.

### MEM-04 - High: nonexistent members are indistinguishable from inactive members, and commands return `500`

Observed:

- `GET /api/members/missing.../subscription-status` returned `200 false`.
- Updating or starting a subscription for a missing member returned generic `500`.
- Logs showed unhandled Axon `AggregateNotFoundException`.
- Borrowing consequently maps a nonexistent member to `409 MEMBERSHIP_INACTIVE` rather than a not-found result.

Required fix:

1. Make the status endpoint verify member existence and return `404` for a missing member.
2. Prefer one structured eligibility response containing at least `memberId`, `exists`, `active`, and current period end, or retain Boolean `200` plus a real `404` distinction.
3. Map `AggregateNotFoundException` to `404` for every member command.
4. Have the Borrowing Feign error decoder preserve the distinction between missing, inactive, and unavailable Membership service.

### MEM-05 - High: email uniqueness is race-safe in MySQL but not in the HTTP contract

Observed:

- Sequential duplicate registration returned `400`.
- Two simultaneous registration requests with the same normalized email produced one `201` and one generic `500`.
- Logs showed MySQL error 1062 on the unique email key.
- Updating contact details to another member's email also returned `400`.

Cause:

- The repository pre-check races with the insert.
- The database constraint correctly protects the data, but its exception is not mapped.
- Duplicate business conditions are represented with `IllegalArgumentException` and therefore treated as bad syntax.

Required fix:

1. Retain the database unique constraint as the race-safe authority.
2. Map the email constraint violation to `409 Conflict` with a stable `DUPLICATE_EMAIL` code.
3. Return the same conflict contract for the pre-check and the race-loser path.
4. Consider registration idempotency so an exact retry can return the existing result where ownership has been proven.

### MEM-06 - Medium: money bounds and scale are left to MySQL

Observed:

- `12.3456` was accepted and later returned/stored as `12.35`.
- An amount larger than the `DECIMAL(19,2)` capacity returned generic `500`.

Required fix:

1. Define scale and rounding explicitly at the API/domain boundary.
2. Prefer rejecting excess scale with a field-level `400`, or document and apply the rounding rule before the event is created.
3. Enforce the maximum integer digits supported by the column.
4. Ensure the event value and persisted aggregate/projection value cannot silently diverge through database rounding.

### MEM-07 - Medium: contact validation is incomplete and its geographic contract is implicit

Observed:

- The email `a..b@<value>.example` passed the current permissive regex.
- International form `+38971234567`, punctuation, and other otherwise common telephone representations were rejected.
- Two members could share the same normalized phone number.

Required fix:

1. Define the accepted email and telephone contract explicitly.
2. Use a maintained email validator or verification workflow rather than trying to implement full RFC syntax with the current regex.
3. If international numbers are supported, normalize to E.164; if only Macedonian mobile numbers are supported, document that and accept/normalize agreed local variants.
4. Decide whether phone uniqueness is a business invariant. Add a database constraint only if shared household/contact numbers are forbidden.

### MEM-08 - High: expected domain failures do not have consistent status semantics

Observed:

- Missing aggregates returned `500`.
- Duplicate email, no-op update, duplicate subscription start, renewal-before-start, and terminal business conflicts returned `400`.
- Jackson/framework `400` responses used a different body from the service's `ApiError`.
- Error bodies lack stable codes, request paths, correlation IDs, and field violations.

Required fix:

- Use one `ProblemDetail`-based contract.
- Map malformed/field validation to `400`, missing resources to `404`, duplicate resources and illegal state transitions to `409`, and downstream unavailability to `503`.
- Unwrap Axon/async exceptions without exposing framework or SQL details.

### MEM-09 - Medium: member and subscription lifecycle operations are incomplete

The model has no member deactivation, suspension, deletion, subscription cancellation, correction, refund, or revocation operation. This leaves no authoritative way to remove eligibility early after a mistaken subscription or account closure.

Required fix:

- Confirm which lifecycle operations are required before adding endpoints.
- Preserve history with explicit events and effective timestamps rather than physically deleting periods.
- Ensure Borrowing eligibility immediately respects deactivation/revocation.

### MEM-10 - Medium: Kafka/AsyncAPI configuration advertises capability that is not implemented

Observed:

- The live Membership AsyncAPI document had no channels.
- No `KafkaTemplate`, listener, external-event type, or publisher exists.
- Kafka producer/consumer and Springwolf properties are otherwise present.

Required fix:

- Either remove unused Kafka/AsyncAPI configuration until a consumer-driven event is required, or implement only the external events identified in section 7.
- Do not publish internal Axon events or PII-heavy profile events without a real consumer and privacy requirement.

### MEM-11 - Medium: operational configuration is inconsistent

Observed:

- `application.properties` defaults Membership to port `8089`, which collides with Inventory; Compose overrides it to `8087`.
- Compose changes the MySQL server's internal port to `3307`, but `.env.example` points to `mysqldb:3306`. Copying the example would break the application.
- The Membership application container has no Docker health check and no restart policy.
- It depends only on MySQL health, not Consul readiness.

Required fix:

1. Use one canonical Membership port (`8087` in this deployment) in defaults and Compose.
2. Prefer leaving MySQL on container port `3306` and mapping only the host port (`3307:3306`), then use `mysqldb:3306` internally.
3. Correct `.env.example` and verify it in a clean deployment.
4. Add an actuator Docker health check, restart policy, and documented shared-infrastructure startup order.

### MEM-12 - High: authentication, authorization, and PII protection are absent

All list/read/update/subscription operations were callable without credentials. `/api/members/all` returns every member's contact data and complete payment/subscription history.

Required fix if the system is not intentionally public:

- Authenticate callers and authorize member self-service versus staff operations.
- Restrict bulk PII listing and add audit identity to mutations.
- Derive privileged correction/payment actions from trusted identity rather than request data.
- Add pagination and data minimization to member listings.

---

# 3. Borrowing Service

## 3.1 Current design

Borrowing stores four JPA-backed Axon aggregate areas in MySQL:

- Loans
- Fees
- Payments and allocations
- Borrowing-ban records and periods

Separate tracking projections back all public reads. Local subscribing policy handlers create fees from terminal loan events, settle fees from payment events, and issue escalating borrowing bans from permanent-damage fees.

Current loan eligibility checks:

1. Membership's Boolean active-subscription endpoint through Feign.
2. Fewer than five active loans in Borrowing's loan table.
3. Fewer than three unpaid fees in Borrowing's fee table.
4. No active temporary or permanent Borrowing ban.

Borrowing declares a second Feign client named `inventory-service` for replacement-price lookup. That target and contract are incorrect. Kafka and Springwolf are configured, but Borrowing publishes and consumes no Kafka records. Its live AsyncAPI document contained no channels.

## 3.2 Exposed Borrowing API

### Loans

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/loans/all` | List loans |
| `GET` | `/api/loans/{id}` | Find a loan |
| `GET` | `/api/loans/member/{memberId}` | List a member's loans |
| `GET` | `/api/loans/member/{memberId}/active` | List active loans |
| `POST` | `/api/loans/create` | Create a loan |
| `POST` | `/api/loans/{id}/extend` | Extend once |
| `POST` | `/api/loans/{id}/return` | Return a book |
| `POST` | `/api/loans/{id}/lost` | Declare a borrowed copy lost |
| `POST` | `/api/loans/{id}/damage` | Record permanent damage |

### Fees, payments, and bans

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/fees/all`, `/api/fees/{id}` | List/get fees |
| `GET` | `/api/fees/member/{memberId}` | List member fees |
| `GET` | `/api/fees/member/{memberId}/unpaid` | List unpaid fees |
| `POST` | `/api/payments/quote` | Quote selected fees |
| `POST` | `/api/payments/record` | Record and allocate a payment |
| `GET` | `/api/payments/all`, `/api/payments/{id}` | List/get payments |
| `GET` | `/api/payments/member/{memberId}` | List member payments |
| `GET` | `/api/borrowing-bans/all`, `/api/borrowing-bans/{id}` | List/get ban records |
| `GET` | `/api/borrowing-bans/member/{memberId}` | Get a member's ban record |

## 3.3 Borrowing behavior that worked

The following behavior worked in the rebuilt Docker deployment:

- A member with no active Membership subscription was rejected with `409 MEMBERSHIP_INACTIVE`.
- An accepted loan was stored as `ACTIVE` with `dueAt = borrowedAt + 14 days`.
- An active loan could be extended once before its due date; a second extension was rejected.
- Return, loss, and permanent damage were terminal transitions; repeated terminal operations were rejected.
- A late return created one deterministic `OVERDUE` fee.
- A two-calendar-day late return quoted as USD `1.00` under the configured Europe/Skopje calendar-day policy and USD `0.50` daily rate.
- Recording the exact quote created a payment/allocation and settled the selected fee.
- An exact payment replay did not add another payment row/event; reuse with changed material fields was rejected.
- Duplicate fee selection, an empty selection, a nonexistent fee, a paid fee, wrong currency, and wrong amount were rejected.
- Three unpaid fees blocked a subsequent loan with `409 TOO_MANY_UNPAID_FEES`.
- After the concurrent limit test had created six active loans, a later seventh request was rejected with `409 ACTIVE_LOAN_LIMIT_REACHED`.
- Aggregate and projection counts matched after the test: 31 loans/views, 21 fees/views, and 4 payments/views. Axon's dead-letter table was empty.
- Representative loans, fees, payments, and projections remained readable after Borrowing restarted.

## 3.4 Borrowing findings and required fixes

### BOR-01 - Critical: loans are disconnected from libraries, stock, and Catalog validity

Observed:

- `CreateLoanDTO`, `Loan`, and `LoanCreatedEvent` contain no `libraryId`.
- A loan for `ghost-book-not-in-catalog` returned `201`.
- A loan with a blank `bookId` returned `201`.
- There is no Inventory availability/reservation call in loan creation.
- Borrowing has no way to identify which library should lose or regain a copy.
- Inventory therefore cannot reliably execute the physical stock behavior represented by a loan.

Impact:

- Borrowing can create valid-looking loans for nonexistent/deleted titles and unavailable copies.
- Even if Kafka publication is added, the required library routing value does not exist.

Required fix:

1. Add scalar `libraryId` to the create request, command, aggregate, internal event, read model, and response.
2. Reject blank/oversized member, book, and library identifiers.
3. At creation, verify an active library, active Catalog title, and an available/reservable copy through Inventory.
4. Do not treat a read-only availability query as a strong reservation; section 7 describes the minimum wiring and the stronger consistency option.
5. Preserve `bookId` and `libraryId` on later loan events/external messages so return/loss/damage affect the same copy pool.

### BOR-02 - Critical: the active-loan limit is raceable

Observed:

- Six simultaneous `POST /api/loans/create` requests were sent for a fresh active member.
- All six returned `201`.
- The read API and MySQL both showed six `ACTIVE` loans although the configured limit is five.
- A later sequential seventh request saw the committed count and returned `409 ACTIVE_LOAN_LIMIT_REACHED`.

Cause:

- Each new loan is a different aggregate, so Axon's per-aggregate lock does not serialize eligibility by member.
- `countByMemberIdAndStatus` and loan insertion are a check-then-act race without a member-scoped lock or atomic capacity claim.
- The same pattern affects the unpaid-fee and ban checks when related state changes concurrently.

Required fix:

1. Serialize the eligibility decision per member, for example with a member-borrowing-limit aggregate, a database row lock/counter, or another atomic capacity reservation.
2. Include the new loan in the same transactional decision.
3. Recheck eligibility after acquiring the lock.
4. Add concurrent API/integration tests, not only sequential command-handler tests.

### BOR-03 - Critical: required loan Kafka events are not published

Observed after rebuilt-image calls:

| Topic | Before | After create/return/lost requests |
|---|---:|---:|
| `loan.created` | 3 | 3 |
| `loan.returned` | 3 | 3 |
| `loan.marked.lost` | 1 | 1 |

- Inventory's rebuilt `inventory-service-group` consumers were connected to all three topics with zero lag.
- No new records were produced by Borrowing.
- The retained records were older manually produced payloads, had null keys, and lacked the new required `eventId`, `loanId`, `eventVersion`, and `occurredAt` fields.

Impact:

- Successful loans never reduce stock.
- Returns never restore availability.
- Lost copies never reduce total stock.

Required fix:

- Implement the external messages, keys, outbox, and scenarios in section 7.
- Migrate/purge the incompatible development records or introduce versioned topics before creating a new consumer group with `auto-offset-reset=earliest`.

### BOR-04 - High: permanent damage has no Inventory contract

Observed in code:

- `LoanMarkedDamagedEvent` is terminal and creates a replacement fee.
- Inventory consumes created, returned, and lost topics only.
- A permanently damaged borrowed copy would remain counted in Inventory as an unavailable borrowed copy indefinitely.

Required fix:

- Add `loan.marked.damaged` and an Inventory consumer/command that removes the borrowed copy from usable total stock, or explicitly models a damaged/non-circulating quantity.
- Do not translate permanent damage into a normal return.

### BOR-05 - High: loan, incident, and payment chronology permits impossible histories

Observed and persisted:

```text
borrowedAt = 2099-01-01
extendedAt = 1900-01-01
returnedAt = 1901-01-01
dueAt      = 2099-01-29
status     = RETURNED
```

Additional observations:

- Future loan creation was accepted and immediately counted as active.
- A lost incident in 1900 was accepted for a 2026 loan.
- A payment in 1900 settled a fee created in 2026; the fee's `settledAt` also became 1900.
- `quotedAt` in 2099 was accepted.

Required fix:

1. Generate ordinary action timestamps on the server with `Clock`.
2. If offline/backdated circulation is required, expose it as a privileged audited workflow with bounded dates.
3. Enforce `extendedAt >= borrowedAt` and `< dueAt`.
4. Enforce return/loss/damage at or after borrowing and not unreasonably in the future.
5. Ensure payment/settlement cannot predate fee creation and use a server-generated quote time.

### BOR-06 - High: replacement-price Feign integration targets the wrong service and wrong schema

Observed:

- `InventoryBookPriceClient` calls `inventory-service` at `/api/books/{bookId}/price`.
- Inventory returned `404` because it has no such endpoint.
- Quoting a lost fee returned generic `500`; logs showed `FeignException.NotFound` wrapped in `NoFallbackAvailableException`.
- Catalog, the owner of book details, returned `200` for `/api/books/{id}/price`.
- Catalog's actual body is `{"price":12.35}`, while Borrowing expects `{"amount":...,"currency":...}`.
- Catalog currently has no book-price currency field.

Impact:

- Lost fees, damaged fees, and overdue fees at the replacement-price threshold cannot be quoted or paid through the normal API.

Required fix:

1. Point the client to `catalog-service` and rename it accordingly.
2. Agree one contract. Preferred: `{ "amount": 12.35, "currency": "USD" }`. Minimal current-model alternative: consume `{ "price": 12.35 }` and explicitly treat it as Borrowing's configured fee currency.
3. Map missing/deleted books to a domain outcome and service outages/timeouts to `503`, not `500`.
4. Add a consumer-driven contract test for the exact response schema.

### BOR-07 - High: dependency failure is a generic `500` while health remains `UP`

Observed:

1. Membership was stopped in a controlled test.
2. Borrowing's actuator still returned `200 UP`.
3. Loan creation waited for the circuit-breaker time limiter and returned generic `500`.
4. Logs reported `NoFallbackAvailableException` with a `TimeoutException`.
5. Membership was restarted and existing data remained available.

Required fix:

- Continue to fail closed when eligibility cannot be proven, but return `503 Service Unavailable` with a stable downstream code and `Retry-After` where appropriate.
- Configure explicit connect/read/time-limiter values, bounded retries only for safe calls, and circuit-breaker metrics.
- Separate liveness from readiness. Readiness/metrics should expose inability to perform new-loan eligibility even if read-only Borrowing endpoints remain usable.
- A fallback must not guess that a member is active.

### BOR-08 - High: missing resources and illegal state transitions have incorrect error semantics

Observed:

- Return/extend on a missing loan returned generic `500`; logs showed `AggregateNotFoundException`.
- A nonexistent Membership member became `409 MEMBERSHIP_INACTIVE`.
- Returning an already lost loan and repeating loss returned `400`, although these are current-state conflicts.
- Missing fee quote returned `400`, not `404`.

Required fix:

- Use `404` for missing loan/member/fee/payment/ban resources.
- Use `409` for invalid state transitions and conflicting reused IDs.
- Use `400` for malformed syntax/field shape and `422` only if the project intentionally distinguishes semantic validation.
- Return one stable `ProblemDetail` contract and preserve useful downstream status without leaking framework internals.

### BOR-09 - High: loan creation has no idempotency key

The server creates a new UUID before every create command. Repeating the same client request therefore creates another loan. When stock events are wired, a retry could also consume another copy.

Required fix:

1. Accept an idempotency key/circulation transaction ID from the authorized checkout operation.
2. Persist it uniquely with the loan.
3. Return the original loan on an exact retry and reject changed payload reuse.
4. Use that identity consistently in Inventory reservation and Kafka correlation.

### BOR-10 - Medium: successful payment replay returns `201 Created`

The payment command is data-idempotent: an exact retry returns the original ID and writes no second row. The controller nevertheless always responds with `201` and a creation `Location`, even when the resource already existed.

Required fix:

- Return `201` only for the first creation and `200` for a recognized replay, or use a documented idempotent `PUT /api/payments/{paymentId}` contract.

### BOR-11 - High: current fee/ban policies cannot complete normally until price integration is fixed

The unpaid-fee limit blocks new loans at three. Damage-ban escalation begins at ten permanent-damage fees. Reaching later damage thresholds through normal API use requires settling earlier damaged fees, but damaged-fee quote currently fails through the broken price Feign client.

Required fix:

- Fix replacement pricing before treating ban escalation as operational.
- Re-evaluate threshold and unpaid-fee policy interaction with business owners.
- The existing code also intentionally declines to issue the next tier while a temporary ban is active and has no deferred scheduler; define when that pending escalation should be reconsidered.

### BOR-12 - Medium: asynchronous projection behavior is not represented in HTTP contracts

Commands return as soon as the aggregate transaction succeeds, while GETs use a tracking projection. A read immediately after `201` can therefore briefly return old/not-found data. The manual tests needed short waits before projection reads.

Required fix:

- Document eventual read consistency and expose event/aggregate version or operation status.
- Alternatively provide a consistent post-command representation or wait for the projection when the endpoint promises immediate read-after-write behavior.

### BOR-13 - Medium: Kafka/AsyncAPI configuration is incomplete

Borrowing's live AsyncAPI document had no channels even though Inventory's document advertised the three loan channels it consumes. Kafka properties and Springwolf dependencies alone do not implement or document publication.

Required fix:

- Add explicit publisher documentation generated from the external DTOs and exact topic constants in section 7.
- Document key, payload, schema version, content type, retry/DLT behavior, and examples.

### BOR-14 - Medium: operational configuration depends on unvalidated local files

Observed:

- Borrowing refuses startup unless billable timezone and day rule are supplied; the real `.env` supplies them, while source defaults are intentionally blank.
- Compose changes MySQL's internal port to `3308`, but `.env.example` points to `mysqldb:3306`.
- The application depends only on MySQL, while Consul and future Kafka publication are shared prerequisites.

Required fix:

- Prefer standard internal MySQL port `3306` with host mapping `3308:3306`, or correct every example/profile consistently.
- Validate `.env.example` in a clean Compose smoke deployment.
- Keep the explicit billable-day decision, but document it as a required deployment value.
- Add shared-infrastructure readiness/retry guidance and Kafka publication health once the producer exists.

### BOR-15 - High: authentication and authorization are absent

Any caller could create a loan for any member, alter loan terminal state, quote another member's fees, submit payments, and read all loan/fee/payment/ban histories. Client-supplied historical/future timestamps amplify the impact.

Required fix if the system is not intentionally public:

- Authenticate and authorize circulation staff/member operations.
- Derive acting staff/member identity from the principal.
- Restrict bulk financial and ban data.
- Add immutable audit actor, branch, correlation, and reason fields to all mutations.
- Paginate list endpoints.

---

# 4. Cross-cutting assessment for Membership and Borrowing

## 4.1 Error contracts are not aligned

Both services broadly turn `IllegalArgumentException` into `400`, even when the exception represents a duplicate or illegal current state. Axon not-found exceptions, database boundary failures, and Feign failures bypass the current advice and become default `500` bodies.

Adopt one problem format and the same meanings in both services:

| Condition | Status |
|---|---:|
| Malformed JSON / field shape | `400` |
| Missing member, loan, fee, payment, or ban | `404` |
| Duplicate unique value / state transition / reused key mismatch | `409` |
| Dependency unavailable or timed out | `503` |
| Unexpected defect | `500` with no implementation detail |

## 4.2 Client timestamps are trusted as facts

Membership payment dates and Borrowing circulation/payment dates can create histories spanning 1900 to 2099. Normal operations should use a server `Clock`; exceptional historical correction should be explicit, authorized, bounded, and audited.

## 4.3 Idempotency is inconsistent

Payment recording is idempotent by client-supplied payment ID. Membership renewal and loan creation are not. The same approach should be applied consistently to every externally retried command, with a unique idempotency key and payload fingerprint.

## 4.4 Service health is too shallow for dependency-driven commands

Both services can report `UP` while an operation required by their public contract is unavailable. Liveness should remain process-focused, while readiness and metrics should expose database, Consul resolution, Membership eligibility, Catalog price, Kafka publication backlog, and Inventory consumer health as appropriate.

---

# 5. Required fix order

## Priority 0 - required before trustworthy circulation

1. Add `libraryId`, Catalog/Inventory validation, and a real stock coordination path to loan creation.
2. Publish reliable loan create/return/lost/damaged integration events or adopt the stronger reservation workflow described in section 7.
3. Serialize per-member eligibility so concurrent requests cannot exceed the active-loan limit.
4. Fix replacement-price ownership/schema so lost, damaged, and escalated-overdue fees can be paid.
5. Prevent zero/unverified Membership payments from granting ordinary active entitlement.

## Priority 1 - integrity and recoverability

1. Add Membership renewal and Borrowing loan idempotency.
2. Enforce chronological invariants with server-controlled clocks.
3. Implement consistent `404`/`409`/`503` problem responses and Feign error decoding.
4. Add outbox/inbox, semantic deduplication, retries, and dead-letter handling for external events.
5. Add the permanent-damage Inventory transition.

## Priority 2 - API/domain completeness

1. Validate money scale, bounds, supported currencies, and tier prices.
2. Finalize email/phone and member lifecycle rules.
3. Align replay HTTP semantics and projection consistency contracts.
4. Add pagination and data-minimized response models.

## Priority 3 - operations and security

1. Add authentication, authorization, and trusted audit identity.
2. Correct ports and `.env.example` files; add Membership health/restart configuration.
3. Expose dependency and Kafka readiness/metrics without making transient dependencies kill liveness.

---

# 6. Test evidence retained in the environment

The MySQL volumes intentionally retain evidence from this and earlier investigations. Representative records created through the rebuilt APIs in this run include:

| Evidence | Identifier/result |
|---|---|
| Membership member | `c4ded25f-6ef3-414c-bc4c-9266a8db393e` |
| Silent money rounding | first period stored `12.35` after request `12.3456` |
| Invalid financial data retained | renewal stored `0.00 ZZZ`, `paidAt=1900-01-01` |
| Identical renewal retries | two extra six-month periods with new IDs |
| Impossible loan chronology | `e320ebc3-6be6-4e1f-9a0f-48bf9b9f6adf` |
| Blank-book lost loan | `7c70a409-5626-41ef-981f-da42c3b9f224` |
| Lost fee whose quote fails | `f399a405-c23c-3a7f-8d55-315a544cde15` |
| Paid overdue fee | `434c0dca-bec1-3f4e-b2c4-666986f6c229` |
| Backdated payment | `7f9dcca0-ee6e-4b11-9794-408247eb1654`, `paidAt=1900-01-01` |
| Concurrent-limit member | `02f91a0c-c710-4dda-8ea3-310b7a702996`, six active loans |

Final Borrowing aggregate/projection counts matched (31/31 loans, 21/21 fees, and 4/4 payments), with 64 Axon domain events and zero Axon dead letters at the time of inspection.

The three loan Kafka topics contained only older manually injected records. Borrowing API calls in this investigation did not change their offsets. Those old payloads do not satisfy the current Inventory DTO validation and should not be treated as valid contract examples.

These databases are test evidence, not clean seed data. Preserve anything still needed for diagnosis, then use disposable volumes for regression testing after fixes.

---

# 7. Feign and Kafka connection analysis

This section is intentionally last, as requested. It is based on review of all four service source trees, Compose/configuration, the currently empty frontend template, live service discovery, live AsyncAPI documents, Kafka topics/groups, and the observed API behavior. It identifies missing connections while preserving the system's existing business responsibilities:

- Catalog owns book identity/details/price/lifecycle.
- Inventory owns libraries and physical stock.
- Membership owns members and subscription entitlement.
- Borrowing owns loans, fees, payments, and borrowing bans.

## 7.1 Current connection map

| Producer/caller | Consumer/target | Mechanism | Current state |
|---|---|---|---|
| Inventory | Catalog | Feign `GET /api/books/{id}/available` | Implemented for adding stock |
| Borrowing | Membership | Feign `GET /api/members/{id}/subscription-status` | Implemented, but missing/inactive/outage semantics are weak |
| Borrowing | Inventory | Feign `GET /api/books/{id}/price` | Incorrect owner and nonexistent endpoint |
| Borrowing | Inventory | Availability/reservation connection | Missing |
| Borrowing | Kafka | `loan.created`, `loan.returned`, `loan.marked.lost` | Missing producer |
| Kafka | Inventory | Consumers for the three loan topics | Implemented with external DTO validation/inbox |
| Borrowing | Inventory | Permanent-damage stock transition | Missing topic and consumer |
| Catalog | Kafka | `book.deleted` | Implemented producer |
| Kafka | Inventory | Catalog deletion consumer | Missing |
| Membership | Kafka | Membership/subscription events | No producer and no current required consumer |

## 7.2 Rule for choosing Feign versus Kafka

Use Feign for information that must be known before a synchronous command can be accepted:

- Is the member present and currently eligible?
- Is the requested library active and is a copy available/reservable?
- What is the authoritative replacement price now?

Use Kafka for facts that have already committed and must update another service asynchronously:

- A loan was created for a specific library/book.
- That loan was returned, marked lost, or permanently damaged.
- A Catalog book was retired/deleted.

Do not publish every internal Axon event automatically. External messages need separate stable DTOs and should exist only where another service has a concrete reaction.

## 7.3 Required Feign connections

### A. Borrowing -> Membership: structured eligibility

Replace or strengthen the current Boolean contract. One suitable response is:

```json
{
  "memberId": "uuid",
  "active": true,
  "activeThrough": "2026-12-01T00:00:00Z"
}
```

Contract behavior:

- Missing member: Membership returns `404`; Borrowing returns a stable member-not-found result.
- Existing but inactive: `200` with `active=false`; Borrowing returns `409 MEMBERSHIP_INACTIVE`.
- Membership unavailable/timeout: Borrowing returns `503`; no loan or outbox event is written.
- Active: continue with local limit/ban checks.

A fallback must fail closed; it must never invent active entitlement. Avoid a second separate existence call because it adds latency and a race between reads.

### B. Borrowing -> Inventory: library/book availability

The create request must first include `libraryId`. Inventory should expose a purpose-built contract rather than making Borrowing deserialize the JPA `BookStock` entity, for example:

```text
GET /api/stock/{libraryId}/{bookId}/availability
```

```json
{
  "libraryId": "uuid",
  "bookId": "uuid",
  "libraryActive": true,
  "bookActive": true,
  "availableQuantity": 3,
  "available": true
}
```

Minimum wiring that preserves the current synchronous `201` loan behavior:

1. Borrowing queries this endpoint.
2. It rejects missing/deleted libraries, missing/deleted books, and zero stock.
3. It commits the loan and an outbox record.
4. Inventory consumes `loan.created` and decreases availability.

Important limitation: a read check plus later Kafka mutation is not an atomic reservation. Concurrent borrowers can still see the same last copy, or the event can fail after Borrowing returns success.

For a hard "no loan without a copy" invariant, choose one stronger strategy:

- Synchronous idempotent Inventory reservation before returning `201`, with compensation if Borrowing fails; in this design Inventory must not decrement the same stock again when it receives `loan.created`.
- An event-driven reservation saga (`PENDING_STOCK` -> Inventory reserved/rejected -> `ACTIVE`/`REJECTED`), normally returning `202` until resolved.

Do not combine a stock-mutating synchronous reservation with a second stock-mutating `loan.created` consumer.

### C. Borrowing -> Catalog: replacement price

The current client must target `catalog-service`, not Inventory. Align the schema to one of these contracts:

Preferred:

```json
{ "amount": 12.35, "currency": "USD" }
```

Minimal current-domain alternative:

```json
{ "price": 12.35 }
```

The minimal alternative must explicitly define that every Catalog price uses Borrowing's configured fee currency. The preferred version makes currency ownership unambiguous and prevents accidental cross-currency replacement charges.

Missing/deleted book should be a domain not-found outcome. Catalog outage is `503`; it is not an Inventory error and not a generic `500`.

### D. Existing Inventory -> Catalog availability call

Keep Inventory's current Catalog availability validation for manual stock addition. Strengthen it with explicit timeout/error handling. Once Catalog deletion events maintain an Inventory-side retired reference, decide whether the Feign check remains the source of truth or is only a reconciliation guard; do not let two sources disagree silently.

## 7.4 Required Borrowing-to-Inventory Kafka events

Inventory's current DTOs already define the minimum flat fields for three topics. Borrowing should publish JSON objects, not JSON strings containing escaped JSON. Use `loanId` as the Kafka key so all transitions for one loan remain ordered.

Common minimum fields:

```json
{
  "eventId": "uuid",
  "loanId": "uuid",
  "eventVersion": 1,
  "occurredAt": "2026-09-12T17:20:31Z",
  "libraryId": "uuid",
  "bookId": "uuid"
}
```

`eventVersion` should mean schema version. If consumers also need transition order, add a separate monotonic `aggregateVersion` or `loanSequence`; do not overload one field with both meanings.

| Topic | Publish after | Inventory action |
|---|---|---|
| `loan.created` | Loan and outbox commit | Decrease available quantity by one, unless a synchronous reservation already did so |
| `loan.returned` | Valid ACTIVE -> RETURNED commit | Increase available quantity by one |
| `loan.marked.lost` | Valid ACTIVE -> LOST commit | Decrease total quantity by one; available stays unchanged |
| `loan.marked.damaged` | Valid ACTIVE -> DAMAGED commit | Remove from usable total or move to explicit damaged quantity; available stays unchanged |

Transition-specific timestamps such as `borrowedAt`, `returnedAt`, `declaredLostAt`, and `damageRecordedAt` may be included for audit, but the routing fields above are mandatory.

Borrowing's current later internal events lack `bookId` and `libraryId`. Add those facts to the aggregate and events, or have a dedicated publisher construct the external event from committed loan state. External DTOs must remain separate from Axon event-sourcing classes.

## 7.5 Catalog deletion event

Catalog already publishes `book.deleted`, but Inventory does not consume it. Inventory should consume a versioned deletion/retirement event and retain historical stock while marking the title unavailable for new stock, loans, and transfers.

The current Catalog payload contains only `bookId`. Add at least `eventId`, schema version, occurrence time, and aggregate version. Key it by base book ID. Inventory should process it idempotently.

Borrowing does not need to delete or rewrite historical loans when a book is retired. New-loan validation must reject the retired title through Inventory/Catalog. A Borrowing consumer is needed only if Borrowing deliberately maintains its own local book lifecycle projection.

## 7.6 Membership events: optional, not required for the current behavior

The current system makes a live Feign eligibility decision for every loan. Under that design, no Membership Kafka event is required for correctness. Publishing `member.registered`, name/contact changes, or payment details would add PII and contracts without a current consumer.

If the future goal is for Borrowing to remain eligible-check capable during short Membership outages, then deliberately replace the live decision with a Borrowing-side entitlement projection fed by versioned events such as:

- `membership.subscription.started`
- `membership.subscription.renewed`
- `membership.subscription.revoked`
- `membership.member.deactivated`

That is an architectural choice, not something to run in parallel silently with the live Feign Boolean. Expiry is time-derived from the period end, so the projection must evaluate time locally or consume an explicit expiry event from a reliable scheduler.

## 7.7 Events that should remain internal for now

No current service needs external copies of:

- loan extension;
- overdue/lost/damaged fee creation;
- fee settlement;
- payment recording/allocation;
- borrowing-ban issuance;
- member name/contact updates.

Keep these as internal Axon events until a concrete consumer such as accounting, notifications, or audit is introduced. If one is introduced later, publish privacy-minimized external DTOs rather than internal events.

## 7.8 Reliability requirements for all external events

1. Write Borrowing state and an outbox record in one local transaction.
2. Publish from the outbox with retry and observable backlog.
3. Give every external transition a stable `eventId`.
4. Inventory already has an event-ID inbox; also prevent semantic duplicates such as two different `eventId` values for the same `(loanId, transition)`.
5. Enforce valid per-loan order: return/lost/damaged must not process before a successful create/reservation.
6. Send exhausted poison records to a dead-letter topic with original topic, partition, offset, key, payload, and exception metadata.
7. Validate exact JSON shape with producer/consumer contract tests.
8. Document each real channel in Springwolf from the same constants and DTOs used at runtime.
9. Plan the migration of the current incompatible retained topic records before starting new groups at `earliest`.

## 7.9 Required end-to-end scenarios

### Normal checkout

1. Client supplies member, book, and library.
2. Borrowing asks Membership for structured eligibility.
3. Borrowing checks/reserves Inventory stock according to the selected consistency strategy.
4. Borrowing atomically writes the loan and outbox event.
5. `loan.created` reaches Inventory exactly once semantically.
6. The loan is `ACTIVE` and one copy is unavailable.

### Rejected checkout

- Missing/inactive member, active ban, loan limit, unpaid-fee limit, deleted library, deleted/missing book, or no stock produces no loan and no stock-mutating event.
- Dependency outage produces a retryable `503`, not false eligibility and not generic `500`.

### Return

1. Borrowing validates chronology and the ACTIVE state.
2. It commits RETURNED and `loan.returned` to the outbox.
3. Inventory restores exactly one available copy.
4. Borrowing independently creates an overdue fee when required.
5. Event redelivery does not restore the copy twice.

### Lost or permanently damaged copy

1. Borrowing commits the terminal state and corresponding outbox event.
2. Inventory removes or reclassifies exactly the borrowed copy.
3. Borrowing creates the fee internally.
4. Payment quote retrieves price from Catalog, not Inventory.

### Catalog book retirement

1. Catalog publishes the versioned retirement event.
2. Inventory marks the reference non-circulating but preserves stock/history.
3. New checkout/stock/transfer attempts are rejected.
4. Existing historical loans and fees remain readable.

### Duplicate, out-of-order, and poison Kafka records

- Same `eventId`: acknowledged without a second stock mutation.
- New `eventId` for the same loan transition: detected as a semantic duplicate.
- Return/loss/damage before create: held/retried or dead-lettered according to a documented order policy, never applied blindly.
- Invalid schema/key: dead-lettered with diagnostics and surfaced in readiness/metrics.

## Final assessment

Membership's profile and subscription-period mechanics run and persist, but entitlement can currently be created with unverified zero-value payment data, identical renewal retries extend it repeatedly, and missing/racing requests produce incorrect error behavior.

Borrowing's internal loan/fee/payment workflows are functional in isolation, but it is not yet a safe microservice circulation workflow. It accepts loans without a library, valid book, or stock; exceeds member limits under concurrency; publishes none of the events Inventory is already waiting for; and cannot price replacement fees because the Feign target/schema are wrong. The stock connection, concurrency control, pricing contract, and Membership payment integrity are release-blocking.
