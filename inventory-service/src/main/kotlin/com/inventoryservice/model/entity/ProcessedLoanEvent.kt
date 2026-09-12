package com.inventoryservice.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "inventory_event_inbox",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_inventory_event_inbox_event_id",
            columnNames = ["event_id"]
        )
    ],
    indexes = [
        Index(
            name = "idx_inventory_event_inbox_loan_id",
            columnList = "loan_id"
        )
    ]
)
class ProcessedLoanEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "event_id", nullable = false, updatable = false, length = 36)
    var eventId: String,

    @Column(name = "loan_id", nullable = false, updatable = false, length = 100)
    var loanId: String,

    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    var eventType: String,

    @Column(name = "event_version", nullable = false, updatable = false)
    var eventVersion: Int,

    @Column(name = "occurred_at", nullable = false, updatable = false)
    var occurredAt: Instant,

    @Column(name = "processed_at", nullable = false, updatable = false)
    var processedAt: Instant
)
