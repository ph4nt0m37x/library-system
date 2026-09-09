# Read Model Design Comparison

## Catalog Service design

The Catalog Service uses `Book` and `BookView` as separate Kotlin classes, but both are mapped to the same `book` database table.

```text
Command
  -> Book aggregate
  -> GenericJpaRepository writes to the book table

Query
  -> BookViewReadService
  -> BookViewRepository
  -> reads the book table as BookView
```

This provides code-level separation between commands and queries, but it is not a true event-driven projection. `BookView` is not created or updated by catalog event handlers. It simply reads the persisted aggregate state from the same table.

### Advantages

- Simple implementation.
- Reads immediately see committed aggregate changes.
- Fewer tables and infrastructure components.
- Appropriate for straightforward CRUD use cases.

### Disadvantages

- The read model is coupled to the aggregate database schema.
- The view cannot be rebuilt independently from stored events.
- Read-oriented schema changes can affect the command model.
- It provides limited CQRS separation.

## Membership Service design

The Membership Service uses dedicated projection tables:

- `member_view`
- `subscription_period_view`

Internal membership events are persisted in the Axon Event Store. A tracking event processor reads those events and invokes `MemberEventHandler`, which updates the projection tables.

```text
Command
  -> Member aggregate
  -> event stored in domain_event_entry
  -> tracking event processor
  -> MemberEventHandler
  -> member_view and subscription_period_view

Query
  -> MemberViewReadService
  -> MemberViewRepository and SubscriptionPeriodViewRepository
  -> combined MemberResponse
```

### Advantages

- Clear separation between command and query models.
- Projections can be rebuilt by replaying stored events.
- Query schemas can evolve independently from aggregate persistence.
- Subscription, renewal, tier, and payment history is preserved naturally.
- The member response can combine profile data, active status, the current subscription, and complete subscription history.
- Historical subscription dates remain unchanged if tier configuration changes later.

### Disadvantages

- More tables and infrastructure components are required.
- Projection handlers must be idempotent and replay-safe.
- Tracking event processors continuously poll the Event Store.
- Read models are eventually consistent: immediately after a successful command, a query may briefly return the previous projection state.

## Comparison

| Concern | Catalog shared-table view | Membership event-driven projection |
|---|---|---|
| Implementation complexity | Lower | Higher |
| Immediate read consistency | Yes | Usually eventual |
| Separate read schema | No | Yes |
| Rebuildable from events | No | Yes |
| Historical data support | Limited by aggregate table | Natural fit |
| Independent query evolution | Limited | Strong |
| Background event processor | Not required | Required |

## Recommendation

The event-driven projection design is the better choice for the Membership Service because membership contains historical subscription periods, payments, renewals, tier changes, and time-dependent active status.

The Catalog Service's shared-table design remains adequate for simple CRUD behavior, but `BookView` is effectively another JPA representation of aggregate state rather than an independently maintained CQRS projection.

Keep the Membership Service's dedicated `member_view` and `subscription_period_view` projections. If architectural consistency across services becomes important, the Catalog Service can later be upgraded to use its own event-driven `BookView` projection.

If immediate query consistency becomes a strict requirement, event processor configuration should be evaluated deliberately. A subscribing processor can update projections in the command publication thread, while the current tracking processor provides stronger decoupling, recovery, and replay support.
