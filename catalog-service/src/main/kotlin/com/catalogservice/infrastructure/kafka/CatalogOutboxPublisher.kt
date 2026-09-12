package com.catalogservice.infrastructure.kafka

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
class CatalogOutboxPublisher(
    private val outbox: CatalogIntegrationOutbox,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val clock: Clock,
    meterRegistry: MeterRegistry,
    @Value("\${catalog.outbox.max-attempts:20}") private val maxAttempts: Int,
    @Value("\${catalog.outbox.send-timeout-seconds:10}") private val sendTimeoutSeconds: Long
) {
    init {
        Gauge.builder("catalog.outbox.pending", outbox) {
            it.countByPublishedAtIsNullAndDeadLetteredAtIsNull().toDouble()
        }.register(meterRegistry)
        Gauge.builder("catalog.outbox.dead.lettered", outbox) {
            it.countByDeadLetteredAtIsNotNull().toDouble()
        }.register(meterRegistry)
    }

    @Scheduled(fixedDelayString = "\${catalog.outbox.publish-delay-ms:1000}")
    @Transactional
    fun publishPending() {
        outbox.findTop50ByPublishedAtIsNullAndDeadLetteredAtIsNullOrderByCreatedAtAsc().forEach { record ->
            try {
                kafkaTemplate.send(record.topic, record.recordKey, record.payload)
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS)
                record.publishedAt = Instant.now(clock)
                record.lastError = null
            } catch (exception: Exception) {
                record.publishAttempts += 1
                record.lastError = rootMessage(exception).take(1000)
                if (record.publishAttempts >= maxAttempts) {
                    record.deadLetteredAt = Instant.now(clock)
                    log.error("Catalog outbox event {} exhausted publication attempts", record.eventId, exception)
                }
            }
            outbox.save(record)
        }
    }

    private fun rootMessage(exception: Throwable): String {
        var current = exception
        while (current.cause != null) current = current.cause!!
        return current.message ?: current.javaClass.simpleName
    }

    private companion object {
        val log = LoggerFactory.getLogger(CatalogOutboxPublisher::class.java)
    }
}
