# Borrowing Service Final Testing Report

Tested on 2026-09-10 against the running Docker deployment at `http://localhost:8090` and the MySQL database `borrowing_service_db`.

## Final result

**PASS.** The focused borrowing-service flows behaved correctly through the real HTTP endpoints, and every successful command produced matching aggregate and read-model data in MySQL. After the expanded extension, multi-fee-payment, and tier-1-ban phase, the database contains 30 borrowing domain events and zero dead letters.

No borrowing-service source defect was found, so no production-code fix was required. The generated test records were intentionally left in the database so the results remain visible.

## Scope and test plan

The test was deliberately practical and moderate. It covered normal use, common client mistakes, and database persistence without stress, concurrency, or high-volume request generation.

| Phase | Practical checks | Result |
|---|---|---|
| 1. Readiness | Container health, empty starting lists, MySQL schema | Pass |
| 2. Eligibility | Reject a loan for a member without an active subscription | Pass |
| 3. Normal loan | Create, read, list by member, extend once, return on time | Pass |
| 4. Extension boundaries | Reject extension exactly at `dueAt`, confirm rollback, accept one second before it | Pass |
| 5. State validation | Reject a second extension and a second return | Pass |
| 6. Overdue fees | Return loans two, three, and four days late and create one fee per loan | Pass |
| 7. Fee payments | Pay one fee and pay two fees together with distinct allocations | Pass |
| 8. Payment validation/retry | Reject duplicate fee selection, wrong currency/amount, paid-fee quote; verify retry | Pass |
| 9. Other terminal states | Declare one book lost and record permanent damage | Pass |
| 10. Tier-1 ban | Trigger the tenth damage, read the ban, reject a new loan while it is active | Pass |
| 11. Read behavior | List/filter data and return `404` for missing resources | Pass |
| 12. Persistence audit | Compare command tables, projection tables, event store, dead letters | Pass |
| 13. Regression | Run the borrowing Maven tests | Pass: 4 tests |

## Environment and test data

- Borrowing application: `borrowing-service_app`, port `8090`, health `UP`.
- Borrowing database: `borrowing_service_db` in `borrowing_service_db`, port `3308`.
- Supporting discovery and messaging containers were running: Consul and Kafka.
- The borrowing service requires membership subscription status during loan creation. Because the membership database started empty, clearly named active-member fixtures were inserted into its read model:
  - `borrowing-http-test-member`
  - `borrowing-extension-test-member`
  - `borrowing-payment-test-member`
  - `borrowing-ban-test-member`
  - persisted member IDs use the `MemberId:` prefix and all subscriptions run from 2026-01-01 through 2027-01-01
- Only borrowing-service HTTP endpoints were used for the business tests. The membership records were dependency setup, not a membership-service test.

The ban test also uses nine paid historical `DAMAGED` loan/fee pairs for `borrowing-ban-test-member`. They were inserted into both borrowing aggregate and projection tables as threshold setup. The tenth loan, tenth damage fee, tier-1 issuance, reads, and blocked follow-up loan were all exercised through the borrowing HTTP API. The nine history fixtures intentionally have no Axon event-store entries; they represent pre-existing history rather than events produced in this run.

The first version of the dependency fixture used an unprefixed ID and was correctly reported as inactive. Inspection showed that membership value-object IDs are persisted with `MemberId:` and `SubscriptionId:` prefixes. Correcting the fixture made the membership status active. This was a test-data setup correction, not a borrowing-service code problem.

## How the service behaves

Borrowing uses a command/event model with separate read projections:

```text
HTTP command -> aggregate table + domain event -> tracking projection -> HTTP read model

late return -> OVERDUE fee -> payment quote -> payment + allocation -> fee settlement
lost book   -> LOST fee    -> replacement amount is calculated when quoted
damage      -> DAMAGED fee -> cumulative damage may eventually issue a ban
```

Successful command endpoints return after command handling, while GET endpoints read tracking projections. A projection can therefore lag a command briefly. During this test, reads were checked after a short wait of approximately 0.75-1 second.

### Loan rules observed

- A loan is created as `ACTIVE` with `dueAt = borrowedAt + 14 days`.
- An active loan can be extended once, before its due date; the extension adds another 14 days.
- Returning on or before the effective due date produces no fee.
- Returning after the due date produces one deterministic `OVERDUE` fee.
- Lost and permanently damaged transitions are terminal and each produces one corresponding fee.
- A second terminal transition is rejected because the loan is no longer `ACTIVE`.

### Fee and payment rules observed

- A three-day overdue period at USD 0.50 per started 24-hour period quoted as `USD 1.50`.
- Payment recording recomputes the quote and requires the exact amount.
- Recording a payment creates one payment allocation per selected fee and settles the fee.
- Reusing the same payment ID with identical fields is idempotent: the endpoint returns success and no second event or row is written.
- A paid fee cannot be quoted again.
- The original lost and damaged fees remain unpaid in this dataset. Their replacement amounts require the inventory-service book-price contract and are calculated at quote time, not when the fee row is created.

### Ban rules observed

- A single permanent-damage fee for the original test member did not create a ban.
- The tenth cumulative permanent-damage fee for the dedicated ban member created `TIER_1`.
- The tier starts immediately, remains active for one month, and reports `permanentlyBanned = false`.
- While tier 1 is active, a new loan is rejected with `409 MEMBER_TEMPORARILY_BANNED`.
- The rejected create wrote neither a loan row nor a domain event.

## HTTP results

### Normal extended/on-time loan

Loan ID: `f724e47c-d85d-4ec7-836f-61f242679d75`

| Request | Expected/observed behavior |
|---|---|
| `POST /api/loans/create` | `201 Created`; returned ID and `Location` header |
| `GET /api/loans/{id}` | `200`; `ACTIVE`, due 2026-09-15T10:00:00Z |
| `POST /api/loans/{id}/extend` | `200`; due moved to 2026-09-29T10:00:00Z |
| Repeat extension | `400`; `A Loan can be extended only once` |
| `POST /api/loans/{id}/return` | `200`; status became `RETURNED` |
| Repeat return | `400`; `Only an ACTIVE Loan can be returned` |
| Fee query after return | No fee, because return was before the extended due date |

### Overdue loan and payment

- Loan ID: `8aa189b4-e139-409d-9dc5-83d449f7145e`
- Fee ID: `5742a1a1-3e26-371c-b519-c67396219d51`
- Payment ID: `ca8383d6-c017-42c7-b5fc-256a5624acf6`
- Allocation ID: `275a5c65-6601-3a67-b578-fe9e1b9767d6`

| Request | Expected/observed behavior |
|---|---|
| Create loan at 2026-08-01T10:00:00Z | `201`; due date 2026-08-15T10:00:00Z |
| Return at 2026-08-18T10:00:00Z | `200`; status `RETURNED` |
| Read member fees | One `OVERDUE`, `UNPAID` fee |
| `POST /api/payments/quote` | `200`; currency normalized from `usd` to `USD`, total `1.50` |
| Record amount `1.00` | `400`; amount did not equal quoted `1.50` |
| Record amount `1.50` | `201`; payment and allocation created |
| Read fee after payment | `PAID`, settlement/payment/allocation IDs populated |
| Repeat identical payment | `201`; idempotent, still one payment/event/allocation |
| Quote the paid fee again | `400`; fee is not `UNPAID` |

### Lost and damaged loans

| Scenario | Loan ID | Fee ID | Final loan | Fee |
|---|---|---|---|---|
| Lost | `164dd7c3-fdba-4266-a842-266884688d6a` | `71040bbc-1a9f-3bd5-a9b2-7588a022c476` | `LOST` | `LOST`, `UNPAID` |
| Permanent damage | `1ddd62cd-ca1a-423d-8e57-d39fecb2cee3` | `d8dc1075-3192-3f8f-999c-95ae974417a9` | `DAMAGED` | `DAMAGED`, `UNPAID` |

Both aggregate and projection rows contain the correct incident timestamps.

### Extension cutoff boundary

Loan ID: `6f8dc0d4-2a49-4723-9834-5aa1367db0cc`

| Request/check | Observed behavior |
|---|---|
| Create at 2026-09-01T10:00:00Z | `201`; initial due date 2026-09-15T10:00:00Z |
| Extend exactly at `dueAt` | `400`; `A Loan cannot be extended at or after its dueAt` |
| Inspect after rejection | Loan remained `ACTIVE`, `extendedAt` remained null, event count remained 1 |
| Extend one second before `dueAt` | `200`; due date moved to 2026-09-29T10:00:00Z |
| Return on 2026-09-20 | `200`; final status `RETURNED`, no fee created |

This confirms that the cutoff is exclusive and a rejected command does not partially mutate the aggregate or event store.

### Multiple-fee payment

- Member: `borrowing-payment-test-member`
- Payment ID: `3f098458-ce0e-4b19-a67d-9cb488a6236f`
- Two-day fee: `9d868423-354d-3a8f-aa05-d58bc042037e`, quoted at USD 1.00.
- Four-day fee: `bc41e19f-985a-339c-8c8b-3281cfbb15ef`, quoted at USD 2.00.

| Request/check | Observed behavior |
|---|---|
| Quote the same fee ID twice | `400`; `A Fee may be selected only once` |
| Quote the USD fees as EUR | `400`; currency mismatch reported |
| Quote both fees as `usd` | `200`; normalized to `USD`, total USD 3.00 |
| Record USD 3.00 payment | `201`; one payment with two allocations |
| Read member unpaid fees | Empty after settlement |
| Inspect MySQL | Both fee rows are `PAID` and reference the correct payment/allocation/amount |

The allocations were persisted independently as USD 1.00 and USD 2.00, and their total matches the payment.

### Tier-1 ban boundary

- Member: `borrowing-ban-test-member`
- Live tenth loan: `6b3729e0-df14-477f-91b2-68fe5dc55cc3`
- Triggering fee: `266842a8-29af-3b52-88a4-d3b71c257b2b`
- Ban record: `aea2ef6b-6a7b-3069-bc0a-bb41d5d90ade`
- Ban period: `b81eed9c-6024-3827-b277-941b42b83d7c`

| Request/check | Observed behavior |
|---|---|
| Create the tenth loan | `201` |
| Record permanent damage | `200`; created the tenth `DAMAGED` fee and issued tier 1 |
| Get ban by member and record ID | `200`; both returned the same record |
| Ban state | `active = true`, tier `TIER_1`, not permanent |
| Ban period | 2026-09-10T20:13:45.266734Z through 2026-10-10T20:13:45.266734Z |
| Try another loan while banned | `409 MEMBER_TEMPORARILY_BANNED` |
| Counts around rejected create | Member stayed at 10 loans; live loan-event count stayed unchanged |

Aggregate and projection ban records and periods match on IDs, tier, dates, trigger fee, and reason `REPEATED_PERMANENT_BOOK_DAMAGE`.

### Read and not-found behavior

- `/api/loans/all`: 17 loans, including nine explicit ban-history fixtures.
- `/api/loans/member/borrowing-http-test-member`: four original-scenario loans.
- `/api/loans/member/borrowing-http-test-member/active`: zero after all transitions.
- `/api/fees/all`: 15 fees, including nine explicit ban-history fixtures.
- `/api/fees/member/borrowing-http-test-member/unpaid`: two unpaid fees after paying the overdue fee.
- `/api/fees/member/borrowing-payment-test-member/unpaid`: zero after the multi-fee payment.
- `/api/payments/all`: two payments.
- `/api/borrowing-bans/all`: one tier-1 record.
- Unknown loan, fee, payment, and ban IDs each returned `404`.
- Loan creation for `missing-member` returned `409` with `MEMBERSHIP_INACTIVE` and created no borrowing data.

## Database evidence

Final table counts:

| Domain data | Aggregate/command table | Projection/read table | Match |
|---|---:|---:|---|
| Loans | 17 | 17 | Yes |
| Fees | 15 | 15 | Yes |
| Payments | 2 | 2 | Yes |
| Payment allocations | 3 | 3 | Yes |
| Borrowing-ban records | 1 | 1 | Yes |
| Borrowing-ban periods | 1 | 1 | Yes |

Additional event-store checks:

| Check | Result |
|---|---:|
| Loan events | 18 |
| Fee events | 9 |
| Payment events | 2 |
| Borrowing-ban events | 1 |
| Total domain events | 30 |
| Dead letters | 0 |
| Recent application `ERROR` log entries | 0 |

Final aggregate state distribution:

- Loans: five `RETURNED`, one `LOST`, and eleven `DAMAGED` (nine are threshold fixtures).
- Fees: three paid `OVERDUE`, nine paid fixture `DAMAGED`, two unpaid `DAMAGED`, and one unpaid `LOST`.
- Payments: USD 1.50 with one allocation and USD 3.00 with two allocations.
- Borrowing bans: one active, temporary `TIER_1` record with one period.

The `loans`/`loan_view`, `fees`/`fee_view`, `payments`/`payment_view`, allocation, ban-record, and ban-period rows matched on identifiers, statuses, tiers, timestamps, amounts, and settlement references.

The event-store total describes operations actually executed through the application. It intentionally excludes the nine paired aggregate/projection damage-history fixtures used to establish the ban boundary.

## Regression result

Command run from `borrowing-service`:

```powershell
.\mvnw.cmd test
```

Result: 4 tests, 0 failures, 0 errors, 0 skipped.

- Application context: 1 test.
- Loan active-limit command handler: 2 tests.
- Loan REST request mapping: 1 test.

The JDK printed a Mockito dynamic-agent deprecation warning; it did not affect the test result.

## Useful database inspection query

Run this inside `borrowing_service_db` after connecting to `borrowing_service_db`:

```sql
SELECT loan_id, book_id, due_at, returned_at, status FROM loans ORDER BY borrowed_at;
SELECT fee_id, loan_id, reason, status, settled_by_payment_id, settlement_amount FROM fees;
SELECT payment_id, member_id, amount, currency, paid_at FROM payments;
SELECT allocation_id, payment_id, fee_id, amount FROM payment_allocations;
SELECT * FROM borrowing_ban_records;
SELECT * FROM ban_periods;

SELECT
  (SELECT COUNT(*) FROM loans) AS loans,
  (SELECT COUNT(*) FROM loan_view) AS loan_views,
  (SELECT COUNT(*) FROM fees) AS fees,
  (SELECT COUNT(*) FROM fee_view) AS fee_views,
  (SELECT COUNT(*) FROM payments) AS payments,
  (SELECT COUNT(*) FROM payment_view) AS payment_views,
  (SELECT COUNT(*) FROM payment_allocations) AS allocations,
  (SELECT COUNT(*) FROM payment_allocation_view) AS allocation_views,
  (SELECT COUNT(*) FROM borrowing_ban_records) AS bans,
  (SELECT COUNT(*) FROM borrowing_ban_record_view) AS ban_views,
  (SELECT COUNT(*) FROM domain_event_entry) AS events,
  (SELECT COUNT(*) FROM dead_letter_entry) AS dead_letters;
```

## Intentionally excluded

These checks were outside the requested moderate borrowing-only pass:

- Stress, concurrency, and race-condition testing.
- Creating 5 active loans solely to retest the configured active-loan boundary; focused unit tests already cover its fifth/sixth-loan behavior.
- Tier 2 and permanent-ban thresholds at 20 and 30 cumulative damage incidents. Tier 1 at 10 was tested.
- Ban expiry after one month, because application time was not manipulated in this practical run.
- Replacement-price quote/payment for lost or damaged fees because the inventory-service price endpoint was not running; testing that external contract would expand beyond the borrowing-only setup.
- Kafka publication/consumption, which is not implemented by the borrowing service.

## Conclusion

For the tested scope, the borrowing service is ready: HTTP responses are appropriate, extension boundaries roll back cleanly, single- and multi-fee payments follow the configured rules, retries are safe, tier-1 damage escalation blocks borrowing as designed, projections catch up correctly, and persisted aggregate/read data is consistent. The retained MySQL rows listed above are the final test evidence.
