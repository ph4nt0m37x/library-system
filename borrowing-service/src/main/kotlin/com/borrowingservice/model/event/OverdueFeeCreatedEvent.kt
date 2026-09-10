package com.borrowingservice.model.event

import java.time.ZonedDateTime

data class OverdueFeeCreatedEvent(
    val feeId: String,
    val loanId: String,
    val memberId: String,
    val currency: String,
    val dueAt: ZonedDateTime,
    val returnedAt: ZonedDateTime,
    val createdAt: ZonedDateTime
)
