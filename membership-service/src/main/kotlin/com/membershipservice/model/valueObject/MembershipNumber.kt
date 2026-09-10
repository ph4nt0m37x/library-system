package com.membershipservice.model.valueObject

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Embeddable

@Embeddable
open class MembershipNumber(value: String) {

    @get:JsonValue
    val value: String = value.trim().also {
        require(it.isNotBlank()) { "Membership number must not be blank" }
        require(it.length <= 64) { "Membership number must not exceed 64 characters" }
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is MembershipNumber && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value
}
