package com.membershipservice.model.command

import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.enums.Tier
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class StartSubscriptionCommand(
    @TargetAggregateIdentifier
    val memberId: MemberId,
    val tier: Tier,
    val startsAt: ZonedDateTime
)
