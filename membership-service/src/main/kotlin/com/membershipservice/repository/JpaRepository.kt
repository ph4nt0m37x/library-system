package com.membershipservice.repository

import com.membershipservice.model.aggregate.Member
import com.membershipservice.model.valueObject.Email
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.MembershipNumber
import com.membershipservice.model.valueObject.SubscriptionId
import com.membershipservice.model.valueObject.enums.SubscriptionPaymentStatus
import com.membershipservice.model.entity.SubscriptionPeriod
import com.membershipservice.model.view.MemberView
import com.membershipservice.model.view.SubscriptionPeriodView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
interface MemberRepository : JpaRepository<Member, MemberId> {
    fun existsByMembershipNumber(membershipNumber: MembershipNumber): Boolean
    fun findByEmail(email: Email): Member?
}

@Repository
interface MemberViewRepository : JpaRepository<MemberView, MemberId> {
    fun findByMembershipNumber(membershipNumber: MembershipNumber): MemberView?
}

@Repository
interface SubscriptionPeriodRepository : JpaRepository<SubscriptionPeriod, SubscriptionId> {
    fun existsByPaymentReference(paymentReference: String): Boolean
}

@Repository
interface SubscriptionPeriodViewRepository : JpaRepository<SubscriptionPeriodView, SubscriptionId> {
    fun findByMemberIdOrderByStartsAtAsc(memberId: MemberId): List<SubscriptionPeriodView>
    fun existsByMemberIdAndPaymentStatusAndStartsAtLessThanEqualAndEndsAtAfter(
        memberId: MemberId,
        paymentStatus: SubscriptionPaymentStatus,
        startsAt: ZonedDateTime,
        endsAt: ZonedDateTime
    ): Boolean
}
