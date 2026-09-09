package com.membershipservice.model.command

import com.membershipservice.model.valueObject.Email
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.PhoneNumber
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class UpdateMemberContactDetailsCommand(
    @TargetAggregateIdentifier
    val memberId: MemberId,
    val email: Email,
    val phoneNumber: PhoneNumber
)
