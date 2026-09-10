package com.borrowingservice.model.view

import com.borrowingservice.model.valueObject.enums.FeeReason
import com.borrowingservice.model.valueObject.enums.FeeStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(
    name = "fee_view",
    indexes = [Index(name = "idx_fee_view_member", columnList = "member_id")]
)
class FeeView(
    @Id
    @Column(name = "fee_id", nullable = false, updatable = false)
    var feeId: String,

    @Column(name = "loan_id", nullable = false, unique = true, updatable = false)
    var loanId: String,

    @Column(name = "member_id", nullable = false, updatable = false)
    var memberId: String,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var reason: FeeReason,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FeeStatus,

    @Column(name = "created_at", nullable = false)
    var createdAt: ZonedDateTime,

    @Column(name = "due_at")
    var dueAt: ZonedDateTime? = null,

    @Column(name = "returned_at")
    var returnedAt: ZonedDateTime? = null,

    @Column(name = "incident_at")
    var incidentAt: ZonedDateTime? = null,

    @Column(name = "settled_at")
    var settledAt: ZonedDateTime? = null,

    @Column(name = "settled_by_payment_id")
    var settledByPaymentId: String? = null,

    @Column(name = "settlement_allocation_id", unique = true)
    var settlementAllocationId: String? = null,

    @Column(name = "settlement_amount", precision = 19, scale = 2)
    var settlementAmount: BigDecimal? = null
)
