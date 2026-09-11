package com.catalogservice.repository

import com.catalogservice.model.event.BookDeletedExternalEvent

interface EventMessagingRepository {
    fun send(topic: String, key: String, payload: BookDeletedExternalEvent)
}
