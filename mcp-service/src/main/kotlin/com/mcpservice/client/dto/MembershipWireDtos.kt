package com.mcpservice.client.dto

import java.math.BigDecimal
import java.time.ZonedDateTime

data class MemberWire(
    val memberId: String,
    val membershipNumber: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val registeredAt: ZonedDateTime,
    val changedAt: ZonedDateTime? = null,
    val active: Boolean,
    val currentSubscription: SubscriptionPeriodWire? = null,
    val subscriptionHistory: List<SubscriptionPeriodWire> = emptyList()
)

data class SubscriptionPeriodWire(
    val subscriptionId: String,
    val previousSubscriptionId: String? = null,
    val tier: String,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime,
    val createdAt: ZonedDateTime,
    val amountPaid: BigDecimal,
    val currency: String,
    val paidAt: ZonedDateTime,
    val paymentReference: String? = null,
    val paymentStatus: String? = null
)
