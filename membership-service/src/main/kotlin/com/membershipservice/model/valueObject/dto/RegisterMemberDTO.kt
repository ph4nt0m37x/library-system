package com.membershipservice.model.valueObject.dto

data class RegisterMemberDTO(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String
)
