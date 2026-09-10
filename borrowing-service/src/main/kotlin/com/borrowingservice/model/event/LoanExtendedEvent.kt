package com.borrowingservice.model.event

import java.time.ZonedDateTime

data class LoanExtendedEvent(
    val loanId: String,
    val extendedAt: ZonedDateTime,
    val dueAt: ZonedDateTime
)
