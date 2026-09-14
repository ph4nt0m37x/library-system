package com.membershipservice.model.valueObject.dto

import com.membershipservice.model.valueObject.enums.Tier
import java.time.ZonedDateTime

data class StartSubscriptionDTO(
    val tier: Tier,
    val startsAt: ZonedDateTime
)
