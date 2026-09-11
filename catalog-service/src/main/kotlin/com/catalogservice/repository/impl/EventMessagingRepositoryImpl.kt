package com.catalogservice.repository.impl

import com.catalogservice.repository.EventMessagingRepository
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Repository

@Repository
class KafkaMessagingRepositoryImpl(
    private val kafkaTemplate: KafkaTemplate<String, String>
) : EventMessagingRepository {

    override fun send(topic: String, key: String, payload: String) {
        kafkaTemplate.send(topic, key, payload)
            .whenComplete { result, ex ->
                if (ex != null) {
                    println("Failed to send event to topic '$topic': ${ex.message}")
                } else {
                    println("Event sent to topic '$topic' with key '$key'")
                }
            }
    }
}