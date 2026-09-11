package com.catalogservice.handler.eventHandler

import com.catalogservice.model.event.AbstractEvent
import com.catalogservice.service.EventMessagingService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.SerializationFeature
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component

@Component
class EventMessagingEventHandler(
    private val eventMessagingService: EventMessagingService
) {

    private val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @EventHandler
    fun on(event: AbstractEvent) {

        println(">>> EventMessagingEventHandler received: ${event.javaClass.simpleName}")

        val externalEvent = event.toExternalEvent() ?: return

        println(">>> External event: ${externalEvent.javaClass.simpleName}")

        val eventJSON = objectMapper.writeValueAsString(externalEvent)

        eventMessagingService.send(
            topic = event.eventTopic(),
            key = event.identifier.value.toString(),
            payload = eventJSON
        )
    }
}