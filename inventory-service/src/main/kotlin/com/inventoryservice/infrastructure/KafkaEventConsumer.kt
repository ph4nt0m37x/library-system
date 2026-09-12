package com.inventoryservice.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.inventoryservice.infrastructure.kafka.LoanEventTranslator
import com.inventoryservice.model.valueObject.dto.LoanCreatedEventDTO
import com.inventoryservice.model.valueObject.dto.LoanEventDTO
import com.inventoryservice.model.valueObject.dto.LoanMarkedLostEventDTO
import com.inventoryservice.model.valueObject.dto.LoanReturnedEventDTO
import com.inventoryservice.service.LoanEventInboxService
import com.inventoryservice.service.LibraryService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaEventConsumer(
    private val objectMapper: ObjectMapper,
    private val loanEventTranslator: LoanEventTranslator,
    private val libraryService: LibraryService,
    private val loanEventInboxService: LoanEventInboxService
) {

    @KafkaListener(
        topics = ["loan.created"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun handleLoanCreated(record: ConsumerRecord<String, String>) {
        process(record, "loan.created", LoanCreatedEventDTO::class.java) { event ->
            libraryService.borrowBookStock(loanEventTranslator.translate(event)).join()
        }
    }

    @KafkaListener(
        topics = ["loan.returned"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun handleLoanReturned(record: ConsumerRecord<String, String>) {
        process(record, "loan.returned", LoanReturnedEventDTO::class.java) { event ->
            libraryService.returnBookStock(loanEventTranslator.translate(event)).join()
        }
    }

    @KafkaListener(
        topics = ["loan.marked.lost"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun handleLoanMarkedLost(record: ConsumerRecord<String, String>) {
        process(record, "loan.marked.lost", LoanMarkedLostEventDTO::class.java) { event ->
            libraryService.markBookStockLost(loanEventTranslator.translate(event)).join()
        }
    }

    private fun <T : LoanEventDTO> process(
        record: ConsumerRecord<String, String>,
        eventType: String,
        eventClass: Class<T>,
        action: (T) -> Unit
    ) {
        val event = objectMapper.readValue(record.value(), eventClass)
        event.validate()

        val correlationKey = event.idempotencyKey ?: event.loanId
        require(record.key() == correlationKey) {
            "Kafka record key must equal correlation key '$correlationKey' for ordered loan processing"
        }

        loanEventInboxService.process(eventType, event) {
            action(event)
        }
    }

}
