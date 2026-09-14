package com.membershipservice.model.valueObject.dto

import com.membershipservice.model.valueObject.enums.Tier

data class RenewSubscriptionDTO(
    val tier: Tier
)
