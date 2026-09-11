package com.catalogservice.handler.eventHandler

import com.catalogservice.model.event.BookDeletedEvent
import com.catalogservice.service.EventMessagingService
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component

@Component
class EventMessagingEventHandler(
    private val eventMessagingService: EventMessagingService
) {

    @EventHandler
    fun on(event: BookDeletedEvent) {

        println(">>> EventMessagingEventHandler received: ${event.javaClass.simpleName}")

        val externalEvent = event.toExternalEvent()

        println(">>> External event: ${externalEvent.javaClass.simpleName}")

        eventMessagingService.send(
            topic = event.eventTopic(),
            key = event.identifier.value.toString(),
            payload = externalEvent
        )
    }
}
