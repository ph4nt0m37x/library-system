package com.mcpservice.projection

import java.math.BigDecimal

data class SubscriptionProjection(
    val subscriptionId: String,
    val tier: String,
    val startsAt: String,
    val endsAt: String,
    val amountPaid: BigDecimal,
    val currency: String,
    val paymentStatus: String?
)

data class MemberProjection(
    val memberId: String,
    val membershipNumber: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val registeredAt: String,
    val changedAt: String?,
    val active: Boolean,
    val currentSubscription: SubscriptionProjection?
)

data class BanPeriodProjection(
    val banId: String,
    val tier: String,
    val startsAt: String,
    val endsAt: String?,
    val issuedAt: String,
    val triggeringFeeId: String,
    val reason: String
)

data class BanProjection(
    val banRecordId: String,
    val active: Boolean,
    val permanentlyBanned: Boolean,
    val lastIssuedTier: String,
    val currentBan: BanPeriodProjection?
)

data class MemberAccountProjection(
    val member: MemberProjection,
    val activeLoans: List<LoanProjection>,
    val unpaidFees: List<FeeProjection>,
    val recentPayments: List<PaymentProjection>,
    val currentBan: BanProjection?,
    val paymentLimit: Int,
    val paymentsTruncated: Boolean
)
