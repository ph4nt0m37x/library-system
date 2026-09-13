package com.mcpservice.service

import com.mcpservice.client.dto.BanPeriodWire
import com.mcpservice.client.dto.BorrowingBanWire
import com.mcpservice.client.dto.CatalogBookWire
import com.mcpservice.client.dto.FeeWire
import com.mcpservice.client.dto.LoanWire
import com.mcpservice.client.dto.MemberWire
import com.mcpservice.client.dto.PaymentAllocationWire
import com.mcpservice.client.dto.PaymentWire
import com.mcpservice.client.dto.SubscriptionPeriodWire
import com.mcpservice.projection.BanPeriodProjection
import com.mcpservice.projection.BanProjection
import com.mcpservice.projection.BookProjection
import com.mcpservice.projection.CategoryProjection
import com.mcpservice.projection.FeeProjection
import com.mcpservice.projection.IdentifierNormalizer
import com.mcpservice.projection.LoanProjection
import com.mcpservice.projection.MemberProjection
import com.mcpservice.projection.MoneyProjection
import com.mcpservice.projection.PaymentAllocationProjection
import com.mcpservice.projection.PaymentProjection
import com.mcpservice.projection.SubscriptionProjection

internal fun CatalogBookWire.toProjection(currency: String? = null) = BookProjection(
    bookId = IdentifierNormalizer.fromWire(id),
    isbn = isbn,
    title = title,
    author = author,
    description = description,
    publicationYear = publicationYear,
    price = MoneyProjection(price.amount, currency),
    category = category?.let { CategoryProjection(it.id, it.name) }
)

internal fun LoanWire.toProjection() = LoanProjection(
    loanId = IdentifierNormalizer.base(loanId),
    memberId = IdentifierNormalizer.base(memberId),
    bookId = IdentifierNormalizer.base(bookId),
    libraryId = libraryId?.let(IdentifierNormalizer::base),
    borrowedAt = borrowedAt.toOffsetDateTime().toString(),
    dueAt = dueAt.toOffsetDateTime().toString(),
    extendedAt = extendedAt?.toOffsetDateTime()?.toString(),
    returnedAt = returnedAt?.toOffsetDateTime()?.toString(),
    status = status,
    incidentDeclaredAt = incidentDeclaredAt?.toOffsetDateTime()?.toString()
)

internal fun FeeWire.toProjection() = FeeProjection(
    feeId = IdentifierNormalizer.base(feeId),
    loanId = IdentifierNormalizer.base(loanId),
    memberId = IdentifierNormalizer.base(memberId),
    currency = currency,
    reason = reason,
    status = status,
    createdAt = createdAt.toOffsetDateTime().toString(),
    dueAt = dueAt?.toOffsetDateTime()?.toString(),
    returnedAt = returnedAt?.toOffsetDateTime()?.toString(),
    incidentAt = incidentAt?.toOffsetDateTime()?.toString(),
    settledAt = settledAt?.toOffsetDateTime()?.toString(),
    settledByPaymentId = settledByPaymentId?.let(IdentifierNormalizer::base),
    settlementAllocationId = settlementAllocationId?.let(IdentifierNormalizer::base),
    settlementAmount = settlementAmount
)

internal fun PaymentAllocationWire.toProjection() = PaymentAllocationProjection(
    allocationId = IdentifierNormalizer.base(allocationId),
    feeId = IdentifierNormalizer.base(feeId),
    amount = amount
)

internal fun PaymentWire.toProjection() = PaymentProjection(
    paymentId = IdentifierNormalizer.base(paymentId),
    memberId = IdentifierNormalizer.base(memberId),
    amount = amount,
    currency = currency,
    paidAt = paidAt.toOffsetDateTime().toString(),
    allocations = allocations.map { it.toProjection() }
)

internal fun SubscriptionPeriodWire.toProjection() = SubscriptionProjection(
    subscriptionId = IdentifierNormalizer.base(subscriptionId),
    tier = tier,
    startsAt = startsAt.toOffsetDateTime().toString(),
    endsAt = endsAt.toOffsetDateTime().toString(),
    amountPaid = amountPaid,
    currency = currency,
    paymentStatus = paymentStatus
)

internal fun MemberWire.toProjection() = MemberProjection(
    memberId = IdentifierNormalizer.base(memberId),
    membershipNumber = membershipNumber,
    firstName = firstName,
    lastName = lastName,
    email = email,
    phoneNumber = phoneNumber,
    registeredAt = registeredAt.toOffsetDateTime().toString(),
    changedAt = changedAt?.toOffsetDateTime()?.toString(),
    active = active,
    currentSubscription = currentSubscription?.toProjection()
)

internal fun BanPeriodWire.toProjection() = BanPeriodProjection(
    banId = IdentifierNormalizer.base(banId),
    tier = tier,
    startsAt = startsAt.toOffsetDateTime().toString(),
    endsAt = endsAt?.toOffsetDateTime()?.toString(),
    issuedAt = issuedAt.toOffsetDateTime().toString(),
    triggeringFeeId = IdentifierNormalizer.base(triggeringFeeId),
    reason = reason
)

internal fun BorrowingBanWire.toProjection() = BanProjection(
    banRecordId = IdentifierNormalizer.base(banRecordId),
    active = active,
    permanentlyBanned = permanentlyBanned,
    lastIssuedTier = lastIssuedTier,
    currentBan = currentBan?.toProjection()
)
