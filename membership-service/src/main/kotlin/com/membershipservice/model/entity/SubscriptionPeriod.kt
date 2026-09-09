package com.membershipservice.model.entity

import com.membershipservice.model.valueObject.enums.Tier
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.SubscriptionId
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.axonframework.modelling.command.EntityId
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(name = "subscription_periods")
class SubscriptionPeriod(

    @EntityId
    @EmbeddedId
    @AttributeOverride(
        name = "value",
        column = Column(name = "subscription_id", nullable = false, updatable = false)
    )
    var subscriptionId: SubscriptionId,

    @Embedded
    @AttributeOverride(
        name = "value",
        column = Column(name = "member_id", nullable = false, updatable = false)
    )
    var memberId: MemberId,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var tier: Tier,

    @Column(name = "starts_at", nullable = false)
    var startsAt: ZonedDateTime,

    @Column(name = "ends_at", nullable = false)
    var endsAt: ZonedDateTime,

    @Column(name = "created_at", nullable = false)
    var createdAt: ZonedDateTime,

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 2)
    var amountPaid: BigDecimal,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Column(name = "paid_at", nullable = false)
    var paidAt: ZonedDateTime
)
