package com.borrowingservice.handlers.commandHandlers

import com.borrowingservice.model.aggregate.Payment
import com.borrowingservice.model.command.RecordPaymentCommand
import com.borrowingservice.model.event.PaymentRecordedEvent
import com.borrowingservice.model.valueObject.PaymentAllocationDetails
import com.borrowingservice.model.valueObject.StateConflictException
import com.borrowingservice.repository.PaymentRepository
import com.borrowingservice.service.FeeQuoteService
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.Repository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.ZonedDateTime

@Component
class PaymentCommandHandler(
    @Qualifier("axonPaymentRepository") private val paymentAggregateRepository: Repository<Payment>,
    private val paymentRepository: PaymentRepository,
    private val feeQuoteService: FeeQuoteService,
    private val clock: Clock
) {
    @CommandHandler
    @Transactional
    fun handle(command: RecordPaymentCommand): Boolean {
        paymentRepository.findById(command.paymentId).orElse(null)?.let { existing ->
            if (existing.memberId != command.memberId) {
                throw StateConflictException("PAYMENT_ID_ALREADY_USED", "Payment ID is already used by another member")
            }
            if (existing.amount.compareTo(command.amount) != 0) {
                throw StateConflictException("PAYMENT_ID_ALREADY_USED", "Payment ID is already used with another amount")
            }
            if (existing.currency != command.currency.trim().uppercase()) {
                throw StateConflictException("PAYMENT_ID_ALREADY_USED", "Payment ID is already used with another currency")
            }
            if (existing.allocations.map { it.feeId } != command.feeIds) {
                throw StateConflictException("PAYMENT_ID_ALREADY_USED", "Payment ID is already used for another Fee selection")
            }
            return false
        }

        val paidAt = now()
        val quote = feeQuoteService.quote(
            paymentId = command.paymentId,
            memberId = command.memberId,
            selectedFeeIds = command.feeIds,
            currency = command.currency,
            quotedAt = paidAt
        )
        require(command.amount.compareTo(quote.amount) == 0) {
            "Payment amount ${command.amount} does not equal quoted amount ${quote.amount}"
        }

        val event = PaymentRecordedEvent(
            paymentId = command.paymentId,
            memberId = command.memberId,
            amount = quote.amount,
            currency = quote.currency,
            paidAt = paidAt,
            allocations = quote.fees.map {
                PaymentAllocationDetails(it.allocationId, it.feeId, it.amount)
            }
        )
        paymentAggregateRepository.newInstance {
            Payment().also { AggregateLifecycle.apply(event) }
        }
        return true
    }

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock)
}
