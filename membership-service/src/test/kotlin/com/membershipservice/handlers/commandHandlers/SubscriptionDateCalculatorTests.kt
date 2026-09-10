package com.membershipservice.handlers.commandHandlers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class SubscriptionDateCalculatorTests {
    private val calculator = SubscriptionDateCalculator()
    private val purchasedAt = ZonedDateTime.parse("2026-01-15T10:00:00+01:00")

    @Test
    fun `calculates all configured tier durations`() {
        assertEquals(purchasedAt.plusMonths(3), calculator.endsAt(purchasedAt, 3))
        assertEquals(purchasedAt.plusMonths(6), calculator.endsAt(purchasedAt, 6))
        assertEquals(purchasedAt.plusMonths(12), calculator.endsAt(purchasedAt, 12))
    }

    @Test
    fun `early renewal begins when the previous period ends`() {
        val previousEndsAt = purchasedAt.plusMonths(3)
        assertEquals(previousEndsAt, calculator.renewalStartsAt(purchasedAt.plusMonths(2), previousEndsAt))
    }

    @Test
    fun `late renewal begins on the renewal date`() {
        val previousEndsAt = purchasedAt.plusMonths(3)
        val renewedAt = purchasedAt.plusMonths(4)
        assertEquals(renewedAt, calculator.renewalStartsAt(renewedAt, previousEndsAt))
    }

    @Test
    fun `renewal at exact expiry begins at expiry`() {
        val previousEndsAt = purchasedAt.plusMonths(3)
        assertEquals(previousEndsAt, calculator.renewalStartsAt(previousEndsAt, previousEndsAt))
    }
}
