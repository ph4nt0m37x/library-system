package com.borrowingservice.model.valueObject.dto

import com.borrowingservice.model.valueObject.enums.BanReason
import com.borrowingservice.model.valueObject.enums.BanTier
import java.time.ZonedDateTime

data class BanPeriodResponse(
    val banId: String,
    val tier: BanTier,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime?,
    val issuedAt: ZonedDateTime,
    val triggeringFeeId: String,
    val reason: BanReason
)

data class BorrowingBanResponse(
    val banRecordId: String,
    val memberId: String,
    val lastIssuedTier: BanTier,
    val active: Boolean,
    val permanentlyBanned: Boolean,
    val currentBan: BanPeriodResponse?,
    val history: List<BanPeriodResponse>
)
