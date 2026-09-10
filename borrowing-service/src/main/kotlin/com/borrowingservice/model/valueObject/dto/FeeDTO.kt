package com.borrowingservice.model.valueObject.dto

import com.borrowingservice.model.valueObject.enums.FeeReason
import com.borrowingservice.model.valueObject.enums.FeeStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

data class FeeResponse(
    val feeId: String,
    val loanId: String,
    val memberId: String,
    val currency: String,
    val reason: FeeReason,
    val status: FeeStatus,
    val createdAt: ZonedDateTime,
    val dueAt: ZonedDateTime?,
    val returnedAt: ZonedDateTime?,
    val incidentAt: ZonedDateTime?,
    val settledAt: ZonedDateTime?,
    val settledByPaymentId: String?,
    val settlementAllocationId: String?,
    val settlementAmount: BigDecimal?
)
