package com.catalogservice.repository


interface EventMessagingRepository {
    fun send(topic: String, key: String, payload: String)
}