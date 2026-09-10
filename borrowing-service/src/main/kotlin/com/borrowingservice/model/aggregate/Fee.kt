package com.borrowingservice.model.aggregate

import com.borrowingservice.handlers.eventSourcingHandlers.FeeEventSourcingHandler
import com.borrowingservice.model.valueObject.enums.FeeReason
import com.borrowingservice.model.valueObject.enums.FeeStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.spring.stereotype.Aggregate
import java.math.BigDecimal
import java.time.ZonedDateTime

@Aggregate(repository = "axonFeeRepository")
@Entity
@Table(name = "fees")
class Fee : FeeEventSourcingHandler() {
    @AggregateIdentifier
    @Id
    @Column(name = "fee_id", nullable = false, updatable = false)
    override lateinit var feeId: String
        protected set

    @Column(name = "loan_id", nullable = false, unique = true, updatable = false)
    override lateinit var loanId: String
        protected set

    @Column(name = "member_id", nullable = false, updatable = false)
    override lateinit var memberId: String
        protected set

    @Column(nullable = false, length = 3)
    override lateinit var currency: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    override lateinit var reason: FeeReason
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    override lateinit var status: FeeStatus
        protected set

    @Column(name = "created_at", nullable = false)
    override lateinit var createdAt: ZonedDateTime
        protected set

    @Column(name = "due_at")
    override var dueAt: ZonedDateTime? = null
        protected set

    @Column(name = "returned_at")
    override var returnedAt: ZonedDateTime? = null
        protected set

    @Column(name = "incident_at")
    override var incidentAt: ZonedDateTime? = null
        protected set

    @Column(name = "settled_at")
    override var settledAt: ZonedDateTime? = null
        protected set

    @Column(name = "settled_by_payment_id")
    override var settledByPaymentId: String? = null
        protected set

    @Column(name = "settlement_allocation_id", unique = true)
    override var settlementAllocationId: String? = null
        protected set

    @Column(name = "settlement_amount", precision = 19, scale = 2)
    override var settlementAmount: BigDecimal? = null
        protected set
}
