package com.membershipservice.model.valueObject.dto

import com.membershipservice.model.valueObject.enums.Tier
import java.math.BigDecimal
import java.time.ZonedDateTime

data class RenewSubscriptionDTO(
    val tier: Tier,
    val amountPaid: BigDecimal,
    val currency: String,
    val paidAt: ZonedDateTime
)
