package com.borrowingservice.model.valueObject.dto

import java.math.BigDecimal
import java.time.ZonedDateTime

data class QuotePaymentDTO(
    val memberId: String,
    val currency: String,
    val quotedAt: ZonedDateTime,
    val feeIds: List<String>
)

data class RecordPaymentDTO(
    val paymentId: String,
    val memberId: String,
    val amount: BigDecimal,
    val currency: String,
    val paidAt: ZonedDateTime,
    val feeIds: List<String>
)

data class FeeQuoteResponse(
    val feeId: String,
    val allocationId: String,
    val amount: BigDecimal
)

data class PaymentQuoteResponse(
    val paymentId: String,
    val memberId: String,
    val currency: String,
    val amount: BigDecimal,
    val quotedAt: ZonedDateTime,
    val fees: List<FeeQuoteResponse>
)

data class PaymentAllocationResponse(
    val allocationId: String,
    val feeId: String,
    val amount: BigDecimal
)

data class PaymentResponse(
    val paymentId: String,
    val memberId: String,
    val amount: BigDecimal,
    val currency: String,
    val paidAt: ZonedDateTime,
    val allocations: List<PaymentAllocationResponse>
)
