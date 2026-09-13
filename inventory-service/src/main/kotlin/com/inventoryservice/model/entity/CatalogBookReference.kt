package com.inventoryservice.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "catalog_book_reference")
class CatalogBookReference(
    @Id
    @Column(name = "book_id", length = 100, nullable = false, updatable = false)
    var bookId: String,

    @Column(name = "retired", nullable = false)
    var retired: Boolean,

    @Column(name = "retired_at", nullable = false)
    var retiredAt: Instant,

    @Column(name = "catalog_aggregate_version", nullable = false)
    var catalogAggregateVersion: Long,

    @Column(name = "event_id", length = 36, nullable = false, unique = true)
    var eventId: String
)
