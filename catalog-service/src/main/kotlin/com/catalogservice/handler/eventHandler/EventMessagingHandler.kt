package com.catalogservice.handler.eventHandler

import com.catalogservice.model.event.BookDeletedEvent
import com.catalogservice.infrastructure.kafka.CatalogIntegrationOutbox
import com.catalogservice.infrastructure.kafka.CatalogOutboxEvent
import com.catalogservice.infrastructure.kafka.CatalogTopics
import com.catalogservice.model.event.BookDeletedExternalEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.eventhandling.Timestamp
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Component
@ProcessingGroup("catalog-integration-outbox")
class EventMessagingEventHandler(
    private val outbox: CatalogIntegrationOutbox,
    private val objectMapper: ObjectMapper
) {

    @EventHandler
    @Transactional
    fun on(event: BookDeletedEvent, @SequenceNumber aggregateVersion: Long, @Timestamp occurredAt: Instant) {
        // Hibernate may hydrate legacy embedded IDs with either the raw or the
        // prefixed representation, so external contracts always normalize it.
        val bookId = event.id.value.substringAfter(":")
        val eventId = UUID.nameUUIDFromBytes(
            "catalog:${CatalogTopics.BOOK_DELETED}:$bookId:$aggregateVersion".toByteArray(StandardCharsets.UTF_8)
        ).toString()
        val externalEvent = BookDeletedExternalEvent(
            eventId = eventId,
            bookId = bookId,
            aggregateVersion = aggregateVersion,
            occurredAt = occurredAt
        )
        outbox.save(
            CatalogOutboxEvent(
                eventId = eventId,
                aggregateId = bookId,
                eventType = CatalogTopics.BOOK_DELETED,
                topic = CatalogTopics.BOOK_DELETED,
                recordKey = bookId,
                payload = objectMapper.writeValueAsString(externalEvent),
                createdAt = occurredAt
            )
        )
    }
}
