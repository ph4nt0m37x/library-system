package com.mcpservice.projection

import java.math.BigDecimal

data class PaymentAllocationProjection(
    val allocationId: String,
    val feeId: String,
    val amount: BigDecimal
)

data class PaymentProjection(
    val paymentId: String,
    val memberId: String,
    val amount: BigDecimal,
    val currency: String,
    val paidAt: String,
    val allocations: List<PaymentAllocationProjection>
)

data class FeeQuoteProjection(
    val feeId: String,
    val allocationId: String,
    val amount: BigDecimal
)

data class PaymentQuoteProjection(
    val paymentId: String,
    val memberId: String,
    val currency: String,
    val amount: BigDecimal,
    val quotedAt: String,
    val fees: List<FeeQuoteProjection>
)

data class PaymentRecordProjection(
    val paymentId: String,
    val recorded: Boolean,
    val message: String
)
