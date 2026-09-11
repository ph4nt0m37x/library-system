package com.catalogservice.service.impl

import com.catalogservice.model.event.BookDeletedExternalEvent
import com.catalogservice.repository.EventMessagingRepository
import com.catalogservice.service.EventMessagingService
import org.springframework.stereotype.Service

@Service
class EventMessagingServiceImpl(
    private val eventMessagingRepository: EventMessagingRepository
) : EventMessagingService {

    override fun send(topic: String, key: String, payload: BookDeletedExternalEvent) {
        eventMessagingRepository.send(topic, key, payload)
    }
}
