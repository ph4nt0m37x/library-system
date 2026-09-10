package com.borrowingservice.model.aggregate

import com.borrowingservice.handlers.eventSourcingHandlers.PaymentEventSourcingHandler
import com.borrowingservice.model.entity.PaymentAllocation
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateMember
import org.axonframework.spring.stereotype.Aggregate
import java.math.BigDecimal
import java.time.ZonedDateTime

@Aggregate(repository = "axonPaymentRepository")
@Entity
@Table(name = "payments")
class Payment : PaymentEventSourcingHandler() {
    @AggregateIdentifier
    @Id
    @Column(name = "payment_id", nullable = false, updatable = false)
    override lateinit var paymentId: String
        protected set

    @Column(name = "member_id", nullable = false, updatable = false)
    override lateinit var memberId: String
        protected set

    @Column(nullable = false, precision = 19, scale = 2)
    override lateinit var amount: BigDecimal
        protected set

    @Column(nullable = false, length = 3)
    override lateinit var currency: String
        protected set

    @Column(name = "paid_at", nullable = false)
    override lateinit var paidAt: ZonedDateTime
        protected set

    @AggregateMember
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "payment_id",
        referencedColumnName = "payment_id",
        insertable = false,
        updatable = false
    )
    override val allocations: MutableList<PaymentAllocation> = mutableListOf()
}
