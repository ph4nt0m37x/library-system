package com.inventoryservice.model.valueObject.dto

import java.time.Instant
import java.util.UUID

data class BookDeletedIntegrationEventDTO(
    val eventId: String,
    val bookId: String,
    val eventVersion: Int,
    val aggregateVersion: Long,
    val occurredAt: Instant
) {
    fun validate() {
        require(runCatching { UUID.fromString(eventId) }.isSuccess) { "eventId must be a UUID" }
        require(bookId.isNotBlank() && bookId.length <= 100) { "bookId must contain 1 to 100 characters" }
        require(eventVersion == 1) { "Unsupported book.deleted schema version: $eventVersion" }
        require(aggregateVersion >= 0) { "aggregateVersion must not be negative" }
    }
}
