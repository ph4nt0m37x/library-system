package com.borrowingservice.model.event

import java.time.ZonedDateTime

data class LoanMarkedLostEvent(
    val loanId: String,
    val memberId: String,
    val bookId: String? = null,
    val libraryId: String? = null,
    val declaredLostAt: ZonedDateTime,
    val idempotencyKey: String? = null
)
