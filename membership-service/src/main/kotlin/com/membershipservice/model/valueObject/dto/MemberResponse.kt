package com.membershipservice.model.valueObject.dto

import com.membershipservice.model.valueObject.enums.Tier
import java.math.BigDecimal
import java.time.ZonedDateTime

data class MemberResponse(
    val memberId: String,
    val membershipNumber: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val registeredAt: ZonedDateTime,
    val changedAt: ZonedDateTime?,
    val active: Boolean,
    val currentSubscription: SubscriptionPeriodResponse?,
    val subscriptionHistory: List<SubscriptionPeriodResponse>
)

data class SubscriptionPeriodResponse(
    val subscriptionId: String,
    val previousSubscriptionId: String?,
    val tier: Tier,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime,
    val createdAt: ZonedDateTime,
    val amountPaid: BigDecimal,
    val currency: String,
    val paidAt: ZonedDateTime
)
