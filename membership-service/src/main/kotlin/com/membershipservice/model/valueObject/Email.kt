package com.membershipservice.model.valueObject

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Embeddable

@Embeddable
open class Email(value: String) {

    @get:JsonValue
    val value: String = value.trim().lowercase().also {
        require(it.length <= 254) { "Email must not exceed 254 characters" }
        require(EMAIL_PATTERN.matches(it)) { "Invalid email address" }
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Email && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
