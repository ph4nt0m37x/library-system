package com.catalogservice.infrastructure.kafka

import com.catalogservice.model.event.BookDeletedEvent
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher
import org.springframework.stereotype.Component

@Component
class KafkaEventDocumentation {

    @AsyncPublisher(
        operation = AsyncOperation(
            channelName = "catalog-events",
            description = "Published when a book is deleted.",
            payloadType = BookDeletedEvent::class
        )
    )
    @KafkaAsyncOperationBinding
    fun publishBookDeletedEvent(event: BookDeletedEvent) {
        // Documentation only.
    }
}