package com.mcpservice.client.dto

import java.math.BigDecimal
import java.time.ZonedDateTime

data class LoanWire(
    val loanId: String,
    val memberId: String,
    val bookId: String,
    val libraryId: String? = null,
    val borrowedAt: ZonedDateTime,
    val dueAt: ZonedDateTime,
    val extendedAt: ZonedDateTime? = null,
    val returnedAt: ZonedDateTime? = null,
    val status: String,
    val incidentDeclaredAt: ZonedDateTime? = null,
    val idempotencyKey: String? = null
)

data class FeeWire(
    val feeId: String,
    val loanId: String,
    val memberId: String,
    val currency: String,
    val reason: String,
    val status: String,
    val createdAt: ZonedDateTime,
    val dueAt: ZonedDateTime? = null,
    val returnedAt: ZonedDateTime? = null,
    val incidentAt: ZonedDateTime? = null,
    val settledAt: ZonedDateTime? = null,
    val settledByPaymentId: String? = null,
    val settlementAllocationId: String? = null,
    val settlementAmount: BigDecimal? = null
)

data class PaymentWire(
    val paymentId: String,
    val memberId: String,
    val amount: BigDecimal,
    val currency: String,
    val paidAt: ZonedDateTime,
    val allocations: List<PaymentAllocationWire> = emptyList()
)

data class PaymentAllocationWire(
    val allocationId: String,
    val feeId: String,
    val amount: BigDecimal
)

data class BorrowingBanWire(
    val banRecordId: String,
    val memberId: String,
    val lastIssuedTier: String,
    val active: Boolean,
    val permanentlyBanned: Boolean,
    val currentBan: BanPeriodWire? = null,
    val history: List<BanPeriodWire> = emptyList()
)

data class BanPeriodWire(
    val banId: String,
    val tier: String,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime? = null,
    val issuedAt: ZonedDateTime,
    val triggeringFeeId: String,
    val reason: String
)

data class CreateLoanWire(
    val memberId: String,
    val bookId: String,
    val libraryId: String,
    val idempotencyKey: String
)

data class QuotePaymentWire(
    val memberId: String,
    val currency: String,
    val feeIds: List<String>
)

data class RecordPaymentWire(
    val paymentId: String,
    val memberId: String,
    val amount: BigDecimal,
    val currency: String,
    val feeIds: List<String>
)

data class PaymentQuoteWire(
    val paymentId: String,
    val memberId: String,
    val currency: String,
    val amount: BigDecimal,
    val quotedAt: ZonedDateTime,
    val fees: List<FeeQuoteWire>
)

data class FeeQuoteWire(
    val feeId: String,
    val allocationId: String,
    val amount: BigDecimal
)

data class CommandResponseWire(val id: String)
