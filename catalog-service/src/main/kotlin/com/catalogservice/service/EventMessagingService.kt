package com.catalogservice.service


interface EventMessagingService {
    fun send(topic: String, key: String, payload: String)
}