package com.inventoryservice.model.valueObject.dto

import java.time.Instant
import java.util.UUID

interface LoanEventDTO {
    val eventId: String
    val loanId: String
    val eventVersion: Int
    val aggregateVersion: Long
    val occurredAt: Instant
    val bookId: String
    val libraryId: String
    val idempotencyKey: String?

    fun validate() {
        require(eventId.isNotBlank()) { "eventId must not be blank" }
        require(runCatching { UUID.fromString(eventId) }.isSuccess) {
            "eventId must be a valid UUID"
        }
        require(loanId.isNotBlank()) { "loanId must not be blank" }
        require(loanId.length <= 100) { "loanId must not exceed 100 characters" }
        require(eventVersion == 1) { "Unsupported loan event schema version: $eventVersion" }
        require(aggregateVersion >= 0) { "aggregateVersion must not be negative" }
        require(bookId.isNotBlank() && bookId.length <= 100) {
            "bookId must contain 1 to 100 characters"
        }
        require(libraryId.isNotBlank() && libraryId.length <= 100) {
            "libraryId must contain 1 to 100 characters"
        }
        idempotencyKey?.let {
            require(it.isNotBlank()) { "idempotencyKey must not be blank" }
            require(it.length <= 100) { "idempotencyKey must not exceed 100 characters" }
        }
    }
}
