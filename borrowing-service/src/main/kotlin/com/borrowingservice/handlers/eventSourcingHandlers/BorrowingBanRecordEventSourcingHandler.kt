package com.borrowingservice.handlers.eventSourcingHandlers

import com.borrowingservice.model.entity.BanPeriod
import com.borrowingservice.model.event.BorrowingBanIssuedEvent
import com.borrowingservice.model.valueObject.enums.BanTier
import org.axonframework.eventsourcing.EventSourcingHandler

abstract class BorrowingBanRecordEventSourcingHandler {
    abstract var banRecordId: String
        protected set

    abstract var memberId: String
        protected set

    abstract var lastIssuedTier: BanTier
        protected set

    abstract val bans: MutableList<BanPeriod>

    @EventSourcingHandler
    fun on(event: BorrowingBanIssuedEvent) {
        banRecordId = event.banRecordId
        memberId = event.memberId
        lastIssuedTier = event.tier
        bans.add(
            BanPeriod(
                banId = event.banId,
                tier = event.tier,
                startsAt = event.startsAt,
                endsAt = event.endsAt,
                issuedAt = event.issuedAt,
                triggeringFeeId = event.triggeringFeeId,
                reason = event.reason
            )
        )
    }
}
