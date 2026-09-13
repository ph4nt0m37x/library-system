package com.borrowingservice.integration

import com.borrowingservice.model.event.LoanCreatedEvent
import com.borrowingservice.model.event.LoanMarkedDamagedEvent
import com.borrowingservice.model.event.LoanMarkedLostEvent
import com.borrowingservice.model.event.LoanReturnedEvent
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
@ProcessingGroup("borrowing-integration-outbox")
class LoanIntegrationOutboxHandler(
    private val outboxRepository: IntegrationOutboxRepository,
    private val objectMapper: ObjectMapper
) {
    @EventHandler
    @Transactional
    fun on(event: LoanCreatedEvent, @SequenceNumber aggregateVersion: Long, @Timestamp occurredAt: Instant) {
        val libraryId = requireNotNull(event.libraryId) { "A new loan must contain libraryId" }
        enqueue(
            LoanTopics.CREATED,
            event.loanId,
            LoanCreatedIntegrationEvent(
                eventId = eventId(event.loanId, LoanTopics.CREATED, aggregateVersion),
                loanId = event.loanId,
                aggregateVersion = aggregateVersion,
                occurredAt = occurredAt,
                libraryId = libraryId,
                bookId = event.bookId,
                idempotencyKey = event.idempotencyKey,
                borrowedAt = event.borrowedAt.toInstant()
            )
        )
    }

    @EventHandler
    @Transactional
    fun on(event: LoanReturnedEvent, @SequenceNumber aggregateVersion: Long, @Timestamp occurredAt: Instant) {
        enqueue(
            LoanTopics.RETURNED,
            event.loanId,
            LoanReturnedIntegrationEvent(
                eventId = eventId(event.loanId, LoanTopics.RETURNED, aggregateVersion),
                loanId = event.loanId,
                aggregateVersion = aggregateVersion,
                occurredAt = occurredAt,
                libraryId = requireNotNull(event.libraryId) { "A returned loan must contain libraryId" },
                bookId = requireNotNull(event.bookId) { "A returned loan must contain bookId" },
                idempotencyKey = event.idempotencyKey,
                returnedAt = event.returnedAt.toInstant()
            )
        )
    }

    @EventHandler
    @Transactional
    fun on(event: LoanMarkedLostEvent, @SequenceNumber aggregateVersion: Long, @Timestamp occurredAt: Instant) {
        enqueue(
            LoanTopics.MARKED_LOST,
            event.loanId,
            LoanMarkedLostIntegrationEvent(
                eventId = eventId(event.loanId, LoanTopics.MARKED_LOST, aggregateVersion),
                loanId = event.loanId,
                aggregateVersion = aggregateVersion,
                occurredAt = occurredAt,
                libraryId = requireNotNull(event.libraryId) { "A lost loan must contain libraryId" },
                bookId = requireNotNull(event.bookId) { "A lost loan must contain bookId" },
                idempotencyKey = event.idempotencyKey,
                declaredLostAt = event.declaredLostAt.toInstant()
            )
        )
    }

    @EventHandler
    @Transactional
    fun on(event: LoanMarkedDamagedEvent, @SequenceNumber aggregateVersion: Long, @Timestamp occurredAt: Instant) {
        enqueue(
            LoanTopics.MARKED_DAMAGED,
            event.loanId,
            LoanMarkedDamagedIntegrationEvent(
                eventId = eventId(event.loanId, LoanTopics.MARKED_DAMAGED, aggregateVersion),
                loanId = event.loanId,
                aggregateVersion = aggregateVersion,
                occurredAt = occurredAt,
                libraryId = requireNotNull(event.libraryId) { "A damaged loan must contain libraryId" },
                bookId = requireNotNull(event.bookId) { "A damaged loan must contain bookId" },
                idempotencyKey = event.idempotencyKey,
                damageRecordedAt = event.damageRecordedAt.toInstant()
            )
        )
    }

    private fun enqueue(topic: String, loanId: String, event: LoanIntegrationEvent) {
        if (outboxRepository.existsById(event.eventId)) return
        outboxRepository.save(
            IntegrationOutboxEvent(
                eventId = event.eventId,
                aggregateId = loanId,
                eventType = topic,
                topic = topic,
                recordKey = loanId,
                payload = objectMapper.writeValueAsString(event),
                createdAt = event.occurredAt
            )
        )
    }

    private fun eventId(loanId: String, topic: String, aggregateVersion: Long): String =
        UUID.nameUUIDFromBytes(
            "borrowing:$topic:$loanId:$aggregateVersion".toByteArray(StandardCharsets.UTF_8)
        ).toString()
}
