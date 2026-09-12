package com.membershipservice.model.valueObject.dto

import com.membershipservice.model.valueObject.enums.Tier
import java.math.BigDecimal
import java.time.ZonedDateTime

data class StartSubscriptionDTO(
    val tier: Tier,
    val startsAt: ZonedDateTime,
    val amountPaid: BigDecimal,
    val currency: String,
    val paidAt: ZonedDateTime,
    val paymentReference: String
)
