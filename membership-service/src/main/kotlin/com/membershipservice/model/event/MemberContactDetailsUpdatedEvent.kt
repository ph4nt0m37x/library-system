package com.membershipservice.model.event

import com.membershipservice.model.valueObject.Email
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.PhoneNumber
import java.time.ZonedDateTime

data class MemberContactDetailsUpdatedEvent(
    override val memberId: MemberId,
    val email: Email,
    val phoneNumber: PhoneNumber,
    val changedAt: ZonedDateTime
) : MemberEvent(memberId)
