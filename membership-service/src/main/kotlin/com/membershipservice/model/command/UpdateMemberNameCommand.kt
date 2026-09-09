package com.membershipservice.model.command

import com.membershipservice.model.valueObject.MemberId
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class UpdateMemberNameCommand(
    @TargetAggregateIdentifier
    val memberId: MemberId,
    val firstName: String,
    val lastName: String
)
