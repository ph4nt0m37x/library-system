package com.borrowingservice.integration

import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher
import org.springframework.stereotype.Component

@Component
class KafkaEventDocumentation {
    @AsyncPublisher(
        operation = AsyncOperation(
            channelName = LoanTopics.CREATED,
            description = "Outbox-published after loan commit. Key: loanId. JSON schema version: 1. " +
                "Retries remain in the outbox; exhausted records are visible through outbox metrics.",
            payloadType = LoanCreatedIntegrationEvent::class,
            message = AsyncMessage(contentType = "application/json", name = "LoanCreated")
        )
    )
    @KafkaAsyncOperationBinding(
        bindingVersion = "0.5.0",
        messageBinding = KafkaAsyncOperationBinding.KafkaAsyncMessageBinding(
            key = KafkaAsyncOperationBinding.KafkaAsyncKey(
                example = "7c70a409-5626-41ef-981f-da42c3b9f224",
                description = "Loan ID; preserves transition order for a loan"
            )
        )
    )
    fun loanCreated(event: LoanCreatedIntegrationEvent) = Unit

    @AsyncPublisher(
        operation = AsyncOperation(
            channelName = LoanTopics.RETURNED,
            description = "Outbox-published after ACTIVE to RETURNED commit. Key: loanId. JSON schema version: 1. " +
                "Publication retries in the outbox; exhausted rows are counted by borrowing.outbox.dead.lettered.",
            payloadType = LoanReturnedIntegrationEvent::class,
            message = AsyncMessage(contentType = "application/json", name = "LoanReturned")
        )
    )
    @KafkaAsyncOperationBinding(messageBinding = KafkaAsyncOperationBinding.KafkaAsyncMessageBinding(
        key = KafkaAsyncOperationBinding.KafkaAsyncKey(
            description = "Loan ID",
            example = "7c70a409-5626-41ef-981f-da42c3b9f224"
        )
    ))
    fun loanReturned(event: LoanReturnedIntegrationEvent) = Unit

    @AsyncPublisher(
        operation = AsyncOperation(
            channelName = LoanTopics.MARKED_LOST,
            description = "Outbox-published after ACTIVE to LOST commit. Key: loanId. JSON schema version: 1. " +
                "Publication retries in the outbox; exhausted rows are counted by borrowing.outbox.dead.lettered.",
            payloadType = LoanMarkedLostIntegrationEvent::class,
            message = AsyncMessage(contentType = "application/json", name = "LoanMarkedLost")
        )
    )
    @KafkaAsyncOperationBinding(messageBinding = KafkaAsyncOperationBinding.KafkaAsyncMessageBinding(
        key = KafkaAsyncOperationBinding.KafkaAsyncKey(
            description = "Loan ID",
            example = "7c70a409-5626-41ef-981f-da42c3b9f224"
        )
    ))
    fun loanMarkedLost(event: LoanMarkedLostIntegrationEvent) = Unit

    @AsyncPublisher(
        operation = AsyncOperation(
            channelName = LoanTopics.MARKED_DAMAGED,
            description = "Outbox-published after ACTIVE to DAMAGED commit. Key: loanId. JSON schema version: 1. " +
                "Publication retries in the outbox; exhausted rows are counted by borrowing.outbox.dead.lettered.",
            payloadType = LoanMarkedDamagedIntegrationEvent::class,
            message = AsyncMessage(contentType = "application/json", name = "LoanMarkedDamaged")
        )
    )
    @KafkaAsyncOperationBinding(messageBinding = KafkaAsyncOperationBinding.KafkaAsyncMessageBinding(
        key = KafkaAsyncOperationBinding.KafkaAsyncKey(
            description = "Loan ID",
            example = "7c70a409-5626-41ef-981f-da42c3b9f224"
        )
    ))
    fun loanMarkedDamaged(event: LoanMarkedDamagedIntegrationEvent) = Unit
}
