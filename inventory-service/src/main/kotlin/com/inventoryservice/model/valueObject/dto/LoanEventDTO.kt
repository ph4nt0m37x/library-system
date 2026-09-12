package com.inventoryservice.model.valueObject.dto

import java.time.Instant
import java.util.UUID

interface LoanEventDTO {
    val eventId: String
    val loanId: String
    val eventVersion: Int
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
        require(eventVersion > 0) { "eventVersion must be greater than zero" }
        require(bookId.isNotBlank()) { "bookId must not be blank" }
        require(libraryId.isNotBlank()) { "libraryId must not be blank" }
        idempotencyKey?.let {
            require(it.isNotBlank()) { "idempotencyKey must not be blank" }
            require(it.length <= 100) { "idempotencyKey must not exceed 100 characters" }
        }
    }
}
