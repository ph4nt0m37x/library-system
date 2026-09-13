package com.mcpservice.projection

import java.math.BigDecimal

data class FeeProjection(
    val feeId: String,
    val loanId: String,
    val memberId: String,
    val currency: String,
    val reason: String,
    val status: String,
    val createdAt: String,
    val dueAt: String?,
    val returnedAt: String?,
    val incidentAt: String?,
    val settledAt: String?,
    val settledByPaymentId: String?,
    val settlementAllocationId: String?,
    val settlementAmount: BigDecimal?
)

data class FeeListProjection(
    val fees: List<FeeProjection>,
    val count: Int,
    val limit: Int,
    val truncated: Boolean
)
