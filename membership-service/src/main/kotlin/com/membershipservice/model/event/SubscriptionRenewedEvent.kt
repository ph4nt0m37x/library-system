package com.membershipservice.model.event

import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.SubscriptionId
import com.membershipservice.model.valueObject.enums.Tier
import com.membershipservice.model.valueObject.enums.SubscriptionPaymentStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

data class SubscriptionRenewedEvent(
    val subscriptionId: SubscriptionId,
    override val memberId: MemberId,
    val previousSubscriptionId: SubscriptionId,
    val tier: Tier,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime,
    val createdAt: ZonedDateTime,
    val amountPaid: BigDecimal,
    val currency: String,
    val paidAt: ZonedDateTime,
    val paymentReference: String? = null,
    val paymentStatus: SubscriptionPaymentStatus? = null
) : MemberEvent(memberId)
