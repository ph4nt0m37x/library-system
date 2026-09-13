package com.catalogservice.infrastructure.kafka

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

@Entity
@Table(
    name = "integration_outbox",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_catalog_outbox_transition", columnNames = ["aggregate_id", "event_type"])
    ],
    indexes = [Index(name = "idx_catalog_outbox_pending", columnList = "published_at, created_at")]
)
class CatalogOutboxEvent(
    @Id
    @Column(name = "event_id", length = 36, nullable = false, updatable = false)
    var eventId: String,

    @Column(name = "aggregate_id", length = 100, nullable = false, updatable = false)
    var aggregateId: String,

    @Column(name = "event_type", length = 100, nullable = false, updatable = false)
    var eventType: String,

    @Column(name = "topic_name", length = 100, nullable = false, updatable = false)
    var topic: String,

    @Column(name = "record_key", length = 100, nullable = false, updatable = false)
    var recordKey: String,

    @Lob
    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "LONGTEXT")
    var payload: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,

    @Column(name = "published_at")
    var publishedAt: Instant? = null,

    @Column(name = "dead_lettered_at")
    var deadLetteredAt: Instant? = null,

    @Column(name = "publish_attempts", nullable = false)
    var publishAttempts: Int = 0,

    @Column(name = "last_error", length = 1000)
    var lastError: String? = null
)

interface CatalogIntegrationOutbox : JpaRepository<CatalogOutboxEvent, String> {
    fun findTop50ByPublishedAtIsNullAndDeadLetteredAtIsNullOrderByCreatedAtAsc(): List<CatalogOutboxEvent>
    fun countByPublishedAtIsNullAndDeadLetteredAtIsNull(): Long
    fun countByDeadLetteredAtIsNotNull(): Long
}
