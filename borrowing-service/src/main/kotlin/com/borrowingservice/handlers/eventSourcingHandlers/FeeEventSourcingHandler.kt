package com.borrowingservice.handlers.eventSourcingHandlers

import com.borrowingservice.model.event.DamagedBookFeeCreatedEvent
import com.borrowingservice.model.event.FeeSettledEvent
import com.borrowingservice.model.event.LostBookFeeCreatedEvent
import com.borrowingservice.model.event.OverdueFeeCreatedEvent
import com.borrowingservice.model.valueObject.enums.FeeReason
import com.borrowingservice.model.valueObject.enums.FeeStatus
import org.axonframework.eventsourcing.EventSourcingHandler
import java.math.BigDecimal
import java.time.ZonedDateTime

abstract class FeeEventSourcingHandler {
    abstract var feeId: String
        protected set

    abstract var loanId: String
        protected set

    abstract var memberId: String
        protected set

    abstract var currency: String
        protected set

    abstract var reason: FeeReason
        protected set

    abstract var status: FeeStatus
        protected set

    abstract var createdAt: ZonedDateTime
        protected set

    abstract var dueAt: ZonedDateTime?
        protected set

    abstract var returnedAt: ZonedDateTime?
        protected set

    abstract var incidentAt: ZonedDateTime?
        protected set

    abstract var settledAt: ZonedDateTime?
        protected set

    abstract var settledByPaymentId: String?
        protected set

    abstract var settlementAllocationId: String?
        protected set

    abstract var settlementAmount: BigDecimal?
        protected set

    @EventSourcingHandler
    fun on(event: OverdueFeeCreatedEvent) {
        create(event.feeId, event.loanId, event.memberId, event.currency, FeeReason.OVERDUE, event.createdAt)
        dueAt = event.dueAt
        returnedAt = event.returnedAt
    }

    @EventSourcingHandler
    fun on(event: LostBookFeeCreatedEvent) {
        create(event.feeId, event.loanId, event.memberId, event.currency, FeeReason.LOST, event.createdAt)
        incidentAt = event.declaredLostAt
    }

    @EventSourcingHandler
    fun on(event: DamagedBookFeeCreatedEvent) {
        create(event.feeId, event.loanId, event.memberId, event.currency, FeeReason.DAMAGED, event.createdAt)
        incidentAt = event.damageRecordedAt
    }

    @EventSourcingHandler
    fun on(event: FeeSettledEvent) {
        status = FeeStatus.PAID
        settledAt = event.settledAt
        settledByPaymentId = event.paymentId
        settlementAllocationId = event.allocationId
        settlementAmount = event.amount
    }

    private fun create(
        feeId: String,
        loanId: String,
        memberId: String,
        currency: String,
        reason: FeeReason,
        createdAt: ZonedDateTime
    ) {
        this.feeId = feeId
        this.loanId = loanId
        this.memberId = memberId
        this.currency = currency
        this.reason = reason
        this.status = FeeStatus.UNPAID
        this.createdAt = createdAt
    }
}
