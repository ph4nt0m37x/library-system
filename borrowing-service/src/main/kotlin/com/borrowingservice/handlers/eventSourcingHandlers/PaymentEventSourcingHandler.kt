package com.borrowingservice.handlers.eventSourcingHandlers

import com.borrowingservice.model.entity.PaymentAllocation
import com.borrowingservice.model.event.PaymentRecordedEvent
import org.axonframework.eventsourcing.EventSourcingHandler
import java.math.BigDecimal
import java.time.ZonedDateTime

abstract class PaymentEventSourcingHandler {
    abstract var paymentId: String
        protected set

    abstract var memberId: String
        protected set

    abstract var amount: BigDecimal
        protected set

    abstract var currency: String
        protected set

    abstract var paidAt: ZonedDateTime
        protected set

    abstract val allocations: MutableList<PaymentAllocation>

    @EventSourcingHandler
    fun on(event: PaymentRecordedEvent) {
        paymentId = event.paymentId
        memberId = event.memberId
        amount = event.amount
        currency = event.currency
        paidAt = event.paidAt
        allocations.addAll(
            event.allocations.map { allocation ->
                PaymentAllocation(
                    allocationId = allocation.allocationId,
                    paymentId = event.paymentId,
                    feeId = allocation.feeId,
                    amount = allocation.amount
                )
            }
        )
    }
}
