package com.borrowingservice.handlers.commandHandlers

import com.borrowingservice.model.aggregate.Payment
import com.borrowingservice.model.command.RecordPaymentCommand
import com.borrowingservice.model.event.PaymentRecordedEvent
import com.borrowingservice.model.valueObject.PaymentAllocationDetails
import com.borrowingservice.repository.PaymentRepository
import com.borrowingservice.service.FeeQuoteService
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.Repository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentCommandHandler(
    @Qualifier("axonPaymentRepository") private val paymentAggregateRepository: Repository<Payment>,
    private val paymentRepository: PaymentRepository,
    private val feeQuoteService: FeeQuoteService
) {
    @CommandHandler
    @Transactional
    fun handle(command: RecordPaymentCommand): String {
        paymentRepository.findById(command.paymentId).orElse(null)?.let { existing ->
            require(existing.memberId == command.memberId) { "Payment ID is already used by another member" }
            require(existing.amount.compareTo(command.amount) == 0) { "Payment ID is already used with another amount" }
            require(existing.currency == command.currency.trim().uppercase()) {
                "Payment ID is already used with another currency"
            }
            require(existing.paidAt == command.paidAt) { "Payment ID is already used with another paidAt" }
            require(existing.allocations.map { it.feeId } == command.feeIds) {
                "Payment ID is already used for another Fee selection"
            }
            return command.paymentId
        }

        val quote = feeQuoteService.quote(
            paymentId = command.paymentId,
            memberId = command.memberId,
            selectedFeeIds = command.feeIds,
            currency = command.currency,
            quotedAt = command.paidAt
        )
        require(command.amount.compareTo(quote.amount) == 0) {
            "Payment amount ${command.amount} does not equal quoted amount ${quote.amount}"
        }

        val event = PaymentRecordedEvent(
            paymentId = command.paymentId,
            memberId = command.memberId,
            amount = quote.amount,
            currency = quote.currency,
            paidAt = command.paidAt,
            allocations = quote.fees.map {
                PaymentAllocationDetails(it.allocationId, it.feeId, it.amount)
            }
        )
        paymentAggregateRepository.newInstance {
            Payment().also { AggregateLifecycle.apply(event) }
        }
        return command.paymentId
    }
}
