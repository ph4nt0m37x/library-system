package com.membershipservice.model.event

import com.membershipservice.model.valueObject.MemberId

abstract class MemberEvent(
    open val memberId: MemberId
) : AbstractEvent(memberId)
