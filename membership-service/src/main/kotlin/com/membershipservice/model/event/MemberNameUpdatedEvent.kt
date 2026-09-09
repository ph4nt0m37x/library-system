package com.membershipservice.model.event

import com.membershipservice.model.valueObject.MemberId
import java.time.ZonedDateTime

data class MemberNameUpdatedEvent(
    override val memberId: MemberId,
    val firstName: String,
    val lastName: String,
    val changedAt: ZonedDateTime
) : MemberEvent(memberId)
