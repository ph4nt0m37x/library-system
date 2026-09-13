package com.membershipservice.service

import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.MembershipNumber
import com.membershipservice.model.valueObject.dto.MemberResponse
import com.membershipservice.model.valueObject.dto.SubscriptionEligibilityResponse

interface MemberViewReadService {
    fun findById(memberId: MemberId): MemberResponse?
    fun findByMembershipNumber(membershipNumber: MembershipNumber): MemberResponse?
    fun findAll(): List<MemberResponse>
    fun hasActiveSubscription(memberId: MemberId): Boolean
    fun findSubscriptionEligibility(memberId: MemberId): SubscriptionEligibilityResponse?
}
