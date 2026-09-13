# Borrowing Service Operations

Start the root Compose project first so Kafka, Consul, and the external `shared_net` exist. Then start Membership, Catalog, Inventory, and Borrowing. Separate Compose projects cannot express cross-project `depends_on`; the clients and outbox therefore use bounded calls/retries and fail closed.

Borrowing listens on `8090`. Its MySQL server stays on container port `3306` and is mapped to host port `3308`. The application datasource is `jdbc:mysql://mysqldb:3306/borrowing_service_db`.

The actuator health endpoint is used for process health. Outbox publication is observable at `/actuator/metrics/borrowing.outbox.pending` and `/actuator/metrics/borrowing.outbox.dead.lettered`. A non-zero pending count during a Kafka outage is expected; dead-lettered records require operator reconciliation.

Inventory consumers retry failed or out-of-order records and publish exhausted records to `<original-topic>.DLT` with Spring Kafka's original-record and exception headers. Before deploying a new consumer group with `earliest`, remove or migrate incompatible pre-versioned development records.
