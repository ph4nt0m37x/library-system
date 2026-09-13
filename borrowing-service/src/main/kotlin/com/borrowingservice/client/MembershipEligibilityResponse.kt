package com.borrowingservice.client

import java.time.ZonedDateTime

data class MembershipEligibilityResponse(
    val memberId: String,
    val exists: Boolean,
    val active: Boolean,
    val currentPeriodEndsAt: ZonedDateTime?
)
