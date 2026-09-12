package com.borrowingservice.model.valueObject.dto

import java.math.BigDecimal
import java.time.ZonedDateTime

data class QuotePaymentDTO(
    val memberId: String,
    val currency: String,
    @Deprecated("Ignored; payment quotes use the server Clock")
    val quotedAt: ZonedDateTime? = null,
    val feeIds: List<String> = emptyList()
)

data class RecordPaymentDTO(
    val paymentId: String,
    val memberId: String,
    val amount: BigDecimal,
    val currency: String,
    @Deprecated("Ignored; payment recording uses the server Clock")
    val paidAt: ZonedDateTime? = null,
    val feeIds: List<String> = emptyList()
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
