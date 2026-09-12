package com.inventoryservice.service

import com.inventoryservice.model.valueObject.dto.LoanEventDTO
import com.inventoryservice.repository.ProcessedLoanEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoanEventInboxService(
    private val processedLoanEventRepository: ProcessedLoanEventRepository
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

        val reserved = processedLoanEventRepository.insertIfAbsent(
            eventId = event.eventId,
            loanId = event.loanId,
            idempotencyKey = event.idempotencyKey,
            eventType = eventType,
            eventVersion = event.eventVersion,
            occurredAt = event.occurredAt
        )

        if (reserved == 0) {
            return false
        }

        action()
        return true
    }
}
