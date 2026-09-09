package com.membershipservice.model.valueObject

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Embeddable

@Embeddable
open class PhoneNumber(value: String) {

    @get:JsonValue
    val value: String = normalize(value)

    override fun equals(other: Any?): Boolean =
        this === other || (other is PhoneNumber && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    private companion object {
        fun normalize(input: String): String {
            val digits = input.filterNot(Char::isWhitespace)
            require(digits.length == 9 && digits.all(Char::isDigit) && digits.startsWith("07")) {
                "Phone number must contain exactly 9 digits and start with 07"
            }
            return "${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6, 9)}"
        }
    }
}
