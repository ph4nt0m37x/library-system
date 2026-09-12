package com.inventoryservice.model.valueObject.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class LoanCreatedEventDTO(
    override val eventId: String,
    override val loanId: String,
    override val eventVersion: Int,
    override val occurredAt: Instant,
    override val bookId: String,
    override val libraryId: String,
    override val idempotencyKey: String? = null
) : LoanEventDTO
