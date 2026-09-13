package com.inventoryservice.model.valueObject.dto

import java.time.Instant

data class LoanMarkedLostEventDTO(
    override val eventId: String,
    override val loanId: String,
    override val eventVersion: Int,
    override val aggregateVersion: Long,
    override val occurredAt: Instant,
    override val bookId: String,
    override val libraryId: String,
    override val idempotencyKey: String? = null,
    val declaredLostAt: Instant
) : LoanEventDTO
