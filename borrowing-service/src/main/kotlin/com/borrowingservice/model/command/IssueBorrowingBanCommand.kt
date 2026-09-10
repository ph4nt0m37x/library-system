package com.borrowingservice.model.command

import com.borrowingservice.model.valueObject.enums.BanReason
import com.borrowingservice.model.valueObject.enums.BanTier
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class IssueBorrowingBanCommand(
    @TargetAggregateIdentifier
    val banRecordId: String,
    val banId: String,
    val memberId: String,
    val tier: BanTier,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime?,
    val issuedAt: ZonedDateTime,
    val triggeringFeeId: String,
    val reason: BanReason
)
