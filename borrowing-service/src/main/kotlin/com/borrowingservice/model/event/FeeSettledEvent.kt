package com.borrowingservice.model.event

import java.math.BigDecimal
import java.time.ZonedDateTime

data class FeeSettledEvent(
    val feeId: String,
    val paymentId: String,
    val allocationId: String,
    val amount: BigDecimal,
    val settledAt: ZonedDateTime
)
