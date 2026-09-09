package com.membershipservice.handlers.commandHandlers

import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class SubscriptionDateCalculator {
    fun endsAt(startsAt: ZonedDateTime, durationMonths: Int): ZonedDateTime {
        require(durationMonths > 0) { "Tier duration must be positive" }
        return startsAt.plusMonths(durationMonths.toLong())
    }

    fun renewalStartsAt(renewedAt: ZonedDateTime, previousEndsAt: ZonedDateTime): ZonedDateTime =
        if (renewedAt.isBefore(previousEndsAt)) previousEndsAt else renewedAt
}
