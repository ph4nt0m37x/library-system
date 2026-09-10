package com.catalogservice.infrastructure.kafka

import com.catalogservice.model.event.BookDeletedEvent
import org.axonframework.eventhandling.EventHandler
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, BookDeletedEvent>
) {

    @EventHandler
    fun on(event: BookDeletedEvent) {
        kafkaTemplate.send(
            "catalog-events",
            event.id.value.toString(),
            event
        )
    }
}