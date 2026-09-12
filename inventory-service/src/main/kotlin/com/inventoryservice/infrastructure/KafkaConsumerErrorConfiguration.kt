package com.inventoryservice.infrastructure

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.common.TopicPartition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConsumerErrorConfiguration {
    @Bean
    fun inventoryKafkaErrorHandler(
        kafkaTemplate: KafkaTemplate<String, String>,
        meterRegistry: MeterRegistry
    ): CommonErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
            Counter.builder("inventory.kafka.dead.letter.recoveries")
                .description("Kafka records exhausted and handed to a dead-letter topic")
                .tag("source.topic", record.topic())
                .register(meterRegistry)
                .increment()
            TopicPartition("${record.topic()}.DLT", record.partition())
        }
        return DefaultErrorHandler(recoverer, FixedBackOff(1000L, 2L)).apply {
            setCommitRecovered(true)
            setAckAfterHandle(true)
        }
    }
}
