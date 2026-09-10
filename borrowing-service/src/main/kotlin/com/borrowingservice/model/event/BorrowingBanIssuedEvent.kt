package com.borrowingservice.model.event

import com.borrowingservice.model.valueObject.enums.BanReason
import com.borrowingservice.model.valueObject.enums.BanTier
import java.time.ZonedDateTime

data class BorrowingBanIssuedEvent(
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
