package com.borrowingservice.model.event

import java.time.ZonedDateTime

data class LoanCreatedEvent(
    val loanId: String,
    val memberId: String,
    val bookId: String,
    val borrowedAt: ZonedDateTime,
    val dueAt: ZonedDateTime
)
