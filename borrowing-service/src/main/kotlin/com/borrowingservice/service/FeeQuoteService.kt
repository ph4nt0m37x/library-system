package com.borrowingservice.service

import com.borrowingservice.client.InventoryBookPriceClient
import com.borrowingservice.config.FeePolicyConfiguration
import com.borrowingservice.model.aggregate.Fee
import com.borrowingservice.model.valueObject.enums.BillableDayRule
import com.borrowingservice.model.valueObject.enums.FeeReason
import com.borrowingservice.model.valueObject.enums.FeeStatus
import com.borrowingservice.model.valueObject.ResourceNotFoundException
import com.borrowingservice.model.valueObject.StateConflictException
import com.borrowingservice.repository.FeeRepository
import com.borrowingservice.repository.LoanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Clock
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class FeeQuote(
    val feeId: String,
    val allocationId: String,
    val amount: BigDecimal
)

data class PaymentQuote(
    val paymentId: String,
    val memberId: String,
    val currency: String,
    val amount: BigDecimal,
    val quotedAt: ZonedDateTime,
    val fees: List<FeeQuote>
)

@Service
@Transactional(readOnly = true)
class FeeQuoteService(
    private val feeRepository: FeeRepository,
    private val loanRepository: LoanRepository,
    private val bookPriceClient: InventoryBookPriceClient,
    private val policy: FeePolicyConfiguration,
    private val clock: Clock
) {
    fun quote(
        paymentId: String,
        memberId: String,
        selectedFeeIds: List<String>,
        currency: String
    ): PaymentQuote = quote(paymentId, memberId, selectedFeeIds, currency, ZonedDateTime.now(clock))

    fun quote(
        paymentId: String,
        memberId: String,
        selectedFeeIds: List<String>,
        currency: String,
        quotedAt: ZonedDateTime
    ): PaymentQuote {
        require(!quotedAt.isAfter(ZonedDateTime.now(clock))) { "A payment quote cannot be dated in the future" }
        require(selectedFeeIds.isNotEmpty()) { "At least one Fee must be selected" }
        require(selectedFeeIds.distinct().size == selectedFeeIds.size) { "A Fee may be selected only once" }

        val normalizedCurrency = currency.trim().uppercase()
        val feesById = feeRepository.findAllById(selectedFeeIds).associateBy { it.feeId }
        val fees = selectedFeeIds.map { feeId ->
            feesById[feeId] ?: throw ResourceNotFoundException("Fee", feeId)
        }

        val quotes = fees.map { fee ->
            if (fee.status != FeeStatus.UNPAID) {
                throw StateConflictException("FEE_NOT_UNPAID", "Fee ${fee.feeId} is not UNPAID")
            }
            require(fee.memberId == memberId) { "Fee ${fee.feeId} does not belong to member $memberId" }
            require(!quotedAt.isBefore(fee.createdAt)) {
                "A payment quote cannot predate Fee ${fee.feeId}"
            }
            require(fee.currency == normalizedCurrency) {
                "Fee ${fee.feeId} uses ${fee.currency}, not $normalizedCurrency"
            }
            FeeQuote(
                feeId = fee.feeId,
                allocationId = deterministicAllocationId(paymentId, fee.feeId),
                amount = amountFor(fee)
            )
        }
        val total = quotes.fold(BigDecimal.ZERO) { sum, quote -> sum.add(quote.amount) }
            .setScale(policy.moneyScale, policy.roundingMode)

        return PaymentQuote(paymentId, memberId, normalizedCurrency, total, quotedAt, quotes)
    }

    private fun amountFor(fee: Fee): BigDecimal = when (fee.reason) {
        FeeReason.OVERDUE -> overdueAmount(fee)
        FeeReason.LOST, FeeReason.DAMAGED -> replacementAmount(fee)
    }

    private fun overdueAmount(fee: Fee): BigDecimal {
        val dueAt = checkNotNull(fee.dueAt) { "OVERDUE Fee ${fee.feeId} has no dueAt" }
        val returnedAt = checkNotNull(fee.returnedAt) { "OVERDUE Fee ${fee.feeId} has no returnedAt" }
        val overdueDays = billableOverdueDays(dueAt, returnedAt)
        require(overdueDays > 0) { "OVERDUE Fee ${fee.feeId} is not late" }
        return if (overdueDays >= policy.escalationThresholdDays) {
            replacementAmount(fee)
        } else {
            policy.dailyLateRate.multiply(BigDecimal.valueOf(overdueDays)).money()
        }
    }

    private fun replacementAmount(fee: Fee): BigDecimal {
        val loan = loanRepository.findById(fee.loanId)
            .orElseThrow { ResourceNotFoundException("Loan", fee.loanId) }
        val price = bookPriceClient.findPrice(loan.bookId)
        val priceCurrency = price.currency.trim().uppercase()
        require(priceCurrency == fee.currency) {
            "Book ${loan.bookId} is priced in $priceCurrency, not ${fee.currency}"
        }
        require(price.amount >= BigDecimal.ZERO) { "Book price must not be negative" }
        return price.amount.multiply(policy.replacementMultiplier).money()
    }

    private fun billableOverdueDays(dueAt: ZonedDateTime, returnedAt: ZonedDateTime): Long {
        require(returnedAt.isAfter(dueAt)) { "A return must be after dueAt to be overdue" }
        return when (policy.billableDayRule) {
            BillableDayRule.STARTED_CALENDAR_DAYS -> {
                val dueDate = dueAt.withZoneSameInstant(policy.billableTimeZone).toLocalDate()
                val returnedDate = returnedAt.withZoneSameInstant(policy.billableTimeZone).toLocalDate()
                maxOf(1L, ChronoUnit.DAYS.between(dueDate, returnedDate))
            }

            BillableDayRule.STARTED_24_HOUR_PERIODS -> {
                val elapsedNanos = Duration.between(dueAt.toInstant(), returnedAt.toInstant()).toNanos()
                BigDecimal.valueOf(elapsedNanos)
                    .divide(BigDecimal.valueOf(NANOS_PER_DAY), 0, RoundingMode.CEILING)
                    .longValueExact()
            }
        }
    }

    private fun deterministicAllocationId(paymentId: String, feeId: String): String =
        UUID.nameUUIDFromBytes("payment:$paymentId:fee:$feeId".toByteArray(StandardCharsets.UTF_8)).toString()

    private fun BigDecimal.money(): BigDecimal = setScale(policy.moneyScale, policy.roundingMode)

    private companion object {
        const val NANOS_PER_DAY = 86_400_000_000_000L
    }
}
