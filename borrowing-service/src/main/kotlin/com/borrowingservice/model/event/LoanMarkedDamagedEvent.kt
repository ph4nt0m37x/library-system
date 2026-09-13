package com.borrowingservice.model.event

import java.time.ZonedDateTime

data class LoanMarkedDamagedEvent(
    val loanId: String,
    val memberId: String,
    val bookId: String? = null,
    val libraryId: String? = null,
    val damageRecordedAt: ZonedDateTime,
    val idempotencyKey: String? = null
)
