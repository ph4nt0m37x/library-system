package com.membershipservice.service.impl

import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.MembershipNumber
import com.membershipservice.model.valueObject.dto.MemberResponse
import com.membershipservice.model.valueObject.dto.SubscriptionPeriodResponse
import com.membershipservice.model.view.MemberView
import com.membershipservice.model.view.SubscriptionPeriodView
import com.membershipservice.repository.MemberViewRepository
import com.membershipservice.repository.SubscriptionPeriodViewRepository
import com.membershipservice.service.MemberViewReadService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
@Transactional(readOnly = true)
class MemberViewReadServiceImpl(
    private val memberViewRepository: MemberViewRepository,
    private val subscriptionPeriodViewRepository: SubscriptionPeriodViewRepository
) : MemberViewReadService {
    override fun findById(memberId: MemberId): MemberResponse? =
        memberViewRepository.findById(memberId).orElse(null)?.toResponse()

    override fun findByMembershipNumber(membershipNumber: MembershipNumber): MemberResponse? =
        memberViewRepository.findByMembershipNumber(membershipNumber)?.toResponse()

    override fun findAll(): List<MemberResponse> =
        memberViewRepository.findAll().map { it.toResponse() }

    private fun MemberView.toResponse(): MemberResponse {
        val now = ZonedDateTime.now()
        val history = subscriptionPeriodViewRepository
            .findByMemberIdOrderByStartsAtAsc(memberId)
            .map { it.toResponse() }
        val current = history
            .filter { !it.startsAt.isAfter(now) && it.endsAt.isAfter(now) }
            .maxByOrNull { it.startsAt }

        return MemberResponse(
            memberId = memberId.baseValue(),
            membershipNumber = membershipNumber.value,
            firstName = firstName,
            lastName = lastName,
            email = email.value,
            phoneNumber = phoneNumber.value,
            registeredAt = registeredAt,
            changedAt = changedAt,
            active = current != null,
            currentSubscription = current,
            subscriptionHistory = history
        )
    }

    private fun SubscriptionPeriodView.toResponse() = SubscriptionPeriodResponse(
        subscriptionId = subscriptionId.baseValue(),
        previousSubscriptionId = previousSubscriptionId?.baseValue(),
        tier = tier,
        startsAt = startsAt,
        endsAt = endsAt,
        createdAt = createdAt,
        amountPaid = amountPaid,
        currency = currency,
        paidAt = paidAt
    )
}
