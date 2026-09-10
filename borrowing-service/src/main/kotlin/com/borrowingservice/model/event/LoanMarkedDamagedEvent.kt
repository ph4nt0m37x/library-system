package com.borrowingservice.model.event

import java.time.ZonedDateTime

data class LoanMarkedDamagedEvent(
    val loanId: String,
    val memberId: String,
    val damageRecordedAt: ZonedDateTime
)
