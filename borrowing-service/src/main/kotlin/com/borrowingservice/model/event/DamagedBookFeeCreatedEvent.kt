package com.borrowingservice.model.event

import java.time.ZonedDateTime

data class DamagedBookFeeCreatedEvent(
    val feeId: String,
    val loanId: String,
    val memberId: String,
    val currency: String,
    val damageRecordedAt: ZonedDateTime,
    val createdAt: ZonedDateTime
)
