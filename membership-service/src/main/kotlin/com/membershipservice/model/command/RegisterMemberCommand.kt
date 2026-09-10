package com.membershipservice.model.command

import com.membershipservice.model.valueObject.Email
import com.membershipservice.model.valueObject.PhoneNumber

data class RegisterMemberCommand(
    val firstName: String,
    val lastName: String,
    val email: Email,
    val phoneNumber: PhoneNumber
)
