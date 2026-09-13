package com.mcpservice.service

import com.mcpservice.client.BorrowingClient
import com.mcpservice.client.dto.QuotePaymentWire
import com.mcpservice.client.dto.RecordPaymentWire
import com.mcpservice.error.LibraryMcpToolException
import com.mcpservice.error.ToolErrorCode
import com.mcpservice.projection.FeeQuoteProjection
import com.mcpservice.projection.IdentifierNormalizer
import com.mcpservice.projection.PaymentQuoteProjection
import com.mcpservice.projection.PaymentRecordProjection
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class PaymentFacade(
    private val borrowingClient: BorrowingClient,
    private val errors: DownstreamErrorTranslator
) {
    fun quote(memberId: String, feeIds: List<String>, currency: String): PaymentQuoteProjection {
        val request = QuotePaymentWire(
            memberId = IdentifierNormalizer.requireUuid(memberId, "memberId"),
            currency = requireCurrency(currency),
            feeIds = requireFeeIds(feeIds)
        )
        val quote = errors.call("Borrowing service", ToolErrorCode.FEE_NOT_FOUND) {
            borrowingClient.quotePayment(request)
        }
        return PaymentQuoteProjection(
            paymentId = IdentifierNormalizer.base(quote.paymentId),
            memberId = IdentifierNormalizer.base(quote.memberId),
            currency = quote.currency,
            amount = quote.amount,
            quotedAt = quote.quotedAt.toOffsetDateTime().toString(),
            fees = quote.fees.map {
                FeeQuoteProjection(
                    IdentifierNormalizer.base(it.feeId),
                    IdentifierNormalizer.base(it.allocationId),
                    it.amount
                )
            }
        )
    }

    fun record(
        paymentId: String,
        memberId: String,
        amount: BigDecimal,
        currency: String,
        feeIds: List<String>
    ): PaymentRecordProjection {
        if (amount <= BigDecimal.ZERO) {
            throw LibraryMcpToolException(ToolErrorCode.INVALID_ARGUMENT, "amount must be greater than zero.")
        }
        val normalizedPaymentId = IdentifierNormalizer.requireUuid(paymentId, "paymentId")
        val response = errors.call("Borrowing service", ToolErrorCode.FEE_NOT_FOUND) {
            borrowingClient.recordPayment(
                RecordPaymentWire(
                    paymentId = normalizedPaymentId,
                    memberId = IdentifierNormalizer.requireUuid(memberId, "memberId"),
                    amount = amount,
                    currency = requireCurrency(currency),
                    feeIds = requireFeeIds(feeIds)
                )
            )
        }
        return PaymentRecordProjection(
            paymentId = IdentifierNormalizer.base(response.id),
            recorded = true,
            message = "Payment recorded as library bookkeeping; no external payment instrument was charged."
        )
    }

    private fun requireCurrency(raw: String): String {
        val currency = raw.trim().uppercase()
        if (!currency.matches(Regex("[A-Z]{3}"))) {
            throw LibraryMcpToolException(
                ToolErrorCode.INVALID_ARGUMENT,
                "currency must be a three-letter ISO currency code."
            )
        }
        return currency
    }

    private fun requireFeeIds(raw: List<String>): List<String> {
        val ids = raw.map { IdentifierNormalizer.requireUuid(it, "feeIds") }.distinct()
        if (ids.isEmpty() || ids.size > MAX_FEES) {
            throw LibraryMcpToolException(
                ToolErrorCode.INVALID_ARGUMENT,
                "feeIds must contain between 1 and $MAX_FEES unique fee IDs."
            )
        }
        return ids
    }

    companion object {
        const val MAX_FEES = 50
    }
}
