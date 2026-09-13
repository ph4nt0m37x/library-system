package com.borrowingservice.model.event

import java.time.ZonedDateTime

data class LoanCreatedEvent(
    val loanId: String,
    val memberId: String,
    val bookId: String,
    val libraryId: String? = null,
    val borrowedAt: ZonedDateTime,
    val dueAt: ZonedDateTime,
    val idempotencyKey: String? = null
)
