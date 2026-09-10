package com.membershipservice.model.event

import com.membershipservice.model.valueObject.Email
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.MembershipNumber
import com.membershipservice.model.valueObject.PhoneNumber
import java.time.ZonedDateTime

data class MemberRegisteredEvent(
    override val memberId: MemberId,
    val membershipNumber: MembershipNumber,
    val firstName: String,
    val lastName: String,
    val email: Email,
    val phoneNumber: PhoneNumber,
    val registeredAt: ZonedDateTime
) : MemberEvent(memberId)
