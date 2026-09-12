package com.catalogservice.infrastructure.kafka

import com.catalogservice.model.event.BookDeletedExternalEvent
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage
import org.springframework.stereotype.Component

@Component
class KafkaEventDocumentation {

    @AsyncPublisher(
        operation = AsyncOperation(
            channelName = CatalogTopics.BOOK_DELETED,
            description = "Outbox-published schema v1 JSON after deletion commits. Key: base bookId. " +
                "Inventory consumes idempotently; exhausted producer records remain observable in the outbox.",
            payloadType = BookDeletedExternalEvent::class,
            message = AsyncMessage(contentType = "application/json", name = "BookDeleted")
        )
    )
    @KafkaAsyncOperationBinding(
        bindingVersion = "0.5.0",
        messageBinding = KafkaAsyncOperationBinding.KafkaAsyncMessageBinding(
            key = KafkaAsyncOperationBinding.KafkaAsyncKey(
                description = "Base Catalog book ID",
                example = "e320ebc3-6be6-4e1f-9a0f-48bf9b9f6adf"
            )
        )
    )
    fun publishBookDeletedEvent(event: BookDeletedExternalEvent) {
        // Documentation only.
    }
}
