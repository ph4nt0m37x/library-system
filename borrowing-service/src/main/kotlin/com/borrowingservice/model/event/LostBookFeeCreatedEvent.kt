package com.borrowingservice.model.event

import java.time.ZonedDateTime

data class LostBookFeeCreatedEvent(
    val feeId: String,
    val loanId: String,
    val memberId: String,
    val currency: String,
    val declaredLostAt: ZonedDateTime,
    val createdAt: ZonedDateTime
)
