package com.membershipservice.model.valueObject.dto

import java.time.ZonedDateTime

data class SubscriptionEligibilityResponse(
    val memberId: String,
    val exists: Boolean,
    val active: Boolean,
    val currentPeriodEndsAt: ZonedDateTime?
)
