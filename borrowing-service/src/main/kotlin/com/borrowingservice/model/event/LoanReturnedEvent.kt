package com.borrowingservice.model.event

import java.time.ZonedDateTime

data class LoanReturnedEvent(
    val loanId: String,
    val memberId: String,
    val dueAt: ZonedDateTime,
    val returnedAt: ZonedDateTime,
    val idempotencyKey: String? = null
)
