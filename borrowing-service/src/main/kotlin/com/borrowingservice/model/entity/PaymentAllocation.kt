package com.borrowingservice.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.axonframework.modelling.command.EntityId
import java.math.BigDecimal

@Entity
@Table(name = "payment_allocations")
class PaymentAllocation(
    @EntityId
    @Id
    @Column(name = "allocation_id", nullable = false, updatable = false)
    var allocationId: String,

    @Column(name = "payment_id", nullable = false, updatable = false)
    var paymentId: String,

    @Column(name = "fee_id", nullable = false, unique = true, updatable = false)
    var feeId: String,

    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal
)
