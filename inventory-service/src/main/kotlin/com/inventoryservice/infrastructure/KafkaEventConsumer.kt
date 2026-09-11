package com.inventoryservice.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.inventoryservice.infrastructure.kafka.LoanEventTranslator
import com.inventoryservice.model.valueObject.dto.LoanCreatedEventDTO
import com.inventoryservice.model.valueObject.dto.LoanMarkedLostEventDTO
import com.inventoryservice.model.valueObject.dto.LoanReturnedEventDTO
import com.inventoryservice.service.LibraryService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaEventConsumer(
    private val objectMapper: ObjectMapper,
    private val loanEventTranslator: LoanEventTranslator,
    private val libraryService: LibraryService
) {

    @KafkaListener(
        topics = ["loan.created"],
        groupId = "inventory-service"
    )
    fun handleLoanCreated(message: String) {

        val event = objectMapper.readValue(
            message,
            LoanCreatedEventDTO::class.java
        )

        val command = loanEventTranslator.translate(event)

        libraryService.borrowBookStock(command).join()
    }

    @KafkaListener(
        topics = ["loan.returned"],
        groupId = "inventory-service"
    )
    fun handleLoanReturned(message: String) {

        val event = objectMapper.readValue(
            message,
            LoanReturnedEventDTO::class.java
        )

        val command = loanEventTranslator.translate(event)

        libraryService.returnBookStock(command).join()
    }

    @KafkaListener(
        topics = ["loan.marked.lost"],
        groupId = "inventory-service"
    )
    fun handleLoanMarkedLost(message: String) {

        val event = objectMapper.readValue(
            message,
            LoanMarkedLostEventDTO::class.java
        )

        val command = loanEventTranslator.translate(event)

        libraryService.markBookStockLost(command).join()
    }

}