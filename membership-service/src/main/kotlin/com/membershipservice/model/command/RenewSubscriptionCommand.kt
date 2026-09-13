package com.membershipservice.model.command

import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.enums.Tier
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.math.BigDecimal
import java.time.ZonedDateTime

data class RenewSubscriptionCommand(
    @TargetAggregateIdentifier
    val memberId: MemberId,
    val tier: Tier,
    val amountPaid: BigDecimal,
    val currency: String,
    val paidAt: ZonedDateTime,
    val paymentReference: String
)
