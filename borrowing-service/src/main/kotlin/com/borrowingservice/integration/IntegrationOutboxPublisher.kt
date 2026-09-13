package com.borrowingservice.integration

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.concurrent.TimeUnit

@Component
class IntegrationOutboxPublisher(
    private val outboxRepository: IntegrationOutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val clock: Clock,
    meterRegistry: MeterRegistry,
    @Value("\${borrowing.outbox.max-attempts:20}") private val maxAttempts: Int,
    @Value("\${borrowing.outbox.send-timeout-seconds:10}") private val sendTimeoutSeconds: Long
) {
    init {
        Gauge.builder("borrowing.outbox.pending", outboxRepository) {
            it.countByPublishedAtIsNullAndDeadLetteredAtIsNull().toDouble()
        }.register(meterRegistry)
        Gauge.builder("borrowing.outbox.dead.lettered", outboxRepository) {
            it.countByDeadLetteredAtIsNotNull().toDouble()
        }.register(meterRegistry)
    }

    @Scheduled(fixedDelayString = "\${borrowing.outbox.publish-delay-ms:1000}")
    @Transactional
    fun publishPending() {
        outboxRepository.findTop50ByPublishedAtIsNullAndDeadLetteredAtIsNullOrderByCreatedAtAsc()
            .forEach(::publish)
    }

    private fun publish(outbox: IntegrationOutboxEvent) {
        try {
            kafkaTemplate.send(outbox.topic, outbox.recordKey, outbox.payload)
                .get(sendTimeoutSeconds, TimeUnit.SECONDS)
            outbox.publishedAt = Instant.now(clock)
            outbox.lastError = null
        } catch (exception: Exception) {
            outbox.publishAttempts += 1
            outbox.lastError = rootMessage(exception).take(1000)
            if (outbox.publishAttempts >= maxAttempts) {
                outbox.deadLetteredAt = Instant.now(clock)
                log.error("Outbox event {} exhausted {} attempts", outbox.eventId, maxAttempts, exception)
            } else {
                log.warn("Outbox event {} publication attempt {} failed", outbox.eventId, outbox.publishAttempts)
            }
        }
        outboxRepository.save(outbox)
    }

    private fun rootMessage(exception: Throwable): String {
        var current = exception
        while (current.cause != null) current = current.cause!!
        return current.message ?: current.javaClass.simpleName
    }

    private companion object {
        val log = LoggerFactory.getLogger(IntegrationOutboxPublisher::class.java)
    }
}
