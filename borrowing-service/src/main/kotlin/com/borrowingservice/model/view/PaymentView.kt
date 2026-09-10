package com.borrowingservice.model.view

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(
    name = "payment_view",
    indexes = [Index(name = "idx_payment_view_member", columnList = "member_id")]
)
class PaymentView(
    @Id
    @Column(name = "payment_id", nullable = false, updatable = false)
    var paymentId: String,

    @Column(name = "member_id", nullable = false, updatable = false)
    var memberId: String,

    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Column(name = "paid_at", nullable = false)
    var paidAt: ZonedDateTime,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "payment_id",
        referencedColumnName = "payment_id",
        insertable = false,
        updatable = false
    )
    var allocations: MutableList<PaymentAllocationView> = mutableListOf()
)
