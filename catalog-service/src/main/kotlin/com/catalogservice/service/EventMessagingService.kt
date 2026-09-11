package com.catalogservice.service

import com.catalogservice.model.event.BookDeletedExternalEvent

interface EventMessagingService {
    fun send(topic: String, key: String, payload: BookDeletedExternalEvent)
}
