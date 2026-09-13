package com.inventoryservice.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.inventoryservice.infrastructure.kafka.LoanEventTranslator
import com.inventoryservice.infrastructure.kafka.InventoryTopics
import com.inventoryservice.model.valueObject.dto.LoanCreatedEventDTO
import com.inventoryservice.model.valueObject.dto.LoanEventDTO
import com.inventoryservice.model.valueObject.dto.LoanMarkedLostEventDTO
import com.inventoryservice.model.valueObject.dto.LoanReturnedEventDTO
import com.inventoryservice.model.valueObject.dto.LoanMarkedDamagedEventDTO
import com.inventoryservice.service.LoanEventInboxService
import com.inventoryservice.service.LibraryService
import com.inventoryservice.service.CatalogBookRetirementService
import com.inventoryservice.model.valueObject.dto.BookDeletedIntegrationEventDTO
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaEventConsumer(
    private val objectMapper: ObjectMapper,
    private val loanEventTranslator: LoanEventTranslator,
    private val libraryService: LibraryService,
    private val loanEventInboxService: LoanEventInboxService,
    private val catalogBookRetirementService: CatalogBookRetirementService
) {

    @KafkaListener(
        topics = [InventoryTopics.LOAN_CREATED],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun handleLoanCreated(record: ConsumerRecord<String, String>) {
        process(record, InventoryTopics.LOAN_CREATED, LoanCreatedEventDTO::class.java) { event ->
            libraryService.borrowBookStock(loanEventTranslator.translate(event)).join()
        }
    }

    @KafkaListener(
        topics = [InventoryTopics.LOAN_RETURNED],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun handleLoanReturned(record: ConsumerRecord<String, String>) {
        process(record, InventoryTopics.LOAN_RETURNED, LoanReturnedEventDTO::class.java) { event ->
            libraryService.returnBookStock(loanEventTranslator.translate(event)).join()
        }
    }

    @KafkaListener(
        topics = [InventoryTopics.LOAN_MARKED_LOST],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun handleLoanMarkedLost(record: ConsumerRecord<String, String>) {
        process(record, InventoryTopics.LOAN_MARKED_LOST, LoanMarkedLostEventDTO::class.java) { event ->
            libraryService.markBookStockLost(loanEventTranslator.translate(event)).join()
        }
    }

    @KafkaListener(
        topics = [InventoryTopics.LOAN_MARKED_DAMAGED],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun handleLoanMarkedDamaged(record: ConsumerRecord<String, String>) {
        process(record, InventoryTopics.LOAN_MARKED_DAMAGED, LoanMarkedDamagedEventDTO::class.java) { event ->
            libraryService.markBookStockDamaged(loanEventTranslator.translate(event)).join()
        }
    }

    @KafkaListener(
        topics = [InventoryTopics.BOOK_DELETED],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun handleBookDeleted(record: ConsumerRecord<String, String>) {
        val event = objectMapper.readValue(record.value(), BookDeletedIntegrationEventDTO::class.java)
        event.validate()
        require(record.key() == event.bookId) { "Kafka record key must equal bookId '${event.bookId}'" }
        catalogBookRetirementService.process(event)
    }

    private fun <T : LoanEventDTO> process(
        record: ConsumerRecord<String, String>,
        eventType: String,
        eventClass: Class<T>,
        action: (T) -> Unit
    ) {
        val event = objectMapper.readValue(record.value(), eventClass)
        event.validate()

        require(record.key() == event.loanId) {
            "Kafka record key must equal loanId '${event.loanId}' for ordered loan processing"
        }

        loanEventInboxService.process(eventType, event) {
            action(event)
        }
    }

}
