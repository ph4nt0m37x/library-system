package com.membershipservice.model.valueObject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PhoneNumberTests {
    @Test
    fun `accepts nine digits without spaces and formats them`() {
        assertEquals("071 123 456", PhoneNumber("071123456").value)
    }

    @Test
    fun `removes whitespace before validating and formatting`() {
        assertEquals("071 123 456", PhoneNumber("0 7 1  12 3 4 5 6").value)
    }

    @Test
    fun `rejects a number that does not start with 07`() {
        assertThrows(IllegalArgumentException::class.java) { PhoneNumber("061123456") }
    }

    @Test
    fun `rejects a number that does not contain exactly nine digits`() {
        assertThrows(IllegalArgumentException::class.java) { PhoneNumber("07112345") }
    }
}
