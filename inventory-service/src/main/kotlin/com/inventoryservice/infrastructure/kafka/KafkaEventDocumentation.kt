package com.inventoryservice.infrastructure.kafka

import com.inventoryservice.model.valueObject.dto.BookDeletedIntegrationEventDTO
import com.inventoryservice.model.valueObject.dto.LoanCreatedEventDTO
import com.inventoryservice.model.valueObject.dto.LoanMarkedDamagedEventDTO
import com.inventoryservice.model.valueObject.dto.LoanMarkedLostEventDTO
import com.inventoryservice.model.valueObject.dto.LoanReturnedEventDTO
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncListener
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import org.springframework.stereotype.Component

@Component
class KafkaEventDocumentation {
    @AsyncListener(operation = AsyncOperation(
        channelName = InventoryTopics.LOAN_CREATED,
        description = "Consumes schema v1 JSON keyed by loanId; idempotent inbox mutation. Failures retry twice then go to loan.created.DLT with original-record and exception headers.",
        payloadType = LoanCreatedEventDTO::class,
        message = AsyncMessage(contentType = "application/json", name = "LoanCreated")
    ))
    @KafkaAsyncOperationBinding(groupId = "inventory-service-group")
    fun loanCreated(event: LoanCreatedEventDTO) = Unit

    @AsyncListener(operation = AsyncOperation(
        channelName = InventoryTopics.LOAN_RETURNED,
        description = "Consumes schema v1 JSON keyed by loanId after a processed create; exhausted failures go to loan.returned.DLT.",
        payloadType = LoanReturnedEventDTO::class,
        message = AsyncMessage(contentType = "application/json", name = "LoanReturned")
    ))
    @KafkaAsyncOperationBinding(groupId = "inventory-service-group")
    fun loanReturned(event: LoanReturnedEventDTO) = Unit

    @AsyncListener(operation = AsyncOperation(
        channelName = InventoryTopics.LOAN_MARKED_LOST,
        description = "Consumes schema v1 JSON keyed by loanId; removes one borrowed copy from total stock. Exhausted failures go to loan.marked.lost.DLT.",
        payloadType = LoanMarkedLostEventDTO::class,
        message = AsyncMessage(contentType = "application/json", name = "LoanMarkedLost")
    ))
    @KafkaAsyncOperationBinding(groupId = "inventory-service-group")
    fun loanLost(event: LoanMarkedLostEventDTO) = Unit

    @AsyncListener(operation = AsyncOperation(
        channelName = InventoryTopics.LOAN_MARKED_DAMAGED,
        description = "Consumes schema v1 JSON keyed by loanId; removes one permanently damaged borrowed copy from usable total stock. Exhausted failures go to loan.marked.damaged.DLT.",
        payloadType = LoanMarkedDamagedEventDTO::class,
        message = AsyncMessage(contentType = "application/json", name = "LoanMarkedDamaged")
    ))
    @KafkaAsyncOperationBinding(groupId = "inventory-service-group")
    fun loanDamaged(event: LoanMarkedDamagedEventDTO) = Unit

    @AsyncListener(operation = AsyncOperation(
        channelName = InventoryTopics.BOOK_DELETED,
        description = "Consumes schema v1 JSON keyed by bookId and retains stock/history while marking the Catalog reference non-circulating. Exhausted failures go to book.deleted.DLT.",
        payloadType = BookDeletedIntegrationEventDTO::class,
        message = AsyncMessage(contentType = "application/json", name = "BookDeleted")
    ))
    @KafkaAsyncOperationBinding(groupId = "inventory-service-group")
    fun bookDeleted(event: BookDeletedIntegrationEventDTO) = Unit
}
