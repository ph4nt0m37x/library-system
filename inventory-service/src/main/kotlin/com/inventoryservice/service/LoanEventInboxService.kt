package com.inventoryservice.service

import com.inventoryservice.model.valueObject.dto.LoanEventDTO
import com.inventoryservice.repository.ProcessedLoanEventRepository
import com.inventoryservice.repository.LoanInventoryStateRepository
import com.inventoryservice.model.entity.ProcessedLoanEvent
import com.inventoryservice.model.entity.LoanInventoryState
import com.inventoryservice.model.entity.InventoryLoanStatus
import java.time.Clock
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoanEventInboxService(
    private val processedLoanEventRepository: ProcessedLoanEventRepository,
    private val loanInventoryStateRepository: LoanInventoryStateRepository,
    private val clock: Clock
) {

    /**
     * Reserves the external event ID and applies its stock change in one database
     * transaction. A Kafka redelivery returns normally when the ID already exists.
     */
    @Transactional
    fun process(eventType: String, event: LoanEventDTO, action: () -> Unit): Boolean {
        event.validate()
        require(eventType.isNotBlank() && eventType.length <= 100) {
            "eventType must contain between 1 and 100 characters"
        }

        if (
            processedLoanEventRepository.existsByEventId(event.eventId) ||
            processedLoanEventRepository.existsByLoanIdAndEventType(event.loanId, eventType)
        ) {
            return false
        }

        val state = loanInventoryStateRepository.findById(event.loanId).orElse(null)
        val nextStatus = statusFor(eventType)

        if (nextStatus == InventoryLoanStatus.BORROWED) {
            require(state == null) { "Loan ${event.loanId} already has Inventory state" }
        } else {
            require(state != null) { "Loan ${event.loanId} transition $eventType arrived before loan.created" }
            require(state.status == InventoryLoanStatus.BORROWED) {
                "Loan ${event.loanId} is ${state.status} and cannot transition through $eventType"
            }
            require(state.libraryId == event.libraryId && state.bookId == event.bookId) {
                "Loan ${event.loanId} routing fields do not match its created event"
            }
            require(event.aggregateVersion > state.lastAggregateVersion) {
                "Loan ${event.loanId} aggregateVersion must increase"
            }
        }

        action()

        if (state == null) {
            loanInventoryStateRepository.save(
                LoanInventoryState(
                    loanId = event.loanId,
                    libraryId = event.libraryId,
                    bookId = event.bookId,
                    status = nextStatus,
                    lastAggregateVersion = event.aggregateVersion
                )
            )
        } else {
            state.status = nextStatus
            state.lastAggregateVersion = event.aggregateVersion
            loanInventoryStateRepository.save(state)
        }

        processedLoanEventRepository.save(
            ProcessedLoanEvent(
                eventId = event.eventId,
                loanId = event.loanId,
                idempotencyKey = event.idempotencyKey,
                eventType = eventType,
                eventVersion = event.eventVersion,
                aggregateVersion = event.aggregateVersion,
                occurredAt = event.occurredAt,
                processedAt = Instant.now(clock)
            )
        )
        return true
    }

    private fun statusFor(eventType: String): InventoryLoanStatus = when (eventType) {
        "loan.created" -> InventoryLoanStatus.BORROWED
        "loan.returned" -> InventoryLoanStatus.RETURNED
        "loan.marked.lost" -> InventoryLoanStatus.LOST
        "loan.marked.damaged" -> InventoryLoanStatus.DAMAGED
        else -> throw IllegalArgumentException("Unsupported loan event type: $eventType")
    }
}
